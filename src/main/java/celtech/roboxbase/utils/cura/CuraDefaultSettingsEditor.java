package celtech.roboxbase.utils.cura;

import celtech.roboxbase.configuration.BaseConfiguration;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * A class to allow editing of the Cura 3 default settings file.
 * 
 * @author George Salter
 */
public class CuraDefaultSettingsEditor {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(CuraDefaultSettingsEditor.class.getName());
    
    private static final String JSON_SETTINGS_FILE = BaseConfiguration.getCommonApplicationDirectory() + "PrintProfiles/fdmprinter.def.json";
    private static final String JSON_EXTRUDER_SETTINGS_FILE = BaseConfiguration.getCommonApplicationDirectory() + "PrintProfiles/fdmextruder.def.json";
    private static final String EDITED_FILE = BaseConfiguration.getApplicationStorageDirectory() + "fdmprinter_robox.def.json";
    private static final String EDITED_EXTRUDER_FILE = BaseConfiguration.getApplicationStorageDirectory() + "fdmextruder_robox.def.json";
    private static final String SETTINGS = "settings";
    private static final String CHILDREN = "children";
    private static final String DEFAULT_VALUE = "default_value";
    private static final String TYPE = "type";
    private static final String METADATA = "metadata";
    private static final String EXTRUDER_TRAINS = "machine_extruder_trains";
    
    private final ObjectMapper mapper = new ObjectMapper();
    
    private final Map<String, JsonNode> settingsNodes = new HashMap<>();  
    
    private JsonNode settingsRootNode = null;
    private JsonNode extruderRootNode = null;
    
    /**
     * Read the JSON file into nodes. 
     */
    public void beginEditing() {
        STENO.info("Reading cura settings file for editing.");
        try {
            settingsRootNode = mapper.readTree(new File(JSON_SETTINGS_FILE));
            Iterator<Entry<String, JsonNode>> sections = settingsRootNode.get(SETTINGS).fields();
            while(sections.hasNext()) {
                Entry<String, JsonNode> settingsNode = sections.next();
                addSettingsToMap(settingsNode);
            }
            
            extruderRootNode = mapper.readTree(new File(JSON_EXTRUDER_SETTINGS_FILE));
        } catch (IOException ex) {
            STENO.error("Failed to read json file: " + JSON_SETTINGS_FILE + " in " + this.getClass().getName());
            STENO.error(ex.getMessage());
        }
    }
    
    /**
     * Write the changes back to JSON.
     */
    public void endEditing() {
        STENO.info("Writing changes made to " + EDITED_FILE);
        try {
            addExtruders();
            
            JsonFactory factory = new JsonFactory();
            JsonGenerator generator = factory.createGenerator(new File(EDITED_FILE), JsonEncoding.UTF8);
            mapper.writeTree(generator, settingsRootNode);
            
            generator = factory.createGenerator(new File(EDITED_EXTRUDER_FILE), JsonEncoding.UTF8);
            mapper.writeTree(generator, extruderRootNode);
        } catch (IOException ex) {
            STENO.error("Failed to write json to file: " + EDITED_FILE + " in " + this.getClass().getName());
            STENO.error(ex.getMessage());
        }
    }
    
    /**
     * Edit the default value of a specified setting.
     * 
     * @param settingId the id of the setting to be changed.
     * @param value the new default value for the setting.
     */
    public void editDefaultValue(String settingId, float value) {
        ObjectNode settingObjectNode = (ObjectNode) settingsNodes.get(settingId);
        String type = settingObjectNode.get(TYPE).asText();
        if (type.equals("float")) {
            settingObjectNode.remove(DEFAULT_VALUE);
            settingObjectNode.put(DEFAULT_VALUE, value);
        } else {
            STENO.error("Setting value is of type: " + type + " is not compatible with float of " + value);
        }
    }
    
    /**
     * Add settings node and any children to the map of settings nodes.
     * 
     * @param settingsNode node to be added.
     */
    private void addSettingsToMap(Entry<String, JsonNode> settingsNode) {
        if(settingsNode.getValue().has(CHILDREN)) {
            Iterator<Entry<String, JsonNode>> children = settingsNode.getValue().get(CHILDREN).fields();
            while(children.hasNext()) {
                Entry<String, JsonNode> childNode = children.next();
                addSettingsToMap(childNode);
            }
        }
        
        settingsNodes.put(settingsNode.getKey(), settingsNode.getValue());
    }
    
    /**
     * Add the extruder file references to the main settings.
     */
    private void addExtruders() {
        ObjectNode extruders = (ObjectNode) settingsRootNode.get(METADATA).get(EXTRUDER_TRAINS);
        extruders.put("0", "fdmextruder_robox");
    }
}
