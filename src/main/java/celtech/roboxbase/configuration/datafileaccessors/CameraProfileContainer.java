package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.comms.DetectedServer;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
import celtech.roboxbase.configuration.fileRepresentation.CameraProfile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class CameraProfileContainer 
{
    private static final Stenographer STENO = StenographerFactory.getStenographer(CameraProfileContainer.class.getName());
    
    private static CameraProfileContainer instance;
    
    private static Map<String, CameraProfile> cameraProfilesMap;
 
    private static String anyCameraName = "";

    private CameraProfileContainer()
    {
        cameraProfilesMap = new HashMap<>();
        CameraProfile defaultCameraProfile = new CameraProfile();
        cameraProfilesMap.put(defaultCameraProfile.getProfileName().toLowerCase(), defaultCameraProfile);
        loadCameraProfiles();
    }
    
    public static CameraProfileContainer getInstance() 
    {
        if(instance == null)
        {
            instance = new CameraProfileContainer();
        }
        return instance;
    }
    
    private void loadCameraProfiles() 
    {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        
        Path cameraProfilesPath = Path.of(BaseConfiguration.getUserCameraProfileDirectory());
        
        String cameraProfileSearchString = "*" + BaseConfiguration.cameraProfileFileExtention;
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(cameraProfilesPath, cameraProfileSearchString))
        {
            for(Path path : stream)
            {
                CameraProfile cameraProfile = objectMapper.readValue(path.toFile(), CameraProfile.class);
                cameraProfilesMap.put(cameraProfile.getProfileName().toLowerCase(), cameraProfile);
            }
        } catch (IOException ex) 
        {
            STENO.exception("Error when loading Camera profiles from " + cameraProfilesPath.toString(), ex);
        }
    }
    
    public void saveCameraProfile(CameraProfile cameraProfile)
    {
        if (cameraProfile.getProfileName().equals(BaseConfiguration.defaultCameraProfileName))
        {
            STENO.warning("Can't save default camera profile.");
        }
        else
        {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new Jdk8Module());

            String cameraProfileFileName = cameraProfile.getProfileName() + BaseConfiguration.cameraProfileFileExtention;
            Path cameraProfilePath = Path.of(BaseConfiguration.getUserCameraProfileDirectory() + File.separator + cameraProfileFileName);

            try 
            {
                objectMapper.writerWithDefaultPrettyPrinter().writeValue(cameraProfilePath.toFile(), cameraProfile);
                cameraProfilesMap.put(cameraProfile.getProfileName().toLowerCase(), cameraProfile);
            } catch (IOException ex) 
            {
                STENO.exception("Error when trying to save profile of " + cameraProfilePath.toString(), ex);
            }
        }
    }
    
    public void deleteCameraProfile(CameraProfile cameraProfile)
    {
        if (cameraProfile.getProfileName().equals(BaseConfiguration.defaultCameraProfileName))
        {
            STENO.warning("Can't delete default camera profile.");
        }
        else
        {
            String profileName = cameraProfile.getProfileName();
            String profileNameLC = profileName.toLowerCase();

            if (cameraProfilesMap.containsKey(profileNameLC))
            {
                cameraProfilesMap.remove(profileNameLC);
            } else
            {
                STENO.error("File " + profileName + ", doesn't exist");
            }

            String filePath = BaseConfiguration.getUserCameraProfileDirectory() + File.separator 
                       + profileName + BaseConfiguration.cameraProfileFileExtention;
            Path fileToDelete = Paths.get(filePath);

            try 
            {
                Files.deleteIfExists(fileToDelete);
            } catch (IOException ex) 
            {
                STENO.exception("Error when trying to delete profile of " + filePath, ex);
            }
        }
    }
    
    public Map<String, CameraProfile> getCameraProfilesMap()
    {
        return cameraProfilesMap;
    }

    public CameraProfile getProfileByName(String profileName)
    {
        return cameraProfilesMap.get(profileName.toLowerCase());
    }

    public CameraProfile getDefaultProfile()
    {
        return cameraProfilesMap.get(BaseConfiguration.defaultCameraProfileName.toLowerCase());
    }
}
