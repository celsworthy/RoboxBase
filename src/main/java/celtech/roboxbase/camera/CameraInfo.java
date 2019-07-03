package celtech.roboxbase.camera;

/**
 *
 * @author George Salter
 */
public class CameraInfo 
{
    private String udevName;
    private String cameraName;
    private int cameraNumber;
    private String motionControlHandle;
    private String motionStreamHandle;
    private String serverIP = "";

    public String getUdevName() 
    {
        return udevName;
    }

    public void setUdevName(String udevName) 
    {
        this.udevName = udevName;
    }

    public String getCameraName()
    {
        return cameraName;
    }

    public void setCameraName(String cameraName) 
    {
        this.cameraName = cameraName;
    }

    public int getCameraNumber() 
    {
        return cameraNumber;
    }

    public void setCameraNumber(int cameraNumber)
    {
        this.cameraNumber = cameraNumber;
    }

    public String getMotionControlHandle()
    {
        return motionControlHandle;
    }

    public void setMotionControlHandle(String motionControlHandle) 
    {
        this.motionControlHandle = motionControlHandle;
    }

    public String getMotionStreamHandle() 
    {
        return motionStreamHandle;
    }

    public void setMotionStreamHandle(String motionStreamHandle)
    {
        this.motionStreamHandle = motionStreamHandle;
    }
    
    public String getServerIP() 
    {
        return serverIP;
    }
    
    public void setServerIP(String serverIP)
    {
        this.serverIP = serverIP;
    }
    
    @Override
    public String toString()
    {
        String cameraInfoString = 
                "Camera name: " + cameraName + "\n" +
                "UDev name: " + udevName + "\n" +
                "Camera Number: " + cameraNumber + "\n" +
                "Motion control handle: " + motionControlHandle + "\n" +
                "Motion stream handle: " + motionStreamHandle + "\n" +
                "Server addess: " + serverIP;
        return cameraInfoString;
    }
}
