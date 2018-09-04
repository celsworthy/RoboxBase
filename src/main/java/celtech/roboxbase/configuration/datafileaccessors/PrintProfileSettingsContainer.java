package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.PrintProfileSetting;
import celtech.roboxbase.configuration.PrintProfileSettings;
import celtech.roboxbase.configuration.SlicerType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class PrintProfileSettingsContainer {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(
            PrintProfileSettingsContainer.class.getName());
    
    private static PrintProfileSettingsContainer instance;
    
    private static Map<SlicerType, PrintProfileSettings> printProfileSettings;
    private static Map<SlicerType, PrintProfileSettings> defaultPrintProfileSettings;
    
    private PrintProfileSettingsContainer() {
        printProfileSettings = new HashMap<>();
        defaultPrintProfileSettings = new HashMap<>();
        loadPrintProfileSettingsFile();
    }
    
    public static PrintProfileSettingsContainer getInstance() {
        if(instance == null) {
            instance = new PrintProfileSettingsContainer();
        }
        return instance;
    }
    
    public PrintProfileSettings getPrintProfileSettingsForSlicer(SlicerType slicerType) {
        return printProfileSettings.get(slicerType);
    }
    
    public PrintProfileSettings getDefaultPrintProfileSettingsForSlicer(SlicerType slicerType) {
        return defaultPrintProfileSettings.get(slicerType);
    }
    
    public Map<String, List<PrintProfileSetting>> compareAndGetDifferencesBetweenSettings(PrintProfileSettings originalSettings, PrintProfileSettings newSettings) {
        Map<String, List<PrintProfileSetting>> changedValuesMap = new HashMap<>();
        
        for(Entry<String, List<PrintProfileSetting>> entry : originalSettings.getPrintProfileSettings().entrySet()) {
            String settingSection = entry.getKey();
            List<PrintProfileSetting> originalSettingsList = originalSettings.getAllSettingsInSection(settingSection);
            List<PrintProfileSetting> newSettingsList = newSettings.getAllSettingsInSection(settingSection);
            List<PrintProfileSetting> changedSettingsInSection = new ArrayList<>();
            
            originalSettingsList.forEach(originalSetting -> {        
                Optional<PrintProfileSetting> possibleChangedSetting = newSettingsList.stream()
                        .filter(newSetting -> originalSetting.getId().equals(newSetting.getId()))
                        .filter(newSetting -> !originalSetting.getValue().equals(newSetting.getValue()))
                        .findFirst();
                
                if(possibleChangedSetting.isPresent()) {
                    changedSettingsInSection.add(possibleChangedSetting.get());
                }
            });
            
            if(!changedSettingsInSection.isEmpty()) {
                changedValuesMap.put(settingSection, changedSettingsInSection);
            }
        }
        
        return changedValuesMap;
    }
    
    private static void loadPrintProfileSettingsFile() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        
        File curaPrintProfileSettingsFile = new File(BaseConfiguration.getPrintProfileSettingsFileLocation(SlicerType.Cura));
        File cura3PrintProfileSettingsFile = new File(BaseConfiguration.getPrintProfileSettingsFileLocation(SlicerType.Cura3));
        
        STENO.debug("File path for cura print profile settings file: " + curaPrintProfileSettingsFile.getAbsolutePath());
        STENO.debug("File path for cura3 print profile settings file: " + cura3PrintProfileSettingsFile.getAbsolutePath());
        
        try {
            PrintProfileSettings curaPrintProfileSettings = objectMapper.readValue(curaPrintProfileSettingsFile, PrintProfileSettings.class);
            PrintProfileSettings cura3PrintProfileSettings = objectMapper.readValue(cura3PrintProfileSettingsFile, PrintProfileSettings.class);
            
            printProfileSettings.put(SlicerType.Cura, curaPrintProfileSettings);
            printProfileSettings.put(SlicerType.Cura3, cura3PrintProfileSettings);
            
            defaultPrintProfileSettings.put(SlicerType.Cura, curaPrintProfileSettings.copy());
            defaultPrintProfileSettings.put(SlicerType.Cura3, cura3PrintProfileSettings.copy());
        } catch (IOException ex) {
            STENO.error(ex.getMessage());
        }
    }
}
