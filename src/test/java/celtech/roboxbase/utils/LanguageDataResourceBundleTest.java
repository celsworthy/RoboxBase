package celtech.roboxbase.utils;

import celtech.roboxbase.UTF8Control;
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
        ResourceBundle bundle = ResourceBundle.getBundle("celtech.roboxbase.utils.LanguageDataResourceBundle", new UTF8Control());
        assertEquals("Nozzle firmware control", bundle.getString("error.ERROR_B_POSITION_LOST"));
        assertEquals(172, bundle.keySet().size());
    }
    
}
