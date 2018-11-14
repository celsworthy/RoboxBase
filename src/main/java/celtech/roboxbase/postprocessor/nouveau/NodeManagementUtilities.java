package celtech.roboxbase.postprocessor.nouveau;

import celtech.roboxbase.postprocessor.NozzleProxy;
import celtech.roboxbase.postprocessor.nouveau.nodes.ExtrusionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.NodeProcessingException;
import celtech.roboxbase.postprocessor.nouveau.nodes.ObjectDelineationNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.OrphanObjectDelineationNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.RetractNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ToolSelectNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.UnretractNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.nodeFunctions.IteratorWithOrigin;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.ExtrusionProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.MovementProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.NozzlePositionProvider;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class NodeManagementUtilities
{

    private final PostProcessorFeatureSet featureSet;
    private final List<NozzleProxy> nozzleProxies;

    public NodeManagementUtilities(PostProcessorFeatureSet featureSet,
            List<NozzleProxy> nozzleProxies)
    {
        this.featureSet = featureSet;
        this.nozzleProxies = nozzleProxies;
    }

    protected void rehabilitateUnretractNodes(LayerNode layerNode)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);
        List<UnretractNode> nodesToDelete = new ArrayList<>();
        NozzleProxy nozzleInUse = nozzleProxies.get(0);

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();

            if (node instanceof ToolSelectNode)
            {
                nozzleInUse = nozzleProxies.get(((ToolSelectNode) node).getToolNumber());
            } else if (node instanceof UnretractNode)
            {
                if (featureSet.isEnabled(PostProcessorFeature.REMOVE_ALL_UNRETRACTS))
                {
                    nodesToDelete.add((UnretractNode) node);
                } else
                {
                    //We need to put the appropriate value in for the unretract
                    ((UnretractNode) node).getExtrusion().setE(nozzleInUse.getNozzleParameters().getEjectionVolume());
                }
            }
        }

        for (UnretractNode unretractNode : nodesToDelete)
        {
            unretractNode.removeFromParent();
        }
    }

    protected Optional<SectionNode> lookForParentSectionNode(GCodeEventNode eventNode)
    {
        Optional<SectionNode> sectionNode = Optional.empty();

        GCodeEventNode nodeUnderConsideration = eventNode;

        while (nodeUnderConsideration.hasParent())
        {
            GCodeEventNode parent = nodeUnderConsideration.getParent().get();
            if (parent instanceof SectionNode)
            {
                sectionNode = Optional.of((SectionNode) parent);
                break;
            }

            nodeUnderConsideration = parent;
        }

        return sectionNode;
    }

    protected Optional<ObjectDelineationNode> lookForParentObjectNode(GCodeEventNode eventNode)
    {
        Optional<ObjectDelineationNode> objectNode = Optional.empty();

        GCodeEventNode nodeUnderConsideration = eventNode;

        while (nodeUnderConsideration.hasParent())
        {
            GCodeEventNode parent = nodeUnderConsideration.getParent().get();
            if (parent instanceof ObjectDelineationNode)
            {
                objectNode = Optional.of((ObjectDelineationNode) parent);
                break;
            }

            nodeUnderConsideration = parent;
        }

        return objectNode;
    }

    protected void recalculateSectionExtrusion(LayerNode layerNode)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();
            if (node instanceof SectionNode)
            {
                ((SectionNode) node).recalculateExtrusion();
            }
        }
    }

    protected void calculatePerRetractExtrusionAndNode(LayerNode layerNode)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

        ExtrusionNode lastExtrusionNode = null;
        double extrusionInRetract = 0;

        List<SectionNode> sectionNodes = new ArrayList<>();
        SectionNode lastSectionNode = null;

        NozzleProxy nozzleInUse = nozzleProxies.get(0);

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();
            if (node instanceof ToolSelectNode)
            {
                extrusionInRetract = 0;
                sectionNodes.clear();
                lastSectionNode = null;
                lastExtrusionNode = null;

                nozzleInUse = nozzleProxies.get(((ToolSelectNode) node).getToolNumber());
            } else if (node instanceof ExtrusionNode)
            {
                Optional<SectionNode> parentSection = lookForParentSectionNode(node);
                if (parentSection.isPresent())
                {
                    if (lastSectionNode != parentSection.get())
                    {
                        sectionNodes.add(parentSection.get());
                        lastSectionNode = parentSection.get();
                    }
                }

                ExtrusionNode extrusionNode = (ExtrusionNode) node;
                extrusionInRetract += extrusionNode.getExtrusion().getE();
                extrusionInRetract += extrusionNode.getExtrusion().getD();
                lastExtrusionNode = extrusionNode;
            } else if (node instanceof RetractNode)
            {
                RetractNode retractNode = (RetractNode) node;
                retractNode.setExtrusionSinceLastRetract(extrusionInRetract);
                retractNode.setSectionsToConsider(sectionNodes);

                if (!featureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
                {
                    //We need to put the appropriate value in for the retract
                    retractNode.getExtrusion().setE(-nozzleInUse.getNozzleParameters().getEjectionVolume());
                }

                List<SectionNode> newSectionNodes = new ArrayList<>();
                newSectionNodes.addAll(sectionNodes);
                sectionNodes = newSectionNodes;
                extrusionInRetract = 0;

                if (lastExtrusionNode != null)
                {
                    retractNode.setPriorExtrusionNode(lastExtrusionNode);
                    lastExtrusionNode = null;
                }
            }
        }
    }

    protected void rehomeOrphanObjects(LayerNode layerNode, final LayerPostProcessResult lastLayerParseResult)
    {
        // Orphans occur when there is no Tn directive in a layer
        //
        // At the start of the file we should treat this as object 0
        // Subsequently we should look at the last layer to see which object was in force and create an object with the same reference

        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

        List<OrphanObjectDelineationNode> orphans = new ArrayList<>();

        while (layerIterator.hasNext())
        {
            GCodeEventNode node = layerIterator.next();
            if (node instanceof OrphanObjectDelineationNode)
            {
                orphans.add((OrphanObjectDelineationNode) node);
            }
        }

        for (OrphanObjectDelineationNode orphanNode : orphans)
        {
            ObjectDelineationNode newObjectNode = new ObjectDelineationNode();

            int potentialObjectNumber = orphanNode.getPotentialObjectNumber();

            if (potentialObjectNumber
                    < 0)
            {
                if (lastLayerParseResult.getLastObjectNumber().isPresent())
                {
                    potentialObjectNumber = lastLayerParseResult.getLastObjectNumber().get();
                } else
                {
                    throw new RuntimeException("Cannot determine object number for orphan on layer " + layerNode.getLayerNumber());
                }

                if (potentialObjectNumber < 0)
                {
                    //Still not set!
                    //Set it to 0
                    potentialObjectNumber = 0;
                }
            }

            newObjectNode.setObjectNumber(potentialObjectNumber);

            //Transfer the children from the orphan to the new node
            Iterator<GCodeEventNode> childIterator = orphanNode.childIterator();
            List<GCodeEventNode> nodesToRemove = new ArrayList<>();

            while (childIterator.hasNext())
            {
                GCodeEventNode childNode = childIterator.next();
                nodesToRemove.add(childNode);
            }

            for (GCodeEventNode nodeToRemove : nodesToRemove)
            {
                nodeToRemove.removeFromParent();
                newObjectNode.addChildAtEnd(nodeToRemove);
            }

            //Add the new node
            orphanNode.addSiblingBefore(newObjectNode);

            //Remove the orphan
            orphanNode.removeFromParent();
        }
    }

    protected Optional<ExtrusionNode> findNextExtrusion(GCodeEventNode topLevelNode, GCodeEventNode node) throws NodeProcessingException
    {
        Optional<ExtrusionNode> nextExtrusion = Optional.empty();

        LinkedList<GCodeEventNode> nodeHierarchy = new LinkedList<>();

        boolean foundTopLevel = false;

        GCodeEventNode currentNode = node;

        nodeHierarchy.add(node);

        while (currentNode.getParent().isPresent() && !foundTopLevel)
        {
            GCodeEventNode parent = currentNode.getParent().get();
            if (parent == topLevelNode)
            {
                foundTopLevel = true;
            } else
            {
                nodeHierarchy.addFirst(parent);
            }

            currentNode = parent;
        }

        Iterator<GCodeEventNode> childIterator = topLevelNode.treeSpanningIterator(nodeHierarchy);

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof ExtrusionNode)
            {
                nextExtrusion = Optional.of((ExtrusionNode) childNode);
                break;
            }
        }

        return nextExtrusion;
    }

    protected Optional<MovementProvider> findNextMovement(GCodeEventNode topLevelNode, GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> nextMovement = Optional.empty();

        LinkedList<GCodeEventNode> nodeHierarchy = new LinkedList<>();

        boolean foundTopLevel = false;

        GCodeEventNode currentNode = node;

        nodeHierarchy.add(node);

        while (currentNode.getParent().isPresent() && !foundTopLevel)
        {
            GCodeEventNode parent = currentNode.getParent().get();
            if (parent == topLevelNode)
            {
                foundTopLevel = true;
            } else
            {
                nodeHierarchy.addFirst(parent);
            }

            currentNode = parent;
        }

        Iterator<GCodeEventNode> childIterator = topLevelNode.treeSpanningIterator(nodeHierarchy);

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                nextMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return nextMovement;
    }

    protected Optional<MovementProvider> findNextMovement(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> nextMovement = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningIterator(null);

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                nextMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return nextMovement;
    }

    protected Optional<SearchSegment> findPriorMovementPoints(GCodeEventNode seed)
    {
        Optional<SearchSegment> priorMovementPoints = Optional.empty();

        MovementProvider startNode = null;
        MovementProvider endNode = null;

        if (seed instanceof MovementProvider)
        {
            if (endNode == null)
            {
                endNode = ((MovementProvider) seed);
            }
        }

        IteratorWithOrigin<GCodeEventNode> treeBackwards = seed.treeSpanningBackwardsIterator();

        while (treeBackwards.hasNext())
        {
            GCodeEventNode node = treeBackwards.next();
            if (node instanceof MovementProvider)
            {
                if (endNode == null)
                {
                    endNode = ((MovementProvider) node);
                } else if (startNode == null)
                {
                    startNode = ((MovementProvider) node);
                    break;
                }
            }
        }

        if (startNode != null
                && endNode != null)
        {
            SearchSegment searchSegment = new SearchSegment(startNode, endNode);
            priorMovementPoints = Optional.of(searchSegment);
        }

        return priorMovementPoints;
    }

    protected Optional<MovementProvider> findPriorMovement(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> priorMovement = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                priorMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return priorMovement;
    }

    protected Optional<ExtrusionNode> findPriorExtrusion(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<ExtrusionNode> priorExtrusion = Optional.empty();

        Iterator<GCodeEventNode> childIterator = node.treeSpanningBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof ExtrusionNode)
            {
                priorExtrusion = Optional.of((ExtrusionNode) childNode);
                break;
            }
        }

        return priorExtrusion;
    }

    protected Optional<MovementProvider> findPriorMovementInPreviousSection(GCodeEventNode node) throws NodeProcessingException
    {
        Optional<MovementProvider> priorMovement = Optional.empty();

        GCodeEventNode nodeParent = node.getParent().get();

        if (nodeParent == null
                || !(nodeParent instanceof SectionNode))
        {
            throw new NodeProcessingException("Parent of is not present or not a section", node);
        }

        Optional<GCodeEventNode> previousSection = nodeParent.getSiblingBefore();

        if (!previousSection.isPresent())
        {
            throw new NodeProcessingException("Couldn't find a prior section", node);

        }

        Iterator<GCodeEventNode> childIterator = previousSection.get().childrenAndMeBackwardsIterator();

        while (childIterator.hasNext())
        {
            GCodeEventNode childNode = childIterator.next();
            if (childNode instanceof MovementProvider)
            {
                priorMovement = Optional.of((MovementProvider) childNode);
                break;
            }
        }

        return priorMovement;
    }

    public class AvailableExtrusion
    {

        private final double availableExtrusion;
        private final GCodeEventNode lastNodeExamined;

        public AvailableExtrusion(double availableExtrusion, GCodeEventNode lastNodeExamined)
        {
            this.availableExtrusion = availableExtrusion;
            this.lastNodeExamined = lastNodeExamined;
        }

        public double getAvailableExtrusion()
        {
            return availableExtrusion;
        }

        public GCodeEventNode getLastNodeExamined()
        {
            return lastNodeExamined;
        }
    }

    public AvailableExtrusion findAvailableExtrusion(final InScopeEvents inScopeEvents,
            int startNodeIndex,
            boolean forwards) throws NodeProcessingException
    {
        double availableExtrusion = 0;
        GCodeEventNode lastNodeExamined = null;

        int delta = (forwards) ? -1 : 1;

        for (int inScopeCounter = startNodeIndex; inScopeCounter >= 0 && inScopeCounter < inScopeEvents.getInScopeEvents().size(); inScopeCounter += delta)
        {
            GCodeEventNode node = inScopeEvents.getInScopeEvents().get(inScopeCounter);

            if (node instanceof NozzlePositionProvider)
            {
                NozzlePositionProvider provider = (NozzlePositionProvider) node;
                if (provider.getNozzlePosition().isBSet())
                {
                    break;
                }
            }

            if (node instanceof ExtrusionProvider)
            {
                lastNodeExamined = node;
                availableExtrusion += ((ExtrusionProvider) node).getExtrusion().getE();
                availableExtrusion += ((ExtrusionProvider) node).getExtrusion().getD();
            }
        }
        return new AvailableExtrusion(availableExtrusion, lastNodeExamined);
    }
}
