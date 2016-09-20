package celtech.roboxbase.services.postProcessor;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.HeadFile;
import celtech.roboxbase.postprocessor.RoboxiserResult;
import celtech.roboxbase.postprocessor.nouveau.PostProcessor;
import celtech.roboxbase.postprocessor.nouveau.PostProcessorFeature;
import celtech.roboxbase.postprocessor.nouveau.PostProcessorFeatureSet;
import celtech.roboxbase.printerControl.PrintJob;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.CameraTriggerData;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class PostProcessorTask extends Task<GCodePostProcessingResult>
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            PostProcessorTask.class.getName());

    private final String printJobUUID;
    private final PrintableMeshes printableMeshes;
    private final String printJobDirectory;
    private final Printer printerToUse;
    private final DoubleProperty taskProgress = new SimpleDoubleProperty(0);
    private final boolean insertCameraControl;
    private final CameraTriggerData cameraTriggerData;

    public PostProcessorTask(
            String printJobUUID,
            PrintableMeshes printableMeshes,
            Printer printerToUse,
            boolean insertCameraControl,
            CameraTriggerData cameraTriggerData)
    {
        this.printJobUUID = printJobUUID;
        this.printableMeshes = printableMeshes;
        this.printJobDirectory = BaseConfiguration.getPrintSpoolDirectory() + printJobUUID + File.separator;
        this.printerToUse = printerToUse;
        this.insertCameraControl = insertCameraControl;
        this.cameraTriggerData = cameraTriggerData;
        updateTitle("Post Processor");
        updateProgress(0.0, 100.0);
    }

    @Override
    protected GCodePostProcessingResult call() throws Exception
    {
        GCodePostProcessingResult postProcessingResult = null;

        try
        {
            updateMessage("");
            updateProgress(0.0, 100.0);
            taskProgress.addListener(
                    (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
                    {
                        updateProgress(newValue.doubleValue(), 100.0);
                    });
            postProcessingResult = doPostProcessing(
                    printJobUUID,
                    printableMeshes,
                    printJobDirectory,
                    printerToUse,
                    taskProgress,
                    insertCameraControl,
                    cameraTriggerData);
        } catch (Exception ex)
        {
            ex.printStackTrace();
            steno.error("Error in post processing");
        }
        return postProcessingResult;
    }

    public static GCodePostProcessingResult doPostProcessing(
            String printJobUUID,
            PrintableMeshes printableMeshes,
            String printJobDirectory,
            Printer printer,
            DoubleProperty taskProgress,
            boolean insertCameraControl,
            CameraTriggerData cameraTriggerData) throws IOException
    {
        SlicerType selectedSlicer = null;
        String headType;
        if (printer != null && printer.headProperty().get() != null)
        {
            headType = printer.headProperty().get().typeCodeProperty().get();
        } else
        {
            headType = HeadContainer.defaultHeadID;
        }
        if (printableMeshes.getSettings().getSlicerOverride() != null)
        {
            selectedSlicer = printableMeshes.getSettings().getSlicerOverride();
        } else
        {
            selectedSlicer = printableMeshes.getDefaultSlicerType();
        }

        PrintJob printJob = PrintJob.readJobFromDirectory(printJobUUID, printJobDirectory);
        String gcodeFileToProcess = printJob.getGCodeFileLocation();
        String gcodeOutputFile = printJob.getRoboxisedFileLocation();

        GCodePostProcessingResult postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printer, new RoboxiserResult());

        PostProcessorFeatureSet ppFeatures = new PostProcessorFeatureSet();

        HeadFile headFileToUse = null;
        if (printer == null
                || printer.headProperty().get() == null)
        {
            headFileToUse = HeadContainer.getHeadByID(HeadContainer.defaultHeadID);
                ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
                ppFeatures.enableFeature(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES);
                ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
                ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);
        } else
        {
            headFileToUse = HeadContainer.getHeadByID(printer.headProperty().get().typeCodeProperty().get());
            if (!headFileToUse.getTypeCode().equals("RBX01-SL")
                    && !headFileToUse.getTypeCode().equals("RBX01-DL"))
            {
                ppFeatures.enableFeature(PostProcessorFeature.REMOVE_ALL_UNRETRACTS);
                ppFeatures.enableFeature(PostProcessorFeature.OPEN_AND_CLOSE_NOZZLES);
                ppFeatures.enableFeature(PostProcessorFeature.OPEN_NOZZLE_FULLY_AT_START);
                ppFeatures.enableFeature(PostProcessorFeature.REPLENISH_BEFORE_OPEN);
            }
        }

        if (insertCameraControl)
        {
            ppFeatures.enableFeature(PostProcessorFeature.INSERT_CAMERA_CONTROL_POINTS);
        }

        Map<Integer, Integer> objectToNozzleNumberMap = new HashMap<>();
        int objectIndex = 0;

        headFileToUse.getNozzles().get(0).getAssociatedExtruder();
        for (int extruderForModel : printableMeshes.getExtruderForModel())
        {
                Optional<Integer> nozzleForExtruder = headFileToUse.getNozzleNumberForExtruderNumber(extruderForModel);
                if (nozzleForExtruder.isPresent())
                {
                    objectToNozzleNumberMap.put(objectIndex, nozzleForExtruder.get());
                } else
                {
                    steno.warning("Couldn't get extruder number for object " + objectIndex);
                }
                objectIndex++;
        }
        PostProcessor postProcessor = new PostProcessor(
                printJobUUID,
                printableMeshes.getProjectName(),
                printableMeshes.getUsedExtruders(),
                printer,
                gcodeFileToProcess,
                gcodeOutputFile,
                headFileToUse,
                printableMeshes.getSettings(),
                printableMeshes.getPrintOverrides(),
                ppFeatures,
                headType,
                taskProgress,
                objectToNozzleNumberMap,
                cameraTriggerData,
                printableMeshes.isSafetyFeaturesRequired());

        RoboxiserResult roboxiserResult = postProcessor.processInput();
        if (roboxiserResult.isSuccess())
        {
            roboxiserResult.getPrintJobStatistics().writeToFile(printJob.getStatisticsFileLocation());
            postProcessingResult = new GCodePostProcessingResult(printJobUUID, gcodeOutputFile, printer, roboxiserResult);
        }

        return postProcessingResult;
    }

}
