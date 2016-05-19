package celtech.roboxbase.postprocessor.nouveau.timeCalc;

import celtech.roboxbase.postprocessor.nouveau.LayerPostProcessResult;
import celtech.roboxbase.postprocessor.nouveau.nodes.FillSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeDirectiveNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.GCodeEventNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.InnerPerimeterSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerChangeDirectiveNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.MCodeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.NozzleValvePositionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.OuterPerimeterSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SkinSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SkirtSectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ToolSelectNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.nodeFunctions.DurationCalculationException;
import celtech.roboxbase.postprocessor.nouveau.nodes.nodeFunctions.SupportsPrintTimeCalculation;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.ExtrusionProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.FeedrateProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.MovementProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.NozzlePositionProvider;
import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Renderable;
import celtech.roboxbase.printerControl.model.Head.HeadType;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class TimeAndVolumeCalc
{

    private final Stenographer steno = StenographerFactory.getStenographer(TimeAndVolumeCalc.class.getName());
    private static final double timeForInitialHoming_s = 20;
    private static final double timeForPurgeAndLevelling_s = 40;
    private static final double timeForNozzleSelect_s = 1;
    private static final double zMoveRate_mms = 4;
    private static final double nozzlePositionChange_s = 0.25;
    // Add a percentage to each movement to factor in acceleration across the whole print
    private static final double movementFudgeFactor = 1.1;

    private final HeadType currentHeadType;

    private enum TimeAllocation
    {

        NOT_ALLOCATED, DEPENDS_ON_E, DEPENDS_ON_D, DEPENDS_ON_SELECTED_TOOL, FEEDRATE_INDEPENDENT
    }

    public TimeAndVolumeCalc(HeadType headType)
    {
        this.currentHeadType = headType;
    }

    //This method must:
    // Update data used for filament saver calculations:
    //              Calculate and set the finish time from start for each node
    //              Calculate the estimated duration for each tool select node
    // 
    // Stash information used for time and cost and ETC displays
    //              Total E volume used
    //              Total D volume used
    //              Per-layer and per tool feedrate independent duration 
    //              Per-layer and per tool feedrate dependent duration
    //
    public TimeAndVolumeCalcResult calculateVolumeAndTime(List<LayerPostProcessResult> allLayerPostProcessResults)
    {
        ExtruderTimeAndVolumeCalcComponent extruderEStats = new ExtruderTimeAndVolumeCalcComponent();
        ExtruderTimeAndVolumeCalcComponent extruderDStats = new ExtruderTimeAndVolumeCalcComponent();
        TimeCalcComponent feedrateIndependentDuration = new TimeCalcComponent();

        SupportsPrintTimeCalculation lastNodeSupportingPrintTimeCalcs = null;
        LayerNode lastLayerNode = null;
        double timeFromStart = 0;
        double timeInThisTool = 0;

        ToolSelectNode lastToolSelectNode = null;
        int lastFeedrateInForce = 0;

        for (int layerCounter = 0;
                layerCounter < allLayerPostProcessResults.size();
                layerCounter++)
        {

            //Make sure we at least have a zero entry for each layer
            extruderEStats.getDuration().incrementDuration(layerCounter, 0);
            extruderDStats.getDuration().incrementDuration(layerCounter, 0);
            feedrateIndependentDuration.incrementDuration(layerCounter, 0);

            if (layerCounter == 0)
            {
                //Insert some data for the pre-print preamble
                feedrateIndependentDuration.incrementDuration(0, timeForInitialHoming_s + timeForPurgeAndLevelling_s);
            }

            LayerPostProcessResult layerPostProcessResult = allLayerPostProcessResults.get(layerCounter);

            LayerNode layerNode = layerPostProcessResult.getLayerData();
            Iterator<GCodeEventNode> layerIterator = layerNode.treeSpanningIterator(null);

            while (layerIterator.hasNext())
            {
                GCodeEventNode node = layerIterator.next();
                TimeAllocation chosenAllocation = TimeAllocation.NOT_ALLOCATED;

                //Total up the extruded volume
                if (node instanceof ExtrusionProvider)
                {
                    ExtrusionProvider extrusionProvider = (ExtrusionProvider) node;
                    extruderEStats.incrementVolume(extrusionProvider.getExtrusion().getE());
                    extruderDStats.incrementVolume(extrusionProvider.getExtrusion().getD());
                }

                //If the tool is selected (or reselected) stash the current elapsed time in tool
                if (node instanceof ToolSelectNode)
                {
                    if (lastToolSelectNode != null)
                    {
                        lastToolSelectNode.setEstimatedDuration(timeInThisTool);
                        lastToolSelectNode.setFinishTimeFromStartOfPrint_secs(timeFromStart);
                    }

                    lastToolSelectNode = (ToolSelectNode) node;
                    timeInThisTool = 0;
                }

                double eventDuration = -1;

                if (node instanceof SupportsPrintTimeCalculation)
                {
                    SupportsPrintTimeCalculation timeCalculationNode = (SupportsPrintTimeCalculation) node;

                    if (lastNodeSupportingPrintTimeCalcs != null)
                    {
                        try
                        {
                            eventDuration = lastNodeSupportingPrintTimeCalcs.timeToReach((MovementProvider) node) * movementFudgeFactor;

                            if (node instanceof NozzlePositionProvider)
                            {
                                chosenAllocation = TimeAllocation.DEPENDS_ON_SELECTED_TOOL;
                            } else if (node instanceof ExtrusionProvider)
                            {
                                if (((ExtrusionProvider) node).getExtrusion().isEInUse())
                                {
                                    chosenAllocation = TimeAllocation.DEPENDS_ON_E;
                                } else if (((ExtrusionProvider) node).getExtrusion().isDInUse())
                                {
                                    chosenAllocation = TimeAllocation.DEPENDS_ON_D;
                                }
                            } else
                            {
                                chosenAllocation = TimeAllocation.FEEDRATE_INDEPENDENT;
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

                            throw new RuntimeException("Unable to calculate duration correctly on layer "
                                    + layerNode.getLayerNumber(), ex);
                        }
                    }
                    lastNodeSupportingPrintTimeCalcs = timeCalculationNode;

                    if (((FeedrateProvider) lastNodeSupportingPrintTimeCalcs).getFeedrate().getFeedRate_mmPerMin() < 0)
                    {
                        if (lastFeedrateInForce > 0)
                        {
                            ((FeedrateProvider) lastNodeSupportingPrintTimeCalcs).getFeedrate().setFeedRate_mmPerMin(lastFeedrateInForce);
                        } else
                        {
                            steno.warning("Couldn't set feedrate during time calculation");
                        }
                    }
                    lastFeedrateInForce = ((FeedrateProvider) lastNodeSupportingPrintTimeCalcs).getFeedrate().getFeedRate_mmPerMin();
                } else if (node instanceof ToolSelectNode)
                {
                    eventDuration = timeForNozzleSelect_s;
                    chosenAllocation = TimeAllocation.FEEDRATE_INDEPENDENT;
                } else if (node instanceof LayerChangeDirectiveNode)
                {
                    LayerChangeDirectiveNode lNode = (LayerChangeDirectiveNode) node;
                    double heightChange = lNode.getMovement().getZ() - layerNode.getLayerHeight_mm();
                    if (heightChange > 0)
                    {
                        eventDuration = heightChange / zMoveRate_mms;
                        chosenAllocation = TimeAllocation.FEEDRATE_INDEPENDENT;
                    }
                } else if (node instanceof NozzleValvePositionNode)
                {
                    eventDuration = nozzlePositionChange_s;
                    chosenAllocation = TimeAllocation.DEPENDS_ON_SELECTED_TOOL;
                } else if (node instanceof GCodeDirectiveNode
                        && ((GCodeDirectiveNode) node).getGValue() == 4)
                {
                    GCodeDirectiveNode directive = (GCodeDirectiveNode) node;
                    if (directive.getGValue() == 4)
                    {
                        //Found a dwell
                        Optional<Integer> sValue = directive.getSValue();
                        if (sValue.isPresent())
                        {
                            //Seconds
                            eventDuration = sValue.get();
                            chosenAllocation = TimeAllocation.FEEDRATE_INDEPENDENT;
                        }
                        Optional<Integer> pValue = directive.getPValue();
                        if (pValue.isPresent())
                        {
                            //Microseconds
                            eventDuration = pValue.get() / 1000.0;
                            chosenAllocation = TimeAllocation.FEEDRATE_INDEPENDENT;
                        }
                    }
                } else if (!(node instanceof FillSectionNode)
                        && !(node instanceof InnerPerimeterSectionNode)
                        && !(node instanceof SkinSectionNode)
                        && !(node instanceof OuterPerimeterSectionNode)
                        && !(node instanceof SkirtSectionNode)
                        && !(node instanceof MCodeNode))
                {
                    steno.info("Not possible to calculate time for: " + node.getClass().getName() + " : " + node.toString());
                }

                //Store the per-layer duration data
                if (eventDuration > 0)
                {
                    switch (chosenAllocation)
                    {
                        case DEPENDS_ON_E:
                            extruderEStats.getDuration().incrementDuration(layerCounter, eventDuration);
                            break;
                        case DEPENDS_ON_D:
                            extruderDStats.getDuration().incrementDuration(layerCounter, eventDuration);
                            break;
                        case DEPENDS_ON_SELECTED_TOOL:
                            int currentToolInUse = (lastToolSelectNode != null) ? lastToolSelectNode.getToolNumber() : 0;
                            switch (currentToolInUse)
                            {
                                case 0:
                                    if (currentHeadType == HeadType.DUAL_MATERIAL_HEAD)
                                    {
                                        extruderDStats.getDuration().incrementDuration(layerCounter, eventDuration);
                                    } else
                                    {
                                        extruderEStats.getDuration().incrementDuration(layerCounter, eventDuration);
                                    }
                                    break;
                                case 1:
                                    extruderEStats.getDuration().incrementDuration(layerCounter, eventDuration);
                                    break;
                            }
                            break;
                        case FEEDRATE_INDEPENDENT:
                            feedrateIndependentDuration.incrementDuration(layerCounter, eventDuration);
                            break;
                        default:
                            steno.warning("Event duration was not allocated");
                            break;
                    }

                    //Store the finish time for this node
                    timeFromStart += eventDuration;
                    timeInThisTool += eventDuration;
                }

                if (timeFromStart > 0)
                {
                    node.setFinishTimeFromStartOfPrint_secs(timeFromStart);
                }
            }

            if (lastLayerNode != null)
            {
                lastLayerNode.setFinishTimeFromStartOfPrint_secs(timeFromStart);
            }
            lastLayerNode = layerNode;
        }

        return new TimeAndVolumeCalcResult(extruderEStats, extruderDStats, feedrateIndependentDuration);
    }
}
