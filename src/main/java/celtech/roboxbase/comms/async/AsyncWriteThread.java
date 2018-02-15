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
    private boolean[] queueInUse = new boolean[NUMBER_OF_SIMULTANEOUS_COMMANDS];
    
    private static CommandHolder poisonedPill = new CommandHolder(-1, null);

    public AsyncWriteThread(CommandInterface commandInterface, String ciReference)
    {
        this.commandInterface = commandInterface;
        this.setDaemon(true);
        this.setName("AsyncCommandProcessor|" + ciReference);
        this.setPriority(Thread.MAX_PRIORITY);

        outboundQueues = new ArrayList<>();
        for (int i = 0; i < NUMBER_OF_SIMULTANEOUS_COMMANDS; i++)
        {
            outboundQueues.add(new ArrayBlockingQueue<>(1));
            queueInUse[i] = false;
        }
    }

    private int addCommandToQueue(CommandPacket command) throws RoboxCommsException
    {
        int queueNumber = -1;

        // Look for an empty outbound queue
        for (int queueIndex = 0; queueIndex < outboundQueues.size(); queueIndex++)
        {
            if (!queueInUse[queueIndex])
            {
                outboundQueues.get(queueIndex).clear(); // Clear out any junk in the queue.
                CommandHolder commandHolder = new CommandHolder(queueIndex, command);
                inboundQueue.add(commandHolder);
                queueNumber = queueIndex;
                queueInUse[queueIndex] = true;
                break;
            }
        }

        if (queueNumber < 0)
        {
            steno.info("Message queue full; can not add command:" + command.getCommand().getPacketType());
            throw new RoboxCommsException("Message queue full");
        }

        return queueNumber;
    }

    public synchronized RoboxRxPacket sendCommand(CommandPacket command) throws RoboxCommsException
    {
        RoboxRxPacket response = null;

        //steno.info("**** Adding command to queue:" + command.getCommand().getPacketType());
        int queueNumber = addCommandToQueue(command);
        try
        {
            //steno.info("**** Awaiting response on queue " + queueNumber);
            // If the async command processor writes to
            // the queue after the listener has timed out, it used to cause the queue to
            // be permanantly lost, because it contained an entry. Now it clears the queue.
            // However, there is still a risk that if a timed-out queue is used, it could
            // get the response intended for the previous queue. This is quite a tricky problem.
            response = outboundQueues.get(queueNumber).poll(1500, TimeUnit.MILLISECONDS);
            //steno.info("Received response on queue " + queueNumber);
            //steno.info("Received response:" + response.getPacketType());
        } catch (InterruptedException ex)
        {
            steno.info("Throwing RoboxCommsException('Interrupted waiting for response') on queue " + queueNumber);
            throw new RoboxCommsException("Interrupted waiting for response");
        }
        finally {
            queueInUse[queueNumber] = false;
        }

        if (response == null
                || response.getPacketType() == RxPacketTypeEnum.NULL_PACKET)
        {
            steno.info("Throwing RoboxCommsException('No response to message from command " + command + "')");
            throw new RoboxCommsException("No response to message from command " + command);
        }
        //steno.info("Returning response " + response.getPacketType() + " for command " + command);
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
                //steno.info("++++ Taking a command");
                commandHolder = inboundQueue.take();
                if (commandHolder != poisonedPill)
                {
                    //steno.info("++++ Processing command for queue " + commandHolder.getQueueIndex() + " : " + commandHolder.getCommandPacket().getCommand().getPacketType());
                    RoboxRxPacket response = processCommand(commandHolder.getCommandPacket());
                    //steno.info("++++ Got response for queue " + commandHolder.getQueueIndex());
                    
                    if (response != null)
                    {
                        createNullPacket = false;
                        //steno.info("++++ sending response to queue " + commandHolder.getQueueIndex());
                        if (outboundQueues.get(commandHolder.getQueueIndex()).offer(response))
                        {
                            //steno.info("++++ sent response to queue " + commandHolder.getQueueIndex());
                        }
                        else
                        {
                            // Queue is full. Nothing is waiting for the response to this queue, so empty the queue.
                            BlockingQueue<RoboxRxPacket> q = outboundQueues.get(commandHolder.getQueueIndex());
                            steno.warning("++++ Unable to send response to queue " + commandHolder.getQueueIndex());
                            //steno.warning("++++ Queue already contains " + Integer.toString(q.size()) + "responses");
                            //if (q.size() > 0)
                            //{
                                //RoboxRxPacket[] r = q.toArray(new RoboxRxPacket[0]);
                               // for (int rIndex = 0; rIndex < r.length; rIndex++)
                                //{
                                //    steno.warning("++++    Response " + Integer.toString(rIndex) + " = " + r[rIndex].getPacketType());
                                //}
                            //}
                            q.clear();
                        }
                    }
                } else
                {
                    //steno.info("++++ Got poisoned pill");
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
                    //steno.info("++++ sending null response to queue " + commandHolder.getQueueIndex());
                    if (outboundQueues.get(commandHolder.getQueueIndex()).offer(RoboxRxPacketFactory.createNullPacket()))
                    {
                        //steno.info("++++ sent null response to queue " + commandHolder.getQueueIndex());
                    }
                    else
                    {
                        // Nothing is waiting for the response to this queue.
                        BlockingQueue<RoboxRxPacket> q = outboundQueues.get(commandHolder.getQueueIndex());
                        //steno.warning("++++ Unable to send null response to queue " + commandHolder.getQueueIndex());
                        //steno.warning("++++ Queue already contains " + Integer.toString(q.size()) + "responses");
                        //if (q.size() > 0)
                        //{
                        //    RoboxRxPacket[] r = q.toArray(new RoboxRxPacket[0]);
                        //    for (int rIndex = 0; rIndex < r.length; rIndex++)
                        //    {
                        //        steno.warning("++++    Response " + Integer.toString(rIndex) + " = " + r[rIndex].getPacketType());
                        //    }
                        //}
                        q.clear();
                    }
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
