package celtech.roboxbase.i18n;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public abstract class LanguagePropertiesResourceBundle extends ResourceBundle
{

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
        System.out.println("Input:" + baseDirectory + " ModInput:" + baseToWorkOn + " Lang:" + languageFolderName + " BaseName:" + baseName + " ind:" + lastSlash);
        if (lastSlash >= 0)
        {
            this.baseDirectory = baseDirectory.substring(0, lastSlash + 1);
            terminalDirectoryName = baseDirectory.substring(lastSlash + 1);
        } else
        {
            this.baseDirectory = baseDirectory;
        }
        this.languageFolderName = languageFolderName;
        this.baseName = baseName;
        System.out.println("BaseDir:" + this.baseDirectory + " Lang:" + this.languageFolderName + " BaseName:" + baseName);
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
        ResourceBundle bundle = null;
        try
        {
            File propFile = new File(resourcePath);
            URL[] urlsToSearch =
            {
                propFile.toURI().toURL()
            };
            URLClassLoader cl = new URLClassLoader(urlsToSearch);

            bundle = ResourceBundle.getBundle(resourceName, Locale.getDefault(), cl, new UTF8Control());
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
        }
    }
}
