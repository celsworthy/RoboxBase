package celtech.roboxbase.configuration;

import java.util.Optional;

/**
 *
 * @author George
 */
public class PrintProfileSetting {
    
    private String id;
    private String settingName;
    private String tooltip;
    private Optional<String> unit = Optional.empty();

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
}
