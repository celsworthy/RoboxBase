package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.SlicerType;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class RoboxProfileSettingsContainer {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(
            RoboxProfileSettingsContainer.class.getName());
    
    private static final RoboxProfileSettingsContainer INSTANCE = new RoboxProfileSettingsContainer();
    
    private static final String TITLE_BORDER = "//==============";
    
    private static Map<String, List<RoboxProfile>> curaRoboxProfiles;
    private static Map<String, List<RoboxProfile>> cura3RoboxProfiles;
    private static Map<String, List<RoboxProfile>> slic3rRoboxProfiles;
    
    public RoboxProfileSettingsContainer() {
        curaRoboxProfiles = new HashMap<>();
        cura3RoboxProfiles = new HashMap<>();
        slic3rRoboxProfiles = new HashMap<>();
        loadRoboxProfiles();
    }
    
    public static RoboxProfileSettingsContainer getInstance() {
        return INSTANCE;
    }
    
    public Map<String, List<RoboxProfile>> getRoboxProfilesForSlicer(SlicerType slicerType) {
        switch (slicerType) {
            case Cura:
                return curaRoboxProfiles;
            case Cura3:
                return cura3RoboxProfiles;
            case Slic3r:
                return slic3rRoboxProfiles;
            default:
                return new HashMap<>();
        }
    }
    
    public RoboxProfile loadHeadProfileForSlicer(String headType, SlicerType slicerType) {
        File slicerApplicationProfileDirectory = new File(BaseConfiguration.getApplicationPrintProfileDirectoryForSlicer(slicerType));
        for(File headDir : slicerApplicationProfileDirectory.listFiles()) {
            if(headDir.getName().equals(headType)) {
                Map<String, String> settingsMap = loadHeadSettingsIntoMap(headType, headDir);
                RoboxProfile headProfile = new RoboxProfile(headType, headType, true);
                headProfile.setSettings(settingsMap);
                return headProfile;
            }
        }
        
        return null;
    }
    
    public RoboxProfile saveCustomProfile(Map<String, List<PrintProfileSetting>> settingsToWrite, String nameForProfile, 
            String headType, SlicerType slicerType) {
        String headDirPath = BaseConfiguration.getUserPrintProfileDirectoryForSlicer(slicerType) + "/" + headType;
        switch(slicerType) {
            case Cura:
                return saveCustomProfile(settingsToWrite, nameForProfile, curaRoboxProfiles.get(headType), headDirPath, headType);
            case Cura3:
                return saveCustomProfile(settingsToWrite, nameForProfile, cura3RoboxProfiles.get(headType), headDirPath, headType);
            case Slic3r:
                return saveCustomProfile(settingsToWrite, nameForProfile, slic3rRoboxProfiles.get(headType), headDirPath, headType);
        }
        
        return null;
    }
    
    private static void loadRoboxProfiles() {
        File curaApplicationProfileDirectory = new File(BaseConfiguration.getApplicationPrintProfileDirectoryForSlicer(SlicerType.Cura));
        File curaUserProfileDirectory = new File(BaseConfiguration.getUserPrintProfileDirectoryForSlicer(SlicerType.Cura));
        loadRoboxProfilesIntoMap(curaApplicationProfileDirectory, curaRoboxProfiles, true);
        loadRoboxProfilesIntoMap(curaUserProfileDirectory, curaRoboxProfiles, false);
        File cura3ApplicationProfileDirectory = new File(BaseConfiguration.getApplicationPrintProfileDirectoryForSlicer(SlicerType.Cura3));
        File cura3UserProfileDirectory = new File(BaseConfiguration.getUserPrintProfileDirectoryForSlicer(SlicerType.Cura3));
        loadRoboxProfilesIntoMap(cura3ApplicationProfileDirectory, cura3RoboxProfiles, true);
        loadRoboxProfilesIntoMap(cura3UserProfileDirectory, cura3RoboxProfiles, false);
    }
    
    private static void loadRoboxProfilesIntoMap(File profileDirectory, Map<String, List<RoboxProfile>> profilesMap, boolean standardProfile) {
        for(File headDir : profileDirectory.listFiles()) {
            if(headDir.isDirectory()) {
                String headType = headDir.getName();
                Map<String, String> settings = loadHeadSettingsIntoMap(headType, headDir);

                List<RoboxProfile> roboxProfiles = new ArrayList<>();

                for(File profile : headDir.listFiles()) {
                    String profileName = profile.getName().split("\\.")[0];
                    if(!profileName.equals(headType)) {
                        Map<String, String> profileSettings = new HashMap<>(settings);
                        addOrOverriteSettings(profile, profileSettings);
                        RoboxProfile roboxProfile = new RoboxProfile(profileName, headType, standardProfile);
                        roboxProfile.setSettings(profileSettings);
                        roboxProfiles.add(roboxProfile);
                    }
                }

                if(profilesMap.containsKey(headType)) {
                    profilesMap.get(headType).addAll(roboxProfiles);
                } else {
                    profilesMap.put(headType, roboxProfiles);
                }
            }
        }
    }
    
    private static Map<String, String> loadHeadSettingsIntoMap(String headType, File headDirectory) {
        File[] headFilesFiltered = headDirectory.listFiles((File dir, String name) -> {
            return name.split("\\.")[0].equals(headType);
        });
            
        if(headFilesFiltered.length == 0) {
            STENO.warning("No head profile exists in folder: " + headDirectory.getPath());
            STENO.warning("Creating empty map for settings");
            return new HashMap<>();
        }
        
        File headFile = headFilesFiltered[0];
        Map<String, String> settingsMap = new HashMap<>();
        
        addOrOverriteSettings(headFile, settingsMap);
        
        return settingsMap;
    }
    
    private static void addOrOverriteSettings(File settings, Map<String, String> settingsMap) {
         try(BufferedReader br = new BufferedReader(new FileReader(settings))) {
            String line;
            while((line = br.readLine()) != null) {
                if(!(line.trim().startsWith("//") || line.trim().equals(""))) {
                    String[] keyValuePair = line.split("=");
                    settingsMap.put(keyValuePair[0], keyValuePair[1]);
                }
            }
        } catch (FileNotFoundException ex) {
            STENO.error(ex.getMessage());
        } catch (IOException ex) {
            STENO.error("Error when reading file: " + settings.getPath());
            STENO.error(ex.getMessage());
        }
    }
    
    private RoboxProfile saveCustomProfile(Map<String, List<PrintProfileSetting>> settingsToWrite, String nameForProfile, 
            List<RoboxProfile> loadedSettingsForHead, String headDirPath, String headType) {
        RoboxProfile roboxProfile;
        
        Optional<RoboxProfile> existingProfile = loadedSettingsForHead.stream()
                .filter(profile -> profile.getName().equals(nameForProfile))
                .findAny();
        if(existingProfile.isPresent()) {
            loadedSettingsForHead.remove(existingProfile.get());
            roboxProfile = saveUserProfile(nameForProfile, headDirPath, settingsToWrite, headType);
        } else {
             roboxProfile = saveUserProfile(nameForProfile, headDirPath, settingsToWrite, headType);
        }
        loadedSettingsForHead.add(roboxProfile);
        return roboxProfile;
    }
    
    private RoboxProfile saveUserProfile(String profileName, String headDirPath, 
            Map<String, List<PrintProfileSetting>> settingsToWrite, String headType) {
        String profileFilePath = headDirPath + "/" + profileName + BaseConfiguration.printProfileFileExtension;
        File file = new File(profileFilePath);
        if(file.exists()) {
            file.delete();
        }
        writeRoboxProfile(profileFilePath, settingsToWrite);
        
        Map<String, String> settingsMap = loadHeadSettingsIntoMap(headType, new File(headDirPath));
        addOrOverriteSettings(new File(profileFilePath), settingsMap);
        RoboxProfile roboxProfile = new RoboxProfile(profileName, headType, false);
        roboxProfile.setSettings(settingsMap);
        return roboxProfile;
    }
    
    private void writeRoboxProfile(String profileFilePath, Map<String, List<PrintProfileSetting>> settingsToWrite) {
        try (PrintWriter printWriter = new PrintWriter(new FileWriter(profileFilePath))) {
            for(Entry<String, List<PrintProfileSetting>> entry : settingsToWrite.entrySet()) {
                String settingsSection = entry.getKey();
                printWriter.println(TITLE_BORDER);
                printWriter.println("//" + settingsSection);
                printWriter.println(TITLE_BORDER);

                entry.getValue().forEach((setting) -> {
                    printWriter.println(setting.getId() + "=" + setting.getValue());
                });

                printWriter.println("");
            }
            
            printWriter.close();
        } catch (IOException ex) {
            STENO.error(ex.getMessage());
        }
    }
}
