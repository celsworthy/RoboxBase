package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.HeadContainer;
import celtech.roboxbase.configuration.PrintProfileFileFilter;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class SlicerParametersContainer
{
    
    public interface SlicerParametersChangesListener
    {
        
        public void whenSlicerParametersSaved(String originalSettingsName, SlicerParametersFile changedParameters);
        
        public void whenSlicerParametersDeleted(String settingsName);
    }
    
    private static final Stenographer steno = StenographerFactory.getStenographer(
            SlicerParametersContainer.class.getName());
    private static SlicerParametersContainer instance = null;
    private static final ObservableList<SlicerParametersFile> appProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerParametersFile> userProfileList = FXCollections.observableArrayList();
    private static final ObservableList<SlicerParametersFile> completeProfileList = FXCollections.observableArrayList();
    private static final ObservableMap<String, SlicerParametersFile> profileMap = FXCollections.observableHashMap();
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final List<SlicerParametersChangesListener> changesListeners = new ArrayList<>();
    
    public static Set<String> getProfileNames()
    {
        return Collections.unmodifiableSet(profileMap.keySet());
    }
    
    private SlicerParametersContainer()
    {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        loadProfileData();
    }
    
    private static String getSettingsKey(String profileName, String headType)
    {
        return profileName + "#" + headType;
    }
    
    public static String constructFilePath(String profileName, String headType)
    {
        return BaseConfiguration.getUserPrintProfileDirectory() + getSettingsKey(profileName, headType)
                + BaseConfiguration.printProfileFileExtension;
    }
    
    private static void loadProfileData()
    {
        completeProfileList.clear();
        appProfileList.clear();
        userProfileList.clear();
        profileMap.clear();
        
        File applicationDirHandle = new File(
                BaseConfiguration.getApplicationPrintProfileDirectory());
        File[] applicationprofiles = applicationDirHandle.listFiles(new PrintProfileFileFilter());
        ArrayList<SlicerParametersFile> profiles = ingestProfiles(applicationprofiles);
        appProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
        
        File userDirHandle = new File(BaseConfiguration.getUserPrintProfileDirectory());
        File[] userprofiles = userDirHandle.listFiles(new PrintProfileFileFilter());
        profiles = ingestProfiles(userprofiles);
        userProfileList.addAll(profiles);
        completeProfileList.addAll(profiles);
        
        for (SlicerParametersFile profile : completeProfileList)
        {
            profileMap.put(profile.getProfileKey(), profile);
        }
    }
    
    private static ArrayList<SlicerParametersFile> ingestProfiles(File[] userprofiles)
    {
        ArrayList<SlicerParametersFile> profileList = new ArrayList<>();
        
        for (File profileFile : userprofiles)
        {
            SlicerParametersFile newSettings = null;
            String profileKey = profileFile.getName().replaceAll(
                    BaseConfiguration.printProfileFileExtension, "");
            String profileName = profileKey.split("#")[0];
            
            if (profileMap.containsKey(profileKey) == false)
            {
                try
                {
                    newSettings = mapper.readValue(profileFile, SlicerParametersFile.class);
                    // Make sure file name and profile name are in sync
                    newSettings.setProfileName(profileName);
                    
                    convertToCurrentVersion(newSettings);
                    
                    profileList.add(newSettings);
                    profileMap.put(newSettings.getProfileKey(), newSettings);
                } catch (Exception ex)
                {
                    steno.error("Error reading profile " + profileKey + ": " + ex.getMessage());
                }
            } else
            {
                steno.warning("Profile with name " + profileKey
                        + " has already been loaded - ignoring " + profileFile.getAbsolutePath());
            }
        }
        
        return profileList;
    }
    
    private static void convertToCurrentVersion(SlicerParametersFile newSettings)
    {
        
        if (newSettings.getVersion() < 4)
        {
            steno.info("Convert " + newSettings.getProfileName() + " profile to version 4");
            newSettings.setRaftAirGapLayer0_mm(0.285f);
            newSettings.setRaftBaseLinewidth_mm(1.0f);
            newSettings.setInterfaceLayers(1);
            newSettings.setInterfaceSpeed_mm_per_s(40);
            newSettings.setVersion(4);
            doSaveEditedUserProfile(newSettings);
        }
        
        if (newSettings.getVersion() < 5)
        {
            steno.info("Convert " + newSettings.getProfileName() + " profile to version 5");
            newSettings.setHeadType(HeadContainer.defaultHeadID);
            newSettings.setVersion(5);
            newSettings.setzHopHeight(0);
            newSettings.setzHopDistance(0);
            doSaveEditedUserProfile(newSettings);
        }

        //TEMPORARILY SUPPRESS SLIC3R
        if (newSettings.getSlicerOverride() == SlicerType.Slic3r)
        {
            newSettings.setSlicerOverride(SlicerType.Cura);
        }
    }
    
    public static void saveProfile(SlicerParametersFile settingsToSave)
    {
        String originalName = getOriginalProfileName(settingsToSave);
        if (!profileMap.containsKey(settingsToSave.getProfileKey()))
        {
            if (userProfileList.contains(settingsToSave))
            {
                doSaveAndChangeUserProfileName(settingsToSave);
            } else
            {
                doAddNewUserProfile(settingsToSave);
            }
        } else
        {
            doSaveEditedUserProfile(settingsToSave);
        }
        for (SlicerParametersChangesListener changesListener : changesListeners)
        {
            changesListener.whenSlicerParametersSaved(originalName, settingsToSave);
        }
        
    }

    /**
     * Save the given user profile which has had its name changed. This amounts
     * to a delete and add new.
     */
    private static void doSaveAndChangeUserProfileName(SlicerParametersFile profile)
    {
        String originalName = "";
        String originalHeadType = null;
        for (Map.Entry<String, SlicerParametersFile> entrySet : profileMap.entrySet())
        {
            originalName = entrySet.getKey().split("#")[0];
            SlicerParametersFile value = entrySet.getValue();
            if (value == profile)
            {
                originalHeadType = profile.getHeadType();
                break;
            }
        }
        if (originalName.equals(""))
        {
            steno.error("Severe error saving profile of changed name.");
        } else
        {
            deleteUserProfile(originalName, originalHeadType);
            doAddNewUserProfile(profile);
        }
    }

    /**
     * Retrieve the original profile name via the profileMap.
     */
    private static String getOriginalProfileName(SlicerParametersFile profile)
    {
        String originalName = "";
        String originalHeadType = null;
        for (Map.Entry<String, SlicerParametersFile> entrySet : profileMap.entrySet())
        {
            originalName = entrySet.getKey().split("#")[0];
            SlicerParametersFile value = entrySet.getValue();
            if (value == profile)
            {
                originalHeadType = profile.getHeadType();
                break;
            }
        }
        return originalName;
    }

    /**
     * Save the new user profile to disk.
     *
     * @param profile
     */
    private static void doAddNewUserProfile(SlicerParametersFile profile)
    {
        doSaveEditedUserProfile(profile);
        userProfileList.add(profile);
        completeProfileList.add(profile);
        profileMap.put(profile.getProfileKey(), profile);
    }

    /**
     * Save the edited user profile to disk.
     */
    private static void doSaveEditedUserProfile(SlicerParametersFile profile)
    {
        try
        {
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            mapper.writeValue(new File(constructFilePath(profile.getProfileName(), profile.getHeadType())), profile);
        } catch (IOException ex)
        {
            steno.error("Error whilst saving profile " + profile.getProfileName());
        }
    }
    
    public static void deleteUserProfile(String profileName, String headType)
    {
        SlicerParametersFile deletedProfile = getSettings(profileName, headType);
        assert (deletedProfile != null);
        File profileToDelete = new File(constructFilePath(profileName, headType));
        profileToDelete.delete();
        
        userProfileList.remove(deletedProfile);
        completeProfileList.remove(deletedProfile);
        profileMap.remove(deletedProfile.getProfileKey());
        
        for (SlicerParametersChangesListener changesListener : changesListeners)
        {
            changesListener.whenSlicerParametersDeleted(profileName);
        }
        
    }
    
    public static SlicerParametersContainer getInstance()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return instance;
    }
    
    public static SlicerParametersFile getSettings(String profileName, String headType)
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return profileMap.get(getSettingsKey(profileName, headType));
    }
    
    public static ObservableList<SlicerParametersFile> getCompleteProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return completeProfileList;
    }
    
    public static ObservableList<SlicerParametersFile> getUserProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return userProfileList;
    }
    
    public static ObservableList<SlicerParametersFile> getApplicationProfileList()
    {
        if (instance == null)
        {
            instance = new SlicerParametersContainer();
        }
        
        return appProfileList;
    }
    
    public static boolean applicationProfileListContainsProfile(String profileName)
    {
        return appProfileList.stream()
                .anyMatch((profile) -> profile.getProfileName().equalsIgnoreCase(profileName));
    }
    
    public static void addChangesListener(SlicerParametersChangesListener listener)
    {
        changesListeners.add(listener);
    }
    
    public static void removeChangesListener(SlicerParametersChangesListener listener)
    {
        changesListeners.remove(listener);
    }

    /**
     * For testing only
     */
    protected static void reload()
    {
        loadProfileData();
    }
}
