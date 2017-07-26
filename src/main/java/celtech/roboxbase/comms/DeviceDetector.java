package celtech.roboxbase.comms;

import java.util.List;

/**
 *
 * @author Ian
 */
public abstract class DeviceDetector
{
    public DeviceDetector()
    {
    }

    public enum PrinterConnectionType
    {
        SERIAL,
        ROBOX_REMOTE,
        DUMMY
    }
    
    public abstract List<DetectedDevice> searchForDevices();
}
