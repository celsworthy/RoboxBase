package celtech.roboxbase.services;

/**
 *
 * @author ianhudson
 */
public class CameraTriggerData
{

    private final String goProWifiPassword;
    private final int xMoveBeforeCapture;
    private final int yMoveBeforeCapture;
    private final long delayBeforeCapture;
    private final int delayAfterCapture;

    public CameraTriggerData(String goProWifiPassword,
            int xMoveBeforeCapture,
            int yMoveBeforeCapture,
            long delayBeforeCapture,
            int delayAfterCapture)
    {
        this.goProWifiPassword = goProWifiPassword;
        this.xMoveBeforeCapture = xMoveBeforeCapture;
        this.yMoveBeforeCapture = yMoveBeforeCapture;
        this.delayBeforeCapture = delayBeforeCapture;
        this.delayAfterCapture = delayAfterCapture;
    }

    public String getGoProWifiPassword()
    {
        return goProWifiPassword;
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
