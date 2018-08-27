package celtech.roboxbase.configuration.slicer;

import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Nozzle;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.utils.cura.CuraDefaultSettingsEditor;
import celtech.roboxbase.utils.models.PrintableMeshes;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class Cura3ConfigConvertor {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(
            Cura3ConfigConvertor.class.getName());
    
    private final Printer printer;
    private final PrintableMeshes printableMeshes;
    
    private CuraDefaultSettingsEditor curaDefaultSettingsEditor;
    
    public Cura3ConfigConvertor(Printer printer, PrintableMeshes printableMeshes) {
        this.printer = printer;
        this.printableMeshes = printableMeshes;
    }
    
    public void injectConfigIntoCura3SettingsFile(String configFile) {
        curaDefaultSettingsEditor = new CuraDefaultSettingsEditor();
        curaDefaultSettingsEditor.beginEditing();
        
        addDefaultsForPrinter();
        addExtrudersAndDefaults();
        addMappedSettings(configFile);
        
        curaDefaultSettingsEditor.endEditing();
    }
    
    private void addDefaultsForPrinter() {
        curaDefaultSettingsEditor.editDefaultFloatValue("machine_width", 
                printer.printerConfigurationProperty().get().getPrintVolumeWidth());
        curaDefaultSettingsEditor.editDefaultFloatValue("machine_depth", 
                printer.printerConfigurationProperty().get().getPrintVolumeDepth());
        curaDefaultSettingsEditor.editDefaultFloatValue("machine_height", 
                printer.printerConfigurationProperty().get().getPrintVolumeHeight());
        
        curaDefaultSettingsEditor.editDefaultFloatValue("mesh_position_x", 
                (float) -printableMeshes.getCentreOfPrintedObject().getX());
        curaDefaultSettingsEditor.editDefaultFloatValue("mesh_position_y", 
                (float) -printableMeshes.getCentreOfPrintedObject().getZ());
        
        curaDefaultSettingsEditor.editDefaultIntValue("machine_extruder_count", 
                printableMeshes.getUsedExtruders().size());
        curaDefaultSettingsEditor.editDefaultIntValue("extruders_enabled_count", 
                printableMeshes.getUsedExtruders().size());
    }
    
    private void addExtrudersAndDefaults() {
        Head headOnPrinter = printer.headProperty().get();
        List<Nozzle> nozzles = headOnPrinter.getNozzles();
        for(int i = 0; i < nozzles.size(); i++) {
            String nozzleReference = "noz" + String.valueOf(i + 1);
            curaDefaultSettingsEditor.beginNewExtruderFile(nozzleReference);
            Nozzle nozzle = nozzles.get(i);
            curaDefaultSettingsEditor.editExtruderValue("machine_nozzle_id", nozzleReference, nozzleReference);
            curaDefaultSettingsEditor.editExtruderValue("machine_nozzle_size", nozzleReference, 
                    String.valueOf(nozzle.diameterProperty().get()));
        }
        
    }
    
    private void addMappedSettings(String configFile) {
        try {
            File configOptions = new File(configFile);
            BufferedReader fileReader = new BufferedReader(new FileReader(configOptions));
            
            String readLine;
            
            while((readLine = fileReader.readLine()) != null) {
                if(!readLine.startsWith("#")) {
                    String[] settingAndValue = readLine.split("=");
                    String settingName = settingAndValue[0];
                    String value = settingAndValue[1];
                    if (value.contains(":")) {
                        String[] valuesForNozzles = value.split(":");
                        for(int i = 0; i < valuesForNozzles.length; i++) {
                            String nozzleReference = "noz" + String.valueOf(i + 1);
                            curaDefaultSettingsEditor.editExtruderValue(settingName, nozzleReference, valuesForNozzles[i]);
                        }
                    } else {
                        curaDefaultSettingsEditor.editDefaultValue(settingName, value);
                    }
                }
            }
        } catch (FileNotFoundException ex) {
            STENO.error("Config file: " + configFile + " could not be found.");
            STENO.error(ex.getMessage());
        } catch (IOException ex) {
            STENO.error("Error while reading config file: " + configFile);
            STENO.error(ex.getMessage());
        }
    }
}
