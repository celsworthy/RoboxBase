package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.LowLevelInterfaceException;
import java.io.UnsupportedEncodingException;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;
import jssc.SerialPortTimeoutException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class SerialPortManager implements SerialPortEventListener
{

    private String serialPortToConnectTo = null;
    protected SerialPort serialPort = null;
    private final Stenographer steno = StenographerFactory.getStenographer(SerialPortManager.class.
            getName());
    // timeout is required on the read particularly for when the firmware is out of date
    // and the returned status report is then too short see issue ROB-453
    private final static int READ_TIMEOUT = 5000;

    public SerialPortManager(String portToConnectTo)
    {
        this.serialPortToConnectTo = portToConnectTo;
    }

    public boolean connect(int baudrate)
    {
        boolean portSetupOK = false;

        steno.info("About to open serial port " + serialPortToConnectTo);
        serialPort = new SerialPort(serialPortToConnectTo);

        try
        {
            serialPort.openPort();
            serialPort.setParams(baudrate, SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1, SerialPort.PARITY_NONE);
            serialPort.setFlowControlMode(SerialPort.FLOWCONTROL_NONE);
            portSetupOK = true;
            steno.info("Finished opening serial port " + serialPortToConnectTo);
        } catch (SerialPortException ex)
        {
            steno.error("Error setting up serial port " + ex.getMessage());
        }

        return portSetupOK;
    }

    public void disconnect() throws LowLevelInterfaceException
    {
        steno.info("Disconnecting port " + serialPortToConnectTo);

        checkSerialPortOK();

        if (serialPort != null)
        {
            try
            {
                serialPort.closePort();
                steno.info("Port " + serialPortToConnectTo + " disconnected");
            } catch (SerialPortException ex)
            {
                steno.error("Error closing serial port");
            }
        }
        serialPort = null;
    }

    public boolean writeBytes(byte[] data) throws LowLevelInterfaceException
    {
        boolean wroteOK = false;

        try
        {
            checkSerialPortOK();

            wroteOK = serialPort.writeBytes(data);
        } catch (SerialPortException ex)
        {
            throw new LowLevelInterfaceException(ex.getMessage() + " method " + ex.getMethodName() + " port " + ex.getPortName());
        }
        return wroteOK;
    }

    public int getInputBufferBytesCount() throws LowLevelInterfaceException
    {
        checkSerialPortOK();
        try
        {
            return serialPort.getInputBufferBytesCount();
        } catch (SerialPortException ex)
        {
            throw new LowLevelInterfaceException(ex.getMessage());
        }
    }

    public void writeAndWaitForData(byte[] data) throws LowLevelInterfaceException
    {
        checkSerialPortOK();

        boolean wroteOK = writeBytes(data);

        if (wroteOK)
        {
            int len = -1;

            int waitCounter = 0;
            while (getInputBufferBytesCount() <= 0)
            {
                try
                {
                    Thread.sleep(0, 100000);
                } catch (InterruptedException ex)
                {
                }

                if (waitCounter >= 5000)
                {
                    steno.error("No response from device - disconnecting");
                    throw new LowLevelInterfaceException(serialPort.getPortName()
                            + " Check availability - Printer did not respond");
                }
                waitCounter++;
            }
        } else
        {
            throw new LowLevelInterfaceException(serialPort.getPortName()
                    + " Failure during write");
        }
    }

    public byte[] readSerialPort(int numBytes) throws LowLevelInterfaceException
    {
        checkSerialPortOK();

        byte[] returnData = null;
        try
        {
            returnData = serialPort.readBytes(numBytes, READ_TIMEOUT);
        } catch (SerialPortTimeoutException | SerialPortException ex)
        {
            throw new LowLevelInterfaceException(serialPort.getPortName()
                    + " Check availability - Printer did not respond in time");
        }
        return returnData;
    }

    public byte[] readAllDataOnBuffer() throws LowLevelInterfaceException
    {
        checkSerialPortOK();
        try
        {
            return serialPort.readBytes();
        } catch (SerialPortException ex)
        {
            throw new LowLevelInterfaceException(ex.getMessage());
        }
    }

    public boolean writeASCIIString(String string) throws LowLevelInterfaceException
    {
        checkSerialPortOK();

        try
        {
            return serialPort.writeString(string, "US-ASCII");
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Strange error with encoding");
            ex.printStackTrace();
            throw new LowLevelInterfaceException(serialPortToConnectTo
                    + " Encoding error whilst writing ASCII string");
        } catch (SerialPortException ex)
        {
            throw new LowLevelInterfaceException(ex.getMessage());
        }
    }

    public String readString() throws LowLevelInterfaceException
    {
        checkSerialPortOK();

        try
        {
            return serialPort.readString();
        } catch (SerialPortException ex)
        {
            throw new LowLevelInterfaceException(ex.getMessage());
        }
    }

    private void checkSerialPortOK() throws LowLevelInterfaceException
    {
        if (serialPort == null)
        {
            throw new LowLevelInterfaceException(serialPortToConnectTo
                    + " Serial port not open");
        }
    }

    public void callback() throws SerialPortException
    {
        serialPort.addEventListener(this, SerialPort.MASK_RXCHAR);
    }

    @Override
    public void serialEvent(SerialPortEvent serialPortEvent)
    {
        if (serialPortEvent.isRXCHAR())
        {
            int numberOfBytesReceived = serialPortEvent.getEventValue();
            steno.info("Got " + numberOfBytesReceived + " bytes");
            try
            {
                serialPort.readBytes(numberOfBytesReceived, READ_TIMEOUT);
            } catch (SerialPortTimeoutException | SerialPortException ex)
            {
                steno.exception("Error whilst auto reading from port " + serialPortToConnectTo, ex);
            }
        } else
        {
            steno.info("Got serial event of type " + serialPortEvent.getEventType()
                    + " that I didn't understand");
        }
    }
}
