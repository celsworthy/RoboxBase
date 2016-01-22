/*
 * Copyright 2014 CEL UK
 */

package celtech.roboxbase;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author tony
 */
public class ApplicationEnvironment
{
    
    private final ResourceBundle i18nBundle;
    private final Locale appLocale;
        
    
    public ApplicationEnvironment(ResourceBundle i18nBundle, Locale appLocale) {
        this.i18nBundle = i18nBundle;
        this.appLocale = appLocale;
    }
    
    public ResourceBundle getLanguageBundle()
    {
        return i18nBundle;
    }    

    /**
     * @return the appLocale
     */
    public Locale getAppLocale()
    {
        return appLocale;
    }
    
}
