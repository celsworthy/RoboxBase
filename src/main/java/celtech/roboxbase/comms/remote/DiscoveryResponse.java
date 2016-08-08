package celtech.roboxbase.comms.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public class DiscoveryResponse
{

    private String serverVersion;
    private List<String> printerIDs;

    public DiscoveryResponse()
    {
        // Jackson deserialization
    }

    public DiscoveryResponse(String serverVersion,
            List<String> printerIDs)
    {
        this.serverVersion = serverVersion;
        this.printerIDs = printerIDs;
    }

    @JsonProperty
    public String getServerVersion()
    {
        return serverVersion;
    }

    @JsonProperty
    public void setServerVersion(String serverVersion)
    {
        this.serverVersion = serverVersion;
    }

    @JsonProperty
    public List<String> getPrinterIDs()
    {
        return printerIDs;
    }

    @JsonProperty
    public void setPrinterIDs(List<String> printerIDs)
    {
        this.printerIDs = printerIDs;
    }

    @Override
    public String toString()
    {
        StringBuilder output = new StringBuilder();
        output.append("DiscoveryResponse");
        output.append('\n');
        output.append("Server version:");
        output.append(serverVersion);
        output.append('\n');
        printerIDs.forEach(id ->
        {
            output.append(id);
            output.append('\n');
        });
        output.append("================");

        return output.toString();
    }
}
