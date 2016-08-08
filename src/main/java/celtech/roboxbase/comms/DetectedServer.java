package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.DiscoveryResponse;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public final class DetectedServer
{

    private String version;
    private final List<DetectedDevice> attachedPrinters = new ArrayList<>();

    DetectedServer(InetAddress address, DiscoveryResponse discoveryResponse)
    {
        this.version = discoveryResponse.getServerVersion();
        discoveryResponse.getPrinterIDs().forEach(printerID ->
        {
            RemoteDetectedPrinter remotePrinter = new RemoteDetectedPrinter(address, DeviceDetector.PrinterConnectionType.ROBOX_REMOTE, printerID);
            addAttachedPrinter(remotePrinter);
        });
    }

    public String getVersion()
    {
        return version;
    }

    public void setVersion(String version)
    {
        this.version = version;
    }

    public List<DetectedDevice> getAttachedPrinters()
    {
        return attachedPrinters;
    }

    public void addAttachedPrinter(DetectedDevice printer)
    {
        attachedPrinters.add(printer);
    }

    public void removeAttachedPrinter(DetectedDevice printer)
    {
        attachedPrinters.remove(printer);
    }
    
    public void removeAllPrinters()
    {
        attachedPrinters.clear();
    }
}
