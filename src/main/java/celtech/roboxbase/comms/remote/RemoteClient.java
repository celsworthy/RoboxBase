package celtech.roboxbase.comms.remote;

import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import celtech.roboxbase.postprocessor.PrintJobStatistics;
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
    private final String associateStatisticsUrlString;
    private final ObjectMapper mapper = new ObjectMapper();

    public RemoteClient(RemoteDetectedPrinter remotePrinterHandle)
    {
        this.remotePrinterHandle = remotePrinterHandle;
        baseUrlString = "/api";
        connectUrlString = Configuration.lowLevelAPIService + Configuration.connectService;
        disconnectUrlString = Configuration.lowLevelAPIService + Configuration.disconnectService;
        writeToPrinterUrlString = Configuration.lowLevelAPIService + Configuration.writeDataService;
        associateStatisticsUrlString = Configuration.lowLevelAPIService + Configuration.associateStatisticsService;
    }

    @Override
    public boolean connect(String printerID) throws RoboxCommsException
    {
        boolean success = false;
        try
        {
            remotePrinterHandle.getServerPrinterIsAttachedTo().postData(connectUrlString);
        } catch (IOException ex)
        {
            steno.error("Failed to connect to remote printer " + remotePrinterHandle);
            throw new RoboxCommsException("Failed to connect to remote printer " + remotePrinterHandle);
        }
        return success;
    }

    @Override
    public void disconnect(String printerID) throws RoboxCommsException
    {
        try
        {
            remotePrinterHandle.getServerPrinterIsAttachedTo().postData(disconnectUrlString);
        } catch (IOException ex)
        {
            steno.error("Failed to disconnect from remote printer " + remotePrinterHandle);
            throw new RoboxCommsException("Failed to disconnect from remote printer " + remotePrinterHandle);
        }
    }

    @Override
    public RoboxRxPacket writeToPrinter(String printerID, RoboxTxPacket messageToWrite) throws RoboxCommsException
    {
        RoboxRxPacket returnedPacket = null;

        try
        {
            String dataToOutput = mapper.writeValueAsString(messageToWrite);
            returnedPacket = remotePrinterHandle.getServerPrinterIsAttachedTo().postData(writeToPrinterUrlString, dataToOutput, RoboxRxPacket.class);
        } catch (IOException ex)
        {
            steno.error("Failed to write to remote printer (" + messageToWrite.getPacketType().name() + ") " + remotePrinterHandle);
            throw new RoboxCommsException("Failed to write to remote printer (" + messageToWrite.getPacketType().name() + ") " + remotePrinterHandle);
        }

        return returnedPacket;
    }

    public void associateStatisticsWithPrintJobID(PrintJobStatistics statistics) throws RoboxCommsException
    {
        try
        {
            String dataToOutput = mapper.writeValueAsString(statistics);
            remotePrinterHandle.getServerPrinterIsAttachedTo().postData(associateStatisticsUrlString, dataToOutput, RoboxRxPacket.class);
        } catch (IOException ex)
        {
            steno.error("Failed to associate statistics on remote printer for job (" + statistics.getPrintJobID() + ") " + remotePrinterHandle);
            throw new RoboxCommsException("Failed to associate statistics on remote printer for job (" + statistics.getPrintJobID() + ") " + remotePrinterHandle);
        }
    }
}
