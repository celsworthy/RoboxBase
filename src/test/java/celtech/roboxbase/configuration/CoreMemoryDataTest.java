package celtech.roboxbase.configuration;

import celtech.roboxbase.comms.DetectedServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.InetAddress;
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
public class CoreMemoryDataTest
{
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String jsonifiedClass = "{\"lastPrinterSerial\":\"ABC\",\"lastPrinterFirmwareVersion\":5.0,\"activeRoboxRoots\":[{\"address\":\"localhost\",\"name\":\"test\",\"serverIP\":\"\",\"version\":\"ABC\",\"pin\":\"1111\"}]}"; 

    public CoreMemoryDataTest()
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
    public void serializesToJSON() throws Exception
    {
        final CoreMemoryData data = createTestData();
        String mappedValue = mapper.writeValueAsString(data);
        assertEquals(jsonifiedClass, mappedValue);
    }

    @Test
    public void deserializesFromJSON() throws Exception
    {
        final CoreMemoryData data = createTestData();

        try
        {
            CoreMemoryData dataDeSerialised = mapper.readValue(jsonifiedClass, CoreMemoryData.class);
            assertEquals(data.getLastPrinterFirmwareVersion(), dataDeSerialised.getLastPrinterFirmwareVersion(), 0.001f);
            assertEquals(data.getLastPrinterSerial(), dataDeSerialised.getLastPrinterSerial());
            assertEquals(1, dataDeSerialised.getActiveRoboxRoots().size());
            assertEquals(data.getActiveRoboxRoots().get(0).getAddress(), dataDeSerialised.getActiveRoboxRoots().get(0).getAddress());
            assertEquals(data.getActiveRoboxRoots().get(0).getName(), dataDeSerialised.getActiveRoboxRoots().get(0).getName());
            assertEquals(data.getActiveRoboxRoots().get(0).getVersion(), dataDeSerialised.getActiveRoboxRoots().get(0).getVersion());
        } catch (Exception e)
        {
            fail();
        }
    }

    private CoreMemoryData createTestData()
    {
        CoreMemoryData data = new CoreMemoryData();

        data.setLastPrinterFirmwareVersion(5f);
        data.setLastPrinterSerial("ABC");
        
        InetAddress inetAddress = InetAddress.getLoopbackAddress();
      
        DetectedServer newServer = new DetectedServer(inetAddress);
        newServer.setName("test");
        newServer.setVersion("ABC");
        data.getActiveRoboxRoots().add(newServer);
        
        return data;
    }
}