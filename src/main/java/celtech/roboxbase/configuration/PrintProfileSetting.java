package celtech.roboxbase.configuration;

import java.util.Map;
import java.util.Optional;

/**
 *
 * @author George
 */
public class PrintProfileSetting {
    
    private String id;
    private String settingName;
    private String value;
    private String valueType;
    private String tooltip;
    private Optional<String> unit = Optional.empty();
    private boolean perExtruder;
    private Optional<Map<String, String>> options = Optional.empty();
    
    // Default constructor for Jackson
    public PrintProfileSetting() {}
    
    public PrintProfileSetting(PrintProfileSetting settingToCopy) {
        id = settingToCopy.getId();
        settingName = settingToCopy.getSettingName();
        value = settingToCopy.getValue();
        valueType = settingToCopy.getValueType();
        tooltip = settingToCopy.getTooltip();
        unit = settingToCopy.getUnit();
        perExtruder = settingToCopy.isPerExtruder();
        options = settingToCopy.getOptions();
    }
    
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSettingName() {
        return settingName;
    }

    public void setSettingName(String settingName) {
        this.settingName = settingName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String defaultValue) {
        this.value = defaultValue;
    }

    public String getValueType() {
        return valueType;
    }

    public void setValueType(String valueType) {
        this.valueType = valueType;
    }
    
    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    public Optional<String> getUnit() {
        return unit;
    }

    public void setUnit(Optional<String> unit) {
        this.unit = unit;
    }

    public boolean isPerExtruder() {
        return perExtruder;
    }

    public void setPerExtruder(boolean perExtruder) {
        this.perExtruder = perExtruder;
    }
    
    public Optional<Map<String, String>> getOptions() {
        return options;
    }

    public void setOptions(Optional<Map<String, String>> options) {
        this.options = options;
    }
}
