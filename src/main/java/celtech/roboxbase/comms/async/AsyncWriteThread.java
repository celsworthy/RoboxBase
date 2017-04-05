package celtech.roboxbase.comms.async;

import celtech.roboxbase.comms.CommandInterface;
import celtech.roboxbase.comms.exceptions.ConnectionLostException;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.rx.RoboxRxPacketFactory;
import celtech.roboxbase.comms.rx.RxPacketTypeEnum;
import java.util.ArrayList;
import java.util.List;
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
    private final int NUMBER_OF_SIMULTANEOUS_COMMANDS = 50;
    private final BlockingQueue<CommandHolder> inboundQueue = new ArrayBlockingQueue<>(NUMBER_OF_SIMULTANEOUS_COMMANDS);
    private final List<BlockingQueue<RoboxRxPacket>> outboundQueues;
    private final CommandInterface commandInterface;
    private boolean keepRunning = true;

    private static CommandHolder poisonedPill = new CommandHolder(-1, null);

    public AsyncWriteThread(CommandInterface commandInterface, String ciReference)
    {
        this.commandInterface = commandInterface;
        this.setDaemon(true);
        this.setName("AsyncCommandProcessor|" + ciReference);

        outboundQueues = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SIMULTANEOUS_COMMANDS; i++)
        {
            outboundQueues.add(new ArrayBlockingQueue<>(1));
        }
    }

    private int addCommandToQueue(CommandPacket command) throws RoboxCommsException
    {
        int queueNumber = -1;

        // Look for an empty outbound queue
        for (int queueIndex = 0; queueIndex < outboundQueues.size(); queueIndex++)
        {
            if (outboundQueues.get(queueIndex).remainingCapacity() > 0)
            {
                CommandHolder commandHolder = new CommandHolder(queueIndex, command);
                inboundQueue.add(commandHolder);
                queueNumber = queueIndex;
                break;
            }
        }

        if (queueNumber < 0)
        {
            throw new RoboxCommsException("Message queue full");
        }

        return queueNumber;
    }

    public synchronized RoboxRxPacket sendCommand(CommandPacket command) throws RoboxCommsException
    {
        RoboxRxPacket response = null;

//        steno.info("Adding command to queue:" + command.getCommand().getPacketType());
        int queueNumber = addCommandToQueue(command);
        try
        {
            response = outboundQueues.get(queueNumber).poll(1500, TimeUnit.MILLISECONDS);
//            steno.info("Received response:" + response.getPacketType());
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
            CommandHolder commandHolder = null;
            try
            {
                commandHolder = inboundQueue.take();
                if (commandHolder != poisonedPill)
                {
                    RoboxRxPacket response = processCommand(commandHolder.getCommandPacket());
                    if (response != null)
                    {
                        createNullPacket = false;
                        outboundQueues.get(commandHolder.getQueueIndex()).add(response);
                    }
                } else
                {
                    //Just drop out - we got the poisoned pill
                    createNullPacket = false;
                }
            } catch (ConnectionLostException ex)
            {
                // This is ok - the printer has probably been unplugged
                steno.info("Connection lost - " + getName());
            } catch (RoboxCommsException | InterruptedException ex)
            {
                steno.exception("Unexpected error during write", ex);
            } finally
            {
                if (createNullPacket)
                {
                    outboundQueues.get(commandHolder.getQueueIndex()).add(RoboxRxPacketFactory.createNullPacket());
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
        inboundQueue.add(poisonedPill);
    }
}
