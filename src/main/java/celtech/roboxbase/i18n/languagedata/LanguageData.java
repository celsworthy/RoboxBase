package celtech.roboxbase.i18n.languagedata;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.i18n.LanguagePropertiesResourceBundle;

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
