package celtech.roboxbase.configuration.fileRepresentation;

import celtech.roboxbase.configuration.BaseConfiguration;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

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
    
    @JsonProperty("lockFocus")
    private boolean lockFocus = false;
    
    @JsonProperty("focusValue")
    private int focusValue = 0;
    
    @JsonProperty("headLight")
    private boolean headLight = false;
    
    @JsonProperty("ambientLight")
    private boolean ambientLight = false;
    
    /**
     * Default constructor of Jackson
     */
    public CameraProfile() {}
    
    public CameraProfile(CameraProfile profileToCopy) 
    {
        profileName = "";
        captureHeight = profileToCopy.getCaptureHeight();
        captureWidth = profileToCopy.getCaptureWidth();
        lockFocus = profileToCopy.isLockFocus();
        focusValue = profileToCopy.getFocusValue();
        headLight = profileToCopy.isHeadLight();
        ambientLight = profileToCopy.isAmbientLight();
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

    public boolean isLockFocus() 
    {
        return lockFocus;
    }

    public void setLockFocus(boolean lockFocus)
    {
        this.lockFocus = lockFocus;
    }

    public int getFocusValue()
    {
        return focusValue;
    }

    public void setFocusValue(int focusValue) 
    {
        this.focusValue = focusValue;
    }

    public boolean isHeadLight() 
    {
        return headLight;
    }

    public void setHeadLight(boolean headLight) 
    {
        this.headLight = headLight;
    }

    public boolean isAmbientLight() 
    {
        return ambientLight;
    }

    public void setAmbientLight(boolean ambientLight) 
    {
        this.ambientLight = ambientLight;
    }
    
}
