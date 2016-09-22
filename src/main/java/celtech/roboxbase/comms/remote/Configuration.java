package celtech.roboxbase.comms.remote;

/**
 *
 * @author Ian
 */
public class Configuration
{
    public static final int remotePort = 8080;
    public static final String discoveryService = "/discovery";
    
    /**
     * Admin API
     */
    public static final String adminAPIService = "/admin";
    public static final String shutdown = "/shutdown";

    /**
     * Low Level API
     */
    public static final String lowLevelAPIService = "/printerControl";
    public static final String connectService = "/connect";
    public static final String disconnectService = "/disconnect";
    public static final String writeDataService = "/writeData";
    public static final String associateStatisticsService = "/associateStatistics";
}
