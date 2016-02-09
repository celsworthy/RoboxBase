package celtech.roboxbase.comms.remote.rx;

import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.rx.StatusResponse;
import celtech.roboxbase.comms.remote.EEPROMState;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class StatusResponseTest
{

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String jsonifiedClass = "{\"@class\":\"celtech.roboxbase.comms.rx.StatusResponse\",\"packetType\":\"STATUS_RESPONSE\",\"messagePayload\":null,\"sequenceNumber\":44,\"includeSequenceNumber\":false,\"includeCharsOfDataInOutput\":false,\"runningPrintJobID\":null,\"printJobLineNumber\":0,\"xSwitchStatus\":false,\"ySwitchStatus\":false,\"zSwitchStatus\":false,\"pauseStatus\":\"NOT_PAUSED\",\"busyStatus\":\"NOT_BUSY\",\"filament1SwitchStatus\":true,\"filament2SwitchStatus\":false,\"nozzleSwitchStatus\":false,\"doorOpen\":false,\"reelButtonPressed\":false,\"topZSwitchStatus\":false,\"extruderEPresent\":false,\"extruderDPresent\":false,\"nozzle0HeaterMode\":\"OFF\",\"nozzle0Temperature\":0,\"nozzle0TargetTemperature\":0,\"nozzle0FirstLayerTargetTemperature\":0,\"nozzle1HeaterMode\":\"OFF\",\"nozzle1Temperature\":0,\"nozzle1TargetTemperature\":144,\"nozzle1FirstLayerTargetTemperature\":0,\"bedHeaterMode\":\"OFF\",\"bedTemperature\":0,\"bedTargetTemperature\":0,\"bedFirstLayerTargetTemperature\":0,\"ambientFanOn\":false,\"ambientTemperature\":0,\"ambientTargetTemperature\":65,\"headFanOn\":false,\"headEEPROMState\":\"NOT_PRESENT\",\"reel0EEPROMState\":\"PROGRAMMED\",\"reel1EEPROMState\":\"NOT_PRESENT\",\"dualReelAdaptorPresent\":false,\"sdCardPresent\":false,\"headXPosition\":0.0,\"headYPosition\":0.0,\"headZPosition\":0.0,\"nozzleInUse\":0,\"feedRateEMultiplier\":0.0,\"feedRateDMultiplier\":0.0,\"whyAreWeWaitingState\":\"NOT_WAITING\",\"headPowerOn\":false,\"eindexStatus\":false,\"dindexStatus\":false,\"bposition\":0.0,\"efilamentDiameter\":0.0,\"efilamentMultiplier\":0.0,\"dfilamentDiameter\":0.0,\"dfilamentMultiplier\":0.0}";

    public StatusResponseTest()
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
        final StatusResponse packet = getTestPacket();

        String mappedValue = mapper.writeValueAsString(packet);
        assertEquals(jsonifiedClass, mappedValue);
    }

    @Test
    public void deserializesFromJSON() throws Exception
    {
        final StatusResponse packet = getTestPacket();

        try
        {
            RoboxRxPacket packetRec = mapper.readValue(jsonifiedClass, RoboxRxPacket.class);
            assertEquals(packet, packetRec);
        } catch (Exception e)
        {
            System.out.println(e.getCause().getMessage());
            fail();
        }
    }

    private StatusResponse getTestPacket()
    {
        StatusResponse packet = new StatusResponse();

        packet.setSequenceNumber(44);
        packet.setAmbientTargetTemperature(65);
        packet.setNozzle1TargetTemperature(144);
        packet.setFilament1SwitchStatus(true);
        packet.setReel0EEPROMState(EEPROMState.PROGRAMMED);

        return packet;
    }
}
