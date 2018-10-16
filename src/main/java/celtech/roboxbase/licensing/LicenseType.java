package celtech.roboxbase.licensing;

import celtech.roboxbase.ApplicationFeature;

/**
 * Describes the type of license a user has. This will directly affect what 
 * {@link ApplicationFeature}s are active
 * 
 * @author George Salter
 */
public enum LicenseType {
    AUTOMAKER_FREE,
    AUTOMAKER_PRO
}
