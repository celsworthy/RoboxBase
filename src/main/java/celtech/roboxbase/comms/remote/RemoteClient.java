package celtech.roboxbase.comms.remote;

import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class RemoteClient implements LowLevelInterface
{

    private final Stenographer steno = StenographerFactory.getStenographer(RemoteClient.class.getName());
    private final RemoteDetectedPrinter remotePrinterHandle;

    private final String baseUrlString;
    private final String connectUrlString;
    private final String disconnectUrlString;
    private final String writeToPrinterUrlString;
    private final ObjectMapper mapper = new ObjectMapper();

    public RemoteClient(RemoteDetectedPrinter remotePrinterHandle)
    {
        this.remotePrinterHandle = remotePrinterHandle;
        baseUrlString = "http://" + remotePrinterHandle.getAddress().getHostAddress() + ":" + Configuration.remotePort + "/api";
        connectUrlString = baseUrlString + "/" + remotePrinterHandle.getConnectionHandle() + Configuration.lowLevelAPIService + Configuration.connectService;
        disconnectUrlString = baseUrlString + "/" + remotePrinterHandle.getConnectionHandle() + Configuration.lowLevelAPIService + Configuration.disconnectService;
        writeToPrinterUrlString = baseUrlString + "/" + remotePrinterHandle.getConnectionHandle() + Configuration.lowLevelAPIService + Configuration.writeDataService;
    }

    @Override
    public boolean connect(String printerID)
    {
        boolean success = false;
        RemoteWebHelper.postData(connectUrlString);
        return success;
    }

    @Override
    public void disconnect(String printerID)
    {
        RemoteWebHelper.postData(disconnectUrlString);
    }

    @Override
    public RoboxRxPacket writeToPrinter(String printerID, RoboxTxPacket messageToWrite)
    {
        RoboxRxPacket returnedPacket = null;

        try
        {
            String dataToOutput = mapper.writeValueAsString(messageToWrite);
            returnedPacket = RemoteWebHelper.postData(writeToPrinterUrlString, dataToOutput, RoboxRxPacket.class);
        } catch (IOException ex)
        {
            steno.error("Failed to write to remote printer (" + messageToWrite.getPacketType().name() + ") " + remotePrinterHandle);
        }

        return returnedPacket;
    }

}
