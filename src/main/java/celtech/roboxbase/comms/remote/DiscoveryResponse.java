package celtech.roboxbase.comms.remote;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public class DiscoveryResponse
{

    private List<String> printerIDs;

    public DiscoveryResponse()
    {
        // Jackson deserialization
    }

    public DiscoveryResponse(List<String> printerIDs)
    {
        this.printerIDs = printerIDs;
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
        printerIDs.forEach(id ->
        {
            output.append(id);
            output.append('\n');
        });
        output.append("================");
        
        return output.toString();
    }
}
