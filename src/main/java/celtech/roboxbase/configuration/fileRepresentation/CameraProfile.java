package celtech.roboxbase.configuration.fileRepresentation;

import celtech.roboxbase.configuration.BaseConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author George Salter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CameraProfile 
{
    @JsonProperty("profileName")
    private String profileName = BaseConfiguration.defaultCameraProfileName;
    
    @JsonProperty("captureHeight")
    private int captureHeight = 720;
    
    @JsonProperty("captureWidth")
    private int captureWidth = 1080;
    
    @JsonProperty("moveBeforeSnapshot")
    private boolean moveBeforeSnapshot = false;
    
    @JsonProperty("moveToX")
    private int moveToX = 0;

    @JsonProperty("moveToY")
    private int moveToY = 0;

    @JsonProperty("headLightOn")
    private boolean headLightOn = false;
    
    @JsonProperty("ambientLightOn")
    private boolean ambientLightOn = false;
    
    @JsonProperty("cameraName")
    private String cameraName = "";

    @JsonProperty("controlSettings")
    private Map<String, String> controlSettings = new HashMap<>();
    /**
     * Default constructor of Jackson
     */
    public CameraProfile() {}
    
    public CameraProfile(CameraProfile profileToCopy) 
    {
        profileName = "";
        captureHeight = profileToCopy.getCaptureHeight();
        captureWidth = profileToCopy.getCaptureWidth();
        headLightOn = profileToCopy.isHeadLightOn();
        ambientLightOn = profileToCopy.isAmbientLightOn();
        moveBeforeSnapshot = profileToCopy.isMoveBeforeSnapshot();
        moveToX = profileToCopy.getMoveToX();
        moveToY = profileToCopy.getMoveToY();
        cameraName = profileToCopy.getCameraName();
        controlSettings = new HashMap<>(profileToCopy.getControlSettings());
    }

    public String getProfileName()
    {
        return profileName;
    }
    
    public void setProfileName(String profileName)
    {
        this.profileName = profileName;
    }
    
    public int getCaptureHeight()
    {
        return captureHeight;
    }

    public void setCaptureHeight(int captureHeight)
    {
        this.captureHeight = captureHeight;
    }

    public int getCaptureWidth()
    {
        return captureWidth;
    }

    public void setCaptureWidth(int captureWidth) 
    {
        this.captureWidth = captureWidth;
    }

    public boolean isHeadLightOn() 
    {
        return headLightOn;
    }

    public void setHeadLightOn(boolean headLight) 
    {
        this.headLightOn = headLight;
    }

    public void setAmbientLightOn(boolean ambientLight) 
    {
        this.ambientLightOn = ambientLight;
    }

    public boolean isAmbientLightOn() 
    {
        return ambientLightOn;
    }

    public void setMoveBeforeSnapshot(boolean moveBeforeSnapshot) 
    {
        this.moveBeforeSnapshot = moveBeforeSnapshot;
    }

    public boolean isMoveBeforeSnapshot() 
    {
        return moveBeforeSnapshot;
    }

    public int getMoveToX()
    {
        return moveToX;
    }

    public void setMoveToX(int moveToX) 
    {
        this.moveToX = moveToX;
    }

    public int getMoveToY()
    {
        return moveToY;
    }

    public void setMoveToY(int moveToY) 
    {
        this.moveToY = moveToY;
    }

    public String getCameraName()
    {
        return cameraName;
    }
    
    public void setCameraName(String cameraName)
    {
        this.cameraName = cameraName;
    }
    
    public Map<String, String> getControlSettings() 
    {
        return controlSettings;
    }

    public void setControlSettings(Map<String, String> controlSettings) 
    {
        this.controlSettings = controlSettings;
    }

    public String getControlSetting(String control) 
    {
        return controlSettings.get(control);
    }

    public void setControlSetting(String control, String value) 
    {
        controlSettings.put(control, value);
    }
}
