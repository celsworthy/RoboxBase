package celtech.roboxbase.services;

/**
 *
 * @author ianhudson
 */
public class CameraTriggerData
{
    private final boolean turnOffHeadLights;
    private final boolean turnOffLED;
    private final String goProWifiPassword;
    private final boolean moveBeforeCapture;
    private final int xMoveBeforeCapture;
    private final int yMoveBeforeCapture;
    private final long delayBeforeCapture;
    private final int delayAfterCapture;

    public CameraTriggerData(boolean turnOffHeadLights,
            boolean turnOffLED,
            String goProWifiPassword,
            boolean moveBeforeCapture,
            int xMoveBeforeCapture,
            int yMoveBeforeCapture,
            long delayBeforeCapture,
            int delayAfterCapture)
    {
        this.turnOffHeadLights = turnOffHeadLights;
        this.turnOffLED = turnOffLED;
        this.moveBeforeCapture = moveBeforeCapture;
        this.goProWifiPassword = goProWifiPassword;
        this.xMoveBeforeCapture = xMoveBeforeCapture;
        this.yMoveBeforeCapture = yMoveBeforeCapture;
        this.delayBeforeCapture = delayBeforeCapture;
        this.delayAfterCapture = delayAfterCapture;
    }

    public boolean isTurnOffHeadLights()
    {
        return turnOffHeadLights;
    }
    
    public boolean isTurnOffLED()
    {
        return turnOffLED;
    }
    
    public String getGoProWifiPassword()
    {
        return goProWifiPassword;
    }

    public boolean isMoveBeforeCapture()
    {
        return moveBeforeCapture;
    }
    
    public int getxMoveBeforeCapture()
    {
        return xMoveBeforeCapture;
    }

    public int getyMoveBeforeCapture()
    {
        return yMoveBeforeCapture;
    }

    public long getDelayBeforeCapture()
    {
        return delayBeforeCapture;
    }

    public int getDelayAfterCapture()
    {
        return delayAfterCapture;
    }
}
