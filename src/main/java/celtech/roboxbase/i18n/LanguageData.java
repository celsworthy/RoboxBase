package celtech.roboxbase.i18n;

import celtech.roboxbase.configuration.BaseConfiguration;

/**
 *
 * @author ianhudson
 */
public class LanguageData extends LanguagePropertiesResourceBundle
{

    public LanguageData()
    {
        super(BaseConfiguration.getApplicationInstallDirectory(null), "Language", "LanguageData");
    }

}
