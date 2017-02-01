package celtech.roboxbase.comms.async;

import celtech.roboxbase.comms.CommandInterface;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.rx.RoboxRxPacketFactory;
import celtech.roboxbase.comms.rx.RxPacketTypeEnum;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class AsyncWriteThread extends Thread
{

    private final Stenographer steno = StenographerFactory.getStenographer(AsyncWriteThread.class.getName());
    private final BlockingQueue<CommandPacket> inboundQueue = new ArrayBlockingQueue<>(10);
    private final BlockingQueue<RoboxRxPacket> outboundQueue = new ArrayBlockingQueue<>(10);
    private final CommandInterface commandInterface;
    private boolean keepRunning = true;

    public AsyncWriteThread(CommandInterface commandInterface, String ciReference)
    {
        this.commandInterface = commandInterface;
        this.setDaemon(true);
        this.setName("AsyncCommandProcessor|" + ciReference);
    }

    public RoboxRxPacket sendCommand(CommandPacket command) throws RoboxCommsException
    {
        RoboxRxPacket response = null;

        inboundQueue.add(command);
        try
        {
            response = outboundQueue.poll(750, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ex)
        {
            throw new RoboxCommsException("Interrupted waiting for response");
        }

        if (response == null
                || response.getPacketType() == RxPacketTypeEnum.NULL_PACKET)
        {
            throw new RoboxCommsException("No response to message from command " + command);
        }
        return response;
    }

    @Override
    public void run()
    {
        while (keepRunning)
        {
            boolean createNullPacket = true;
            try
            {
                RoboxRxPacket response = processCommand(inboundQueue.take());
                if (response != null)
                {
                    createNullPacket = false;
                    outboundQueue.add(response);
                }
            } catch (RoboxCommsException | InterruptedException ex)
            {
            } finally
            {
                if (createNullPacket)
                {
                    outboundQueue.add(RoboxRxPacketFactory.createNullPacket());
                }
            }
        }
    }

    private RoboxRxPacket processCommand(CommandPacket command) throws RoboxCommsException
    {
        RoboxRxPacket response = commandInterface.writeToPrinterImpl(command.getCommand(), command.getDontPublish());
        return response;
    }

    public void shutdown()
    {
        keepRunning = false;
    }
}
