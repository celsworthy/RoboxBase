package celtech.roboxbase.configuration;

import java.util.List;
import java.util.Map;

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
    
}
