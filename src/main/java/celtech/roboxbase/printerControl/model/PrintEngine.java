package celtech.roboxbase.printerControl.model;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.MaterialType;
import celtech.roboxbase.appManager.NotificationType;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.comms.rx.ListFilesResponse;
import celtech.roboxbase.comms.rx.SendFile;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Macro;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.configuration.slicer.SlicerConfigWriter;
import celtech.roboxbase.configuration.slicer.SlicerConfigWriterFactory;
import celtech.roboxbase.postprocessor.PrintJobStatistics;
import celtech.roboxbase.printerControl.PrintJob;
import celtech.roboxbase.printerControl.PrintQueueStatus;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.PrinterStatus;
import celtech.roboxbase.printerControl.comms.commands.GCodeMacros;
import celtech.roboxbase.printerControl.comms.commands.MacroLoadException;
import celtech.roboxbase.printerControl.comms.commands.MacroPrintException;
import celtech.roboxbase.services.CameraTriggerData;
import celtech.roboxbase.services.CameraTriggerManager;
import celtech.roboxbase.services.ControllableService;
import celtech.roboxbase.services.postProcessor.GCodePostProcessingResult;
import celtech.roboxbase.services.postProcessor.PostProcessorService;
import celtech.roboxbase.services.printing.GCodePrintResult;
import celtech.roboxbase.services.printing.TransferGCodeToPrinterService;
import celtech.roboxbase.services.slicer.AbstractSlicerService;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import celtech.roboxbase.services.slicer.SliceResult;
import celtech.roboxbase.services.slicer.SlicerService;
import celtech.roboxbase.utils.SystemUtils;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author ianhudson
 */
public class PrintEngine implements ControllableService
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            PrintEngine.class.getName());

    private Printer associatedPrinter = null;
    public final AbstractSlicerService slicerService = new SlicerService();
    public final PostProcessorService postProcessorService = new PostProcessorService();
    public final TransferGCodeToPrinterService transferGCodeToPrinterService = new TransferGCodeToPrinterService();
    private final IntegerProperty linesInPrintingFile = new SimpleIntegerProperty(0);

    /**
     * Indicates if ETC data is available for the current print
     */
    private final BooleanProperty etcAvailable = new SimpleBooleanProperty(false);
    /*
     * 
     */
    private EventHandler<WorkerStateEvent> scheduledSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> failedSliceEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededSliceEventHandler = null;

    private EventHandler<WorkerStateEvent> scheduledGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> failedGCodePostProcessEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededGCodePostProcessEventHandler = null;

    private EventHandler<WorkerStateEvent> scheduledPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> cancelPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> failedPrintEventHandler = null;
    private EventHandler<WorkerStateEvent> succeededPrintEventHandler = null;

    private final StringProperty printProgressTitle = new SimpleStringProperty();
    private final StringProperty printProgressMessage = new SimpleStringProperty();
    private final BooleanProperty dialogRequired = new SimpleBooleanProperty(
            false);
    private final DoubleProperty primaryProgressPercent = new SimpleDoubleProperty(
            0);
    private final DoubleProperty secondaryProgressPercent = new SimpleDoubleProperty(
            0);
    private final ObjectProperty<Date> printJobStartTime = new SimpleObjectProperty<>();
    public final ObjectProperty<Macro> macroBeingRun = new SimpleObjectProperty<>();

    private ObjectProperty<PrintQueueStatus> printQueueStatus = new SimpleObjectProperty<>(PrintQueueStatus.IDLE);
    private ObjectProperty<PrintJob> printJob = new SimpleObjectProperty<>(null);

    /*
     * 
     */
    private ChangeListener<Number> printLineNumberListener = null;
    private ChangeListener<String> printJobIDListener = null;

    private boolean consideringPrintRequest = false;
    ETCCalculator etcCalculator;
    /**
     * progressETC holds the number of seconds predicted for the ETC of the
     * print
     */
    private final IntegerProperty progressETC = new SimpleIntegerProperty();
    /**
     * The current layer being processed
     */
    private final IntegerProperty progressCurrentLayer = new SimpleIntegerProperty();
    /**
     * The total number of layers in the model being printed
     */
    private final IntegerProperty progressNumLayers = new SimpleIntegerProperty();

    /**
     * The movie maker task
     */
//    private MovieMakerTask movieMakerTask = null;
    private boolean raiseProgressNotifications = true;
    private boolean canDisconnectDuringPrint = true;

    private CameraTriggerManager cameraTriggerManager;
    private CameraTriggerData cameraTriggerData;
    private boolean cameraIsEnabled = false;

    private BooleanProperty highIntensityCommsInProgress = new SimpleBooleanProperty(false);

    private boolean iAmTakingItThroughTheBackDoor = false;

    public PrintEngine(Printer associatedPrinter)
    {
        this.associatedPrinter = associatedPrinter;
        cameraTriggerManager = new CameraTriggerManager(associatedPrinter);

        cancelSliceEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on slice cancel");
            }
        };

        failedSliceEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has failed");
            if (raiseProgressNotifications)
            {
                BaseLookup.getSystemNotificationHandler().showDismissableNotification(BaseLookup.i18n(
                        "notification.sliceFailed"), BaseLookup.i18n(
                                "notification.slicerFailure.dismiss"), NotificationType.CAUTION);
            }
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on slice fail");
            }
        };

        succeededSliceEventHandler = (WorkerStateEvent t) ->
        {
            SliceResult result = (SliceResult) (t.getSource().getValue());

            if (result.isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                postProcessorService.reset();
                postProcessorService.setPrintJobUUID(
                        result.getPrintJobUUID());
                postProcessorService.setPrinterToUse(
                        result.getPrinterToUse());
                postProcessorService.setPrintableMeshes(result.getPrintableMeshes());
                postProcessorService.start();

                if (raiseProgressNotifications)
                {
                    BaseLookup.getSystemNotificationHandler().showSliceSuccessfulNotification();
                }
            } else
            {
                if (raiseProgressNotifications)
                {
                    BaseLookup.getSystemNotificationHandler().showDismissableNotification(BaseLookup.i18n(
                            "notification.sliceFailed"), BaseLookup.i18n(
                                    "notification.slicerFailure.dismiss"), NotificationType.CAUTION);
                }
                try
                {
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on slice fail");
                }
            }
        };

        cancelGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on post process cancel");
            }
        };

        failedGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has failed");
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on post process fail");
            }
        };

        succeededGCodePostProcessEventHandler = (WorkerStateEvent t) ->
        {
            GCodePostProcessingResult result = (GCodePostProcessingResult) (t.getSource().
                    getValue());

            if (result != null
                    && result.getRoboxiserResult() != null
                    && result.getRoboxiserResult().isSuccess())
            {
                steno.info(t.getSource().getTitle() + " has succeeded");
                String jobUUID = result.getPrintJobUUID();

                PrintJobStatistics printJobStatistics = result.getRoboxiserResult().
                        getPrintJobStatistics();

                makeETCCalculator(printJobStatistics, associatedPrinter);

                transferGCodeToPrinterService.reset();
                transferGCodeToPrinterService.setCurrentPrintJobID(jobUUID);
                transferGCodeToPrinterService.setStartFromSequenceNumber(0);
                transferGCodeToPrinterService.setModelFileToPrint(result.getOutputFilename());
                transferGCodeToPrinterService.setPrinterToUse(result.getPrinterToUse());
                transferGCodeToPrinterService.setPrintJobStatistics(printJobStatistics);
                transferGCodeToPrinterService.start();

                printJobStartTime.set(new Date());

                if (raiseProgressNotifications)
                {
                    BaseLookup.getSystemNotificationHandler().
                            showGCodePostProcessSuccessfulNotification();
                }
            } else
            {
                try
                {
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on post process fail");
                }

            }
        };

        cancelPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been cancelled");
            if (raiseProgressNotifications)
            {
                BaseLookup.getSystemNotificationHandler().showPrintJobCancelledNotification();
            }
        };

        failedPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.error(t.getSource().getTitle() + " has failed");
            if (raiseProgressNotifications)
            {
                BaseLookup.getSystemNotificationHandler().showPrintJobFailedNotification();
            }
            try
            {
                associatedPrinter.cancel(null);
            } catch (PrinterException ex)
            {
                steno.error("Couldn't abort on print job fail");
            }
        };

        succeededPrintEventHandler = (WorkerStateEvent t) ->
        {
            GCodePrintResult result = (GCodePrintResult) (t.getSource().getValue());
            if (result.isSuccess())
            {
                steno.info("Transfer of file to printer complete for job: " + result.getPrintJobID());
//                if (associatedPrinter.printerStatusProperty().get()
//                    == PrinterStatus.EJECTING_STUCK_MATERIAL)
//                {
////                    associatedPrinter.setPrinterStatus(PrinterStatus.EXECUTING_MACRO);
//                    //Remove the print job from disk
//                    String printjobFilename = ApplicationConfiguration.
//                        getApplicationStorageDirectory()
//                        + ApplicationConfiguration.macroFileSubpath
//                        + File.separator
//                        + result.getPrintJobID();
//                    File fileToDelete = new File(printjobFilename);
//                    try
//                    {
//                        FileDeleteStrategy.FORCE.delete(fileToDelete);
//                    } catch (IOException ex)
//                    {
//                        steno.error(
//                            "Error whilst deleting macro print directory "
//                            + printjobFilename + " exception - "
//                            + ex.getMessage());
//                    }
//                } else
                {
                    if (raiseProgressNotifications && canDisconnectDuringPrint)
                    {
                        BaseLookup.getSystemNotificationHandler().
                                showPrintTransferSuccessfulNotification(
                                        associatedPrinter.getPrinterIdentity().printerFriendlyNameProperty().
                                                get());
                    }
                }
            } else
            {
                if (raiseProgressNotifications)
                {
                    BaseLookup.getSystemNotificationHandler().showPrintTransferFailedNotification(
                            associatedPrinter.getPrinterIdentity().printerFriendlyNameProperty().get());
                }
                steno.error("Submission of job to printer failed");
                try
                {
                    //TODO - can't submit in this case...?
                    associatedPrinter.cancel(null);
                } catch (PrinterException ex)
                {
                    steno.error("Couldn't abort on print job failed to submit");
                }
            }
        };

        printJobIDListener = (ObservableValue<? extends String> ov, String oldValue, String newValue) ->
        {
            detectAlreadyPrinting();
        };

        scheduledPrintEventHandler = (WorkerStateEvent t) ->
        {
            steno.info(t.getSource().getTitle() + " has been scheduled");
            if (raiseProgressNotifications)
            {
                BaseLookup.getSystemNotificationHandler().showPrintTransferInitiatedNotification();
            }
        };

        printLineNumberListener = new ChangeListener<Number>()
        {
            @Override
            public void changed(ObservableValue<? extends Number> ov,
                    Number oldValue,
                    Number newValue)
            {
                if (etcAvailable.get())
                {
                    updateETCUsingETCCalculator(newValue);
                } else
                {
                    updateETCUsingLineNumber(newValue);
                }
            }
        };

        slicerService.setOnScheduled(scheduledSliceEventHandler);
        slicerService.setOnCancelled(cancelSliceEventHandler);

        slicerService.setOnFailed(failedSliceEventHandler);

        slicerService.setOnSucceeded(succeededSliceEventHandler);

        postProcessorService.setOnCancelled(
                cancelGCodePostProcessEventHandler);

        postProcessorService.setOnFailed(failedGCodePostProcessEventHandler);

        postProcessorService.setOnSucceeded(
                succeededGCodePostProcessEventHandler);

        transferGCodeToPrinterService.setOnScheduled(scheduledPrintEventHandler);

        transferGCodeToPrinterService.setOnCancelled(cancelPrintEventHandler);

        transferGCodeToPrinterService.setOnFailed(failedPrintEventHandler);

        transferGCodeToPrinterService.setOnSucceeded(succeededPrintEventHandler);

        associatedPrinter.printJobLineNumberProperty().addListener(printLineNumberListener);
        associatedPrinter.printJobIDProperty().addListener(printJobIDListener);

        printQueueStatus.addListener(new ChangeListener<PrintQueueStatus>()
        {
            @Override
            public void changed(ObservableValue<? extends PrintQueueStatus> ov, PrintQueueStatus t, PrintQueueStatus t1)
            {
                if (t1 == PrintQueueStatus.PRINTING)
                {
                    if (macroBeingRun.get() == null && cameraIsEnabled)
                    {
                        cameraTriggerManager.listenForCameraTrigger();
                    }
                    printJob.set(new PrintJob(associatedPrinter.printJobIDProperty().get()));
                } else
                {
                    if (macroBeingRun.get() == null)
                    {
                        cameraTriggerManager.stopListeningForCameraTrigger();
                    }
                    printJob.set(null);
                }
            }
        });

        highIntensityCommsInProgress.bind(slicerService.runningProperty()
                .or(postProcessorService.runningProperty())
                .or(transferGCodeToPrinterService.runningProperty()));

        detectAlreadyPrinting();
    }

    /**
     * Create the ETCCalculator based on the given PrintJobStatistics.
     */
    private void makeETCCalculator(PrintJobStatistics printJobStatistics,
            Printer associatedPrinter)
    {
        int numberOfLines = printJobStatistics.getNumberOfLines();
        linesInPrintingFile.set(numberOfLines);
        Map<Integer, Double> layerNumberToPredictedDuration_E = printJobStatistics
                .getLayerNumberToPredictedDuration_E_FeedrateDependent();
        Map<Integer, Double> layerNumberToPredictedDuration_D = printJobStatistics
                .getLayerNumberToPredictedDuration_D_FeedrateDependent();
        Map<Integer, Double> layerNumberToPredictedDuration_feedrateIndependent = printJobStatistics
                .getLayerNumberToPredictedDuration_FeedrateIndependent();
        List<Integer> layerNumberToLineNumber = printJobStatistics.getLayerNumberToLineNumber();
        etcCalculator = new ETCCalculator(associatedPrinter,
                layerNumberToPredictedDuration_E,
                layerNumberToPredictedDuration_D,
                layerNumberToPredictedDuration_feedrateIndependent,
                layerNumberToLineNumber);
        if (layerNumberToLineNumber != null)
        {
            progressNumLayers.set(layerNumberToLineNumber.size());
        }
        primaryProgressPercent.unbind();
        primaryProgressPercent.set(0);
        progressETC.set(etcCalculator.getETCPredicted(0));
        etcAvailable.set(true);
    }

    private void updateETCUsingETCCalculator(Number newValue)
    {
        int lineNumber = newValue.intValue();
        primaryProgressPercent.set(etcCalculator.getPercentCompleteAtLine(lineNumber));
        progressETC.set(etcCalculator.getETCPredicted(lineNumber));
        progressCurrentLayer.set(etcCalculator.getCurrentLayerNumberForLineNumber(lineNumber));
    }

    private void updateETCUsingLineNumber(Number newValue)
    {
        if (linesInPrintingFile.get() > 0)
        {
            double percentDone = newValue.doubleValue()
                    / linesInPrintingFile.doubleValue();
            primaryProgressPercent.set(percentDone);
        }
    }

    public void makeETCCalculatorForJobOfUUID(String printJobID)
    {
        PrintJob localPrintJob = new PrintJob(printJobID);
        PrintJobStatistics statistics = null;
        try
        {
            statistics = localPrintJob.getStatistics();
            makeETCCalculator(statistics, associatedPrinter);
        } catch (IOException ex)
        {
            if (associatedPrinter.getCommandInterface() instanceof RoboxRemoteCommandInterface)
            {
                //OK - ask for the stats from the remote end
                try
                {
                    statistics = ((RoboxRemoteCommandInterface) associatedPrinter.getCommandInterface()).retrieveStatistics();
                    if (statistics != null)
                    {
                        makeETCCalculator(statistics, associatedPrinter);
                        statistics.writeStatisticsToFile(localPrintJob.getStatisticsFileLocation());
                    }
                } catch (RoboxCommsException | IOException rex)
                {
                    steno.error("Failed to get statistics from remote server and persist");
                }
            }
        }

        if (statistics == null)
        {
            etcAvailable.set(false);
        }
    }

    /**
     *
     */
    public void shutdown()
    {
        stopAllServices();
    }

    public synchronized boolean printProject(PrintableMeshes printableMeshes)
    {
        boolean acceptedPrintRequest = false;
        canDisconnectDuringPrint = true;
        etcAvailable.set(false);

        cameraIsEnabled = printableMeshes.isCameraEnabled();

        if (cameraIsEnabled)
        {
            cameraTriggerManager.setTriggerData(printableMeshes.getCameraTriggerData());
            cameraTriggerData = printableMeshes.getCameraTriggerData();
        }

        if (associatedPrinter.printerStatusProperty().get() == PrinterStatus.IDLE)
        {
            boolean printFromScratchRequired = false;

            if (printableMeshes.getRequiredPrintJobID() != null
                    && !printableMeshes.getRequiredPrintJobID().equals(""))
            {
                String jobUUID = printableMeshes.getRequiredPrintJobID();
                PrintJob printJob = new PrintJob(jobUUID);

                //Reprint the last job
                //Is it still on the printer?
                try
                {
                    ListFilesResponse listFilesResponse = associatedPrinter.
                            transmitListFiles();
                    if (listFilesResponse.getPrintJobIDs().contains(jobUUID))
                    {
                        acceptedPrintRequest = reprintDirectFromPrinter(printJob);
                    } else
                    {
                        //Need to send the file to the printer
                        //Is it still on disk?

                        if (printJob.roboxisedFileExists())
                        {
                            acceptedPrintRequest = reprintFileFromDisk(printJob);
                        } else
                        {
                            printFromScratchRequired = true;
                            steno.error(
                                    "Print job " + jobUUID
                                    + " not found on printer or disk - going ahead with print from scratch");
                        }
                    }

                    try
                    {
                        makeETCCalculator(printJob.getStatistics(), associatedPrinter);
                    } catch (IOException ex)
                    {
                        etcAvailable.set(false);
                    }
                } catch (RoboxCommsException ex)
                {
                    printFromScratchRequired = true;
                    steno.error(
                            "Error whilst attempting to list files on printer - going ahead with print from scratch");
                }
            } else
            {
                printFromScratchRequired = true;
            }

            if (printFromScratchRequired)
            {
                acceptedPrintRequest = printFromScratch(acceptedPrintRequest, printableMeshes);
            }
        }

        return acceptedPrintRequest;
    }

    private boolean printFromScratch(boolean acceptedPrintRequest, PrintableMeshes printableMeshes)
    {
        SlicerParametersFile settingsToUse = printableMeshes.getSettings().clone();

        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();
        String printJobDirectoryName = BaseConfiguration.
                getPrintSpoolDirectory() + printUUID;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();
        //Erase old print job directories
        File printSpoolDirectory = new File(
                BaseConfiguration.getPrintSpoolDirectory());
        File[] filesOnDisk = printSpoolDirectory.listFiles();
        if (filesOnDisk.length > BaseConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                    - BaseConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk, (File f1, File f2) -> Long.valueOf(
                    f1.lastModified()).compareTo(f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }

        //Write out the slicer config
        SlicerType slicerTypeToUse = null;
        if (settingsToUse.getSlicerOverride() != null)
        {
            slicerTypeToUse = settingsToUse.getSlicerOverride();
        } else
        {
            slicerTypeToUse = printableMeshes.getDefaultSlicerType();
        }

        SlicerConfigWriter configWriter = SlicerConfigWriterFactory.getConfigWriter(
                slicerTypeToUse);

        //TODO material-dependent profiles
        // This is a hack to force the fan speed to 100% when using PLA
        if (associatedPrinter.reelsProperty().containsKey(0))
        {
            if (associatedPrinter.reelsProperty().get(0).material.get() == MaterialType.PLA
                    && SlicerParametersContainer.applicationProfileListContainsProfile(settingsToUse.
                            getProfileName()))
            {
                settingsToUse.setEnableCooling(true);
                settingsToUse.setMinFanSpeed_percent(100);
                settingsToUse.setMaxFanSpeed_percent(100);
            }
        }

        if (associatedPrinter.reelsProperty().containsKey(1))
        {
            if (associatedPrinter.reelsProperty().get(1).material.get() == MaterialType.PLA
                    && SlicerParametersContainer.applicationProfileListContainsProfile(settingsToUse.
                            getProfileName()))
            {
                settingsToUse.setEnableCooling(true);
                settingsToUse.setMinFanSpeed_percent(100);
                settingsToUse.setMaxFanSpeed_percent(100);
            }
        }
        // End of hack

        // Hack to change raft related settings for Draft ABS prints
        if (printableMeshes.getPrintQuality() == PrintQualityEnumeration.DRAFT
                && ((associatedPrinter.effectiveFilamentsProperty().get(0) != null
                && associatedPrinter.effectiveFilamentsProperty().get(0).getMaterial() == MaterialType.ABS)
                || (associatedPrinter.effectiveFilamentsProperty().get(1) != null
                && associatedPrinter.effectiveFilamentsProperty().get(0).getMaterial() == MaterialType.ABS)))
        {
            settingsToUse.setRaftBaseLinewidth_mm(1.250f);
            settingsToUse.setRaftAirGapLayer0_mm(0.285f);
            settingsToUse.setInterfaceLayers(1);
        }

        if (printableMeshes.getPrintQuality() == PrintQualityEnumeration.NORMAL
                && ((associatedPrinter.effectiveFilamentsProperty().get(0) != null
                && associatedPrinter.effectiveFilamentsProperty().get(0).getMaterial() == MaterialType.ABS)
                || (associatedPrinter.effectiveFilamentsProperty().get(1) != null
                && associatedPrinter.effectiveFilamentsProperty().get(1).getMaterial() == MaterialType.ABS)))
        {
            settingsToUse.setRaftAirGapLayer0_mm(0.4f);
        }
        // End of hack

        // Overwrite the settings 
        PrintableMeshes actualMeshesToPrint = new PrintableMeshes(
                printableMeshes.getMeshesForProcessing(),
                printableMeshes.getUsedExtruders(),
                printableMeshes.getExtruderForModel(),
                printableMeshes.getProjectName(),
                printableMeshes.getRequiredPrintJobID(),
                settingsToUse,
                printableMeshes.getPrintOverrides(),
                printableMeshes.getPrintQuality(),
                printableMeshes.getDefaultSlicerType(),
                printableMeshes.getCentreOfPrintedObject(),
                printableMeshes.isSafetyFeaturesRequired(),
                printableMeshes.isCameraEnabled(),
                printableMeshes.getCameraTriggerData());

        configWriter.setPrintCentre((float) (printableMeshes.getCentreOfPrintedObject().getX()),
                (float) (printableMeshes.getCentreOfPrintedObject().getZ()));
        configWriter.generateConfigForSlicer(settingsToUse,
                printJobDirectoryName
                + File.separator
                + printUUID
                + BaseConfiguration.printProfileFileExtension);

        slicerService.reset();
        slicerService.setPrintJobUUID(printUUID);
        slicerService.setPrinterToUse(associatedPrinter);
        slicerService.setPrintableMeshes(actualMeshesToPrint);
        slicerService.start();

        // Do we need to slice?
        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private boolean reprintFileFromDisk(PrintJob printJob, int startFromLineNumber)
    {
        String gCodeFileName = printJob.getRoboxisedFileLocation();
        String jobUUID = printJob.getJobUUID();
        boolean acceptedPrintRequest = false;
        canDisconnectDuringPrint = true;

        try
        {
            PrintJobStatistics printJobStatistics = printJob.getStatistics();
            linesInPrintingFile.set(printJobStatistics.getNumberOfLines());

            steno.info("Respooling job " + jobUUID + " to printer from line " + startFromLineNumber);
            transferGCodeToPrinterService.reset();
            transferGCodeToPrinterService.setCurrentPrintJobID(jobUUID);
            transferGCodeToPrinterService.setStartFromSequenceNumber(startFromLineNumber);
            transferGCodeToPrinterService.setModelFileToPrint(gCodeFileName);
            transferGCodeToPrinterService.setPrinterToUse(associatedPrinter);
            transferGCodeToPrinterService.setPrintJobStatistics(printJobStatistics);
            transferGCodeToPrinterService.start();
            acceptedPrintRequest = true;
        } catch (IOException ex)
        {
            steno.error("Couldn't get job statistics for job " + jobUUID);
        }
        return acceptedPrintRequest;
    }

    protected boolean reprintFileFromDisk(PrintJob printJob)
    {
        return reprintFileFromDisk(printJob, 0);
    }

    private boolean reprintDirectFromPrinter(PrintJob printJob) throws RoboxCommsException
    {
        boolean acceptedPrintRequest;
        //Reprint directly from printer
        steno.info("Printing job " + printJob.getJobUUID() + " from printer store");
        if (raiseProgressNotifications)
        {
            BaseLookup.getSystemNotificationHandler().showReprintStartedNotification();
        }

        if (printJob.roboxisedFileExists())
        {
            try
            {
                linesInPrintingFile.set(printJob.getStatistics().getNumberOfLines());
            } catch (IOException ex)
            {
                steno.error("Couldn't get job statistics for job " + printJob.getJobUUID());
            }
        }
        associatedPrinter.initiatePrint(printJob.getJobUUID());
        acceptedPrintRequest = true;
        return acceptedPrintRequest;
    }

    /**
     *
     * @return
     */
    public ReadOnlyDoubleProperty secondaryProgressProperty()
    {
        return secondaryProgressPercent;
    }

    @Override
    public ReadOnlyBooleanProperty runningProperty()
    {
        return dialogRequired;
    }

    @Override
    public ReadOnlyStringProperty messageProperty()
    {
        return printProgressMessage;
    }

    @Override
    public ReadOnlyDoubleProperty progressProperty()
    {
        return primaryProgressPercent;
    }

    @Override
    public ReadOnlyStringProperty titleProperty()
    {
        return printProgressTitle;
    }

    @Override
    public boolean cancelRun()
    {
        return false;
    }

    public ReadOnlyIntegerProperty linesInPrintingFileProperty()
    {
        return linesInPrintingFile;
    }

    protected boolean printGCodeFile(final String printJobName, final String filename, final boolean useSDCard, final boolean canDisconnectDuringPrint) throws MacroPrintException
    {
        return printGCodeFile(printJobName, filename, useSDCard, false, canDisconnectDuringPrint);
    }

    protected boolean printGCodeFile(final String printJobName, final String filename, final boolean useSDCard,
            final boolean dontInitiatePrint, final boolean canDisconnectDuringPrint) throws MacroPrintException
    {
        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;
        this.canDisconnectDuringPrint = canDisconnectDuringPrint;

        //Create the print job directory
        String printUUID = createPrintJobDirectory();

        tidyPrintSpoolDirectory();

        String printjobFilename = BaseConfiguration.getPrintSpoolDirectory()
                + printUUID + File.separator + printUUID
                + BaseConfiguration.gcodeTempFileExtension;

        if (printJobName != null)
        {
            PrintJob printJob = new PrintJob(printUUID);
            PrintJobStatistics printJobStatistics = new PrintJobStatistics();
            printJobStatistics.setProjectName(printJobName);
            try
            {
                printJobStatistics.writeStatisticsToFile(printJob.getStatisticsFileLocation());
            } catch (IOException ex)
            {
                steno.exception("Failed to write statistics to file: " + printJob.getStatisticsFileLocation(), ex);
            }
        }

        File src = new File(filename);
        File dest = new File(printjobFilename);
        try
        {
            FileUtils.copyFile(src, dest);
            BaseLookup.getTaskExecutor().runOnGUIThread(() ->
            {
                int numberOfLines = GCodeMacros.countLinesInMacroFile(dest, ";");
                raiseProgressNotifications = true;
                linesInPrintingFile.set(numberOfLines);
                transferGCodeToPrinterService.reset();
                transferGCodeToPrinterService.setPrintUsingSDCard(useSDCard);
                transferGCodeToPrinterService.setCurrentPrintJobID(printUUID);
                transferGCodeToPrinterService.setModelFileToPrint(printjobFilename);
                transferGCodeToPrinterService.setPrinterToUse(associatedPrinter);
                transferGCodeToPrinterService.dontInitiatePrint(dontInitiatePrint);
                transferGCodeToPrinterService.start();
                consideringPrintRequest = false;
            });

            acceptedPrintRequest = true;
        } catch (IOException ex)
        {
            steno.error("Error copying file");
        }

        return acceptedPrintRequest;
    }

    private void tidyPrintSpoolDirectory()
    {
        //Erase old print job directories
        File printSpoolDirectory = new File(
                BaseConfiguration.getPrintSpoolDirectory());
        File[] filesOnDisk = printSpoolDirectory.listFiles();
        if (filesOnDisk.length > BaseConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                    - BaseConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk,
                    (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(
                            f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }
    }

    private void tidyMacroSpoolDirectory()
    {
        //Erase old print job directories
        File printSpoolDirectory = new File(
                BaseConfiguration.getApplicationStorageDirectory()
                + BaseConfiguration.macroFileSubpath);
        File[] filesOnDisk = printSpoolDirectory.listFiles();

        if (filesOnDisk.length > BaseConfiguration.maxPrintSpoolFiles)
        {
            int filesToDelete = filesOnDisk.length
                    - BaseConfiguration.maxPrintSpoolFiles;
            Arrays.sort(filesOnDisk,
                    (File f1, File f2) -> Long.valueOf(f1.lastModified()).compareTo(
                            f2.lastModified()));
            for (int i = 0; i < filesToDelete; i++)
            {
                FileUtils.deleteQuietly(filesOnDisk[i]);
            }
        }
    }

    protected boolean runMacroPrintJob(Macro macro) throws MacroPrintException
    {
        return runMacroPrintJob(macro, true, false, false);
    }

    protected boolean runMacroPrintJob(Macro macro,
            boolean requireNozzle0,
            boolean requireNozzle1,
            boolean requireSafetyFeatures) throws MacroPrintException
    {
        return runMacroPrintJob(macro, true, requireNozzle0, requireNozzle1, requireSafetyFeatures);
    }

    protected boolean runMacroPrintJob(Macro macro, boolean useSDCard,
            boolean requireNozzle0,
            boolean requireNozzle1,
            boolean requireSafetyFeatures) throws MacroPrintException
    {
        macroBeingRun.set(macro);

        boolean acceptedPrintRequest = false;
        consideringPrintRequest = true;
        canDisconnectDuringPrint = false;

        //Create the print job directory
        String printUUID = macro.getMacroJobNumber();
        String printJobDirectoryName = BaseConfiguration.getApplicationStorageDirectory()
                + BaseConfiguration.macroFileSubpath;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();

        tidyMacroSpoolDirectory();

        String printjobFilename = printJobDirectoryName + printUUID
                + BaseConfiguration.gcodeTempFileExtension;

        File printjobFile = new File(printjobFilename);

        try
        {
            ArrayList<String> macroContents = GCodeMacros.getMacroContents(macro.getMacroFileName(),
                    associatedPrinter.headProperty().get().typeCodeProperty().get(),
                    requireNozzle0, requireNozzle1,
                    requireSafetyFeatures);
            // Write the contents of the macro file to the print area
            FileUtils.writeLines(printjobFile, macroContents, false);
        } catch (IOException ex)
        {
            throw new MacroPrintException("Error writing macro print job file: "
                    + printjobFilename + " : "
                    + ex.getMessage());
        } catch (MacroLoadException ex)
        {
            throw new MacroPrintException("Error whilst generating macro - " + ex.getMessage());
        }

        BaseLookup.getTaskExecutor().runOnGUIThread(() ->
        {
            int numberOfLines = GCodeMacros.countLinesInMacroFile(printjobFile, ";");
            raiseProgressNotifications = false;
            linesInPrintingFile.set(numberOfLines);
            steno.
                    info("Print service is in state:" + transferGCodeToPrinterService.stateProperty().
                            get().name());
            if (transferGCodeToPrinterService.isRunning())
            {
                transferGCodeToPrinterService.cancel();
            }
            transferGCodeToPrinterService.reset();
            transferGCodeToPrinterService.setPrintUsingSDCard(useSDCard);
            transferGCodeToPrinterService.setStartFromSequenceNumber(0);
            transferGCodeToPrinterService.setCurrentPrintJobID(printUUID);
            transferGCodeToPrinterService.setModelFileToPrint(printjobFilename);
            transferGCodeToPrinterService.setPrinterToUse(associatedPrinter);
            transferGCodeToPrinterService.setThisCanBeReprinted(false);
            transferGCodeToPrinterService.start();
            consideringPrintRequest = false;
        });

        acceptedPrintRequest = true;

        return acceptedPrintRequest;
    }

    private String createPrintJobDirectory()
    {
        //Create the print job directory
        String printUUID = SystemUtils.generate16DigitID();
        String printJobDirectoryName = BaseConfiguration.getPrintSpoolDirectory()
                + printUUID;
        File printJobDirectory = new File(printJobDirectoryName);
        printJobDirectory.mkdirs();
        return printUUID;
    }

    public boolean isConsideringPrintRequest()
    {
        return consideringPrintRequest;
    }

    public IntegerProperty progressETCProperty()
    {
        return progressETC;
    }

    public ReadOnlyBooleanProperty etcAvailableProperty()
    {
        return etcAvailable;
    }

    public ReadOnlyIntegerProperty progressCurrentLayerProperty()
    {
        return progressCurrentLayer;
    }

    public ReadOnlyIntegerProperty progressNumLayersProperty()
    {
        return progressNumLayers;
    }

    /**
     * Stop all services, in the GUI thread. Block current thread until the
     * routine has completed.
     */
    protected void stopAllServices()
    {

        Callable<Boolean> stopServices = new Callable()
        {
            @Override
            public Boolean call() throws Exception
            {
                steno.debug("Shutdown print services...");
                if (slicerService.isRunning())
                {
                    steno.debug("Shutdown slicer service...");
                    slicerService.cancelRun();
                }
                if (postProcessorService.isRunning())
                {
                    steno.debug("Shutdown PP...");
                    postProcessorService.cancelRun();
                }
                if (transferGCodeToPrinterService.isRunning())
                {
                    steno.debug("Shutdown print service...");
                    transferGCodeToPrinterService.cancelRun();
                }
//                if (movieMakerTask.isRunning())
//                {
//                    steno.info("Shutdown move maker");
//                    movieMakerTask.shutdown();
//                }
                steno.debug("Shutdown print services complete");
                return true;
            }
        };
        FutureTask<Boolean> stopServicesTask = new FutureTask<>(stopServices);
        BaseLookup.getTaskExecutor().runOnGUIThread(stopServicesTask);
        try
        {
            stopServicesTask.get();
        } catch (InterruptedException | ExecutionException ex)
        {
            steno.error("Error while stopping services: " + ex);
        }
    }

    public boolean reEstablishTransfer(String printJobID, int expectedSequenceNumber)
    {
        PrintJob printJob = new PrintJob(printJobID);
        boolean acceptedPrintRequest = false;

        if (printJob.roboxisedFileExists())
        {
            acceptedPrintRequest = reprintFileFromDisk(printJob, expectedSequenceNumber);
            if (raiseProgressNotifications)
            {
                BaseLookup.getSystemNotificationHandler().removePrintTransferFailedNotification();
            }
        }

        return acceptedPrintRequest;
    }

    private void detectAlreadyPrinting()
    {
        boolean roboxIsPrinting = false;

        if (associatedPrinter != null)
        {
            String printJobID = associatedPrinter.printJobIDProperty().get();
            if (printJobID != null)
            {
                if (!printJobID.equals("")
                        && printJobID.codePointAt(0) != 0)
                {
                    roboxIsPrinting = true;
                }
            }

            if (roboxIsPrinting)
            {
                if (!iAmTakingItThroughTheBackDoor
                        && !transferGCodeToPrinterService.isRunning())
                {
                    try
                    {
                        SendFile sendFileData = (SendFile) associatedPrinter.requestSendFileReport();

                        if (sendFileData != null
                                && sendFileData.getFileID() != null && !sendFileData.getFileID().equals(""))
                        {
                            if (reEstablishTransfer(sendFileData.getFileID(),
                                    sendFileData.
                                            getExpectedSequenceNumber()))
                            {
                                steno.info("The printer is printing an incomplete job: File ID: "
                                        + sendFileData.getFileID()
                                        + " Expected sequence number: " + sendFileData.getExpectedSequenceNumber());
                            }
                        }
                    } catch (RoboxCommsException ex)
                    {
                        steno.error(
                                "Error determining whether the printer has a partially transferred job in progress");
                    }
                }

                Optional<Macro> macroRunning = Macro.getMacroForPrintJobID(printJobID);

                if (macroRunning.isPresent())
                {
                    steno.debug("Printer "
                            + associatedPrinter.getPrinterIdentity().printerFriendlyName.get()
                            + " is running macro " + macroRunning.get().name());

                    macroBeingRun.set(macroRunning.get());
                    printQueueStatus.set(PrintQueueStatus.RUNNING_MACRO);
                    setParentPrintStatusIfIdle(PrinterStatus.RUNNING_MACRO_FILE);
                } else
                {
                    makeETCCalculatorForJobOfUUID(printJobID);

                    if (etcAvailable.get())
                    {
                        updateETCUsingETCCalculator(associatedPrinter.printJobLineNumberProperty().get());
                    } else
                    {
                        updateETCUsingLineNumber(associatedPrinter.printJobLineNumberProperty().get());
                    }

                    steno.debug("Printer "
                            + associatedPrinter.getPrinterIdentity().printerFriendlyName.get()
                            + " is printing");

                    printQueueStatus.set(PrintQueueStatus.PRINTING);
                    setParentPrintStatusIfIdle(PrinterStatus.PRINTING_PROJECT);
                }
            } else
            {
                printQueueStatus.set(PrintQueueStatus.IDLE);
                switch (associatedPrinter.printerStatusProperty().get())
                {
                    case PRINTING_PROJECT:
                    case RUNNING_MACRO_FILE:
                        associatedPrinter.setPrinterStatus(PrinterStatus.IDLE);
                        steno.info("Print Job complete - " + associatedPrinter.getPrinterIdentity().printerFriendlyName.get() + "---------------------------------------<");
                        break;
                }
                macroBeingRun.set(null);
            }
        }
    }

    private void setParentPrintStatusIfIdle(PrinterStatus desiredStatus)
    {
        switch (associatedPrinter.printerStatusProperty().get())
        {
            case IDLE:
                associatedPrinter.setPrinterStatus(desiredStatus);
                break;
        }
    }

    public ReadOnlyObjectProperty<PrintQueueStatus> printQueueStatusProperty()
    {
        return printQueueStatus;
    }

    public ReadOnlyObjectProperty<PrintJob> printJobProperty()
    {
        return printJob;
    }

    public ReadOnlyBooleanProperty highIntensityCommsInProgressProperty()
    {
        return highIntensityCommsInProgress;
    }

    public void takingItThroughTheBackDoor(boolean ohYesIAm)
    {
        iAmTakingItThroughTheBackDoor = ohYesIAm;
    }
}
