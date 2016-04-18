package celtech.roboxbase.configuration;

/**
 *
 * @author Ian
 */
public class CoreMemoryData
{

    private String lastPrinterSerial;
    private float lastPrinterFirmwareVersion;

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
}
