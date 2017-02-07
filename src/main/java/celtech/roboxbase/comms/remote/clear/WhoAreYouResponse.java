package celtech.roboxbase.comms.remote.clear;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author ianhudson
 */
public class WhoAreYouResponse
{

    private String name;
    private String serverVersion;
    private String serverIP;

    public WhoAreYouResponse()
    {
        // Jackson deserialization
    }

    public WhoAreYouResponse(String name,
            String serverVersion,
            String serverIP)
    {
        this.name = name;
        this.serverVersion = serverVersion;
        this.serverIP = serverIP;
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
}
