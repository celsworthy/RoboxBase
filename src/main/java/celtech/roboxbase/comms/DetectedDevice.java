package celtech.roboxbase.comms;

import java.util.Objects;

/**
 *
 * @author Ian
 */
public class DetectedDevice
{
    private final DeviceDetector.PrinterConnectionType connectionType;
    private final String connectionHandle;

    public DetectedDevice(DeviceDetector.PrinterConnectionType connectionType,
            String connectionHandle)
    {
        this.connectionType = connectionType;
        this.connectionHandle = connectionHandle;
    }

    public DeviceDetector.PrinterConnectionType getConnectionType()
    {
        return connectionType;
    }

    public String getConnectionHandle()
    {
        return connectionHandle;
    }

    @Override
    public String toString()
    {
        return connectionType.name() + ":" + connectionHandle;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equal = false;

        if (obj instanceof DetectedDevice
                && ((DetectedDevice) obj).getConnectionHandle().equals(connectionHandle)
                && ((DetectedDevice) obj).getConnectionType() == connectionType)
        {
            equal = true;
        }

        return equal;
    }

    @Override
    public int hashCode()
    {
        int hash = 5;
        hash = 41 * hash + Objects.hashCode(this.connectionType);
        hash = 41 * hash + Objects.hashCode(this.connectionHandle);
        return hash;
    }
}
