package celtech.roboxbase.utils;

import celtech.roboxbase.i18n.UTF8Control;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Locale;
import java.util.ResourceBundle;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author ianhudson
 */
public class LanguageDataResourceBundleTest
{

    public LanguageDataResourceBundleTest()
    {
    }

    @BeforeClass
    public static void setUpClass()
    {
    }

    @AfterClass
    public static void tearDownClass()
    {
    }

    @Before
    public void setUp()
    {
    }

    @After
    public void tearDown()
    {
    }

    @Test
    public void testLocaleUK()
    {
        Locale.setDefault(Locale.ENGLISH);
        
        File file = new File("src/test/resources/InstallDir/Common/Language");

        try
        {
            URL[] urls =
            {
                file.toURI().toURL()
            };
            ClassLoader loader = new URLClassLoader(urls);
            ResourceBundle bundle = ResourceBundle.getBundle("LanguageData", Locale.getDefault(), loader, new UTF8Control());

            assertEquals("Nozzle firmware control", bundle.getString("error.ERROR_B_POSITION_LOST"));
            assertEquals(175, bundle.keySet().size());
        } catch (MalformedURLException ex)
        {
            fail();
        }
    }
}
