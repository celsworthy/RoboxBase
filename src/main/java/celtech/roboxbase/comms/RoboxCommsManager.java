package celtech.roboxbase.comms;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.comms.rx.StatusResponse;
import celtech.roboxbase.printerControl.model.HardwarePrinter;
import celtech.roboxbase.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RoboxCommsManager extends Thread implements PrinterStatusConsumer
{

    private static RoboxCommsManager instance = null;

    private boolean keepRunning = true;

    private final String printerToSearchFor = "Robox";
    private final String roboxVendorID = "16D0";
    private final String roboxProductID = "081B";

    private Stenographer steno = null;
    private final ObservableMap<DetectedDevice, Printer> activePrinters = FXCollections.observableHashMap();
    private boolean suppressPrinterIDChecks = false;
    private int sleepBetweenStatusChecksMS = 1000;

    private String dummyPrinterPort = "DummyPrinterPort";

    private int dummyPrinterCounter = 0;

    private final SerialDeviceDetector usbSerialDeviceDetector;
    private final RemotePrinterDetector remotePrinterDetector;

    private boolean doNotCheckForPresenceOfHead = false;
    private BooleanProperty detectLoadedFilamentOverride = new SimpleBooleanProperty(true);
    private boolean searchForRemotePrinters = false;

    private final BooleanBinding tooManyRoboxAttachedProperty;

    private RoboxCommsManager(String pathToBinaries,
            boolean suppressPrinterIDChecks,
            boolean doNotCheckForPresenceOfHead,
            BooleanProperty detectLoadedFilamentProperty,
            boolean searchForRemotePrinters)
    {
        this.suppressPrinterIDChecks = suppressPrinterIDChecks;
        this.doNotCheckForPresenceOfHead = doNotCheckForPresenceOfHead;
        this.detectLoadedFilamentOverride = detectLoadedFilamentProperty;
        this.searchForRemotePrinters = searchForRemotePrinters;

        this.setDaemon(true);
        this.setName("Robox Comms Manager");
        this.setPriority(6);

        usbSerialDeviceDetector = new SerialDeviceDetector(pathToBinaries, roboxVendorID, roboxProductID, printerToSearchFor);
        remotePrinterDetector = new RemotePrinterDetector();

        steno = StenographerFactory.getStenographer(this.getClass().getName());

        tooManyRoboxAttachedProperty = Bindings.size(activePrinters).greaterThan(9);
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
            instance = new RoboxCommsManager(pathToBinaries, false, false, new SimpleBooleanProperty(true), true);
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
                    new SimpleBooleanProperty(detectLoadedFilament),
                    searchForRemotePrinters);
        }

        return instance;
    }

    /**
     *
     * @param pathToBinaries
     * @param doNotCheckForHeadPresence
     * @param detectLoadedFilamentProperty
     * @param searchForRemotePrinters
     * @return
     */
    public static RoboxCommsManager getInstance(String pathToBinaries,
            boolean doNotCheckForHeadPresence,
            BooleanProperty detectLoadedFilamentProperty,
            boolean searchForRemotePrinters)
    {
        if (instance == null)
        {
            instance = new RoboxCommsManager(pathToBinaries,
                    false,
                    doNotCheckForHeadPresence,
                    detectLoadedFilamentProperty,
                    searchForRemotePrinters);
        }

        return instance;
    }

    private void assessCandidatePrinter(DetectedDevice detectedPrinter)
    {
        if (detectedPrinter != null
                && !activePrinters.keySet().contains(detectedPrinter)
                && !tooManyRoboxAttachedProperty.get())
        {
            // We need to connect!
            steno.info("Adding new printer on " + detectedPrinter.getConnectionHandle());

            Printer newPrinter = makePrinter(detectedPrinter);
            activePrinters.put(detectedPrinter, newPrinter);
            newPrinter.startComms();
        }
    }

    private Printer makePrinter(DetectedDevice detectedPrinter)
    {
        HardwarePrinter.FilamentLoadedGetter filamentLoadedGetter
                = (StatusResponse statusResponse, int extruderNumber) ->
        {
            if (!detectLoadedFilamentOverride.get())
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
            case DUMMY:
                DummyPrinterCommandInterface dummyCommandInterface = new DummyPrinterCommandInterface(this,
                        detectedPrinter,
                        suppressPrinterIDChecks,
                        sleepBetweenStatusChecksMS,
                        "DP "
                        + dummyPrinterCounter);
                newPrinter = new HardwarePrinter(this, dummyCommandInterface, filamentLoadedGetter,
                        doNotCheckForPresenceOfHead);
                break;
            default:
                steno.error("Don't know how to handle connected printer: " + detectedPrinter);
                break;
        }
        return newPrinter;
    }

    @Override
    public void run()
    {
        while (keepRunning)
        {
            long startOfRunTime = System.currentTimeMillis();

            //Search
            List<DetectedDevice> directlyAttachedDevices = usbSerialDeviceDetector.searchForDevices();
            List<DetectedDevice> remotelyAttachedDevices = remotePrinterDetector.searchForDevices();

            //Now new connections
            List<DetectedDevice> printersToConnect = new ArrayList<>();
            directlyAttachedDevices.forEach(newPrinter ->
            {
                if (!activePrinters.keySet().contains(newPrinter))
                {
                    printersToConnect.add(newPrinter);
                }
            });
            remotelyAttachedDevices.forEach(newPrinter ->
            {
                if (!activePrinters.keySet().contains(newPrinter))
                {
                    printersToConnect.add(newPrinter);
                }
            });

            for (DetectedDevice printerToConnect : printersToConnect)
            {
                steno.debug("We have found a new printer " + printerToConnect);
                assessCandidatePrinter(printerToConnect);
            }

            long endOfRunTime = System.currentTimeMillis();
            long runTime = endOfRunTime - startOfRunTime;
            long sleepTime = 500 - runTime;

            if (sleepTime > 0)
            {
                try
                {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException ex)
                {
                    steno.info("Comms manager was interrupted during sleep");
                }
            }
        }
    }

    /**
     *
     */
    public void shutdown()
    {
        keepRunning = false;

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
        Printer printerHusk = activePrinters.get(detectedPrinter);
        printerHusk.connectionEstablished();

        BaseLookup.printerConnected(printerHusk);
    }

    /**
     *
     */
    @Override
    public void disconnected(DetectedDevice printerHandle)
    {
        final Printer printerToRemove = activePrinters.get(printerHandle);
        if (printerToRemove != null)
        {
            printerToRemove.shutdown();
        }

        BaseLookup.printerDisconnected(printerToRemove);

        if (activePrinters.containsKey(printerHandle))
        {
            if (activePrinters.get(printerHandle) != null
                    && activePrinters.get(printerHandle).getPrinterIdentity() != null
                    && activePrinters.get(printerHandle).getPrinterIdentity().printerFriendlyNameProperty().get() != null
                    && !activePrinters.get(printerHandle).getPrinterIdentity().printerFriendlyNameProperty().get().equals(""))
            {
                steno.info("Disconnected from " + activePrinters.get(printerHandle).getPrinterIdentity().printerFriendlyNameProperty().get());
            } else
            {
                steno.info("Disconnected");
            }
            activePrinters.remove(printerHandle);
        }
    }

    public void addDummyPrinter()
    {
        dummyPrinterCounter++;
        String actualPrinterPort = dummyPrinterPort + " " + dummyPrinterCounter;
        DetectedDevice printerHandle = new DetectedDevice(DeviceDetector.PrinterConnectionType.DUMMY,
                actualPrinterPort);
        assessCandidatePrinter(printerHandle);
    }

    public void removeDummyPrinter(DetectedDevice printerHandle)
    {
        disconnected(printerHandle);
    }

    public List<Printer> getDummyPrinters()
    {
        return activePrinters.entrySet()
                             .stream()
                             .filter(p -> p.getKey().getConnectionType() == DeviceDetector.PrinterConnectionType.DUMMY)
                             .map(e -> e.getValue())
                             .collect(Collectors.toList()); 
    }

    /**
     *
     * @param milliseconds
     */
    public void setSleepBetweenStatusChecks(int milliseconds)
    {
        sleepBetweenStatusChecksMS = milliseconds;
    }

    private void deviceNoLongerPresent(DetectedDevice detectedDevice)
    {
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

    public BooleanBinding tooManyRoboxAttachedProperty()
    {
        return tooManyRoboxAttachedProperty;
    }
}
