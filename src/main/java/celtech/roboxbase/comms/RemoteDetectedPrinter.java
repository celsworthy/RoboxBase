package celtech.roboxbase.comms;

import java.net.InetAddress;
import java.util.Objects;

/**
 *
 * @author Ian
 */
public class RemoteDetectedPrinter extends DetectedDevice
{
    private final InetAddress address;

    public RemoteDetectedPrinter(InetAddress address, DeviceDetector.PrinterConnectionType connectionType, String connectionHandle)
    {
        super(connectionType, connectionHandle);
        this.address = address;
    }

    public InetAddress getAddress()
    {
        return address;
    }

    @Override
    public boolean equals(Object obj)
    {
        boolean equal = false;

        if (obj instanceof RemoteDetectedPrinter
                && ((RemoteDetectedPrinter) obj).getConnectionHandle().equals(getConnectionHandle())
                && ((RemoteDetectedPrinter) obj).getConnectionType() == getConnectionType()
                && ((RemoteDetectedPrinter) obj).address.equals(address))
        {
            equal = true;
        }

        return equal;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(getConnectionType());
        hash = 32 * hash + Objects.hashCode(getConnectionHandle());
        hash = 66 * hash + Objects.hashCode(this.address);
        return hash;
    }

    @Override
    public String toString()
    {
        return super.toString() + ":" + this.address.getHostAddress().toString();
    }
}
