package celtech.roboxbase.comms.remote;

import celtech.roboxbase.comms.CommandInterface;
import celtech.roboxbase.comms.PrinterStatusConsumer;
import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.exceptions.ConnectionLostException;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.PrinterNotFound;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import celtech.roboxbase.printerControl.model.Printer;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RoboxRemoteCommandInterface extends CommandInterface
{

    private final RemoteClient remoteClient;

    public RoboxRemoteCommandInterface(PrinterStatusConsumer controlInterface,
            RemoteDetectedPrinter printerHandle,
            boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        super(controlInterface, printerHandle, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName("RemoteCI:" + printerHandle + " " + this.toString());
        remoteClient = new RemoteClient(printerHandle);
    }

    @Override
    protected boolean connectToPrinter()
    {
        remoteClient.connect(printerHandle.getConnectionHandle());
        return true;
    }

    @Override
    protected void disconnectPrinter()
    {
        remoteClient.disconnect(printerHandle.getConnectionHandle());
        controlInterface.disconnected(printerHandle);
        keepRunning = false;
    }

    @Override
    public synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite,
            boolean dontPublishResult) throws RoboxCommsException
    {
        RoboxRxPacket rxPacket = remoteClient.writeToPrinter(printerHandle.getConnectionHandle(), messageToWrite);

        if (rxPacket != null)
        {
            if (rxPacket instanceof PrinterNotFound)
            {
                actionOnCommsFailure();
            }
            else if (!dontPublishResult)
            {
                printerToUse.processRoboxResponse(rxPacket);
            }
        }

        return rxPacket;
    }

    private void actionOnCommsFailure() throws ConnectionLostException
    {
        //If we get an exception then abort and treat
        steno.debug("Error during write to printer");
        disconnectPrinter();
        keepRunning = false;
        throw new ConnectionLostException();
    }

    void setPrinterToUse(Printer newPrinter)
    {
        this.printerToUse = newPrinter;
    }

    /**
     *
     * @param sleepMillis
     */
    @Override
    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        sleepBetweenStatusChecks = sleepMillis;
    }
}
