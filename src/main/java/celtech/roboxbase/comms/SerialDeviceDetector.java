package celtech.roboxbase.comms;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.MachineType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SerialDeviceDetector extends DeviceDetector
{

    private static final Stenographer steno = StenographerFactory.getStenographer(SerialDeviceDetector.class.getName());
    private List<DetectedDevice> currentPrinters = new ArrayList<>();
    private final String deviceDetectorStringMac;
    private final String deviceDetectorStringWindows;
    private final String deviceDetectorStringLinux;
    private final String deviceDetectionCommand;
    private final String notConnectedString = "NOT_CONNECTED";

    public SerialDeviceDetector(String pathToBinaries,
            String vendorID,
            String productID,
            String deviceNameToSearchFor,
            DeviceDetectionListener deviceDetectionListener)
    {
        super(deviceDetectionListener);

        this.setName("SerialDeviceDetector");

        deviceDetectorStringMac = pathToBinaries + "RoboxDetector.mac.sh";
        deviceDetectorStringLinux = pathToBinaries + "RoboxDetector.linux.sh";
        deviceDetectorStringWindows = pathToBinaries + "RoboxDetector.exe";

        MachineType machineType = BaseConfiguration.getMachineType();

        switch (machineType)
        {
            case WINDOWS:
                deviceDetectionCommand = deviceDetectorStringWindows + " " + vendorID + " "
                        + productID;
                break;
            case MAC:
                deviceDetectionCommand = deviceDetectorStringMac + " " + deviceNameToSearchFor;
                break;
            case LINUX_X86:
            case LINUX_X64:
                deviceDetectionCommand = deviceDetectorStringLinux + " " + deviceNameToSearchFor
                        + " "
                        + vendorID;
                break;
            default:
                deviceDetectionCommand = null;
                steno.error("Unsupported OS - cannot establish comms.");
                break;
        }
        steno.trace("Device detector command: " + deviceDetectionCommand);
    }

    private List<DetectedDevice> searchForDevices()
    {
        StringBuilder outputBuffer = new StringBuilder();
        List<String> command = new ArrayList<>();
        for (String subcommand : deviceDetectionCommand.split(" "))
        {
            command.add(subcommand);
        }

        ProcessBuilder builder = new ProcessBuilder(command);
        Map<String, String> environ = builder.environment();

        Process process = null;

        try
        {
            process = builder.start();
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.equalsIgnoreCase(notConnectedString) == false)
                {
                    outputBuffer.append(line);
                }
            }
        } catch (IOException ex)
        {
            steno.error("Error " + ex);
        }

        List<DetectedDevice> detectedPrinters = new ArrayList<>();

        if (outputBuffer.length() > 0)
        {
            for (String handle : outputBuffer.toString().split(" "))
            {
                detectedPrinters.add(new DetectedDevice(PrinterConnectionType.SERIAL, handle));
            }
        }

        return detectedPrinters;
    }

    @Override
    public void run()
    {
        while (keepRunning)
        {
            List<DetectedDevice> newlyDetectedPrinters = searchForDevices();

            //Deal with disconnections
            List<DetectedDevice> printersToDisconnect = new ArrayList<>();
            currentPrinters.forEach(existingPrinter ->
            {
                if (!newlyDetectedPrinters.contains(existingPrinter))
                {
                    printersToDisconnect.add(existingPrinter);
                }
            });

            for (DetectedDevice printerToDisconnect : printersToDisconnect)
            {
                steno.info("Disconnecting from " + printerToDisconnect + " as it doesn't seem to be present anymore");
                deviceDetectionListener.deviceNoLongerPresent(printerToDisconnect);
                currentPrinters.remove(printerToDisconnect);
            }

            //Now new connections
            List<DetectedDevice> printersToConnect = new ArrayList<>();
            newlyDetectedPrinters.forEach(newPrinter ->
            {
                if (!currentPrinters.contains(newPrinter))
                {
                    printersToConnect.add(newPrinter);
                }
            });

            for (DetectedDevice printerToConnect : printersToConnect)
            {
                steno.info("We have found a new printer " + printerToConnect);
                currentPrinters.add(printerToConnect);
                deviceDetectionListener.deviceDetected(printerToConnect);
            }

            try
            {
                Thread.sleep(500);
            } catch (InterruptedException ex)
            {
                steno.warning("Interrupted within remote host discovery loop");
            }
        }
    }
}
