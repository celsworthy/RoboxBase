package celtech.roboxbase.comms;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.AckResponse;
import celtech.roboxbase.comms.rx.FirmwareError;
import celtech.roboxbase.comms.rx.FirmwareResponse;
import celtech.roboxbase.comms.rx.PrinterIDResponse;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.rx.StatusResponse;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import celtech.roboxbase.comms.tx.RoboxTxPacketFactory;
import celtech.roboxbase.comms.tx.StatusRequest;
import celtech.roboxbase.comms.tx.TxPacketTypeEnum;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterException;
import celtech.roboxbase.services.firmware.FirmwareLoadResult;
import celtech.roboxbase.services.firmware.FirmwareLoadService;
import celtech.roboxbase.utils.PrinterUtils;
import javafx.concurrent.WorkerStateEvent;
import libertysystems.configuration.ConfigItemIsAnArray;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public abstract class CommandInterface extends Thread
{

    protected boolean keepRunning = true;

    protected Stenographer steno = StenographerFactory.getStenographer(
            HardwareCommandInterface.class.getName());
    protected PrinterStatusConsumer controlInterface = null;
    protected DetectedDevice printerHandle = null;
    protected Printer printerToUse = null;
    protected String printerFriendlyName = "Robox";
    protected RoboxCommsState commsState = RoboxCommsState.FOUND;
    protected PrinterID printerID = new PrinterID();

    protected final FirmwareLoadService firmwareLoadService = new FirmwareLoadService();
    protected String requiredFirmwareVersionString = "";
    protected float requiredFirmwareVersion = 0;
    protected float firmwareVersionInUse = 0;

    protected boolean suppressPrinterIDChecks = false;
    protected int sleepBetweenStatusChecks = 1000;
    private boolean loadingFirmware = false;

    protected boolean suppressComms = false;

    private String printerName = null;

    private StatusResponse latestStatusResponse = null;
    private AckResponse latestErrorResponse = null;
    private PrinterIDResponse lastPrinterIDResponse = null;

    private boolean isConnected = false;

    /**
     *
     * @param controlInterface
     * @param printerHandle
     * @param suppressPrinterIDChecks
     * @param sleepBetweenStatusChecks
     */
    public CommandInterface(PrinterStatusConsumer controlInterface,
            DetectedDevice printerHandle,
            boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        this.controlInterface = controlInterface;
        this.printerHandle = printerHandle;
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.sleepBetweenStatusChecks = sleepBetweenStatusChecks;

        try
        {
            Configuration applicationConfiguration = Configuration.getInstance();
            try
            {
                requiredFirmwareVersionString = applicationConfiguration.getString(
                        BaseConfiguration.applicationConfigComponent, "requiredFirmwareVersion").
                        trim();
                requiredFirmwareVersion = Float.valueOf(requiredFirmwareVersionString);
            } catch (ConfigItemIsAnArray ex)
            {
                steno.error("Firmware version was an array... can't interpret firmware version");
            }
        } catch (ConfigNotLoadedException ex)
        {
            steno.error("Couldn't load configuration - will not be able to check firmware version");
        }

        firmwareLoadService.setOnSucceeded((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            BaseLookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
            disconnectPrinter();
        });

        firmwareLoadService.setOnFailed((WorkerStateEvent t) ->
        {
            FirmwareLoadResult result = (FirmwareLoadResult) t.getSource().getValue();
            BaseLookup.getSystemNotificationHandler().showFirmwareUpgradeStatusNotification(result);
            disconnectPrinter();
        });

        BaseLookup.getSystemNotificationHandler().configureFirmwareProgressDialog(firmwareLoadService);
    }

    @Override

    public void run()
    {
        while (keepRunning)
        {
            switch (commsState)
            {
                case FOUND:
                    steno.debug("Trying to connect to printer in " + printerHandle);

                    boolean printerCommsOpen = connectToPrinter();
                    if (printerCommsOpen)
                    {
                        steno.debug("Connected to Robox on " + printerHandle);
                        commsState = RoboxCommsState.CHECKING_FIRMWARE;
                    } else
                    {
                        steno.error("Failed to connect to Robox on " + printerHandle);
                        controlInterface.failedToConnect(printerHandle);
                        keepRunning = false;
                    }
                    break;

                case CHECKING_FIRMWARE:
                    steno.debug("Check firmware " + printerHandle);
                    if (loadingFirmware)
                    {
                        try
                        {
                            Thread.sleep(200);
                        } catch (InterruptedException ex)
                        {
                            steno.error("Interrupted while waiting for firmware to be loaded " + ex);
                        }
                        break;
                    }

                    FirmwareResponse firmwareResponse = null;
                    boolean loadRequiredFirmware = false;

                    try
                    {
                        firmwareResponse = printerToUse.readFirmwareVersion();

                        if (firmwareResponse.getFirmwareRevisionFloat() != requiredFirmwareVersion)
                        {
                            // The firmware version is different to that associated with AutoMaker
                            steno.warning("Firmware version is "
                                    + firmwareResponse.getFirmwareRevisionString() + " and should be "
                                    + requiredFirmwareVersionString);

//                            Lookup.setFirmwareVersion()
                            //ROB-931 - don't check for presence of the SD card if firmware version earlier than 691
                            if (firmwareResponse.getFirmwareRevisionFloat() >= 691)
                            {
                                // Is the SD card present?
                                try
                                {
                                    StatusRequest request = (StatusRequest) RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.STATUS_REQUEST);
                                    firmwareVersionInUse = firmwareResponse.getFirmwareRevisionFloat();
                                    StatusResponse response = (StatusResponse) writeToPrinter(request, true);
                                    if (!response.issdCardPresent())
                                    {
                                        steno.warning("SD Card not present");
                                        BaseLookup.getSystemNotificationHandler().processErrorPacketFromPrinter(FirmwareError.SD_CARD, printerToUse);
                                        disconnectPrinter();
                                        keepRunning = false;
                                        break;
                                    } else
                                    {
                                        BaseLookup.getSystemNotificationHandler().clearAllDialogsOnDisconnect();
                                    }
                                } catch (RoboxCommsException ex)
                                {
                                    steno.error("Failure during printer status request. " + ex.toString());
                                    break;
                                }
                            }

                            // Tell the user to update
                            loadRequiredFirmware = BaseLookup.getSystemNotificationHandler().
                                    askUserToUpdateFirmware();
                        }

                        if (loadRequiredFirmware)
                        {
                            loadingFirmware = true;
                            loadFirmware(BaseConfiguration.getCommonApplicationDirectory()
                                    + "robox_r" + requiredFirmwareVersionString + ".bin");
                        } else
                        {
                            firmwareVersionInUse = firmwareResponse.getFirmwareRevisionFloat();
                            moveOnFromFirmwareCheck(firmwareResponse);
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Exception whilst checking firmware version: " + ex);
                        disconnectPrinter();
                    }
                    break;

                case CHECKING_ID:
                    steno.debug("Check id " + printerHandle);

                    try
                    {
                        lastPrinterIDResponse = printerToUse.readPrinterID();

                        printerName = lastPrinterIDResponse.getPrinterFriendlyName();

                        if (printerName == null
                                || (printerName.length() > 0
                                && printerName.charAt(0) == '\0'))
                        {
                            steno.info("Connected to unknown printer");
                            BaseLookup.getSystemNotificationHandler().
                                    showNoPrinterIDDialog(printerToUse);
                            lastPrinterIDResponse = printerToUse.readPrinterID();
                        } else
                        {
                            steno.info("Connected to printer " + printerName);
                        }
                    } catch (PrinterException ex)
                    {
                        steno.error("Error whilst checking printer ID");
                    }

                    commsState = RoboxCommsState.DETERMINING_PRINTER_STATUS;
                    break;

                case DETERMINING_PRINTER_STATUS:
                    steno.debug("Determining printer status on port " + printerHandle);

                    try
                    {
                        StatusResponse statusResponse = (StatusResponse) writeToPrinter(
                                RoboxTxPacketFactory.createPacket(
                                        TxPacketTypeEnum.STATUS_REQUEST), true);

                        determinePrinterStatus(statusResponse);

                        controlInterface.printerConnected(printerHandle);

                        //Stash the connected printer info
                        String printerIDToUse = null;
                        if (lastPrinterIDResponse != null
                                && lastPrinterIDResponse.getAsFormattedString() != null)
                        {
                            printerIDToUse = lastPrinterIDResponse.getAsFormattedString();
                        }
                        BaseConfiguration.getCoreMemory().setLastPrinterSerial(printerIDToUse);
                        BaseConfiguration.getCoreMemory().setLastPrinterFirmwareVersion(firmwareVersionInUse);

                        commsState = RoboxCommsState.CONNECTED;
                    } catch (RoboxCommsException ex)
                    {
                        if (printerFriendlyName != null)
                        {
                            steno.error("Failed to determine printer status on "
                                    + printerFriendlyName);
                        } else
                        {
                            steno.error("Failed to determine printer status on unknown printer");
                        }
                        disconnectPrinter();
                    }

                    break;

                case CONNECTED:
//                    steno.debug("CONNECTED " + portName);
                    try
                    {
                        this.sleep(sleepBetweenStatusChecks);

                        if (!suppressComms && isConnected)
                        {
                            try
                            {
//                        steno.debug("STATUS REQUEST: " + portName);
                                latestStatusResponse = (StatusResponse) writeToPrinter(RoboxTxPacketFactory.createPacket(
                                        TxPacketTypeEnum.STATUS_REQUEST));

                                latestErrorResponse = (AckResponse) writeToPrinter(RoboxTxPacketFactory.createPacket(TxPacketTypeEnum.REPORT_ERRORS));
                            } catch (RoboxCommsException ex)
                            {
                                steno.error("Failure during printer status request. " + ex.toString());
                                disconnectPrinter();
                            }
                        }
                    } catch (InterruptedException ex)
                    {
                        steno.info("Comms interrupted");
                    }
                    break;

                case DISCONNECTED:
                    steno.debug("state is disconnected");
                    break;
            }
        }
        steno.info(
                "Handler for " + printerHandle + " exiting");
    }

    private void moveOnFromFirmwareCheck(FirmwareResponse firmwareResponse)
    {
        if (suppressPrinterIDChecks == false)
        {
            commsState = RoboxCommsState.CHECKING_ID;
        } else
        {
            commsState = RoboxCommsState.DETERMINING_PRINTER_STATUS;
        }
        loadingFirmware = false;
    }

    public void loadFirmware(String firmwareFilePath)
    {
        suppressComms = true;
        this.interrupt();
        firmwareLoadService.reset();
        firmwareLoadService.setPrinterToUse(printerToUse);
        firmwareLoadService.setFirmwareFileToLoad(firmwareFilePath);
        firmwareLoadService.start();
    }

    public void shutdown()
    {
        steno.info("Shutdown command interface...");
        suppressComms = true;
        if (firmwareLoadService.isRunning())
        {
            steno.info("Shutdown command interface firmware service...");
            firmwareLoadService.cancel();
        }
        steno.debug("set state to disconnected");
        commsState = RoboxCommsState.DISCONNECTED;
        disconnectPrinter();
        steno.info("Shutdown command interface complete");
        keepRunning = false;
    }

    /**
     *
     * @param sleepMillis
     */
    protected abstract void setSleepBetweenStatusChecks(int sleepMillis);

    /**
     *
     * @param messageToWrite
     * @return
     * @throws RoboxCommsException
     */
    public final synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        if (isConnected)
        {
            return writeToPrinterImpl(messageToWrite, false);
        } else
        {
            return null;
        }
    }

    public final RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite,
            boolean dontPublishResult) throws RoboxCommsException
    {
        if (isConnected)
        {
            return writeToPrinterImpl(messageToWrite, dontPublishResult);
        } else
        {
            return null;
        }
    }

    protected abstract RoboxRxPacket writeToPrinterImpl(RoboxTxPacket messageToWrite,
            boolean dontPublishResult) throws RoboxCommsException;

    /**
     *
     * @param printer
     */
    public void setPrinter(Printer printer)
    {
        this.printerToUse = printer;
    }

    /**
     *
     * @return
     */
    public final boolean connectToPrinter()
    {
        isConnected = connectToPrinterImpl();
        return isConnected;
    }

    protected abstract boolean connectToPrinterImpl();

    /**
     *
     */
    protected final void disconnectPrinter()
    {
        isConnected = false;
        disconnectPrinterImpl();
    }

    protected abstract void disconnectPrinterImpl();

    private void determinePrinterStatus(StatusResponse statusResponse)
    {
        if (PrinterUtils.printJobIDIndicatesPrinting(statusResponse.getRunningPrintJobID()))
        {
            if (printerFriendlyName != null)
            {
                steno.info(printerFriendlyName + " is printing");
            } else
            {
                steno.error("Connected to an unknown printer that is printing");
            }

        }
    }

    public void operateRemotely(boolean enableRemoteOperation)
    {
        suppressComms = enableRemoteOperation;
    }

    public AckResponse getLastErrorResponse()
    {
        return latestErrorResponse;
    }

    public StatusResponse getLastStatusResponse()
    {
        return latestStatusResponse;
    }
}
