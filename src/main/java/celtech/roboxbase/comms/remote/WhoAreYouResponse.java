package celtech.roboxbase.comms.remote;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author ianhudson
 */
public class WhoAreYouResponse
{

    private String name;
    private String serverVersion;

    public WhoAreYouResponse()
    {
        // Jackson deserialization
    }

    public WhoAreYouResponse(String name,
            String serverVersion)
    {
        this.name = name;
        this.serverVersion = serverVersion;
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
}
