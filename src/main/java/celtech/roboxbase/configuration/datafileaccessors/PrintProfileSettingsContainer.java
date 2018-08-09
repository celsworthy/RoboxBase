package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.PrintProfileSettings;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class PrintProfileSettingsContainer {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(
            PrintProfileSettingsContainer.class.getName());
    
    private static final PrintProfileSettingsContainer INSTANCE = new PrintProfileSettingsContainer();
    
    private static PrintProfileSettings printProfileSettings;
    
    private PrintProfileSettingsContainer() {
        loadPrintProfileSettingsFile();
    }
    
    public static PrintProfileSettingsContainer getInstance() {
        return INSTANCE;
    }
    
    public PrintProfileSettings getPrintProfileSettings() {
        return printProfileSettings;
    }
    
    private static void loadPrintProfileSettingsFile() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new Jdk8Module());
        
        File printProfileSettingsFile = new File(BaseConfiguration.getPrintProfileSettingsFileLocation());
        
        try {
            printProfileSettings = objectMapper.readValue(printProfileSettingsFile, PrintProfileSettings.class);
        } catch (IOException ex) {
            STENO.error("Failed to load file: " + printProfileSettingsFile.getPath());
            STENO.error(ex.getMessage());
        }
    }
}
