package celtech.roboxbase.configuration;

import java.util.Map;

/**
 *
 * @author George Salter
 */
public class RoboxProfile {
    
    private final String name;
    private final String headType;
    private final boolean standardProfile;
    
    private Map<String, String> settings;
    
    public RoboxProfile(String name, String headType, boolean standardProfile) {
        this.name = name;
        this.headType = headType;
        this.standardProfile = standardProfile;
    }

    public String getName() {
        return name;
    }

    public String getHeadType() {
        return headType;
    }

    public boolean isStandardProfile() {
        return standardProfile;
    }

    public Map<String, String> getSettings() {
        return settings;
    }

    public void setSettings(Map<String, String> settings) {
        this.settings = settings;
    }

    @Override
    public String toString() {
        return name;
    }
}
