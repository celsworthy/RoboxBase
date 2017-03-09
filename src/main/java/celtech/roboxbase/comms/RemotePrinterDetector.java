package celtech.roboxbase.comms;

import celtech.roboxbase.configuration.CoreMemory;
import java.util.ArrayList;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class RemotePrinterDetector extends DeviceDetector
{

    private final Stenographer steno = StenographerFactory.getStenographer("RemotePrinterDetector");
    private final List<DetectedDevice> currentPrinters = new ArrayList<>();

    public RemotePrinterDetector(DeviceDetectionListener listener)
    {
        super(listener);
        this.setName("RemotePrinterDetector");
    }

    @Override
    public void run()
    {
        while (keepRunning)
        {
            List<DetectedDevice> newlyDetectedPrinters = new ArrayList();
            
            //Take a copy of the list in case it gets changed under our feet
            List<DetectedServer> activeRoboxRoots = new ArrayList<>(CoreMemory.getInstance().getActiveRoboxRoots());

            // Search the roots that have been registered in core memory
            for (DetectedServer server : activeRoboxRoots)
            {
                if (server.getServerStatus() == DetectedServer.ServerStatus.NOT_CONNECTED
                        || server.getServerStatus() == DetectedServer.ServerStatus.CONNECTED)
                {
                    List<DetectedDevice> attachedPrinters = server.listAttachedPrinters();
                    newlyDetectedPrinters.addAll(attachedPrinters);
                }
            }

            //Deal with disconnections
            List<DetectedDevice> printersToDisconnect = new ArrayList<>();
            currentPrinters.forEach(existingPrinter ->
            {
                if (!newlyDetectedPrinters.contains(existingPrinter))
                {
                    steno.info("Disconnecting " + existingPrinter.getConnectionHandle());
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
                steno.debug("We have found a new printer " + printerToConnect);
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

    @Override
    public void notifyOfFailedCommsForPrinter(DetectedDevice printerHandle)
    {
        currentPrinters.remove(printerHandle);
    }
}
