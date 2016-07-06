package celtech.roboxbase.i18n;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

public abstract class LanguagePropertiesResourceBundle extends ResourceBundle
{

    private final Stenographer steno = StenographerFactory.getStenographer(LanguagePropertiesResourceBundle.class.getName());

    /**
     * The base name for the ResourceBundles to load in.
     */
    private String baseName;

    /**
     * The package name where the properties files should be.
     */
    private String baseDirectory;

    /**
     */
    private String terminalDirectoryName;

    /**
     * The package name where the properties files should be.
     */
    private String languageFolderName = null;

    /**
     * A Map containing the combined resources of all parts building this
     * MultiplePropertiesResourceBundle.
     */
    private Map<String, Object> combined;

    private final Set<Locale> availableLocales = new HashSet<>();

    /**
     * Construct a <code>MultiplePropertiesResourceBundle</code> for the passed
     * in base-name.
     *
     * @param baseName the base-name that must be part of the properties file
     * names.
     */
    protected LanguagePropertiesResourceBundle(String baseName)
    {
        this(null, baseName);
    }

    /**
     * Construct a <code>MultiplePropertiesResourceBundle</code> for the passed
     * in base-name.
     *
     * @param packageName the package name where the properties files should be.
     * @param baseName the base-name that must be part of the properties file
     * names.
     */
    protected LanguagePropertiesResourceBundle(String packageName, String baseName)
    {
        this(packageName, "", baseName);
    }

    /**
     * Construct a <code>MultiplePropertiesResourceBundle</code> for the passed
     * in base-name.
     *
     * @param baseDirectory the package name where the properties files should
     * be.
     * @param languageFolderName
     * @param baseName the base-name that must be part of the properties file
     * names.
     */
    protected LanguagePropertiesResourceBundle(String baseDirectory,
            String languageFolderName,
            String baseName)
    {
        String baseToWorkOn = baseDirectory.replaceFirst("\\/$", "");
        int lastSlash = baseToWorkOn.lastIndexOf("/");
        if (lastSlash >= 0)
        {
            this.baseDirectory = baseToWorkOn.substring(0, lastSlash + 1);
            terminalDirectoryName = baseToWorkOn.substring(lastSlash + 1);
        } else
        {
            this.baseDirectory = baseToWorkOn;
        }
        this.languageFolderName = languageFolderName;
        this.baseName = baseName;
                steno.info("combined is null " + (combined == null));

        loadBundlesOnce();
    }

    @Override
    public Object handleGetObject(String key)
    {
        if (key == null)
        {
            throw new NullPointerException();
        }
        loadBundlesOnce();
        return combined.get(key);
    }

    @Override
    public Enumeration<String> getKeys()
    {
        loadBundlesOnce();
        ResourceBundle parent = this.parent;
        return new ResourceBundleEnumeration(combined.keySet(), (parent != null) ? parent.getKeys()
                : null);
    }

    private void addBundleData(String resourcePath, String resourceName)
    {
        steno.debug("Adding language resources from " + resourcePath + " with resource name " + resourceName);

        ResourceBundle bundle = null;
        try
        {
            File propFile = new File(resourcePath);
            URL[] urlsToSearch =
            {
                propFile.toURI().toURL()
            };
            URLClassLoader cl = new URLClassLoader(urlsToSearch);

            bundle = ResourceBundle.getBundle(resourceName, BaseLookup.getApplicationLocale(), cl, new UTF8Control());
            Enumeration<String> keys = bundle.getKeys();
            String key = null;
            while (keys.hasMoreElements())
            {
                key = keys.nextElement();
                combined.put(key, bundle.getObject(key));
            }
        } catch (MalformedURLException ex)
        {
            System.err.println("Failed to load multi-language data");
        }

    }

    /**
     * Load the resources once.
     */
    private void loadBundlesOnce()
    {
        if (combined == null)
        {
            combined = new HashMap<String, Object>(128);

            String commonResourcePath = baseDirectory + "Common/" + languageFolderName;
            String specifiedResourcePath = baseDirectory + terminalDirectoryName + "/" + languageFolderName;

            addBundleData(commonResourcePath, baseName);
            addBundleData(specifiedResourcePath, baseName);

            addAvailableLocales(commonResourcePath);
        }
    }

    private void addAvailableLocales(String commonResourcePath)
    {
        File commonDir = new File(commonResourcePath);

        availableLocales.add(Locale.ENGLISH);

        for (String filename : commonDir.list())
        {
            filename = filename.replaceFirst(".properties", "");

            Locale locale = null;

            String[] languageStringParts = filename.split("_");

            switch (languageStringParts.length)
            {
                case 2:
                    locale = new Locale(languageStringParts[1]);
                    steno.debug("Adding locale: " + locale.getDisplayName());
                    availableLocales.add(locale);
                    break;
                case 3:
                    locale = new Locale(languageStringParts[1], languageStringParts[2]);
                    steno.debug("Adding locale: " + locale.getDisplayName());
                    availableLocales.add(locale);
                    break;
            }
        }
    }

    public Set<Locale> getAvailableLocales()
    {
        return availableLocales;
    }
}
