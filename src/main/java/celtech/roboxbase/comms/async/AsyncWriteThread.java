package celtech.roboxbase.comms.async;

import celtech.roboxbase.comms.CommandInterface;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
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

    public AsyncWriteThread(CommandInterface commandInterface)
    {
        this.commandInterface = commandInterface;
        this.setDaemon(true);
        this.setName("AsyncCommandProcessor");
    }

    public RoboxRxPacket sendCommand(CommandPacket command)
    {
        RoboxRxPacket response = null;

        inboundQueue.add(command);
        try
        {
            response = outboundQueue.take();
        } catch (InterruptedException ex)
        {
            steno.error("Interrupted outbound async processor");
        }

        return response;
    }

    @Override
    public void run()
    {
        while (keepRunning)
        {
            try
            {
                RoboxRxPacket response = processCommand(inboundQueue.take());
                outboundQueue.add(response);
            } catch (InterruptedException ex)
            {
                steno.error("Interrupted async command sender");
            }
        }
    }

    private RoboxRxPacket processCommand(CommandPacket command)
    {
        RoboxRxPacket response = null;

        try
        {
            response = commandInterface.writeToPrinterImpl(command.getCommand(), command.getDontPublish());
        } catch (RoboxCommsException ex)
        {
            steno.error("Error processing async command");
        }
        return response;
    }

    public void shutdown()
    {
        keepRunning = false;
    }
}
