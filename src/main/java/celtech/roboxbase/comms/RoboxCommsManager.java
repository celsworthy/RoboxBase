package celtech.roboxbase.comms;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.comms.rx.StatusResponse;
import celtech.roboxbase.printerControl.model.HardwarePrinter;
import celtech.roboxbase.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RoboxCommsManager implements PrinterStatusConsumer, DeviceDetectionListener
{

    private static RoboxCommsManager instance = null;

    private final String printerToSearchFor = "Robox";
    private final String roboxVendorID = "16D0";
    private final String roboxProductID = "081B";

    private Stenographer steno = null;
    private final List<Printer> dummyPrinters = new ArrayList<>();
    private final HashMap<DetectedDevice, Printer> pendingPrinters = new HashMap<>();
    private final HashMap<DetectedDevice, Printer> activePrinters = new HashMap<>();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecksMS = 1000;

    private String dummyPrinterPort = "DummyPrinterPort";

    private int dummyPrinterCounter = 0;

    private final SerialDeviceDetector usbSerialDeviceDetector;
    private final RemotePrinterDetector remotePrinterDetector;

    private boolean doNotCheckForPresenceOfHead = false;
    private boolean detectLoadedFilament = false;
    private boolean searchForRemotePrinters = false;

    private RoboxCommsManager(String pathToBinaries,
            boolean suppressPrinterIDChecks,
            boolean doNotCheckForPresenceOfHead,
            boolean detectLoadedFilament,
            boolean searchForRemotePrinters)
    {
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.doNotCheckForPresenceOfHead = doNotCheckForPresenceOfHead;
        this.detectLoadedFilament = detectLoadedFilament;
        this.searchForRemotePrinters = searchForRemotePrinters;

        usbSerialDeviceDetector = new SerialDeviceDetector(pathToBinaries, roboxVendorID, roboxProductID, printerToSearchFor, this);
        remotePrinterDetector = new RemotePrinterDetector(this);

        steno = StenographerFactory.getStenographer(this.getClass().getName());
    }

    /**
     *
     * @return
     */
    public static RoboxCommsManager getInstance()
    {
        return instance;
    }

    /**
     *
     * @param pathToBinaries
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries, false, false, true, true);
        }

        return instance;
    }

    /**
     *
     * @param pathToBinaries
     * @param doNotCheckForHeadPresence
     * @param detectLoadedFilament
     * @param searchForRemotePrinters
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries,
            boolean doNotCheckForHeadPresence,
            boolean detectLoadedFilament,
            boolean searchForRemotePrinters)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries,
                    false,
                    doNotCheckForHeadPresence,
                    detectLoadedFilament,
                    searchForRemotePrinters);
        }

        return instance;
    }

    private void assessCandidatePrinter(DetectedDevice detectedPrinter)
    {
        if (detectedPrinter != null)
        {
            boolean noNeedToAddPrinter = false;

            for (DetectedDevice pendingPrinterToCheck : pendingPrinters.keySet())
            {
                if (detectedPrinter.equals(pendingPrinterToCheck))
                {
                    noNeedToAddPrinter = true;
                    break;
                }
            }

            if (!noNeedToAddPrinter)
            {
                for (DetectedDevice activePrinterToCheck : activePrinters.keySet())
                {
                    if (detectedPrinter.equals(activePrinterToCheck))
                    {
                        noNeedToAddPrinter = true;
                        break;
                    }
                }
            }

            if (!noNeedToAddPrinter)
            {
                // We need to connect!
                steno.debug("Adding new printer " + detectedPrinter);

                Printer newPrinter = makePrinter(detectedPrinter);
                pendingPrinters.put(detectedPrinter, newPrinter);
                newPrinter.startComms();
            }
        }
    }

    private Printer makePrinter(DetectedDevice detectedPrinter)
    {
        HardwarePrinter.FilamentLoadedGetter filamentLoadedGetter
                = (StatusResponse statusResponse, int extruderNumber) ->
        {
            if (!detectLoadedFilament)
            {
                // if this preference has been deselected then always say that the filament
                // has been detected as loaded.
                return true;
            } else
            {
                if (extruderNumber == 1)
                {
                    return statusResponse.isFilament1SwitchStatus();
                } else
                {
                    return statusResponse.isFilament2SwitchStatus();
                }
            }
        };
        Printer newPrinter = null;

        switch (detectedPrinter.getConnectionType())
        {
            case SERIAL:
                newPrinter = new HardwarePrinter(this, new HardwareCommandInterface(
                        this, detectedPrinter, suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS), filamentLoadedGetter,
                        doNotCheckForPresenceOfHead);
                break;
            case ROBOX_REMOTE:
                RoboxRemoteCommandInterface commandInterface = new RoboxRemoteCommandInterface(
                        this, (RemoteDetectedPrinter) detectedPrinter, suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS);

                newPrinter = new HardwarePrinter(this, commandInterface, filamentLoadedGetter,
                        doNotCheckForPresenceOfHead);
                break;
            default:
                steno.error("Don't know how to handle connected printer: " + detectedPrinter);
                break;
        }
        return newPrinter;
    }

    public void start()
    {
        BaseLookup.getTaskExecutor().runOnBackgroundThread(() ->
        {   
            if (!remotePrinterDetector.isAlive() && searchForRemotePrinters)
            {
                remotePrinterDetector.start();
            }
        });

        if (!usbSerialDeviceDetector.isAlive())
        {
            usbSerialDeviceDetector.start();
        }
    }

    /**
     *
     */
    public void shutdown()
    {
        usbSerialDeviceDetector.shutdownDetector();
        remotePrinterDetector.shutdownDetector();

        List<Printer> printersToShutdown = new ArrayList<>();
        BaseLookup.getConnectedPrinters().forEach(printer ->
        {
            printersToShutdown.add(printer);
        });

        for (Printer printer : printersToShutdown)
        {
            steno.debug("Shutdown printer " + printer);
            try
            {
                printer.getCommandInterface().shutdown();
                printer.shutdown();
            } catch (Exception ex)
            {
                steno.error("Error shutting down printer");
            }
        }
    }

    /**
     *
     * @param detectedPrinter
     */
    @Override
    public void printerConnected(DetectedDevice detectedPrinter)
    {
        Printer printer = pendingPrinters.get(detectedPrinter);
        activePrinters.put(detectedPrinter, printer);
        printer.connectionEstablished();

        BaseLookup.printerConnected(printer);
    }

    /**
     *
     */
    @Override
    public void disconnected(DetectedDevice printerHandle)
    {
        pendingPrinters.remove(printerHandle);

        final Printer printerToRemove = activePrinters.get(printerHandle);
        if (printerToRemove != null)
        {
            printerToRemove.shutdown();
        }
        usbSerialDeviceDetector.notifyOfFailedCommsForPrinter(printerHandle);

        BaseLookup.printerDisconnected(printerToRemove);

        if (activePrinters.containsKey(printerHandle))
        {
            steno.info("Disconnected from " + activePrinters.get(printerHandle).getPrinterIdentity().printerFriendlyNameProperty().get());
            activePrinters.remove(printerHandle);
        }
    }

    public void addDummyPrinter()
    {
        dummyPrinterCounter++;
        String actualPrinterPort = dummyPrinterPort + " " + dummyPrinterCounter;
        DetectedDevice printerHandle = new DetectedDevice(DeviceDetector.PrinterConnectionType.SERIAL, actualPrinterPort);
        Printer nullPrinter = new HardwarePrinter(this,
                new DummyPrinterCommandInterface(this,
                        printerHandle,
                        suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS,
                        "DP "
                        + dummyPrinterCounter));
        pendingPrinters.put(printerHandle, nullPrinter);
        dummyPrinters.add(nullPrinter);
        nullPrinter.startComms();
    }

    public void removeDummyPrinter(DetectedDevice printerHandle)
    {
        disconnected(printerHandle);
    }

    public List<Printer> getDummyPrinters()
    {
        return dummyPrinters;
    }

    /**
     *
     * @param milliseconds
     */
    public void setSleepBetweenStatusChecks(int milliseconds)
    {
        sleepBetweenStatusChecksMS = milliseconds;
    }

    @Override
    public void deviceDetected(DetectedDevice detectedDevice)
    {
        assessCandidatePrinter(detectedDevice);
    }

    @Override
    public void deviceNoLongerPresent(DetectedDevice detectedDevice)
    {
        steno.info("Robox Comms Manager has been told that a printer is no longer detected: " + detectedDevice);
        Printer printerToDisconnect = activePrinters.get(detectedDevice);
        if (printerToDisconnect != null)
        {
            CommandInterface commandInterface = printerToDisconnect.getCommandInterface();
            if (commandInterface != null)
            {
                commandInterface.shutdown();
            } else
            {
                steno.info("CI was null");
            }
        } else
        {
            steno.info("not in active list");
        }
    }
}
