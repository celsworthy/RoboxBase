package celtech.roboxbase.configuration.fileRepresentation;

import celtech.roboxbase.camera.CameraInfo;
import celtech.roboxbase.camera.CameraInfo;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.fileRepresentation.CameraProfile;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.parboiled.common.FileUtils;

/**
 *
 * @author George Salter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CameraSettings 
{
    @JsonProperty("profile")
    private CameraProfile profile;
    
    @JsonProperty("camera")
    private CameraInfo camera;

    /**
     * Default constructor of Jackson
     */
    public CameraSettings() {}
    
    public CameraSettings(CameraProfile profile, CameraInfo camera) 
    {
        this.profile = profile;
        this.camera = camera;
    }

    public CameraProfile getProfile()
    {
        return profile;
    }
    
    public void setProfile(CameraProfile profile)
    {
        this.profile = profile;
    }

    public CameraInfo getCamera()
    {
        return camera;
    }
    
    public void setCamera(CameraInfo camera)
    {
        this.camera = camera;
    }
    
    @JsonIgnore
    public void writeToFile(String fileLocation) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        File file = new File(fileLocation);
        FileUtils.ensureParentDir(file);
        mapper.writeValue(file, this);
    }

    @JsonIgnore
    public static CameraSettings readFromFile(String fileLocation) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();

        File file = new File(fileLocation);
        return mapper.readValue(file, CameraSettings.class);
    }

    @JsonIgnore
    public List<String> encodeSettingsForRootScript(String firstParameter)
    {
        List<String> parameters = new ArrayList<>();
        if (firstParameter != null && !firstParameter.isBlank())
            parameters.add(firstParameter);
        parameters.add(camera.getUdevName());
        parameters.add(String.format("%dx%d",
                                  profile.getCaptureWidth(),
                                  profile.getCaptureHeight()));
        profile.getControlSettings().forEach((k, v) -> {
            if (k.startsWith("--")) {
                parameters.add(k);
                parameters.add(v);
            }
            else {
                parameters.add("-s");
                parameters.add(String.format("%s=%s", k, v));
        }});
        return parameters;
    }
}
