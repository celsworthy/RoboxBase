package celtech.roboxbase.postprocessor.nouveau;

import celtech.roboxbase.configuration.HeadContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.postprocessor.CannotCloseFromPerimeterException;
import celtech.roboxbase.postprocessor.GCodeOutputWriter;
import celtech.roboxbase.postprocessor.NoPerimeterToCloseOverException;
import celtech.roboxbase.postprocessor.NotEnoughAvailableExtrusionException;
import celtech.roboxbase.postprocessor.NozzleProxy;
import celtech.roboxbase.postprocessor.PostProcessingError;
import celtech.roboxbase.postprocessor.nouveau.nodes.ExtrusionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerChangeDirectiveNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.MCodeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.MergeableWithToolchange;
import celtech.roboxbase.postprocessor.nouveau.nodes.NodeProcessingException;
import celtech.roboxbase.postprocessor.nouveau.nodes.NozzleValvePositionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ToolSelectNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.nodeFunctions.DurationCalculationException;
import celtech.roboxbase.postprocessor.nouveau.nodes.nodeFunctions.IteratorWithStartPoint;
import celtech.roboxbase.postprocessor.nouveau.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Movement;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.MovementProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.NozzlePositionProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Renderable;
import celtech.roboxbase.services.CameraTriggerData;
import celtech.roboxbase.services.CameraTriggerManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class UtilityMethods
{

    private final Stenographer steno = StenographerFactory.getStenographer(UtilityMethods.class.getName());
    private final PostProcessorFeatureSet ppFeatureSet;
    private final CloseLogic closeLogic;
    private final SlicerParametersFile settings;
    private final CameraTriggerManager cameraTriggerManager;

    public UtilityMethods(final PostProcessorFeatureSet ppFeatureSet,
            SlicerParametersFile settings,
            String headType,
            CameraTriggerData cameraTriggerData)
    {
        this.ppFeatureSet = ppFeatureSet;
        this.settings = settings;
        this.closeLogic = new CloseLogic(settings, ppFeatureSet, headType);
        this.cameraTriggerManager = new CameraTriggerManager(null);
        cameraTriggerManager.setTriggerData(cameraTriggerData);
    }

    protected void insertCameraTriggersAndCloses(LayerNode layerNode,
            LayerPostProcessResult lastLayerPostProcessResult,
            List<NozzleProxy> nozzleProxies)
    {
        if (ppFeatureSet.isEnabled(PostProcessorFeature.INSERT_CAMERA_CONTROL_POINTS))
        {
            IteratorWithStartPoint<GCodeEventNode> layerForwards = layerNode.treeSpanningIterator(null);
            while (layerForwards.hasNext())
            {
                GCodeEventNode layerForwardsEvent = layerForwards.next();

                if (layerForwardsEvent instanceof LayerChangeDirectiveNode)
                {
                    cameraTriggerManager.appendLayerEndTriggerCode((LayerChangeDirectiveNode) layerForwardsEvent);
                    break;
                }
            }

            Iterator<GCodeEventNode> layerBackwards = layerNode.childBackwardsIterator();

            while (layerBackwards.hasNext())
            {
                GCodeEventNode layerChild = layerBackwards.next();
                if (layerChild instanceof ToolSelectNode)
                {
                    closeAtEndOfToolSelectIfNecessary((ToolSelectNode) layerChild, nozzleProxies);
                    break;
                }
            }
        }
    }

    protected void suppressUnnecessaryToolChangesAndInsertToolchangeCloses(LayerNode layerNode,
            LayerPostProcessResult lastLayerPostProcessResult,
            List<NozzleProxy> nozzleProxies)
    {
        ToolSelectNode lastToolSelectNode = null;

        if (lastLayerPostProcessResult.getLastToolSelectInForce() != null)
        {
            lastToolSelectNode = lastLayerPostProcessResult.getLastToolSelectInForce();
        }

        // We know that tool selects come directly under a layer node...        
        Iterator<GCodeEventNode> layerIterator = layerNode.childIterator();

        List<ToolSelectNode> toolSelectNodes = new ArrayList<>();

        while (layerIterator.hasNext())
        {
            GCodeEventNode potentialToolSelectNode = layerIterator.next();

            if (potentialToolSelectNode instanceof ToolSelectNode)
            {
                toolSelectNodes.add((ToolSelectNode) potentialToolSelectNode);
            }
        }

        for (ToolSelectNode toolSelectNode : toolSelectNodes)
        {
            if (lastToolSelectNode == null)
            {
                //Our first ever tool select node...
            } else if (lastToolSelectNode.getToolNumber() == toolSelectNode.getToolNumber())
            {
                toolSelectNode.suppressNodeOutput(true);
            } else
            {
                if (ppFeatureSet.isEnabled(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES))
                {
                    closeAtEndOfToolSelectIfNecessary(lastToolSelectNode, nozzleProxies);
                }

                //Now look to see if we can consolidate the tool change with a travel
                if (lastToolSelectNode.getChildren().size() > 0)
                {
                    if (lastToolSelectNode.getChildren().get(lastToolSelectNode.getChildren().size() - 1) instanceof MergeableWithToolchange)
                    {
                        ((MergeableWithToolchange) lastToolSelectNode.getChildren().get(lastToolSelectNode.getChildren().size() - 1)).changeToolDuringMovement(toolSelectNode.getToolNumber());
                        toolSelectNode.suppressNodeOutput(true);
                    }
                }
            }

            lastToolSelectNode = toolSelectNode;
        }
    }

    protected void closeAtEndOfToolSelectIfNecessary(ToolSelectNode toolSelectNode, List<NozzleProxy> nozzleProxies)
    {
        // The tool has changed
        // Close the nozzle if it isn't already...
        //Insert a close at the end if there isn't already a close following the last extrusion
        Iterator<GCodeEventNode> nodeIterator = toolSelectNode.childBackwardsIterator();
        boolean keepLooking = true;
        boolean needToClose = false;
        GCodeEventNode eventToCloseFrom = null;

        List<SectionNode> sectionsToConsiderForClose = new ArrayList<>();

        //If we see a nozzle event BEFORE an extrusion then the nozzle has already been closed
        //If we see an extrusion BEFORE a nozzle event then we must close
        //Keep looking until we find a nozzle event, so that 
        while (nodeIterator.hasNext()
                && keepLooking)
        {
            GCodeEventNode node = nodeIterator.next();

            if (node instanceof SectionNode)
            {
                Iterator<GCodeEventNode> sectionIterator = node.childBackwardsIterator();
                while (sectionIterator.hasNext()
                        && keepLooking)
                {
                    GCodeEventNode sectionChild = sectionIterator.next();
                    if (sectionChild instanceof NozzlePositionProvider
                            && ((NozzlePositionProvider) sectionChild).getNozzlePosition().isBSet())
                    {
                        keepLooking = false;
                    } else if (sectionChild instanceof ExtrusionNode)
                    {
                        if (!sectionsToConsiderForClose.contains((SectionNode) node))
                        {
                            sectionsToConsiderForClose.add(0, (SectionNode) node);
                        }
                        if (eventToCloseFrom == null)
                        {
                            eventToCloseFrom = sectionChild;
                            needToClose = true;
                        }
                    }
                }
            } else
            {
                if (node instanceof NozzlePositionProvider
                        && ((NozzlePositionProvider) node).getNozzlePosition().isBSet())
                {
                    keepLooking = false;
                } else if (node instanceof ExtrusionNode)
                {
                    if (eventToCloseFrom == null)
                    {
                        eventToCloseFrom = node;
                        needToClose = true;
                    }
                }
            }
        }

        if (needToClose)
        {
            try
            {
                Optional<CloseResult> closeResult = closeLogic.insertProgressiveNozzleClose(eventToCloseFrom, sectionsToConsiderForClose, nozzleProxies.get(toolSelectNode.getToolNumber()));
                if (!closeResult.isPresent())
                {
                    steno.warning("Close failed - unable to record replenish");
                }
            } catch (NodeProcessingException | CannotCloseFromPerimeterException | NoPerimeterToCloseOverException | NotEnoughAvailableExtrusionException | PostProcessingError ex)
            {
                throw new RuntimeException("Error locating available extrusion during tool select normalisation", ex);
            }
        }
    }

    protected OpenResult insertOpens(LayerNode layerNode,
            OpenResult lastOpenResult,
            List<NozzleProxy> nozzleProxies,
            String headTypeCode)
    {
        Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);
        Movement lastMovement = null;
        boolean nozzleOpen = false;
        double lastNozzleValue = 0;
        int lastToolNumber = -1;
        double replenishExtrusionE = 0;
        double replenishExtrusionD = 0;
        int opensInThisTool = 0;

        Map<ExtrusionNode, NozzleValvePositionNode> nodesToAdd = new HashMap<>();
        Map<ExtrusionNode, Integer> toolReselectsToAdd = new HashMap<>();

        if (lastOpenResult != null)
        {
            nozzleOpen = lastOpenResult.isNozzleOpen();
            replenishExtrusionE = lastOpenResult.getOutstandingEReplenish();
            replenishExtrusionD = lastOpenResult.getOutstandingDReplenish();
            lastToolNumber = lastOpenResult.getLastToolNumber();
            opensInThisTool = lastOpenResult.getOpensInLastTool();
        }

        while (layerIterator.hasNext())
        {
            GCodeEventNode layerEvent = layerIterator.next();

            if (layerEvent instanceof ToolSelectNode)
            {
                if (lastToolNumber != ((ToolSelectNode) layerEvent).getToolNumber())
                {
                lastToolNumber = ((ToolSelectNode) layerEvent).getToolNumber();
                opensInThisTool = 0;
                }
            } else if (layerEvent instanceof NozzlePositionProvider
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().isPartialOpen())
            {
                nozzleOpen = true;
                lastNozzleValue = ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB();

                //As a special case for partials, insert the elided extrusion here
                double replenishEToUse = 0;
                double replenishDToUse = 0;

                switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                {
                    case "E":
                        replenishEToUse = replenishExtrusionE;
                        replenishExtrusionE = 0;
                        replenishDToUse = 0;
                        break;
                    case "D":
                        replenishDToUse = replenishExtrusionD;
                        replenishExtrusionD = 0;
                        replenishEToUse = 0;
                        break;
                }

                if (replenishDToUse == 0 && replenishEToUse == 0)
                {
                    String outputString = "No replenish on open in layer " + layerNode.getLayerNumber() + " before partial open " + ((NozzleValvePositionNode) layerEvent).renderForOutput();
                    if (layerNode.getGCodeLineNumber().isPresent())
                    {
                        outputString += " on line " + layerNode.getGCodeLineNumber().get();
                    }
                    steno.warning(outputString);
                }

                ((NozzleValvePositionNode)layerEvent).setReplenishExtrusionE(replenishEToUse);
                ((NozzleValvePositionNode)layerEvent).setReplenishExtrusionD(replenishDToUse);
            } else if (layerEvent instanceof NozzlePositionProvider
                    && (((NozzlePositionProvider) layerEvent).getNozzlePosition().isBSet()
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB() == 1.0))
            {
                nozzleOpen = true;
                lastNozzleValue = ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB();
                switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                {
                    case "E":
                        replenishExtrusionE = 0;
                        break;
                    case "D":
                        replenishExtrusionD = 0;
                        break;
                }

                if (layerEvent instanceof ExtrusionNode)
                {
                    if (lastMovement == null)
                    {
                        lastMovement = ((ExtrusionNode) layerEvent).getMovement();
                    }
                }
            } else if (layerEvent instanceof NozzlePositionProvider
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().isBSet()
                    && ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB() < 1.0)
            {
                nozzleOpen = false;
                lastNozzleValue = ((NozzlePositionProvider) layerEvent).getNozzlePosition().getB();
                if (layerEvent instanceof ExtrusionNode)
                {
                    switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                    {
                        case "E":
                            replenishExtrusionE = ((ExtrusionNode) layerEvent).getElidedExtrusion();
                            break;
                        case "D":
                            replenishExtrusionD = ((ExtrusionNode) layerEvent).getElidedExtrusion();
                            break;
                    }

                    if (lastMovement == null)
                    {
                        lastMovement = ((ExtrusionNode) layerEvent).getMovement();
                    }
                }
            } else if (layerEvent instanceof ExtrusionNode
                    && !nozzleOpen)
            {
                if (lastNozzleValue > 0)
                {
                    String outputString = "Nozzle was not closed properly on layer " + layerNode.getLayerNumber() + " before extrusion " + ((ExtrusionNode) layerEvent).renderForOutput();
                    if (layerNode.getGCodeLineNumber().isPresent())
                    {
                        outputString += " on line " + layerNode.getGCodeLineNumber().get();
                    }
                    steno.warning(outputString);
                }
                NozzleValvePositionNode newNozzleValvePositionNode = new NozzleValvePositionNode();
                newNozzleValvePositionNode.getNozzlePosition().setB(1);

                double replenishEToUse = 0;
                double replenishDToUse = 0;

                switch (HeadContainer.getHeadByID(headTypeCode).getNozzles().get(lastToolNumber).getAssociatedExtruder())
                {
                    case "E":
                        replenishEToUse = replenishExtrusionE;
                        replenishExtrusionE = 0;
                        replenishDToUse = 0;
                        break;
                    case "D":
                        replenishDToUse = replenishExtrusionD;
                        replenishExtrusionD = 0;
                        replenishEToUse = 0;
                        break;
                }

                if (replenishDToUse == 0 && replenishEToUse == 0)
                {
                    String outputString = "No replenish on open in layer " + layerNode.getLayerNumber() + " before extrusion " + ((ExtrusionNode) layerEvent).renderForOutput();
                    if (layerNode.getGCodeLineNumber().isPresent())
                    {
                        outputString += " on line " + layerNode.getGCodeLineNumber().get();
                    }
                    steno.warning(outputString);
                }

                newNozzleValvePositionNode.setReplenishExtrusionE(replenishEToUse);
                newNozzleValvePositionNode.setReplenishExtrusionD(replenishDToUse);
                nodesToAdd.put((ExtrusionNode) layerEvent, newNozzleValvePositionNode);
                nozzleOpen = true;

                opensInThisTool++;
                if (opensInThisTool > settings.getMaxClosesBeforeNozzleReselect())
                {
                    toolReselectsToAdd.put((ExtrusionNode) layerEvent, lastToolNumber);
                    opensInThisTool = 0;
                }

                if (lastMovement == null)
                {
                    lastMovement = ((ExtrusionNode) layerEvent).getMovement();
                }
            }
//            else if (layerEvent instanceof TravelNode)
//            {
//                if (lastMovement == null)
//                {
//                    lastMovement = ((TravelNode) layerEvent).getMovement();
//                } else
//                {
//                    Movement thisMovement = ((TravelNode) layerEvent).getMovement();
//                    Vector2D thisPoint = thisMovement.toVector2D();
//                    Vector2D lastPoint = lastMovement.toVector2D();
//
//                    if (lastPoint.distance(thisPoint) > 5 && !nozzleOpen)
//                    {
//                        steno.warning("Travel without close on layer " + layerNode.getLayerNumber() + " at " + ((TravelNode) layerEvent).renderForOutput());
//                    }
//                }
//            }
        }

        nodesToAdd.entrySet().stream().forEach((entryToUpdate) ->
        {
            if (toolReselectsToAdd.containsKey(entryToUpdate.getKey()))
            {
                int toolToReselect = toolReselectsToAdd.get(entryToUpdate.getKey());
                ToolSelectNode reselect = new ToolSelectNode();
                reselect.setToolNumber(toolToReselect);
                reselect.appendCommentText("Reselect nozzle");
                entryToUpdate.getKey().addSiblingBefore(reselect);
            }

            //Add an M109 to make sure temperature is maintained
            MCodeNode tempSustain = new MCodeNode(109);
            entryToUpdate.getKey().addSiblingBefore(tempSustain);
            entryToUpdate.getKey().addSiblingBefore(entryToUpdate.getValue());
        });

        return new OpenResult(replenishExtrusionE, replenishExtrusionD, nozzleOpen, lastToolNumber, opensInThisTool);
    }

    protected void updateLayerToLineNumber(LayerPostProcessResult lastLayerParseResult,
            List<Integer> layerNumberToLineNumber,
            GCodeOutputWriter writer)
    {
        if (lastLayerParseResult.getLayerData()
                != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToLineNumber.add(layerNumber, writer.getNumberOfLinesOutput());
            }
        }
    }

    protected double updateLayerToPredictedDuration(LayerPostProcessResult lastLayerParseResult,
            List<Double> layerNumberToPredictedDuration,
            GCodeOutputWriter writer)
    {
        double predictedDuration = 0;

        if (lastLayerParseResult.getLayerData() != null)
        {
            int layerNumber = lastLayerParseResult.getLayerData().getLayerNumber();
            if (layerNumber >= 0)
            {
                layerNumberToPredictedDuration.add(layerNumber, lastLayerParseResult.getTimeForLayer());
                predictedDuration += lastLayerParseResult.getTimeForLayer();
            }
        }

        return predictedDuration;
    }

    public void recalculatePerSectionExtrusion(LayerNode layerNode)
    {
        Iterator<GCodeEventNode> childrenOfTheLayer = layerNode.childIterator();
        while (childrenOfTheLayer.hasNext())
        {
            GCodeEventNode potentialSectionNode = childrenOfTheLayer.next();

            if (potentialSectionNode instanceof SectionNode)
            {
                ((SectionNode) potentialSectionNode).recalculateExtrusion();
            }
        }
    }

    public void heaterSave(LayerNode layerNode, LayerPostProcessResult lastLayerParseResult)
    {
        ToolSelectNode firstToolSelectNode = null;
        ToolSelectNode lastToolSelectNode = null;
        double timeInCurrentTool = 0;

        boolean sHeaterOn = true;
        boolean tHeaterOn = true;

        if (lastLayerParseResult.getLastToolSelectInForce() != null)
        {
            firstToolSelectNode = lastLayerParseResult.getLastToolSelectOfSameNumber();
            lastToolSelectNode = lastLayerParseResult.getLastToolSelectInForce();
            timeInCurrentTool = lastLayerParseResult.getTimeUsingLastTool();
        }

        // We know that tool selects come directly under a layer node...        
        Iterator<GCodeEventNode> layerIterator = layerNode.childIterator();

        List<ToolSelectNode> toolSelectNodes = new ArrayList<>();

        while (layerIterator.hasNext())
        {
            GCodeEventNode potentialToolSelectNode = layerIterator.next();

            if (potentialToolSelectNode instanceof ToolSelectNode)
            {
                toolSelectNodes.add((ToolSelectNode) potentialToolSelectNode);
            }
        }

        for (ToolSelectNode toolSelectNode : toolSelectNodes)
        {
            if (lastToolSelectNode == null)
            {
                //Our first ever tool select node...
                firstToolSelectNode = toolSelectNode;
                timeInCurrentTool = 0;
            } else if (lastToolSelectNode.getToolNumber() != toolSelectNode.getToolNumber())
            {
                if ((toolSelectNode.getToolNumber() == 0 && !sHeaterOn)
                        || (toolSelectNode.getToolNumber() == 1 && !tHeaterOn))
                {
                    GCodeEventNode node = goBack1Minute(lastToolSelectNode);
                    if (node != null)
                    {
                        MCodeNode tempControlNode = new MCodeNode();
                        if (layerNode.getLayerNumber() <= 0)
                        {
                            tempControlNode.setMNumber(103);
                        } else
                        {
                            tempControlNode.setMNumber(104);
                        }
                        if (toolSelectNode.getToolNumber() == 0)
                        {
                            tempControlNode.setSOnly(true);
                        } else
                        {
                            tempControlNode.setTOnly(true);
                        }

                        tempControlNode.appendCommentText("Switching heater on");
                        node.addSiblingAfter(tempControlNode);
                    } else
                    {
                        throw new RuntimeException("Couldn't find place to reheat nozzle");
                    }
                }

                firstToolSelectNode = toolSelectNode;
                timeInCurrentTool = 0;
            }

            timeInCurrentTool += toolSelectNode.getEstimatedDuration();

            if (timeInCurrentTool > 180)
            {
                MCodeNode tempControlNode = new MCodeNode();
                if (layerNode.getLayerNumber() <= 0)
                {
                    tempControlNode.setMNumber(103);
                } else
                {
                    tempControlNode.setMNumber(104);
                }

                if (firstToolSelectNode.getToolNumber() == 0)
                {
                    tempControlNode.setSNumber(0);
                    sHeaterOn = false;
                } else
                {
                    tempControlNode.setTNumber(0);
                    tHeaterOn = false;
                }

                tempControlNode.appendCommentText("Switching heater off");

                firstToolSelectNode.addChildAtStart(tempControlNode);

                timeInCurrentTool = 0;
            }

            lastToolSelectNode = toolSelectNode;
        }
    }

    private GCodeEventNode goBack1Minute(ToolSelectNode toolSelectNode)
    {
        GCodeEventNode oneMinuteNode = null;

        Iterator<GCodeEventNode> toolSelectIterator = toolSelectNode.childBackwardsIterator();

        SupportsPrintTimeCalculation lastMovementProvider = null;

        double timeSoFar = 0;

        while (toolSelectIterator.hasNext())
        {
            GCodeEventNode node = toolSelectIterator.next();

            if (node instanceof SupportsPrintTimeCalculation)
            {
                SupportsPrintTimeCalculation timeCalculationNode = (SupportsPrintTimeCalculation) node;

                if (lastMovementProvider != null)
                {
                    try
                    {
                        double time = lastMovementProvider.timeToReach((MovementProvider) node);
                        timeSoFar += time;

                        if (timeSoFar >= 60)
                        {
                            oneMinuteNode = node;
                            break;
                        }
                    } catch (DurationCalculationException ex)
                    {
                        if (ex.getFromNode() instanceof Renderable
                                && ex.getToNode() instanceof Renderable)
                        {
                            steno.error("Unable to calculate duration correctly for nodes source:"
                                    + ((Renderable) ex.getFromNode()).renderForOutput()
                                    + " destination:"
                                    + ((Renderable) ex.getToNode()).renderForOutput());
                        } else
                        {
                            steno.error("Unable to calculate duration correctly for nodes source:"
                                    + ex.getFromNode().getMovement().renderForOutput()
                                    + " destination:"
                                    + ex.getToNode().getMovement().renderForOutput());
                        }
                    }
                }
                lastMovementProvider = timeCalculationNode;
            }
        }

        return oneMinuteNode;
    }
}
