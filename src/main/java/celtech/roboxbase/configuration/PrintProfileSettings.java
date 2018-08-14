package celtech.roboxbase.configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author George Salter
 */
public class PrintProfileSettings {
    
    private Map<String, List<PrintProfileSetting>> printProfileSettings;

    public Map<String, List<PrintProfileSetting>> getPrintProfileSettings() {
        return printProfileSettings;
    }

    public void setPrintProfileSettings(Map<String, List<PrintProfileSetting>> printProfileSettings) {
        this.printProfileSettings = printProfileSettings;
    }
    
    public PrintProfileSettings copy() {
        Map<String, List<PrintProfileSetting>> printProfileSettingsMapClone = new HashMap<>();
        
        printProfileSettings.entrySet().forEach((entry) -> {
            List<PrintProfileSetting> settingsClones = new ArrayList<>();
            entry.getValue().forEach((setting) -> {
                settingsClones.add(new PrintProfileSetting(setting));
            });
            
            printProfileSettingsMapClone.put(entry.getKey(), settingsClones);
        });
        
        PrintProfileSettings printProfileSettingsClone = new PrintProfileSettings();
        printProfileSettingsClone.setPrintProfileSettings(printProfileSettingsMapClone);
        return printProfileSettingsClone;
    }
}
