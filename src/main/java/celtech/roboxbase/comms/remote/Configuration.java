package celtech.roboxbase.comms.remote;

/**
 *
 * @author Ian
 */
public class Configuration
{
    public static final int remotePort = 9000;
    public static final String discoveryService = "/discovery";
    
    /**
     * Admin API
     */
    public static final String adminAPIService = "/admin";
    public static final String shutdown = "/shutdown";

    /**
     * High Level API
     */
    public static final String highLevelAPIService = "/remoteControl";
    public static final String openDoorService = "/openDoor";
    public static final String pauseService = "/pause";
    public static final String resumeService = "/resume";
    public static final String cancelService = "/cancel";
    public static final String statusService = "/status";

    /**
     * Low Level API
     */
    public static final String lowLevelAPIService = "/printerControl";
    public static final String connectService = "/connect";
    public static final String disconnectService = "/disconnect";
    public static final String writeDataService = "/writeData";
}
