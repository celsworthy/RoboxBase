package celtech.roboxbase.comms.remote;

import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.postprocessor.PrintJobStatistics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
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

    private final String baseAPIString;
    private final String connectUrlString;
    private final String disconnectUrlString;
    private final String writeToPrinterUrlString;
    private final String sendStatisticsUrlString;
    private final String retrieveStatisticsUrlString;
    private final String overrideFilamentUrlString;
    private final ObjectMapper mapper = new ObjectMapper();

    public RemoteClient(RemoteDetectedPrinter remotePrinterHandle)
    {
        this.remotePrinterHandle = remotePrinterHandle;
        baseAPIString = "/api";
        connectUrlString = Configuration.lowLevelAPIService + Configuration.connectService;
        disconnectUrlString = Configuration.lowLevelAPIService + Configuration.disconnectService;
        writeToPrinterUrlString = Configuration.lowLevelAPIService + Configuration.writeDataService;
        sendStatisticsUrlString = Configuration.lowLevelAPIService + Configuration.sendStatisticsService;
        retrieveStatisticsUrlString = Configuration.lowLevelAPIService + Configuration.retrieveStatisticsService;
        overrideFilamentUrlString = Configuration.lowLevelAPIService + Configuration.overrideFilamentService;
    }

    @Override
    public boolean connect(String printerID) throws RoboxCommsException
    {
        boolean success = false;
        try
        {
            remotePrinterHandle.getServerPrinterIsAttachedTo().postRoboxPacket(baseAPIString + "/" + printerID + connectUrlString);
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
            remotePrinterHandle.getServerPrinterIsAttachedTo().postRoboxPacket(baseAPIString + "/" + printerID + disconnectUrlString);
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
            returnedPacket = (RoboxRxPacket) remotePrinterHandle.getServerPrinterIsAttachedTo().postRoboxPacket(baseAPIString + "/" + printerID + writeToPrinterUrlString, dataToOutput, RoboxRxPacket.class);
        } catch (JsonProcessingException ex)
        {
            steno.warning("Didn't get correct JSON from request - passing back null for " + messageToWrite.getPacketType().name());
        } catch (IOException ex)
        {
            steno.error("Failed to write to remote printer (" + messageToWrite.getPacketType().name() + ") " + remotePrinterHandle.getConnectionHandle() + " :" + ex.getMessage());
            throw new RoboxCommsException("Failed to write to remote printer (" + messageToWrite.getPacketType().name() + ") " + remotePrinterHandle.getConnectionHandle());
        }

        return returnedPacket;
    }

    public void sendStatistics(String printerID, PrintJobStatistics printJobStatistics) throws RoboxCommsException
    {
        try
        {
            String dataToOutput = mapper.writeValueAsString(printJobStatistics);
            remotePrinterHandle.getServerPrinterIsAttachedTo().postRoboxPacket(baseAPIString + "/" + printerID + sendStatisticsUrlString, dataToOutput, null);
        } catch (IOException ex)
        {
            steno.exception("Failed to send statistics to remote printer " + remotePrinterHandle, ex);
            throw new RoboxCommsException("Failed to send statistics to remote printer" + remotePrinterHandle);
        }
    }

    public PrintJobStatistics retrieveStatistics(String printerID) throws RoboxCommsException
    {
        PrintJobStatistics statistics = null;
        try
        {
            statistics = (PrintJobStatistics) remotePrinterHandle.getServerPrinterIsAttachedTo().postRoboxPacket(baseAPIString + "/" + printerID + retrieveStatisticsUrlString, null, PrintJobStatistics.class);
        } catch (IOException ex)
        {
            throw new RoboxCommsException("Failed to retrieve statistics from remote printer" + remotePrinterHandle);
        }

        return statistics;
    }

    public void overrideFilament(String printerID, int reelNumber, Filament filament) throws RoboxCommsException
    {
        Map<Integer, String> filamentMap = new HashMap();
        filamentMap.put(reelNumber, filament.getFilamentID());
        try
        {
            String jsonified = mapper.writeValueAsString(filamentMap);
            remotePrinterHandle.getServerPrinterIsAttachedTo().postRoboxPacket(baseAPIString + "/" + printerID + overrideFilamentUrlString, jsonified, null);
        } catch (IOException ex)
        {
            steno.error("Failed to override filament on remote printer " + remotePrinterHandle);
            throw new RoboxCommsException("Failed to override filament on remote printer" + remotePrinterHandle);
        }
    }
}
