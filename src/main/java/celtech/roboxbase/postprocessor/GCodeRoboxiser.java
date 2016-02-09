package celtech.roboxbase.postprocessor;

import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.postprocessor.events.CommentEvent;
import celtech.roboxbase.postprocessor.events.EndOfFileEvent;
import celtech.roboxbase.postprocessor.events.ExtrusionEvent;
import celtech.roboxbase.postprocessor.events.GCodeEvent;
import celtech.roboxbase.postprocessor.events.GCodeParseEvent;
import celtech.roboxbase.postprocessor.events.LayerChangeEvent;
import celtech.roboxbase.postprocessor.events.LayerChangeWithTravelEvent;
import celtech.roboxbase.postprocessor.events.LayerChangeWithoutTravelEvent;
import celtech.roboxbase.postprocessor.events.MCodeEvent;
import celtech.roboxbase.postprocessor.events.MovementEvent;
import celtech.roboxbase.postprocessor.events.NozzleChangeBValueEvent;
import celtech.roboxbase.postprocessor.events.NozzleChangeEvent;
import celtech.roboxbase.postprocessor.events.NozzleCloseFullyEvent;
import celtech.roboxbase.postprocessor.events.NozzleOpenFullyEvent;
import celtech.roboxbase.postprocessor.events.NozzlePositionChangeEvent;
import celtech.roboxbase.postprocessor.events.RetractDuringExtrusionEvent;
import celtech.roboxbase.postprocessor.events.RetractEvent;
import celtech.roboxbase.postprocessor.events.TravelEvent;
import celtech.roboxbase.postprocessor.events.UnretractEvent;
import celtech.roboxbase.printerControl.comms.commands.MacroLoadException;
import celtech.roboxbase.printerControl.comms.commands.GCodeMacros;
import celtech.roboxbase.utils.Math.MathUtils;
import static celtech.roboxbase.utils.Math.MathUtils.EQUAL;
import static celtech.roboxbase.utils.Math.MathUtils.MORE_THAN;
import static celtech.roboxbase.utils.Math.MathUtils.compareDouble;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.TreeMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.twod.Line;
import org.apache.commons.math3.geometry.euclidean.twod.Segment;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author Ian
 */
public class GCodeRoboxiser extends GCodeRoboxisingEngine
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            GCodeTranslationEventHandler.class.getName());

    private Tool selectedTool = Tool.Unknown;

    //Profile variables
    private double startClosingByMM = 2;

    protected LegacyExtrusionBuffer extrusionBuffer = new LegacyExtrusionBuffer();

    private boolean triggerCloseFromTravel = true;
    private boolean triggerCloseFromRetract = true;

    private int tempNozzleMemory = 0;
    private int nozzleInUse = -1;
    private boolean nozzleHasBeenReinstated = false;
    private static final int POINT_3MM_NOZZLE = 0;
    private static final int POINT_8MM_NOZZLE = 1;

    private boolean internalClose = true;
    private boolean autoGenerateNozzleChangeEvents = true;

    private double autoUnretractEValue = 0.0;
    private double autoUnretractDValue = 0.0;

    private boolean mixExtruderOutputs = false;
    private ArrayList<ExtruderMix> extruderMixPoints = new ArrayList<>();
    private double currentEMixValue = 1;
    private double currentDMixValue = 0;
    private double startingEMixValue = 1;
    private double startingDMixValue = 0;
    private double endEMixValue = 1;
    private double endDMixValue = 0;
    private int mixFromLayer = 0;
    private int mixToLayer = 0;
    private int currentMixPoint = 0;

    private int layerIndex = 0;
    private double distanceSoFarInLayer = 0;

    // Causes home and return events to be inserted, triggering the camera
    private boolean movieMakerEnabled = false;

    private int closeCounter = 0;

    /**
     *
     * @param line
     */
    @Override
    public void unableToParse(String line)
    {
        processUnparsedLine(line);
    }

    private void resetMeasuringThing()
    {
        distanceSoFar = 0;
    }

    /**
     *
     * @param event
     * @throws celtech.roboxbase.postprocessor.NozzleCloseSettingsError
     */
    @Override
    public void processEvent(GCodeParseEvent event) throws PostProcessingError
    {
        //Buffer extrusion events only
        // Triggers to empty the buffer are written after the buffer has been dealt with
        // Non-triggers are written immediately

        Vector2D currentPoint = null;
        double distance = 0;

        // Home event - used when triggering the camera
        GCodeEvent homeEvent = new GCodeEvent();
        homeEvent.setComment("Home for camera");
        homeEvent.setGNumber(28);
        homeEvent.setGString("X");

        // Dwell event - used when triggering the camera to allow the home to be registered
        GCodeEvent dwellEvent = new GCodeEvent();
        dwellEvent.setComment("Dwell for camera - say cheese! :)");
        dwellEvent.setGNumber(4);
        dwellEvent.setGString("P550");

        GCodeEvent moveUpEvent = new GCodeEvent();
        moveUpEvent.setComment("Move up");
        moveUpEvent.setGNumber(0);
        moveUpEvent.setGString("Z5");

        GCodeEvent relativeMoveEvent = new GCodeEvent();
        relativeMoveEvent.setComment("Relative move");
        relativeMoveEvent.setGNumber(91);

        GCodeEvent absoluteMoveEvent = new GCodeEvent();
        absoluteMoveEvent.setComment("Absolute move");
        absoluteMoveEvent.setGNumber(90);

        GCodeEvent moveToMiddleYEvent = new GCodeEvent();
        moveToMiddleYEvent.setComment("Go to middle of bed in Y");
        moveToMiddleYEvent.setGNumber(0);
        moveToMiddleYEvent.setGString("Y50");

        if (currentNozzle == null)
        {
            NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
            nozzleChangeEvent.setNozzleNumber(POINT_3MM_NOZZLE);
            nozzleChangeEvent.setComment("Initialise using nozzle 0");
            extrusionBuffer.add(nozzleChangeEvent);
            currentNozzle = nozzleProxies.get(POINT_3MM_NOZZLE);
            lastNozzle = currentNozzle;
        }

        if (event instanceof ExtrusionEvent)
        {
            if (lineNumberOfFirstExtrusion == -1)
            {
                lineNumberOfFirstExtrusion = event.getLinesSoFar();
            }

            if (((slicerType == SlicerType.Slic3r && layer == 2) || (slicerType == SlicerType.Cura
                    && layer == 2)) && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenReinstated == false)
            {
                nozzleHasBeenReinstated = true;
                int nozzleToUse = chooseNozzleByTask(event);

                if (nozzleToUse >= 0)
                {
                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                    nozzleChangeEvent.setNozzleNumber(nozzleToUse);
                    nozzleChangeEvent.setComment("return to required nozzle");
                    extrusionBuffer.add(nozzleChangeEvent);
                    closeCounter = 0;

                    setCurrentNozzle(nozzleToUse);
                } else
                {
                    steno.warning("Couldn't derive required nozzle to return to...");
                }
            } else if (autoGenerateNozzleChangeEvents && ((forcedNozzleOnFirstLayer >= 0
                    && nozzleHasBeenReinstated)
                    || forcedNozzleOnFirstLayer < 0))
            {
                int requiredNozzle = chooseNozzleByTask(event);

                if (currentNozzle != null && nozzleProxies.get(requiredNozzle) != currentNozzle)
                {
                    // Close the old nozzle
                    writeEventsWithNozzleClose("Closing last used nozzle");
                }

                if (currentNozzle == null || nozzleProxies.get(requiredNozzle) != currentNozzle)
                {
                    //Select the nozzle
                    NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
                    nozzleChangeEvent.setComment("Selecting nozzle " + requiredNozzle);
                    nozzleChangeEvent.setNozzleNumber(requiredNozzle);
                    extrusionBuffer.add(nozzleChangeEvent);

                    setCurrentNozzle(requiredNozzle);
                }
            }

            ExtrusionEvent extrusionEvent = (ExtrusionEvent) event;
            currentPoint = new Vector2D(extrusionEvent.getX(),
                    extrusionEvent.getY());

            totalExtrudedVolume += extrusionEvent.getE() + extrusionEvent.getD();

            if (currentNozzle.getState() != NozzleState.OPEN
                    && currentNozzle.getNozzleParameters().getOpenOverVolume() == 0)
            {
                // Unretract and open
                NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                openNozzle.setComment("Open and replenish from extrusion");
                openNozzle.setE(autoUnretractEValue);
                openNozzle.setD(autoUnretractDValue);

                autoUnretractEValue = 0;
                autoUnretractDValue = 0;

                extrusionBuffer.add(openNozzle);
                currentNozzle.openNozzleFully();
                nozzleLastOpenedAt = lastPoint;
            }

            resetMeasuringThing();

            // Open the nozzle if it isn't already open
            // This will always be a single event prior to extrusion
            if (currentNozzle.getState() != NozzleState.OPEN)
            {
                if (currentNozzle.getNozzleParameters().getOpenOverVolume() <= 0)
                {
                    NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                    openNozzle.setComment("Extrusion trigger - open without replenish");
                    extrusionBuffer.add(openNozzle);
                }
                currentNozzle.openNozzleFully();
                nozzleLastOpenedAt = currentPoint;
            }

            // Calculate how long this line is
            if (lastPoint != null)
            {
                distance = lastPoint.distance(currentPoint);
                extrusionEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                distanceSoFar += distance;
                totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);
            }

            lastPoint = currentPoint;

            if (unretractFeedRate > 0)
            {
                if (extrusionEvent.getFeedRate() <= 0)
                {
                    extrusionEvent.setFeedRate(unretractFeedRate);
                }
                unretractFeedRate = 0;
            }

            extrusionBuffer.add(extrusionEvent);
        } else if (event instanceof RetractDuringExtrusionEvent)
        {
            RetractDuringExtrusionEvent extrusionEvent = (RetractDuringExtrusionEvent) event;
            currentPoint = new Vector2D(extrusionEvent.getX(),
                    extrusionEvent.getY());

            // Calculate how long this line is
            if (lastPoint != null)
            {
                distance = lastPoint.distance(currentPoint);
                extrusionEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                distanceSoFar += distance;
                totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);
            }

            lastPoint = currentPoint;

            extrusionBuffer.add(extrusionEvent);
        } else if (event instanceof TravelEvent)
        {
            TravelEvent travelEvent = (TravelEvent) event;
            currentPoint = new Vector2D(travelEvent.getX(),
                    travelEvent.getY());

            if (lastPoint != null)
            {
                distance = lastPoint.distance(currentPoint);
                travelEvent.setLength(distance);
//                        System.out.println("Distance " + distance);
                distanceSoFar += distance;
                totalXYMovement += distance;
//                        System.out.println("Total Distance " + distanceSoFar);

                if (layer == 1
                        && currentNozzle.getNozzleParameters().getTravelBeforeForcedClose() > 0
                        && (currentNozzle.getState() != NozzleState.CLOSED
                        && distance
                        > currentNozzle.getNozzleParameters().getTravelBeforeForcedClose()))
                {
                    writeEventsWithNozzleClose("travel trigger");
                }

                extrusionBuffer.add(event);
            }
            lastPoint = currentPoint;

        } else if (event instanceof LayerChangeWithoutTravelEvent)
        {
            LayerChangeWithoutTravelEvent layerChangeEvent = (LayerChangeWithoutTravelEvent) event;

            currentZHeight = layerChangeEvent.getZ();

            handleForcedNozzleAtLayerChange();

            insertTemperatureCommandsIfRequired();

            layer++;

            handleMovieMakerAtLayerChange(relativeMoveEvent, moveUpEvent, absoluteMoveEvent,
                    moveToMiddleYEvent, homeEvent, dwellEvent);

            extrusionBuffer.add(event);
        } else if (event instanceof LayerChangeWithTravelEvent)
        {
            LayerChangeWithTravelEvent layerChangeEvent = (LayerChangeWithTravelEvent) event;

            currentZHeight = layerChangeEvent.getZ();

            handleForcedNozzleAtLayerChange();

            insertTemperatureCommandsIfRequired();

            layer++;

            handleMovieMakerAtLayerChange(relativeMoveEvent, moveUpEvent, absoluteMoveEvent,
                    moveToMiddleYEvent, homeEvent, dwellEvent);

            extrusionBuffer.add(event);
        } else if (event instanceof NozzleChangeEvent && !autoGenerateNozzleChangeEvents)
        {
            NozzleChangeEvent nozzleChangeEvent = (NozzleChangeEvent) event;

            if (layer == 0 && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenForced == false)
            {
                nozzleHasBeenForced = true;
                tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
                //Force to required nozzle
                nozzleChangeEvent.setNozzleNumber(forcedNozzleOnFirstLayer);
                nozzleChangeEvent.setComment(
                        nozzleChangeEvent.getComment()
                        + " - force to nozzle " + forcedNozzleOnFirstLayer + " on first layer");
                extrusionBuffer.add(nozzleChangeEvent);
                nozzleInUse = forcedNozzleOnFirstLayer;
                setCurrentNozzle(nozzleInUse);
            } else if (layer < 1)
            {
                tempNozzleMemory = nozzleChangeEvent.getNozzleNumber();
            } else if (layer > 1)
            {
                extrusionBuffer.add(nozzleChangeEvent);
                nozzleInUse = nozzleChangeEvent.getNozzleNumber();
                setCurrentNozzle(nozzleInUse);
            }

            // Reset the nozzle close counter
            closeCounter = 0;
        } else if (event instanceof RetractEvent)
        {
            RetractEvent retractEvent = (RetractEvent) event;

            if (triggerCloseFromRetract == true && currentNozzle.getState()
                    != NozzleState.CLOSED)
            {
                writeEventsWithNozzleClose("retract trigger");
            }

            resetMeasuringThing();
        } else if (event instanceof UnretractEvent && !autoGenerateNozzleChangeEvents)
        {
            UnretractEvent unretractEvent = (UnretractEvent) event;

            totalExtrudedVolume += unretractEvent.getE();

            if (currentNozzle.getState() != NozzleState.OPEN
                    && currentNozzle.getNozzleParameters().getOpenOverVolume() <= 0)
            {
                // Unretract and open
                NozzleOpenFullyEvent openNozzle = new NozzleOpenFullyEvent();
                openNozzle.setComment("Open and replenish");
                openNozzle.setE(autoUnretractEValue);
                openNozzle.setD(autoUnretractDValue);
                extrusionBuffer.add(openNozzle);
            } else if (autoUnretractDValue > 0 || autoUnretractEValue > 0)
            {
                // Just unretract
                unretractEvent.setComment("Replenish before open");
                unretractEvent.setE(autoUnretractEValue);
                unretractEvent.setD(autoUnretractDValue);
                extrusionBuffer.add(unretractEvent);
            }

            if (currentNozzle.getState() != NozzleState.OPEN)
            {
                currentNozzle.openNozzleFully();
                nozzleLastOpenedAt = lastPoint;
            }

            resetMeasuringThing();
            autoUnretractEValue = 0;
            autoUnretractDValue = 0;

            if (unretractEvent.getFeedRate() > 0)
            {
                unretractFeedRate = unretractEvent.getFeedRate();
            }
        } else if (event instanceof UnretractEvent)
        {
            UnretractEvent unretractEvent = (UnretractEvent) event;

            if (unretractEvent.getFeedRate() > 0)
            {
                unretractFeedRate = unretractEvent.getFeedRate();
            }
        } else if (event instanceof MCodeEvent)
        {
            MCodeEvent mCodeEvent = (MCodeEvent) event;
            if (mCodeEvent.getMNumber() != 104
                    && mCodeEvent.getMNumber() != 109
                    && mCodeEvent.getMNumber() != 140
                    && mCodeEvent.getMNumber() != 190)
            {
                extrusionBuffer.add(event);
            }
        } else if (event instanceof GCodeEvent)
        {
            extrusionBuffer.add(event);
        } else if (event instanceof CommentEvent)
        {
            extrusionBuffer.add(event);
        } else if (event instanceof EndOfFileEvent)
        {
            if (currentNozzle.getState() != NozzleState.CLOSED)
            {
                writeEventsWithNozzleClose("End of file");
            }

            try
            {
                outputWriter.writeOutput(";\n; Post print gcode\n");
                for (String macroLine : GCodeMacros.getMacroContents("after_print", null, false, false, false))
                {
                    outputWriter.writeOutput(macroLine + "\n");
                }
                outputWriter.writeOutput("; End of Post print gcode\n");
            } catch (IOException ex)
            {
                throw new PostProcessingError("IO Error whilst writing post-print gcode to file: "
                        + ex.getMessage());
            } catch (MacroLoadException ex)
            {
                throw new PostProcessingError(
                        "Error whilst writing post-print gcode to file - couldn't add after print footer due to circular macro reference");
            }

            writeEventToFile(event);
        }
    }

    private void setCurrentNozzle(int nozzleToUse)
    {
        currentNozzle = nozzleProxies.get(nozzleToUse);
    }

    private void insertTemperatureCommandsIfRequired()
    {
        if (((slicerType == SlicerType.Slic3r && layer == 1)
                || (slicerType == SlicerType.Cura && layer == 1))
                && subsequentLayersTemperaturesWritten == false)
        {
            extrusionBuffer.insertSubsequentLayerTemperatures();
            subsequentLayersTemperaturesWritten = true;
        }
    }

    private void handleMovieMakerAtLayerChange(GCodeEvent relativeMoveEvent, GCodeEvent moveUpEvent,
            GCodeEvent absoluteMoveEvent, GCodeEvent moveToMiddleYEvent, GCodeEvent homeEvent,
            GCodeEvent dwellEvent)
    {
        if (movieMakerEnabled && lastPoint != null)
        {
            GCodeEvent moveBackIntoPlace = new GCodeEvent();
            moveBackIntoPlace.setComment("Return to last position");
            moveBackIntoPlace.setGNumber(0);
            moveBackIntoPlace.setGString("X" + lastPoint.getX() + " Y" + lastPoint.getY());

            if (currentNozzle.getState() != NozzleState.CLOSED)
            {
                extrusionBuffer.add(new NozzleCloseFullyEvent());
            }

            extrusionBuffer.add(relativeMoveEvent);
            extrusionBuffer.add(moveUpEvent);
            extrusionBuffer.add(absoluteMoveEvent);
            extrusionBuffer.add(moveToMiddleYEvent);
            extrusionBuffer.add(homeEvent);
            extrusionBuffer.add(dwellEvent);
            extrusionBuffer.add(moveBackIntoPlace);

            if (currentNozzle.getState() != NozzleState.CLOSED)
            {
                extrusionBuffer.add(new NozzleOpenFullyEvent());
            }

        }
    }

    private void handleForcedNozzleAtLayerChange() throws PostProcessingError
    {
        if (((slicerType == SlicerType.Slic3r && layer == 0) || (slicerType == SlicerType.Cura
                && layer == 0)) && forcedNozzleOnFirstLayer >= 0 && nozzleHasBeenForced == false)
        {
            nozzleHasBeenForced = true;
            NozzleChangeEvent nozzleChangeEvent = new NozzleChangeEvent();
            //Force to required nozzle
            nozzleChangeEvent.setNozzleNumber(forcedNozzleOnFirstLayer);
            if (nozzleChangeEvent.getComment() != null)
            {
                nozzleChangeEvent.setComment(
                        nozzleChangeEvent.getComment()
                        + " - force to nozzle " + forcedNozzleOnFirstLayer + " on first layer");
            } else
            {
                nozzleChangeEvent.setComment(" - force to nozzle " + forcedNozzleOnFirstLayer
                        + " on first layer");
            }
            tempNozzleMemory = 0;
            extrusionBuffer.add(nozzleChangeEvent);
            nozzleInUse = forcedNozzleOnFirstLayer;
            setCurrentNozzle(nozzleInUse);
        }

        if (((slicerType == SlicerType.Slic3r && layer == 1) || (slicerType == SlicerType.Cura
                && layer == 1))
                && forcedNozzleOnFirstLayer >= 0
                && currentNozzle.getState() == NozzleState.OPEN)
        {
            writeEventsWithNozzleClose(
                    "closing nozzle after forced nozzle select on layer 0");
        }
    }

    private int chooseNozzleByTask(GCodeParseEvent event)
    {
        int nozzleToUse;

        ExtrusionTask extrusionTask = ((ExtrusionEvent) event).getExtrusionTask();

        if (extrusionTask != null)
        {
            switch (extrusionTask)
            {
                case Perimeter:
                case ExternalPerimeter:
                    nozzleToUse = currentSettings.getPerimeterNozzle();
                    break;
                case Fill:
                case Skin:
                    nozzleToUse = currentSettings.getFillNozzle();
                    break;
                case Support:
                    nozzleToUse = currentSettings.getSupportNozzle();
                    break;
                case Support_Interface:
                    nozzleToUse = currentSettings.getSupportInterfaceNozzle();
                    break;
                default:
                    nozzleToUse = currentSettings.getFillNozzle();
                    break;
            }
        } else
        {
            nozzleToUse = currentSettings.getFillNozzle();
        }
        return nozzleToUse;
    }

    private void writeEventToFile(GCodeParseEvent event)
    {
        try
        {
            outputWriter.writeOutput(event.renderForOutput());
            outputWriter.flush();
        } catch (IOException ex)
        {
            steno.error("Error whilst writing event to file");
        }
    }

    private void writeAllEvents()
    {
        for (GCodeParseEvent event : extrusionBuffer)
        {
            writeEventToFile(event);
        }

        extrusionBuffer.clear();
    }

    class ExtrusionBounds
    {

        private ExtrusionTask extrusionTask = null;
        private int startIndex = -1;
        private int endIndex = -1;
        private double eExtrusion = 0;
        private double dExtrusion = 0;

        public ExtrusionBounds()
        {
        }

        public void setExtrusionTask(ExtrusionTask task)
        {
            this.extrusionTask = task;
        }

        public void setStartIndex(int startIndex)
        {
            this.startIndex = startIndex;
        }

        public void setEndIndex(int endIndex)
        {
            this.endIndex = endIndex;
        }

        public void setEExtrusion(double eExtrusion)
        {
            this.eExtrusion = eExtrusion;
        }

        public void setDExtrusion(double dExtrusion)
        {
            this.dExtrusion = dExtrusion;
        }

        public ExtrusionTask getExtrusionTask()
        {
            return extrusionTask;
        }

        public int getStartIndex()
        {
            return startIndex;
        }

        public int getEndIndex()
        {
            return endIndex;
        }

        public double getEExtrusion()
        {
            return eExtrusion;
        }

        public double getDExtrusion()
        {
            return dExtrusion;
        }
    }

    class ExtrusionBufferDigest
    {

        private final List<ExtrusionBounds> extrusionBoundaries;
        private final int lastLayerEvent;
        private final int firstNozzleEvent;

        private ExtrusionBufferDigest(List<ExtrusionBounds> extrusionBoundaries,
                int firstNozzleEvent,
                int lastLayerEvent)
        {
            this.extrusionBoundaries = extrusionBoundaries;
            this.firstNozzleEvent = firstNozzleEvent;
            this.lastLayerEvent = lastLayerEvent;
        }

        public List<ExtrusionBounds> getExtrusionBoundaries()
        {
            return extrusionBoundaries;
        }

        public int getFirstNozzleEvent()
        {
            return firstNozzleEvent;
        }

        public int getLastLayerEvent()
        {
            return lastLayerEvent;
        }
    }

    private ExtrusionBufferDigest getExtrusionBufferDigest(LegacyExtrusionBuffer buffer)
    {
        List<ExtrusionBounds> extrusionBoundaries = new ArrayList<>();
        int firstNozzleEvent = -1;
        int lastLayerIndex = -1;

        int indexOfLastDetectedExtrusionEvent = -1;
        ExtrusionTask lastDetectedExtrusionTask = null;
        ExtrusionBounds extrusionBoundary = null;

        for (int extrusionBufferIndex = buffer.size() - 1; extrusionBufferIndex >= 0; extrusionBufferIndex--)
        {
            GCodeParseEvent event = buffer.get(extrusionBufferIndex);

            if (event instanceof ExtrusionEvent)
            {
                ExtrusionEvent extrusionEvent = (ExtrusionEvent) event;

                if (extrusionEvent.getExtrusionTask() != lastDetectedExtrusionTask)
                {
                    if (extrusionBoundary != null)
                    {
                        // Populate the end event in the last boundary
                        extrusionBoundary.setStartIndex(indexOfLastDetectedExtrusionEvent);
                        extrusionBoundaries.add(0, extrusionBoundary);
                    }

                    extrusionBoundary = new ExtrusionBounds();
                    extrusionBoundary.setEndIndex(extrusionBufferIndex);
                    extrusionBoundary.setExtrusionTask(extrusionEvent.getExtrusionTask());

                    lastDetectedExtrusionTask = extrusionEvent.getExtrusionTask();
                }

                if (extrusionBoundary != null)
                {
                    extrusionBoundary.setEExtrusion(extrusionBoundary.getEExtrusion() + extrusionEvent.getE());
                    extrusionBoundary.setDExtrusion(extrusionBoundary.getDExtrusion() + extrusionEvent.getD());
                }
                indexOfLastDetectedExtrusionEvent = extrusionBufferIndex;
            } else if (event instanceof NozzleChangeBValueEvent
                    || event instanceof NozzleOpenFullyEvent)
            {
                firstNozzleEvent = extrusionBufferIndex;
                // Don't ever go beyond a nozzle event - this would make closing far more complicated
                break;
            } else if (event instanceof LayerChangeEvent
                    && lastLayerIndex < 0)
            {
                //Record the last layer event - we need this for perimeter checks
                lastLayerIndex = extrusionBufferIndex;
                if (extrusionBoundary != null)
                {
                    // Populate the end event in the last boundary
                    extrusionBoundary.setStartIndex(indexOfLastDetectedExtrusionEvent);
                    extrusionBoundaries.add(0, extrusionBoundary);
                    extrusionBoundary = null;
                    lastDetectedExtrusionTask = null;
                }
            }
        }

        if (extrusionBoundary != null)
        {
            extrusionBoundary.setStartIndex(indexOfLastDetectedExtrusionEvent);
            extrusionBoundaries.add(0, extrusionBoundary);
        }

        return new ExtrusionBufferDigest(extrusionBoundaries, firstNozzleEvent, lastLayerIndex);
    }

    private List<Integer> getInwardMoves(LegacyExtrusionBuffer buffer)
    {
        List<Integer> inwardsMoveIndexList = new ArrayList<>();

        for (int extrusionBufferIndex = buffer.size() - 1; extrusionBufferIndex >= 0; extrusionBufferIndex--)
        {
            GCodeParseEvent event = buffer.get(extrusionBufferIndex);

            if (event instanceof TravelEvent)
            {
                switch (slicerType)
                {
                    case Slic3r:
                        String eventComment = event.getComment();
                        if (eventComment != null)
                        {
                            //TODO Slic3r specific code!
                            if (eventComment.contains("move inwards"))
                            {
                                inwardsMoveIndexList.add(0, extrusionBufferIndex);
                            }
                        }
                }
            }
        }
        return inwardsMoveIndexList;
    }

    class CloseResult
    {

        private final double nozzleStartPosition;
        private final double nozzleCloseOverVolume;

        public CloseResult(double nozzleStartPosition, double nozzleCloseOverVolume)
        {
            this.nozzleStartPosition = nozzleStartPosition;
            this.nozzleCloseOverVolume = nozzleCloseOverVolume;
        }

        public double getNozzleStartPosition()
        {
            return nozzleStartPosition;
        }

        public double getNozzleCloseOverVolume()
        {
            return nozzleCloseOverVolume;
        }
    }

    // If we're here then the last extrusion bounds will not be a perimeter.
    private Optional<CloseResult> closeToEndOfExtrusion(List<ExtrusionBounds> extrusionBoundaries,
            String comment,
            Map<EventType, Integer> eventIndices,
            boolean allowCloseOverPerimeter)
    {
        Optional<CloseResult> closeResult = Optional.empty();
        double nozzleStartPosition = 0;
        double nozzleCloseOverVolume = 0;
        double availableExtrusion = 0;

        for (int extrusionBoundsCount = extrusionBoundaries.size() - 1; extrusionBoundsCount >= 0; extrusionBoundsCount--)
        {
            ExtrusionBounds extrusionBounds = extrusionBoundaries.get(extrusionBoundsCount);

            if ((extrusionBounds.getExtrusionTask() == ExtrusionTask.ExternalPerimeter
                    || extrusionBounds.getExtrusionTask() == ExtrusionTask.Perimeter)
                    && !allowCloseOverPerimeter)
            {
                break;
            }

            availableExtrusion += extrusionBounds.getEExtrusion();
        }

        if (availableExtrusion >= (currentNozzle.getNozzleParameters().getEjectionVolume()
                + currentNozzle.getNozzleParameters().getWipeVolume()))
        {
            //OK - we're go for a normal close with wipe
            nozzleStartPosition = 1.0;
            nozzleCloseOverVolume = currentNozzle.getNozzleParameters().
                    getEjectionVolume();

            insertWipeOverVolume(eventIndices, comment);

            insertVolumeBreak(extrusionBuffer,
                    eventIndices,
                    EventType.NOZZLE_CLOSE_START,
                    currentNozzle.getNozzleParameters().getEjectionVolume()
                    + currentNozzle.getNozzleParameters().getWipeVolume(),
                    comment,
                    FindEventDirection.BACKWARDS_FROM_END);

            closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));

        } else if (availableExtrusion >= currentNozzle.getNozzleParameters().getEjectionVolume())
        {
            //Full open, normal close without wipe
            nozzleStartPosition = 1.0;
            nozzleCloseOverVolume = currentNozzle.getNozzleParameters().getEjectionVolume();

            insertVolumeBreak(extrusionBuffer,
                    eventIndices,
                    EventType.NOZZLE_CLOSE_START,
                    currentNozzle.getNozzleParameters().getEjectionVolume(),
                    comment,
                    FindEventDirection.BACKWARDS_FROM_END);

            closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));
        }

        return closeResult;
    }

    private Optional<CloseResult> closeInwardsFromEndOfPerimeter(List<ExtrusionBounds> extrusionBoundaries,
            TravelEvent lastInwardMoveEvent,
            Map<EventType, Integer> eventIndices,
            final int lastLayerIndex) throws CannotCloseFromPerimeterException, NotEnoughAvailableExtrusionException, NoPerimeterToCloseOverException
    {
        return closeInwardsFrom(extrusionBoundaries, lastInwardMoveEvent, eventIndices, lastLayerIndex, true);
    }

    private Optional<CloseResult> closeInwardsFromEndOfLastExtrusion(List<ExtrusionBounds> extrusionBoundaries,
            TravelEvent lastInwardMoveEvent,
            Map<EventType, Integer> eventIndices,
            final int lastLayerIndex) throws CannotCloseFromPerimeterException, NotEnoughAvailableExtrusionException, NoPerimeterToCloseOverException
    {
        return closeInwardsFrom(extrusionBoundaries, lastInwardMoveEvent, eventIndices, lastLayerIndex, false);
    }

    private Optional<CloseResult> closeInwardsFrom(List<ExtrusionBounds> extrusionBoundaries,
            TravelEvent lastInwardMoveEvent,
            Map<EventType, Integer> eventIndices,
            final int lastLayerIndex,
            boolean closeFromEndOfPerimeter) throws CannotCloseFromPerimeterException, NotEnoughAvailableExtrusionException, NoPerimeterToCloseOverException
    {
        Optional<CloseResult> closeResult = Optional.empty();
        double nozzleStartPosition = 0;
        double nozzleCloseOverVolume = 0;

        ExtrusionBounds externalPerimeterBounds = null;
        ExtrusionBounds finalInnerPerimeterBounds = null;

        for (int extrusionBoundsCount = extrusionBoundaries.size() - 1; extrusionBoundsCount >= 0; extrusionBoundsCount--)
        {
            ExtrusionBounds extrusionBounds = extrusionBoundaries.get(extrusionBoundsCount);

            if (extrusionBounds.getExtrusionTask() == ExtrusionTask.ExternalPerimeter
                    && extrusionBounds.getStartIndex() > lastLayerIndex
                    && extrusionBounds.getEndIndex() > lastLayerIndex)
            {
                externalPerimeterBounds = extrusionBounds;
            } else if (extrusionBounds.getExtrusionTask() == ExtrusionTask.Perimeter
                    && extrusionBounds.getStartIndex() > lastLayerIndex
                    && extrusionBounds.getEndIndex() > lastLayerIndex)
            {
                finalInnerPerimeterBounds = extrusionBounds;
                break;
            }
        }

        ExtrusionBounds boundsContainingInitialExtrusion = null;
        ExtrusionBounds boundsContainingFinalExtrusion = null;
        ExtrusionBounds boundsContainingExtrusionCloseMustStartFrom = extrusionBoundaries.get(extrusionBoundaries.size() - 1);

        double availableExtrusion = 0;

        if (externalPerimeterBounds != null
                && finalInnerPerimeterBounds != null)
        {
            //Can we close on the inner perimeter only?
            if (finalInnerPerimeterBounds.getEExtrusion() >= currentNozzle.getNozzleParameters().getEjectionVolume())
            {
                boundsContainingInitialExtrusion = finalInnerPerimeterBounds;
                boundsContainingFinalExtrusion = finalInnerPerimeterBounds;
                if (closeFromEndOfPerimeter)
                {
                    boundsContainingExtrusionCloseMustStartFrom = externalPerimeterBounds;
                }
                availableExtrusion = boundsContainingInitialExtrusion.getEExtrusion();
            } else if ((finalInnerPerimeterBounds.getEExtrusion()
                    + externalPerimeterBounds.getEExtrusion()) >= currentNozzle.getNozzleParameters().getEjectionVolume())
            {
                boundsContainingInitialExtrusion = finalInnerPerimeterBounds;
                boundsContainingFinalExtrusion = externalPerimeterBounds;
                if (closeFromEndOfPerimeter)
                {
                    boundsContainingExtrusionCloseMustStartFrom = externalPerimeterBounds;
                }
                availableExtrusion = boundsContainingInitialExtrusion.getEExtrusion() + boundsContainingFinalExtrusion.getEExtrusion();
            } else
            {
                throw new NotEnoughAvailableExtrusionException("Not enough available extrusion to close");
            }
        } else if (externalPerimeterBounds != null)
        {
            if (externalPerimeterBounds.getEExtrusion() >= currentNozzle.getNozzleParameters().getEjectionVolume())
            {
                boundsContainingInitialExtrusion = externalPerimeterBounds;
                boundsContainingFinalExtrusion = externalPerimeterBounds;
                if (closeFromEndOfPerimeter)
                {
                    boundsContainingExtrusionCloseMustStartFrom = externalPerimeterBounds;
                }
                availableExtrusion = boundsContainingInitialExtrusion.getEExtrusion();
            } else
            {
                throw new NotEnoughAvailableExtrusionException("Not enough available extrusion to close");
            }
        } else if (finalInnerPerimeterBounds != null)
        {
            if (finalInnerPerimeterBounds.getEExtrusion() >= currentNozzle.getNozzleParameters().getEjectionVolume())
            {
                boundsContainingInitialExtrusion = finalInnerPerimeterBounds;
                boundsContainingFinalExtrusion = finalInnerPerimeterBounds;
                if (closeFromEndOfPerimeter)
                {
                    boundsContainingExtrusionCloseMustStartFrom = finalInnerPerimeterBounds;
                }
                availableExtrusion = boundsContainingInitialExtrusion.getEExtrusion();
            } else
            {
                throw new NotEnoughAvailableExtrusionException("Not enough available extrusion to close");
            }
        } else
        {
            throw new NoPerimeterToCloseOverException("No perimeter");
        }

        if (availableExtrusion >= currentNozzle.getNozzleParameters().getEjectionVolume())
        {
            nozzleStartPosition = 1.0;
            nozzleCloseOverVolume = currentNozzle.getNozzleParameters().getEjectionVolume();

            try
            {
                int startOfClose = closeTowardsInnerPerimeter(
                        boundsContainingInitialExtrusion,
                        boundsContainingFinalExtrusion,
                        boundsContainingExtrusionCloseMustStartFrom,
                        "Close towards perimeter",
                        lastInwardMoveEvent,
                        currentNozzle.getNozzleParameters().getEjectionVolume(),
                        closeFromEndOfPerimeter,
                        !closeFromEndOfPerimeter
                );

                eventIndices.put(EventType.NOZZLE_CLOSE_START, startOfClose);

                closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));
            } catch (PostProcessingError ex)
            {
                throw new CannotCloseFromPerimeterException(ex.getMessage());
            }
        }

        return closeResult;
    }

    private Optional<CloseResult> partialOpenAndCloseAtEndOfExtrusion(List<ExtrusionBounds> extrusionBoundaries,
            Map<EventType, Integer> eventIndices)
    {
        Optional<CloseResult> closeResult = Optional.empty();
        double nozzleStartPosition = 0;
        double nozzleCloseOverVolume = 0;

        double extrusionForCompletePath = 0;

        for (ExtrusionBounds extrusionBounds : extrusionBoundaries)
        {
            extrusionForCompletePath += extrusionBounds.getEExtrusion();
        }

        // We shouldn't have been asked to partial open - there is more than the ejection volume of material available
        if (extrusionForCompletePath > currentNozzle.getNozzleParameters().getEjectionVolume())
        {
            return closeResult;
        }

        double bValue = Math.min(1, extrusionForCompletePath
                / currentNozzle.getNozzleParameters().
                getEjectionVolume());

        bValue = Math.max(bValue, currentNozzle.getNozzleParameters().getPartialBMinimum());

        nozzleStartPosition = bValue;
        nozzleCloseOverVolume = extrusionForCompletePath;

        replaceOpenNozzleWithPartialOpen(bValue);

        eventIndices.put(EventType.NOZZLE_CLOSE_START, extrusionBoundaries.get(0).getStartIndex());
        extrusionBuffer.get(extrusionBoundaries.get(0).getStartIndex()).setComment(
                "Partial open and close at end of extrusion");

        closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));

        return closeResult;
    }

    private Optional<CloseResult> reverseCloseFromEndOfExtrusion(List<ExtrusionBounds> extrusionBoundaries,
            Map<EventType, Integer> eventIndices,
            final int lastLayerIndex) throws PostProcessingError, NotEnoughAvailableExtrusionException
    {
        Optional<CloseResult> closeResult = Optional.empty();
        double nozzleStartPosition = 0;
        double nozzleCloseOverVolume = 0;

        double availableExtrusion = 0;

        ExtrusionBounds boundsContainingInitialExtrusion = null;
        ExtrusionBounds boundsContainingFinalExtrusion = null;
        ExtrusionBounds boundsContainingExtrusionCloseMustStartFrom = null;

        for (int extrusionBoundCounter = extrusionBoundaries.size() - 1; extrusionBoundCounter >= 0; extrusionBoundCounter--)
        {
            ExtrusionBounds extrusionBounds = extrusionBoundaries.get(extrusionBoundCounter);
            if (lastLayerIndex >= 0
                    && lastLayerIndex >= extrusionBounds.getStartIndex()
                    && lastLayerIndex <= extrusionBounds.getEndIndex())
            {
                break;
            }

            availableExtrusion += extrusionBounds.getEExtrusion();

            if (boundsContainingFinalExtrusion == null)
            {
                boundsContainingFinalExtrusion = extrusionBounds;
            }

            boundsContainingInitialExtrusion = extrusionBounds;
        }

        boundsContainingExtrusionCloseMustStartFrom = boundsContainingFinalExtrusion;

        if (availableExtrusion >= currentNozzle.getNozzleParameters().getEjectionVolume())
        {
            nozzleStartPosition = 1.0;
            nozzleCloseOverVolume = currentNozzle.getNozzleParameters().getEjectionVolume();

            copyExtrusionEvents(nozzleCloseOverVolume,
                    boundsContainingExtrusionCloseMustStartFrom.getEndIndex(),
                    boundsContainingInitialExtrusion.getStartIndex(),
                    boundsContainingFinalExtrusion.getEndIndex(),
                    boundsContainingExtrusionCloseMustStartFrom.getEndIndex(),
                    null,
                    "",
                    "Reverse close",
                    boundsContainingExtrusionCloseMustStartFrom.getEndIndex() + 1,
                    -1);

            eventIndices.put(EventType.NOZZLE_CLOSE_START, boundsContainingExtrusionCloseMustStartFrom.getEndIndex() + 1);

            closeResult = Optional.of(new CloseResult(nozzleStartPosition, nozzleCloseOverVolume));
        } else
        {
            throw new NotEnoughAvailableExtrusionException("Not enough extrusion when attempting to reverse close");
        }

        return closeResult;
    }

    protected void writeEventsWithNozzleClose(String comment) throws PostProcessingError
    {
        boolean closeAtEndOfPath = false;

        Map<EventType, Integer> eventIndices = new EnumMap<EventType, Integer>(EventType.class);

        double nozzleStartPosition = 1.0;
        double nozzleCloseOverVolume = 1;

        if ((currentNozzle.getNozzleParameters()
                .getPreejectionVolume() == 0
                && currentNozzle.getNozzleParameters().getEjectionVolume() == 0
                && currentNozzle.getNozzleParameters().getWipeVolume() == 0)
                && extrusionBuffer.containsExtrusionEvents())
        {
            // Write the extrudes unchanged
            for (GCodeParseEvent extrusionEvent : extrusionBuffer)
            {
                writeEventToFile(extrusionEvent);
            }

            extrusionBuffer.clear();

            // Now write a close nozzle at the end of the path
            NozzleCloseFullyEvent closeNozzle = new NozzleCloseFullyEvent();
            closeNozzle.setComment(comment);
            closeNozzle.setLength(0);
            writeEventToFile(closeNozzle);
            currentNozzle.closeNozzleFully();
        } else if (extrusionBuffer.containsExtrusionEvents())
        {
            List<Integer> inwardsMoveIndexList = getInwardMoves(extrusionBuffer);

            //Remove any inward moves that were found (only happens with Slic3r)
            TravelEvent lastInwardMoveEvent = null;

            if (!inwardsMoveIndexList.isEmpty())
            {
                int tempLastInwardsMoveIndex = inwardsMoveIndexList.get(inwardsMoveIndexList.size()
                        - 1);

                lastInwardMoveEvent = (TravelEvent) extrusionBuffer.
                        get(tempLastInwardsMoveIndex);

                for (int i = inwardsMoveIndexList.size() - 1; i >= 0; i--)
                {
                    int indexToRemove = inwardsMoveIndexList.get(i);
                    extrusionBuffer.remove(indexToRemove);
                }
            }

            ExtrusionBufferDigest bufferDigest = getExtrusionBufferDigest(extrusionBuffer);
            List<ExtrusionBounds> extrusionBoundaries = bufferDigest.getExtrusionBoundaries();

            if (extrusionBoundaries.size() > 0)
            {
                ExtrusionBounds lastExtrusionBounds = extrusionBoundaries.get(extrusionBoundaries.size() - 1);
                Optional<CloseResult> closeResult = Optional.empty();

                boolean failedToCloseOverNonPerimeter = false;

                if (lastExtrusionBounds.getExtrusionTask() != ExtrusionTask.ExternalPerimeter
                        && lastExtrusionBounds.getExtrusionTask() != ExtrusionTask.Perimeter)
                {
                    //Try closing towards the end of the extrusion
                    closeResult = closeToEndOfExtrusion(extrusionBoundaries, comment, eventIndices, false);
                    failedToCloseOverNonPerimeter = !closeResult.isPresent();
                }

                if (!closeResult.isPresent())
                {
                    try
                    {
                        if (failedToCloseOverNonPerimeter)
                        {
                            closeResult = closeInwardsFromEndOfLastExtrusion(extrusionBoundaries,
                                    lastInwardMoveEvent,
                                    eventIndices,
                                    bufferDigest.getLastLayerEvent());
                        } else
                        {
                            closeResult = closeInwardsFromEndOfPerimeter(extrusionBoundaries,
                                    lastInwardMoveEvent,
                                    eventIndices,
                                    bufferDigest.getLastLayerEvent());
                        }
                    } catch (CannotCloseFromPerimeterException ex)
                    {
                        steno.warning("Close failed: " + ex.getMessage());
                    } catch (NotEnoughAvailableExtrusionException ex)
                    {
                        closeResult = partialOpenAndCloseAtEndOfExtrusion(extrusionBoundaries, eventIndices);

                        if (!closeResult.isPresent())
                        {
                            //Fallback to close to end
                            closeResult = closeToEndOfExtrusion(extrusionBoundaries, comment, eventIndices, true);
                        }
                    } catch (NoPerimeterToCloseOverException ex)
                    {
                        try
                        {
                            closeResult = reverseCloseFromEndOfExtrusion(extrusionBoundaries, eventIndices, bufferDigest.getLastLayerEvent());
                        } catch (NotEnoughAvailableExtrusionException ex2)
                        {
                            closeResult = partialOpenAndCloseAtEndOfExtrusion(extrusionBoundaries, eventIndices);
                        }
                    }
                }

                if (!closeResult.isPresent())
                {
                    throw new PostProcessingError("Failed to close nozzle - layer " + layerIndex);
                }

                nozzleCloseOverVolume = closeResult.get().getNozzleCloseOverVolume();
                nozzleStartPosition = closeResult.get().getNozzleStartPosition();
            }

            if (eventIndices.containsKey(EventType.NOZZLE_CLOSE_START) || closeAtEndOfPath)
            {
                int foundRetractDuringExtrusion = -1;
                int foundNozzleChange = -1;
                double currentNozzlePosition = nozzleStartPosition;
                double currentFeedrate = 0;

                int minimumSearchIndex = 0;

                if (eventIndices.containsKey(EventType.WIPE_START))
                {
                    minimumSearchIndex = eventIndices.get(EventType.WIPE_START);
                }

                for (int tSearchIndex = extrusionBuffer.size() - 1;
                        tSearchIndex > minimumSearchIndex; tSearchIndex--)
                {

                    GCodeParseEvent event = extrusionBuffer.get(
                            tSearchIndex);
                    if (event instanceof RetractDuringExtrusionEvent
                            && foundRetractDuringExtrusion < 0)
                    {
                        foundRetractDuringExtrusion = tSearchIndex;
                    }

                    if (event instanceof NozzleChangeEvent
                            && foundRetractDuringExtrusion >= 0)
                    {
                        foundNozzleChange = tSearchIndex;
                        break;
                    }
                }

                if (foundNozzleChange >= 0
                        && foundRetractDuringExtrusion >= 0)
                {
                    NozzleChangeEvent eventToMove = (NozzleChangeEvent) extrusionBuffer.get(
                            foundNozzleChange);
                    extrusionBuffer.remove(foundNozzleChange);
                    extrusionBuffer.add(foundRetractDuringExtrusion,
                            eventToMove);
                }

                int nozzleOpenEndIndex = -1;
                if (eventIndices.containsKey(EventType.NOZZLE_OPEN_END))
                {
                    nozzleOpenEndIndex = eventIndices.get(
                            EventType.NOZZLE_OPEN_END);

                    currentNozzlePosition = 0;
                }

                int preCloseStarveIndex = -1;
                if (eventIndices.containsKey(EventType.PRE_CLOSE_STARVATION_START))
                {
                    preCloseStarveIndex = eventIndices.get(
                            EventType.PRE_CLOSE_STARVATION_START);
                }

                int nozzleCloseStartIndex = -1;
                if (eventIndices.containsKey(EventType.NOZZLE_CLOSE_START))
                {
                    nozzleCloseStartIndex = eventIndices.get(EventType.NOZZLE_CLOSE_START);
                }

                int nozzleCloseMidpointIndex = -1;
                if (eventIndices.containsKey(EventType.NOZZLE_CLOSE_MIDPOINT))
                {
                    nozzleCloseMidpointIndex = eventIndices.get(
                            EventType.NOZZLE_CLOSE_MIDPOINT);
                }

                int wipeIndex = -1;
                if (eventIndices.containsKey(EventType.WIPE_START))
                {
                    wipeIndex = eventIndices.get(EventType.WIPE_START);
                }

                for (int eventWriteIndex = 0; eventWriteIndex
                        < extrusionBuffer.size(); eventWriteIndex++)
                {
                    GCodeParseEvent candidateevent = extrusionBuffer.get(
                            eventWriteIndex);

                    // Check that the replenish is not too big
                    if (candidateevent instanceof NozzleOpenFullyEvent)
                    {
                        NozzleOpenFullyEvent event = ((NozzleOpenFullyEvent) candidateevent);
                        if (MathUtils.compareDouble(event.getE(),
                                lastNozzle.getNozzleParameters().getEjectionVolume()
                                + lastNozzle.getNozzleParameters().getWipeVolume(),
                                0.01) == MathUtils.MORE_THAN)
                        {
                            steno.warning("E value exceeds maximum on full open " + event.getE() + " - layer " + layerIndex);
                            event.setComment(event.getComment().concat(" - E exceeds maximum"));
                        }
                    }

                    if (candidateevent instanceof NozzleChangeBValueEvent)
                    {
                        NozzleChangeBValueEvent event = ((NozzleChangeBValueEvent) candidateevent);
                        if (MathUtils.compareDouble(event.getE(),
                                lastNozzle.getNozzleParameters().getEjectionVolume()
                                + lastNozzle.getNozzleParameters().getWipeVolume(),
                                0.01) == MathUtils.MORE_THAN)
                        {
                            steno.warning("E value exceeds maximum on nozzle position change " + event.getE() + " - layer " + layerIndex);
                            event.setComment(event.getComment().concat(" - E exceeds maximum"));
                        }
                    }

                    if (candidateevent instanceof UnretractEvent)
                    {
                        UnretractEvent event = ((UnretractEvent) candidateevent);
                        if (MathUtils.compareDouble(event.getE(),
                                lastNozzle.getNozzleParameters().getEjectionVolume()
                                + lastNozzle.getNozzleParameters().getWipeVolume(),
                                0.01) == MathUtils.MORE_THAN)
                        {
                            steno.warning("E value exceeds maximum on unretract " + event.getE() + " - layer " + layerIndex);
                            event.setComment(event.getComment().concat(" - E exceeds maximum"));
                        }
                    }

                    if (candidateevent.getFeedRate() > 0)
                    {
                        currentFeedrate = candidateevent.getFeedRate();
                    }

                    if (candidateevent.getLength() > 0
                            && currentFeedrate > 0)
                    {
                        double timePerEvent = candidateevent.getLength()
                                / currentFeedrate * 60d;
                        predictedDurationInLayer += timePerEvent;
                        distanceSoFarInLayer += candidateevent.getLength();
                    }

                    if (candidateevent instanceof RetractEvent)
                    {
                        volumeUsed += ((RetractEvent) candidateevent).getE();
                    } else if (candidateevent instanceof UnretractEvent)
                    {
                        volumeUsed += ((UnretractEvent) candidateevent).getE();
                    } else if (candidateevent instanceof RetractDuringExtrusionEvent)
                    {
                        volumeUsed += ((RetractDuringExtrusionEvent) candidateevent).getE();
                    }

                    if (candidateevent instanceof LayerChangeEvent)
                    {
                        if (mixExtruderOutputs)
                        {
                            if (layer == mixFromLayer)
                            {
                                currentEMixValue = startingEMixValue;
                            } else if (layer == mixToLayer)
                            {
                                currentEMixValue = endEMixValue;

                                if (currentMixPoint
                                        < extruderMixPoints.size() - 1)
                                {
                                    ExtruderMix firstMixPoint = extruderMixPoints.get(
                                            currentMixPoint);
                                    startingEMixValue = firstMixPoint.getEFactor();
                                    startingDMixValue = firstMixPoint.getDFactor();
                                    mixFromLayer = firstMixPoint.getLayerNumber();

                                    currentMixPoint++;
                                    ExtruderMix secondMixPoint = extruderMixPoints.get(
                                            currentMixPoint);
                                    endEMixValue = secondMixPoint.getEFactor();
                                    endDMixValue = secondMixPoint.getDFactor();
                                    mixToLayer = secondMixPoint.getLayerNumber();
                                }
                            } else if (layer > mixFromLayer && layer
                                    < mixToLayer)
                            {
                                // Mix the values
                                int layerSpan = mixToLayer
                                        - mixFromLayer;
                                double layerRatio = (layer
                                        - mixFromLayer) / (double) layerSpan;
                                double eSpan = endEMixValue
                                        - startingEMixValue;
                                double dSpan = endDMixValue
                                        - startingDMixValue;
                                currentEMixValue = startingEMixValue
                                        + (layerRatio * eSpan);
                            }
                            currentDMixValue = 1 - currentEMixValue;
                        }

                        layerIndex++;
                        layerNumberToLineNumber.add(layerIndex,
                                outputWriter.getNumberOfLinesOutput());
                        layerNumberToDistanceTravelled.add(layerIndex,
                                distanceSoFarInLayer);
                        layerNumberToPredictedDuration.add(layerIndex,
                                predictedDurationInLayer);
                        distanceSoFarInLayer = 0;
                        predictedDurationInLayer = 0;

                    }

                    if (candidateevent instanceof MovementEvent)
                    {
                        lastProcessedMovementEvent = (MovementEvent) candidateevent;
                    }

                    if (candidateevent instanceof ExtrusionEvent)
                    {
                        ExtrusionEvent event = (ExtrusionEvent) candidateevent;

                        if (eventWriteIndex == wipeIndex
                                && eventWriteIndex == nozzleCloseStartIndex)
                        {
                            // No extrusion
                            // Proportional B value
                            NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                            nozzleEvent.setX(event.getX());
                            nozzleEvent.setY(event.getY());
                            nozzleEvent.setLength(event.getLength());
                            nozzleEvent.setFeedRate(event.getFeedRate());
                            nozzleEvent.setComment(
                                    event.getComment()
                                    + " after start of close");
                            nozzleStartPosition = 0;
                            nozzleEvent.setB(0);
                            nozzleEvent.setNoExtrusionFlag(true);
                            // Set E and D so we have a record of the elided extrusion
                            nozzleEvent.setE(event.getE());
                            nozzleEvent.setD(event.getD());

                            writeEventToFile(nozzleEvent);
                            if (mixExtruderOutputs)
                            {
                                autoUnretractEValue += event.getE()
                                        * currentEMixValue;
                                autoUnretractDValue += event.getE()
                                        * currentDMixValue;
                            } else
                            {
                                autoUnretractEValue += event.getE() + event.getD();
                            }
                        } else if (eventWriteIndex <= nozzleOpenEndIndex
                                && nozzleOpenEndIndex != -1)
                        {
                            // Normal extrusion plus auto unretract
                            // Proportional B value
                            NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                            nozzleEvent.setX(event.getX());
                            nozzleEvent.setY(event.getY());
                            nozzleEvent.setLength(event.getLength());
                            nozzleEvent.setFeedRate(event.getFeedRate());

                            nozzleEvent.setComment("Normal open");
                            currentNozzlePosition = currentNozzlePosition
                                    + (event.getE()
                                    / currentNozzle.getNozzleParameters().getOpenOverVolume());

                            if (compareDouble(currentNozzlePosition, 1, 10e-5)
                                    == EQUAL
                                    || currentNozzlePosition > 1)
                            {
                                currentNozzlePosition = 1;
                            }
                            nozzleEvent.setB(currentNozzlePosition);
                            nozzleEvent.setE(event.getE());
                            nozzleEvent.setD(event.getD());
                            writeEventToFile(nozzleEvent);
                        } else if (wipeIndex != -1 && eventWriteIndex >= wipeIndex)
                        {
                            outputNoBNoE(event, "Wipe");
                        } else if ((nozzleCloseStartIndex >= 0 && eventWriteIndex
                                >= nozzleCloseStartIndex)
                                && (nozzleCloseMidpointIndex == -1
                                || eventWriteIndex < nozzleCloseMidpointIndex))
                        {
                            // No extrusion
                            // Proportional B value
                            NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                            nozzleEvent.setX(event.getX());
                            nozzleEvent.setY(event.getY());
                            nozzleEvent.setLength(event.getLength());
                            nozzleEvent.setFeedRate(event.getFeedRate());

                            if (nozzleCloseMidpointIndex == -1)
                            {
                                String commentToOutput = ((event.getComment() == null) ? "" : event.
                                        getComment()) + " Linear close";
                                nozzleEvent.setComment(commentToOutput);
                                currentNozzlePosition = currentNozzlePosition
                                        - (nozzleStartPosition * (event.getE()
                                        / nozzleCloseOverVolume));
                            } else
                            {
                                nozzleEvent.setComment(event.getComment()
                                        + " Differential close - part 1");
                                currentNozzlePosition = currentNozzlePosition
                                        - (nozzleStartPosition
                                        * (1
                                        - currentNozzle.getNozzleParameters().
                                        getOpenValueAtMidPoint()) * (event.getE()
                                        / (nozzleCloseOverVolume
                                        * (currentNozzle.getNozzleParameters().
                                        getMidPointPercent()
                                        / 100.0))));
                            }
                            if (compareDouble(currentNozzlePosition, 0.07, 0.001)
                                    == MathUtils.LESS_THAN
                                    || currentNozzlePosition < 0)
                            {
                                currentNozzlePosition = 0;
                            }
                            nozzleEvent.setB(currentNozzlePosition);
                            nozzleEvent.setNoExtrusionFlag(true);
                            // Set E and D so we have a record of the elided extrusion
                            nozzleEvent.setE(event.getE());
                            nozzleEvent.setD(event.getD());

                            writeEventToFile(nozzleEvent);
                            if (mixExtruderOutputs)
                            {
                                autoUnretractEValue += event.getE()
                                        * currentEMixValue;
                                autoUnretractDValue += event.getE()
                                        * currentDMixValue;
                            } else
                            {
                                autoUnretractEValue += event.getE() + event.getD();
                            }
                        } else if (nozzleCloseMidpointIndex != -1
                                && eventWriteIndex >= nozzleCloseMidpointIndex)
                        {
                            // No extrusion
                            // Proportional B value
                            NozzlePositionChangeEvent nozzleEvent = new NozzlePositionChangeEvent();
                            nozzleEvent.setX(event.getX());
                            nozzleEvent.setY(event.getY());
                            nozzleEvent.setLength(event.getLength());
                            nozzleEvent.setFeedRate(event.getFeedRate());
                            nozzleEvent.setComment("Differential close - part 2");
                            currentNozzlePosition = currentNozzlePosition
                                    - (nozzleStartPosition
                                    * currentNozzle.getNozzleParameters().
                                    getOpenValueAtMidPoint()
                                    * (event.getE()
                                    / (nozzleCloseOverVolume * (1
                                    - (currentNozzle.getNozzleParameters().getMidPointPercent()
                                    / 100.0)))));
                            if (compareDouble(currentNozzlePosition, 0, 10e-5)
                                    == EQUAL
                                    || currentNozzlePosition < 0)
                            {
                                currentNozzlePosition = 0;
                            }
                            nozzleEvent.setB(currentNozzlePosition);
                            nozzleEvent.setNoExtrusionFlag(true);
                            // Set E and D so we have a record of the elided extrusion
                            nozzleEvent.setE(event.getE());
                            nozzleEvent.setD(event.getD());

                            writeEventToFile(nozzleEvent);
                            if (mixExtruderOutputs)
                            {
                                autoUnretractEValue += event.getE()
                                        * currentEMixValue;
                                autoUnretractDValue += event.getE()
                                        * currentDMixValue;
                            } else
                            {
                                autoUnretractEValue += event.getE() + event.getD();
                            }
                        } else if (preCloseStarveIndex != -1
                                && eventWriteIndex >= preCloseStarveIndex)
                        {
                            outputNoBNoE(event, "Pre-close starvation - eliding " + event.
                                    getE()
                                    + event.getD());
                        } else
                        {
                            volumeUsed += event.getE();
                            event.setD(event.getE() * currentDMixValue);
                            event.setE(event.getE() * currentEMixValue);
                            writeEventToFile(event);
                        }
                    } else
                    {
                        writeEventToFile(candidateevent);
                        if (candidateevent instanceof NozzleChangeEvent)
                        {
                            setCurrentNozzle(((NozzleChangeEvent) candidateevent).getNozzleNumber());
                            closeCounter = 0;
                        } else if (candidateevent instanceof NozzleChangeBValueEvent)
                        {
                            currentNozzlePosition = ((NozzleChangeBValueEvent) candidateevent).getB();
                        }
                    }
                }

                lastNozzle = currentNozzle;

                extrusionBuffer.clear();

                currentNozzle.closeNozzleFully();

                // Determine whether to insert a nozzle reselect at the end of this extrusion path
                if (closeCounter >= triggerNozzleReselectAfterNCloses)
                {
                    if (triggerNozzleReselectAfterNCloses >= 0)
                    {
                        NozzleChangeEvent nozzleReselect = new NozzleChangeEvent();
                        nozzleReselect.setComment("Reselect nozzle");
                        nozzleReselect.setNozzleNumber(currentNozzle.getNozzleReferenceNumber());
                        writeEventToFile(nozzleReselect);
                    }
                    closeCounter = 0;
                } else
                {
                    closeCounter++;
                }

                // Always output an M109 after nozzle close
                // Required to ensure that print temperature is maintained if nozzle heater inhibit is active
                MCodeEvent m109Event = new MCodeEvent();
                m109Event.setMNumber(109);
                writeEventToFile(m109Event);

            } else if (extrusionBuffer.size() > 0 && extrusionBuffer.containsExtrusionEvents())
            {
                CommentEvent failureComment = new CommentEvent();
                failureComment.setComment(
                        "Error locating start / end of close");
                writeEventToFile(failureComment);
                throw new PostProcessingError("Didn't locate start / end of close");
            } else
            {
                // Pass through the events - no extrusions to deal with
                for (GCodeParseEvent event : extrusionBuffer)
                {
                    writeEventToFile(event);
                    if (event instanceof NozzleChangeEvent)
                    {
                        setCurrentNozzle(((NozzleChangeEvent) event).getNozzleNumber());
                    }
                }
            }
        }
    }

    private void insertPreejectionOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        // Calculate the pre-ejection point (if we need to...)
        if (currentNozzle.getNozzleParameters().getPreejectionVolume() > 0)
        {
            insertVolumeBreak(extrusionBuffer,
                    eventIndices,
                    EventType.PRE_CLOSE_STARVATION_START,
                    currentNozzle.getNozzleParameters().
                    getPreejectionVolume()
                    + currentNozzle.getNozzleParameters().
                    getEjectionVolume()
                    + currentNozzle.getNozzleParameters().getWipeVolume(),
                    comment,
                    FindEventDirection.BACKWARDS_FROM_END);
        }
    }

    private void insertWipeOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        // Calculate the wipe point (if we need to...)
        if (currentNozzle.getNozzleParameters().getWipeVolume() > 0)
        {

            insertVolumeBreak(extrusionBuffer,
                    eventIndices,
                    EventType.WIPE_START,
                    currentNozzle.getNozzleParameters().getWipeVolume(),
                    comment,
                    FindEventDirection.BACKWARDS_FROM_END);
        }
    }

    private boolean insertOpenNozzleOverVolume(Map<EventType, Integer> eventIndices, String comment)
    {
        boolean insertAttempt = false;

        if (currentNozzle.getNozzleParameters().getOpenOverVolume() > 0)
        {

            insertVolumeBreak(extrusionBuffer,
                    eventIndices,
                    EventType.NOZZLE_OPEN_END,
                    currentNozzle.getNozzleParameters().
                    getOpenOverVolume(),
                    comment,
                    FindEventDirection.FORWARDS_FROM_START);

            insertAttempt = true;
        }

        return insertAttempt;
    }

    protected int closeTowardsInnerPerimeter(final ExtrusionBounds initialExtrusionBounds,
            final ExtrusionBounds finalExtrusionBounds,
            final ExtrusionBounds boundsContainingExtrusionToCloseFrom,
            final String originalComment,
            TravelEvent lastInwardsMoveEvent,
            double targetVolume,
            boolean intersectOrthogonally,
            boolean closeOverLastExtrusionBoundary) throws PostProcessingError, CannotCloseFromPerimeterException, NoPerimeterToCloseOverException
    {
        boolean reverseWipePath = false;

        int indexOfFirstCopiedEvent = boundsContainingExtrusionToCloseFrom.getEndIndex() + 1;
        int startOfClose = -1;
        int closestEventIndex = -1;

        Segment segmentToIntersectWith = null;
        Vector2D segmentToIntersectWithMeasurementPoint = null;
        Vector2D lastPointConsidered = null;

        TreeMap<Double, Integer> intersectedPointDistances = new TreeMap<>();

        int intersectionCounter = 0;
        int maxNumberOfIntersectionsToConsider = currentSettings.getNumberOfPerimeters();
        float maxDistanceFromEndPoint = currentSettings.getPerimeterExtrusionWidth_mm()
                * 1.01f * maxNumberOfIntersectionsToConsider;

        int closeFromThisEventIndex = boundsContainingExtrusionToCloseFrom.getEndIndex();
        ExtrusionEvent closeFromThisEvent = (ExtrusionEvent) extrusionBuffer.
                get(closeFromThisEventIndex);

        Vector2D endOfExtrusion = new Vector2D(closeFromThisEvent.getX(),
                closeFromThisEvent.getY());

        // Attempt to use the inwards move to find the innermost perimeter
        if (lastInwardsMoveEvent != null)
        {
            Vector2D inwardsMoveEndPoint = new Vector2D(lastInwardsMoveEvent.getX(),
                    lastInwardsMoveEvent.getY());
            inwardsMoveEndPoint.scalarMultiply(4);
            segmentToIntersectWith = new Segment(endOfExtrusion, inwardsMoveEndPoint, new Line(
                    endOfExtrusion, inwardsMoveEndPoint, 1e-12));

            segmentToIntersectWithMeasurementPoint = inwardsMoveEndPoint;
        } else
        {
            int absolutelyTheLastMovementEventIndexEver = extrusionBuffer.
                    getPreviousMovementEventIndex(closeFromThisEventIndex);
            MovementEvent absolutelyTheLastMovementEventEver = (MovementEvent) extrusionBuffer.
                    get(absolutelyTheLastMovementEventIndexEver);
            Vector2D absolutelyTheLastMovementVectorEver = new Vector2D(
                    absolutelyTheLastMovementEventEver.getX(),
                    absolutelyTheLastMovementEventEver.getY());

            if (intersectOrthogonally)
            {
                segmentToIntersectWith = MathUtils.getOrthogonalLineToLinePoints(
                        maxDistanceFromEndPoint, absolutelyTheLastMovementVectorEver, endOfExtrusion);

                segmentToIntersectWithMeasurementPoint = MathUtils.findMidPoint(segmentToIntersectWith.getStart(),
                        segmentToIntersectWith.getEnd());
            } else
            {
                int ultimateMovementEventIndex = closeFromThisEventIndex;
                MovementEvent ultimateMovementEvent = (MovementEvent) extrusionBuffer.
                        get(ultimateMovementEventIndex);
                Vector2D ultimateMovementVector = new Vector2D(
                        ultimateMovementEvent.getX(),
                        ultimateMovementEvent.getY());

                int penultimateMovementEventIndex = extrusionBuffer.
                        getPreviousMovementEventIndex(ultimateMovementEventIndex);
                MovementEvent penultimateMovementEvent = (MovementEvent) extrusionBuffer.
                        get(penultimateMovementEventIndex);
                Vector2D penultimateMovementVector = new Vector2D(
                        penultimateMovementEvent.getX(),
                        penultimateMovementEvent.getY());

                Vector2D normalisedVectorToEndOfExtrusion = ultimateMovementVector.subtract(penultimateMovementVector).normalize();
                Vector2D scaledVectorToEndOfExtrusion = normalisedVectorToEndOfExtrusion.scalarMultiply(maxDistanceFromEndPoint);

                Vector2D segmentEndPoint = ultimateMovementVector.add(scaledVectorToEndOfExtrusion);

                Line intersectionLine = new Line(ultimateMovementVector, segmentEndPoint, 1e-12);
                segmentToIntersectWith = new Segment(ultimateMovementVector, segmentEndPoint, intersectionLine);

                segmentToIntersectWithMeasurementPoint = ultimateMovementVector;
            }
        }

        //Prime the last movement if we can...
        int indexToBeginSearchAt = initialExtrusionBounds.getStartIndex();
        int indexOfPriorMovement = extrusionBuffer.getPreviousMovementEventIndex(
                indexToBeginSearchAt);

        if (indexOfPriorMovement >= 0)
        {
            MovementEvent priorMovementEvent = (MovementEvent) extrusionBuffer.get(
                    indexOfPriorMovement);
            lastPointConsidered = new Vector2D(priorMovementEvent.getX(),
                    priorMovementEvent.getY());

        } else
        {
            indexOfPriorMovement = extrusionBuffer.getPreviousEventIndex(
                    indexToBeginSearchAt, LayerChangeWithTravelEvent.class
            );

            if (indexOfPriorMovement
                    >= 0)
            {
                LayerChangeWithTravelEvent priorMovementEvent = (LayerChangeWithTravelEvent) extrusionBuffer.
                        get(indexOfPriorMovement);
                lastPointConsidered = new Vector2D(priorMovementEvent.getX(),
                        priorMovementEvent.getY());
            }
        }

        for (int eventIndex = indexToBeginSearchAt;
                intersectionCounter <= maxNumberOfIntersectionsToConsider
                && eventIndex <= finalExtrusionBounds.getEndIndex();
                eventIndex++)
        {
            if (extrusionBuffer.get(eventIndex) instanceof MovementEvent)
            {
                MovementEvent thisMovementEvent = (MovementEvent) extrusionBuffer.get(eventIndex);
                Vector2D thisMovement = new Vector2D(thisMovementEvent.getX(),
                        thisMovementEvent.getY());

                if (extrusionBuffer.get(eventIndex) instanceof ExtrusionEvent
                        && lastPointConsidered != null)
                {
                    // Detect intersections
                    Segment segmentUnderConsideration = new Segment(lastPointConsidered,
                            thisMovement, new Line(
                                    lastPointConsidered,
                                    thisMovement, 1e-12));
                    Vector2D intersectionPoint = MathUtils.getSegmentIntersection(
                            segmentToIntersectWith, segmentUnderConsideration);
                    if (intersectionPoint != null)
                    {
                        double distanceFromMidPoint = intersectionPoint.distance(
                                segmentToIntersectWithMeasurementPoint);

//                                if (distanceFromEndPoint <= maxDistanceFromEndPoint)
//                                {
                        intersectedPointDistances.put(distanceFromMidPoint, eventIndex);
                        intersectionCounter++;
//                                }
                    }
                }

                lastPointConsidered = thisMovement;
            }
        }

        if (intersectedPointDistances.size() > 0)
        {
            //TreeMaps are sorted
            // Use the farthest point if we're intersecting orthogonally (which should yield the innermost point)
            // Use the nearest point if we're intersecting extending from the end of the extrusion

            if (intersectOrthogonally)
            {
                closestEventIndex = (int) intersectedPointDistances.values().toArray()[intersectedPointDistances.size() - 1];
            } else
            {
                closestEventIndex = (int) intersectedPointDistances.values().toArray()[0];
            }
        }

        if (closestEventIndex < 0)
        {
            throw new NoPerimeterToCloseOverException("Couldn't find inner perimeter for close - defaulting to reverse. Got up to line "
                    + extrusionBuffer.get(extrusionBuffer.size() - 1).getLinesSoFar());
        }

        String additionalComment = "";

        boolean reverseAlongPath = true;

        // Determine whether we need to go forward or backwards
        // Prefer backwards moves if there's enough space
        if (closestEventIndex >= 0)
        {
            boolean forwardsSearch = true;

            double forwardsVolume = 0;
            double reverseVolume = 0;

            for (int iterations = 0; iterations <= 1; iterations++)
            {
                double volumeTotal = 0;

                //Count up the available volume - forwards first
                for (int movementIndex = closestEventIndex;
                        movementIndex >= initialExtrusionBounds.getStartIndex() && movementIndex
                        <= finalExtrusionBounds.getEndIndex();
                        movementIndex += ((forwardsSearch) ? 1 : -1))
                {
                    GCodeParseEvent eventUnderExamination = extrusionBuffer.get(movementIndex);
                    if (eventUnderExamination instanceof ExtrusionEvent)
                    {
                        volumeTotal += ((ExtrusionEvent) eventUnderExamination).getE()
                                + ((ExtrusionEvent) eventUnderExamination).getD();
                    }
                }

                if (forwardsSearch)
                {
                    forwardsVolume = volumeTotal;
                } else
                {
                    reverseVolume = volumeTotal;
                }

                forwardsSearch = !forwardsSearch;
            }

            if (reverseVolume >= targetVolume
                    || forwardsVolume <= reverseVolume)
            {
                reverseAlongPath = true;
            } else
            {
                reverseAlongPath = false;
            }
        }

        int lowestIndexToCopyFrom = -1;

        TravelEvent travelToClosestPoint = null;

        if (!reverseAlongPath)
        {
            additionalComment = "Close - forwards";

            reverseWipePath = false;

            // Add a travel to the closest point
            MovementEvent closestEventReverse = null;

            int previousMovement = extrusionBuffer.getPreviousMovementEventIndex(closestEventIndex);
            if (previousMovement > 0)
            {
                closestEventReverse = (MovementEvent) extrusionBuffer.get(previousMovement);

            } else
            {
                closestEventReverse = (MovementEvent) extrusionBuffer.get(closestEventIndex);
            }

            travelToClosestPoint = new TravelEvent();
            travelToClosestPoint.setX(closestEventReverse.getX());
            travelToClosestPoint.setY(closestEventReverse.getY());
            travelToClosestPoint.setComment(originalComment);
            travelToClosestPoint.setFeedRate(wipeFeedRate_mmPerMin);

            extrusionBuffer.add(indexOfFirstCopiedEvent, travelToClosestPoint);
            indexOfFirstCopiedEvent++;

            lowestIndexToCopyFrom = initialExtrusionBounds.getStartIndex();

        } else
        {
//            if (finalExtrusionEventIndex != modifiedFinalExtrusionEventIndex)
//            {
//                // We need to travel to the start of the close 
//                // Add a travel to the closest point
//                travelToClosestPoint = new TravelEvent();
//                ExtrusionEvent closestEventForward = (ExtrusionEvent) extrusionBuffer.get(
//                        modifiedFinalExtrusionEventIndex);
//                travelToClosestPoint.setX(closestEventForward.getX());
//                travelToClosestPoint.setY(closestEventForward.getY());
//                travelToClosestPoint.setComment("Travelling to start of close");
//                travelToClosestPoint.setFeedRate(wipeFeedRate_mmPerMin);
//
//                extrusionBuffer.add(insertedEventIndex, travelToClosestPoint);
//                insertedEventIndex++;
//            }

            additionalComment = "Close - reverse";

            reverseWipePath = true;

//            int previousTravelEventIndex = extrusionBuffer.getPreviousEventIndex(
//                    finalExtrusionBounds.getEndIndex(), TravelEvent.class
//            );
//            if (previousTravelEventIndex
//                    >= 0)
//            {
//                copyEventIndexLimit = previousTravelEventIndex;
//            } else
//            {
            lowestIndexToCopyFrom = initialExtrusionBounds.getStartIndex();
//            }
        }

        startOfClose = indexOfFirstCopiedEvent;

        int indexDelta = (reverseWipePath == true) ? -1 : 1;

        if (lowestIndexToCopyFrom >= 0)
        {
            if (closeOverLastExtrusionBoundary)
            {
                startOfClose = boundsContainingExtrusionToCloseFrom.getStartIndex();
                targetVolume -= boundsContainingExtrusionToCloseFrom.getEExtrusion();
            }

            if (targetVolume > 0)
            {
                copyExtrusionEvents(targetVolume,
                        closestEventIndex,
                        lowestIndexToCopyFrom,
                        finalExtrusionBounds.getEndIndex(),
                        boundsContainingExtrusionToCloseFrom.getEndIndex(),
                        travelToClosestPoint,
                        originalComment,
                        additionalComment,
                        indexOfFirstCopiedEvent,
                        indexDelta);
            }
        } else
        {
            throw new CannotCloseFromPerimeterException("Failed to close nozzle correctly - minimum index <= 0");
        }

        return startOfClose;
    }

    private void copyExtrusionEvents(double targetVolume,
            int indexToStartCopyingFrom,
            int lowestIndexToCopyFrom,
            int highestIndexToCopyFrom,
            int finalExtrusionEventIndex,
            TravelEvent travelToClosestPoint,
            final String originalComment,
            String additionalComment,
            int indexToStartInsertingAt,
            int indexDelta) throws PostProcessingError
    {
        int closeExtrusionFeedRate_mmPerMin = wipeFeedRate_mmPerMin;

        //Find the last extrusion rate used in this buffer
        for (int feedRateIndex = finalExtrusionEventIndex;
                feedRateIndex > 0; feedRateIndex--)
        {
            if (extrusionBuffer.get(feedRateIndex) instanceof ExtrusionEvent)
            {
                ExtrusionEvent eventToExamine = (ExtrusionEvent) extrusionBuffer.get(feedRateIndex);
                if (eventToExamine.getFeedRate() > 0)
                {
                    closeExtrusionFeedRate_mmPerMin = (int) eventToExamine.getFeedRate();
                    break;
                }
            }
        }

        double cumulativeExtrusionVolume = 0;

        boolean startMessageOutput = false;

        int currentCopyIndex = indexToStartCopyingFrom;

        while (cumulativeExtrusionVolume < targetVolume
                && currentCopyIndex <= highestIndexToCopyFrom
                && currentCopyIndex >= lowestIndexToCopyFrom)
        {
            boolean dontIncrementEventIndex = false;

            if (extrusionBuffer.get(currentCopyIndex) instanceof ExtrusionEvent)
            {
                ExtrusionEvent eventToCopy = (ExtrusionEvent) extrusionBuffer.get(
                        currentCopyIndex);

                double segmentVolume = eventToCopy.getE() + eventToCopy.getD();
                double volumeDifference = targetVolume - cumulativeExtrusionVolume
                        - segmentVolume;

                if (volumeDifference < 0)
                {
                    double requiredSegmentVolume = segmentVolume + volumeDifference;
                    double segmentAlterationRatio = requiredSegmentVolume / segmentVolume;

                    ExtrusionEvent eventToInsert = new ExtrusionEvent();
                    eventToInsert.setE(eventToCopy.getE() * segmentAlterationRatio);
                    eventToInsert.setD(eventToCopy.getD() * segmentAlterationRatio);
                    eventToInsert.setFeedRate(closeExtrusionFeedRate_mmPerMin);

                    Vector2D fromPosition = null;

                    Vector2D fromReferencePosition = null;

                    // Prevent the extrusion being output for the events we've copied from
                    eventToCopy.setDontOutputExtrusion(true);

                    if (travelToClosestPoint != null)
                    {
                        fromReferencePosition = new Vector2D(travelToClosestPoint.getX(), travelToClosestPoint.getY());
                        travelToClosestPoint = null;
                    } else
                    {

                        if (indexDelta < 0)
                        {
                            fromReferencePosition = getNextPosition(currentCopyIndex,
                                    highestIndexToCopyFrom);
                        } else
                        {
                            fromReferencePosition = getLastPosition(currentCopyIndex);
                        }
                    }

                    if (fromReferencePosition != null)
                    {
                        fromPosition = fromReferencePosition;
                    } else
                    {
                        throw new PostProcessingError(
                                "Couldn't locate from position for auto wipe");
                    }

                    Vector2D toPosition = new Vector2D(eventToCopy.getX(),
                            eventToCopy.getY());

                    Vector2D actualVector = toPosition.subtract(fromPosition);
                    Vector2D firstSegment = fromPosition.add(segmentAlterationRatio,
                            actualVector);

                    eventToInsert.setX(firstSegment.getX());
                    eventToInsert.setY(firstSegment.getY());
                    eventToInsert.setComment(originalComment + ":" + additionalComment
                            + " - end -");

                    extrusionBuffer.add(indexToStartInsertingAt, eventToInsert);
                    cumulativeExtrusionVolume += requiredSegmentVolume;
                } else
                {
                    ExtrusionEvent eventToInsert = new ExtrusionEvent();
                    eventToInsert.setE(eventToCopy.getE());
                    eventToInsert.setD(eventToCopy.getD());
                    eventToInsert.setX(eventToCopy.getX());
                    eventToInsert.setY(eventToCopy.getY());
                    eventToInsert.setComment(originalComment + ":" + additionalComment
                            + ((startMessageOutput == false) ? " - start -" : " - in progress -"));
                    startMessageOutput = true;
                    eventToInsert.setFeedRate(closeExtrusionFeedRate_mmPerMin);

                    extrusionBuffer.add(indexToStartInsertingAt, eventToInsert);
                    cumulativeExtrusionVolume += eventToCopy.getE() + eventToCopy.getD();

                    // Prevent the extrusion being output for the events we've copied from
                    eventToCopy.setDontOutputExtrusion(true);
                }
            } else
            {
                GCodeParseEvent event = extrusionBuffer.get(currentCopyIndex);

                if (event instanceof TravelEvent)
                {
                    TravelEvent eventToCopy = (TravelEvent) event;
                    TravelEvent eventToInsert = new TravelEvent();
                    eventToInsert.setX(eventToCopy.getX());
                    eventToInsert.setY(eventToCopy.getY());
                    eventToInsert.setComment(eventToCopy.getComment());
                    eventToInsert.setFeedRate(closeExtrusionFeedRate_mmPerMin);

                    extrusionBuffer.add(indexToStartInsertingAt, eventToInsert);
                } else
                {
                    dontIncrementEventIndex = true;
//                        steno.info("Elided event of type " + event.getClass().getName()
//                                + " during close");
                }
            }

            if (!dontIncrementEventIndex)
            {
                indexToStartInsertingAt++;
            }

            currentCopyIndex += indexDelta;
        }
    }

    private void outputNoBNoE(ExtrusionEvent event, String comment)
    {
        // No extrusion
        // No B
        TravelEvent noBNoETravel = new TravelEvent();
        noBNoETravel.setX(event.getX());
        noBNoETravel.setY(event.getY());
        noBNoETravel.setLength(event.getLength());
        noBNoETravel.setFeedRate(event.getFeedRate());
        noBNoETravel.setComment(comment);
        writeEventToFile(noBNoETravel);
        if (mixExtruderOutputs)
        {
            autoUnretractEValue += event.getE()
                    * currentEMixValue;
            autoUnretractDValue += event.getE()
                    * currentDMixValue;
        } else
        {
            autoUnretractEValue += event.getE();
        }
    }

    private boolean replaceOpenNozzleWithPartialOpen(double partialOpenValue)
    {
        boolean success = false;

        int startingIndex = extrusionBuffer.size() - 1;
        int eventSearchIndex = startingIndex;
        while (eventSearchIndex >= 0)
        {
            if (extrusionBuffer.get(eventSearchIndex) instanceof NozzleOpenFullyEvent)
            {
                NozzleOpenFullyEvent originalEvent = (NozzleOpenFullyEvent) extrusionBuffer.get(
                        eventSearchIndex);

                NozzleChangeBValueEvent newBEvent = new NozzleChangeBValueEvent();
                newBEvent.setB(partialOpenValue);
                newBEvent.setComment("Partial open with replenish");
                newBEvent.setE(originalEvent.getE());
                newBEvent.setD(originalEvent.getD());

                extrusionBuffer.add(eventSearchIndex + 1, newBEvent);
                extrusionBuffer.remove(eventSearchIndex);
                success = true;
                break;
            }
//            else if (extrusionBuffer.get(eventSearchIndex) instanceof LayerChangeEvent
//                    && eventSearchIndex != startingIndex)
//            {
//                //We didn't have a close at the start of our layer - don't go any further and don't bother trying to change the open
//                steno.info("No open in this layer - decided not to partial open");
//                break;
//            }

            eventSearchIndex--;
        }

        return success;
    }

    private Vector2D getLastPosition(int eventIndex)
    {
        Vector2D position = null;

        for (int index = eventIndex - 1; index >= 0; index--)
        {
            if (extrusionBuffer.get(index) instanceof MovementEvent)
            {
                MovementEvent event = (MovementEvent) extrusionBuffer.get(index);
                position = new Vector2D(event.getX(), event.getY());
                break;
            }
        }

        return position;
    }

    private Vector2D getNextPosition(int eventIndex, int indexMax)
    {
        Vector2D position = null;

        for (int index = eventIndex; index <= indexMax; index++)
        {
            if (extrusionBuffer.get(index) instanceof MovementEvent)
            {
                MovementEvent event = (MovementEvent) extrusionBuffer.get(index);
                position = new Vector2D(event.getX(), event.getY());
                break;
            }
        }

        return position;
    }

    private int insertVolumeBreak(ArrayList<GCodeParseEvent> buffer,
            Map<EventType, Integer> eventIndices,
            EventType eventType,
            double requiredEjectionVolume,
            String comment,
            FindEventDirection findEventDirection)
    {
        int volumeIndex = -1;
        int eventIndex;
        double volumeConsidered = 0;

        if (findEventDirection == FindEventDirection.BACKWARDS_FROM_END)
        {
            eventIndex = buffer.size() - 1;
            while (eventIndex >= 0)
            {
                if (buffer.get(eventIndex) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent currentEvent = (ExtrusionEvent) buffer.get(eventIndex);

                    double segmentExtrusion = currentEvent.getE();
                    volumeConsidered += segmentExtrusion;

                    if (volumeIndex == -1)
                    {
                        if (compareDouble(volumeConsidered,
                                requiredEjectionVolume, 10e-5) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        } else if (compareDouble(volumeConsidered,
                                requiredEjectionVolume, 10e-5)
                                == MORE_THAN)
                        {
                            // Split the line
                            double initialSegmentExtrusion = volumeConsidered
                                    - requiredEjectionVolume;
                            double scaleFactor = initialSegmentExtrusion
                                    / segmentExtrusion;

                            Vector2D fromPosition = null;

                            if (eventIndex > 0)
                            {
                                Vector2D lastPosition = getLastPosition(eventIndex);
                                if (lastPosition != null)
                                {
                                    fromPosition = lastPosition;
                                } else
                                {
                                    fromPosition = new Vector2D(lastProcessedMovementEvent.getX(),
                                            lastProcessedMovementEvent.getY());
                                }
                            } else
                            {
                                fromPosition = new Vector2D(lastProcessedMovementEvent.getX(),
                                        lastProcessedMovementEvent.getY());
                            }

                            Vector2D toPosition = new Vector2D(currentEvent.getX(),
                                    currentEvent.getY());

//                            steno.debug("Vector from " + fromPosition + " to " + toPosition);
                            Vector2D actualVector = toPosition.subtract(fromPosition);
                            Vector2D firstSegment = fromPosition.add(scaleFactor,
                                    actualVector);

                            ExtrusionEvent firstSegmentExtrusionEvent = new ExtrusionEvent();
                            firstSegmentExtrusionEvent.setX(firstSegment.getX());
                            firstSegmentExtrusionEvent.setY(firstSegment.getY());
                            firstSegmentExtrusionEvent.setE(segmentExtrusion
                                    * scaleFactor);
                            firstSegmentExtrusionEvent.setLength(
                                    ((ExtrusionEvent) currentEvent).getLength()
                                    * scaleFactor);
                            firstSegmentExtrusionEvent.setFeedRate(
                                    currentEvent.getFeedRate());
                            firstSegmentExtrusionEvent.setComment(comment);

                            ExtrusionEvent secondSegmentExtrusionEvent = new ExtrusionEvent();
                            secondSegmentExtrusionEvent.setX(currentEvent.getX());
                            secondSegmentExtrusionEvent.setY(currentEvent.getY());
                            secondSegmentExtrusionEvent.setE(
                                    segmentExtrusion - firstSegmentExtrusionEvent.getE());
                            secondSegmentExtrusionEvent.setLength(
                                    ((ExtrusionEvent) currentEvent).getLength() * (1
                                    - scaleFactor));
                            secondSegmentExtrusionEvent.setFeedRate(
                                    currentEvent.getFeedRate());
                            secondSegmentExtrusionEvent.setComment(comment);

                            for (Entry<EventType, Integer> eventEntry : eventIndices.entrySet())
                            {
                                if (eventEntry.getValue() > eventIndex)
                                {
                                    eventEntry.setValue(eventEntry.getValue() + 1);
                                }
                            }

                            buffer.add(eventIndex, firstSegmentExtrusionEvent);
                            buffer.remove(eventIndex + 1);
                            buffer.add(eventIndex + 1, secondSegmentExtrusionEvent);

                            volumeIndex = eventIndex + 1;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        }
                    }
                }
                eventIndex--;
            }
        } else
        {
            eventIndex = 0;
            while (eventIndex < buffer.size())
            {
                if (buffer.get(eventIndex) instanceof ExtrusionEvent)
                {
                    ExtrusionEvent currentEvent = (ExtrusionEvent) buffer.get(eventIndex);

                    double segmentExtrusion = currentEvent.getE();
                    volumeConsidered += segmentExtrusion;

                    if (volumeIndex == -1)
                    {
                        if (compareDouble(volumeConsidered,
                                requiredEjectionVolume, 10e-5) == EQUAL)
                        {
                            // No need to split line - replace the current event with a nozzle change event
                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        } else if (compareDouble(volumeConsidered,
                                requiredEjectionVolume, 10e-5)
                                == MORE_THAN)
                        {
                            // Split the line
                            double secondSegmentExtrusion = volumeConsidered
                                    - requiredEjectionVolume;

                            double scaleFactor = 1 - (secondSegmentExtrusion
                                    / segmentExtrusion);

                            Vector2D fromPosition = null;

                            if (eventIndex > 0)
                            {
                                Vector2D lastPosition = getLastPosition(eventIndex);
                                if (lastPosition != null)
                                {
                                    fromPosition = lastPosition;
                                } else
                                {
                                    fromPosition = nozzleLastOpenedAt;
                                }
                            } else
                            {
                                fromPosition = nozzleLastOpenedAt;
                            }

                            Vector2D toPosition = new Vector2D(currentEvent.getX(),
                                    currentEvent.getY());
//                            steno.debug("Vector from " + fromPosition + " to " + toPosition);
                            Vector2D actualVector = toPosition.subtract(fromPosition);
                            Vector2D firstSegment = fromPosition.add(scaleFactor,
                                    actualVector);

                            ExtrusionEvent firstSegmentExtrusionEvent = new ExtrusionEvent();
                            firstSegmentExtrusionEvent.setX(firstSegment.getX());
                            firstSegmentExtrusionEvent.setY(firstSegment.getY());
                            firstSegmentExtrusionEvent.setE(segmentExtrusion
                                    * scaleFactor);
                            firstSegmentExtrusionEvent.setLength(
                                    ((ExtrusionEvent) currentEvent).getLength()
                                    * scaleFactor);
                            firstSegmentExtrusionEvent.setFeedRate(
                                    currentEvent.getFeedRate());
                            firstSegmentExtrusionEvent.setComment(comment);

                            ExtrusionEvent secondSegmentExtrusionEvent = new ExtrusionEvent();
                            secondSegmentExtrusionEvent.setX(currentEvent.getX());
                            secondSegmentExtrusionEvent.setY(currentEvent.getY());
                            secondSegmentExtrusionEvent.setE(secondSegmentExtrusion);
                            secondSegmentExtrusionEvent.setLength(
                                    ((ExtrusionEvent) currentEvent).getLength() * (1
                                    - scaleFactor));
                            secondSegmentExtrusionEvent.setFeedRate(
                                    currentEvent.getFeedRate());
                            secondSegmentExtrusionEvent.setComment(comment);

                            for (Entry<EventType, Integer> eventEntry : eventIndices.entrySet())
                            {
                                if (eventEntry.getValue() > eventIndex)
                                {
                                    eventEntry.setValue(eventEntry.getValue() + 1);
                                }
                            }

                            buffer.add(eventIndex, firstSegmentExtrusionEvent);
                            buffer.remove(eventIndex + 1);
                            buffer.add(eventIndex + 1, secondSegmentExtrusionEvent);

                            volumeIndex = eventIndex;
                            eventIndices.put(eventType, volumeIndex);
                            break;
                        }
                    }
                }
                eventIndex++;
            }
        }

        return volumeIndex;
    }

    private double calculateVolume(ArrayList<GCodeParseEvent> extrusionBufferToAssess,
            int startIndex, int endIndex)
    {
        double totalExtrusionForPath = 0;

        for (int extrusionBufferIndex = startIndex; extrusionBufferIndex <= endIndex; extrusionBufferIndex++)
        {
            GCodeParseEvent event = extrusionBufferToAssess.get(extrusionBufferIndex);

            if (event instanceof ExtrusionEvent)
            {
                totalExtrusionForPath += ((ExtrusionEvent) event).getE() + ((ExtrusionEvent) event).
                        getD();
            }
        }

        return totalExtrusionForPath;
    }

    // Walk backwards through the extrusion buffer until the total of the counted extrusion equals or exceeds the target
    private int determineShortPathCloseStart(LegacyExtrusionBuffer localExtrusionBuffer, double desiredExtrusionVolume)
    {
        double extrusionSoFar = 0;
        int indexToBeReturned = 0;

        for (int extrusionIndex = localExtrusionBuffer.size() - 1; extrusionIndex > 0; extrusionIndex--)
        {
            if (extrusionBuffer.get(extrusionIndex) instanceof ExtrusionEvent)
            {
                ExtrusionEvent eventToBeCounted = (ExtrusionEvent) extrusionBuffer.get(extrusionIndex);
                extrusionSoFar += eventToBeCounted.getD() + eventToBeCounted.getE();

                if (extrusionSoFar >= desiredExtrusionVolume)
                {
                    indexToBeReturned = extrusionIndex;
                    break;
                }
            }
        }

        return indexToBeReturned;
    }
}
