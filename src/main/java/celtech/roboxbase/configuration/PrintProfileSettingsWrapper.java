package celtech.roboxbase.configuration;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author George Salter
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PrintProfileSettingsWrapper {
    
    private Map<String, List<PrintProfileSetting>> printProfileSettings;

    @JsonProperty
    public Map<String, List<PrintProfileSetting>> getPrintProfileSettings() {
        return printProfileSettings;
    }

    public void setPrintProfileSettings(Map<String, List<PrintProfileSetting>> printProfileSettings) {
        this.printProfileSettings = printProfileSettings;
    }
    
    @JsonIgnore
    public List<PrintProfileSetting> getAllSettingsInSection(String section) {
        List<PrintProfileSetting> settings = printProfileSettings.get(section);
        List<PrintProfileSetting> allSettingsList = new ArrayList<>();
        settings.forEach(setting -> {
            allSettingsList.add(setting);
            if(setting.getChildren().isPresent()) {
                allSettingsList.addAll(setting.getChildren().get());
            }
        });
        
        return allSettingsList;
    }
    
    @JsonIgnore
    public List<PrintProfileSetting> getAllSettings() {
        List<PrintProfileSetting> allSettings = new ArrayList<>();
        printProfileSettings.keySet().forEach(section -> {
            allSettings.addAll(getAllSettingsInSection(section));
        });
        
        return allSettings;
    }
    
    public PrintProfileSettingsWrapper copy() {
        Map<String, List<PrintProfileSetting>> printProfileSettingsMapClone = new HashMap<>();
        
        printProfileSettings.entrySet().forEach((entry) -> {
            List<PrintProfileSetting> settingsClones = new ArrayList<>();
            entry.getValue().forEach((setting) -> {
                settingsClones.add(new PrintProfileSetting(setting));
            });
            
            printProfileSettingsMapClone.put(entry.getKey(), settingsClones);
        });
        
        PrintProfileSettingsWrapper printProfileSettingsClone = new PrintProfileSettingsWrapper();
        printProfileSettingsClone.setPrintProfileSettings(printProfileSettingsMapClone);
        return printProfileSettingsClone;
    }
}
