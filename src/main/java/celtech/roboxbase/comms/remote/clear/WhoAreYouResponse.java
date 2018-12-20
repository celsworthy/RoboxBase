package celtech.roboxbase.comms.remote.clear;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public class WhoAreYouResponse
{

    private String name;
    private String serverVersion;
    private String serverIP;
    private List<String> printerColours;

    public WhoAreYouResponse()
    {
        // Jackson deserialization
    }

    public WhoAreYouResponse(String name,
            String serverVersion,
            String serverIP,
            List<String> printerColours)
    {
        this.name = name;
        this.serverVersion = serverVersion;
        this.serverIP = serverIP;
        this.printerColours = printerColours;
    }

    @JsonProperty
    public String getName()
    {
        return name;
    }

    @JsonProperty
    public void setName(String name)
    {
        this.name = name;
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
    public String getServerIP()
    {
        return serverIP;
    }

    @JsonProperty
    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }

    @JsonProperty
    public List<String> getPrinterColours()
    {
        return printerColours;
    }

    @JsonProperty
    public void setPrinterColours(List<String> printerColours) 
    {
        this.printerColours = printerColours;
    }
    
    
}
