package celtech.roboxbase.configuration;

import celtech.roboxbase.comms.DetectedServer;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ian
 */
public class CoreMemoryData
{

    private String lastPrinterSerial = "";
    private float lastPrinterFirmwareVersion = 0f;
    private List<DetectedServer> activeRoboxRoots = new ArrayList();

    public CoreMemoryData()
    {
    }

    public String getLastPrinterSerial()
    {
        return lastPrinterSerial;
    }

    public void setLastPrinterSerial(String lastPrinterSerial)
    {
        this.lastPrinterSerial = lastPrinterSerial;
    }

    public float getLastPrinterFirmwareVersion()
    {
        return lastPrinterFirmwareVersion;
    }

    public void setLastPrinterFirmwareVersion(float lastPrinterFirmwareVersion)
    {
        this.lastPrinterFirmwareVersion = lastPrinterFirmwareVersion;
    }

    public List<DetectedServer> getActiveRoboxRoots()
    {
        return activeRoboxRoots;
    }

    public void setActiveRoboxRoots(List<DetectedServer> roboxRoots)
    {
        this.activeRoboxRoots = roboxRoots;
    }
}
