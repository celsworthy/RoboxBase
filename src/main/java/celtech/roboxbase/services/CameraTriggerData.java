package celtech.roboxbase.services;

/**
 *
 * @author ianhudson
 */
public class CameraTriggerData
{
    private final boolean turnOffHeadLights;
    private final boolean turnOffLED;
    private final boolean moveBeforeCapture;
    private final int xMoveBeforeCapture;
    private final int yMoveBeforeCapture;

    public CameraTriggerData(boolean turnOffHeadLights,
            boolean turnOffLED,
            boolean moveBeforeCapture,
            int xMoveBeforeCapture,
            int yMoveBeforeCapture)
    {
        this.turnOffHeadLights = turnOffHeadLights;
        this.turnOffLED = turnOffLED;
        this.moveBeforeCapture = moveBeforeCapture;
        this.xMoveBeforeCapture = xMoveBeforeCapture;
        this.yMoveBeforeCapture = yMoveBeforeCapture;
    }

    public boolean isTurnOffHeadLights()
    {
        return turnOffHeadLights;
    }
    
    public boolean isTurnOffLED()
    {
        return turnOffLED;
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
}
