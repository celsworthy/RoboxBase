package celtech.roboxbase.camera;

import java.util.Objects;

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

    @Override
    public int hashCode() 
    {
        int hash = 7;
        hash = 79 * hash + Objects.hashCode(this.udevName);
        hash = 79 * hash + Objects.hashCode(this.cameraName);
        hash = 79 * hash + this.cameraNumber;
        hash = 79 * hash + Objects.hashCode(this.motionControlHandle);
        hash = 79 * hash + Objects.hashCode(this.motionStreamHandle);
        hash = 79 * hash + Objects.hashCode(this.serverIP);
        return hash;
    }

    @Override
    public boolean equals(Object obj) 
    {
        if (this == obj) 
        {
            return true;
        }
        if (obj == null) 
        {
            return false;
        }
        if (getClass() != obj.getClass()) 
        {
            return false;
        }
        
        final CameraInfo other = (CameraInfo) obj;
        
        if (this.cameraNumber != other.cameraNumber) 
        {
            return false;
        }
        if (!Objects.equals(this.udevName, other.udevName)) 
        {
            return false;
        }
        if (!Objects.equals(this.cameraName, other.cameraName))
        {
            return false;
        }
        if (!Objects.equals(this.motionControlHandle, other.motionControlHandle))
        {
            return false;
        }
        if (!Objects.equals(this.motionStreamHandle, other.motionStreamHandle))
        {
            return false;
        }
        if (!Objects.equals(this.serverIP, other.serverIP)) 
        {
            return false;
        }
        return true;
    }
}
