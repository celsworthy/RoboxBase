package celtech.roboxbase.configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private Optional<String> minimumValue = Optional.empty();
    private Optional<String> maximumValue = Optional.empty();
    private Optional<Map<String, String>> options = Optional.empty();
    private Optional<List<PrintProfileSetting>> children = Optional.empty();
    
    // Default constructor for Jackson
    public PrintProfileSetting() {}
    
    /**
     * Copy constructor
     * 
     * @param settingToCopy 
     */
    public PrintProfileSetting(PrintProfileSetting settingToCopy) {
        id = settingToCopy.getId();
        settingName = settingToCopy.getSettingName();
        value = settingToCopy.getValue();
        valueType = settingToCopy.getValueType();
        tooltip = settingToCopy.getTooltip();
        unit = settingToCopy.getUnit();
        perExtruder = settingToCopy.isPerExtruder();
        
        if(settingToCopy.getUnit().isPresent()) {
            unit = Optional.of(settingToCopy.getUnit().get());
        }
        
        if(settingToCopy.getMaximumValue().isPresent()) {
            minimumValue = Optional.of(settingToCopy.getMinimumValue().get());
        }
        
        if(settingToCopy.getMaximumValue().isPresent()) {
            maximumValue = Optional.of(settingToCopy.getMaximumValue().get());
        }
        
        if(settingToCopy.getOptions().isPresent()) {
            options = Optional.of(settingToCopy.getOptions().get());
        }
        
        if(settingToCopy.getChildren().isPresent()) {
            List<PrintProfileSetting> copiedChildren = settingToCopy.getChildren().get().stream()
                    .map(profile -> new PrintProfileSetting(profile))
                    .collect(Collectors.toList());
            children = Optional.of(copiedChildren);
        }
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

    public Optional<String> getMinimumValue() {
        return minimumValue;
    }

    public void setMinimumValue(Optional<String> minimumValue) {
        this.minimumValue = minimumValue;
    }

    public Optional<String> getMaximumValue() {
        return maximumValue;
    }

    public void setMaximumValue(Optional<String> maximumValue) {
        this.maximumValue = maximumValue;
    }
    
    public Optional<Map<String, String>> getOptions() {
        return options;
    }

    public void setOptions(Optional<Map<String, String>> options) {
        this.options = options;
    }

    public Optional<List<PrintProfileSetting>> getChildren() {
        return children;
    }

    public void setChildren(Optional<List<PrintProfileSetting>> children) {
        this.children = children;
    }
}
