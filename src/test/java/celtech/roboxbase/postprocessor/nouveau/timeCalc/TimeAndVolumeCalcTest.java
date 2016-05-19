package celtech.roboxbase.postprocessor.nouveau.timeCalc;

import celtech.roboxbase.postprocessor.nouveau.LayerPostProcessResult;
import celtech.roboxbase.postprocessor.nouveau.helpers.LayerDefinition;
import celtech.roboxbase.postprocessor.nouveau.helpers.TestDataGenerator;
import celtech.roboxbase.postprocessor.nouveau.helpers.ToolDefinition;
import celtech.roboxbase.postprocessor.nouveau.nodes.MCodeNode;
import celtech.roboxbase.printerControl.model.Head;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Ian
 */
public class TimeAndVolumeCalcTest
{
    
    public TimeAndVolumeCalcTest()
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

    /**
     * Test of calculateVolumeAndTime method, of class TimeAndVolumeCalc.
     */
    @Test
    public void testCalculateVolumeAndTime()
    {
        System.out.println("testCalculateVolumeAndTime");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 50, 100),
            new ToolDefinition(1, 500, 5000)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults_NoDuration(layers);
        
        TimeAndVolumeCalc timeAndVolumeCalc = new TimeAndVolumeCalc(Head.HeadType.DUAL_MATERIAL_HEAD);
        TimeAndVolumeCalcResult result = timeAndVolumeCalc.calculateVolumeAndTime(allLayerPostProcessResults);

        assertEquals(1.0, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).getFinishTimeFromStartOfPrint_secs().get(), 0.0);
//        assertEquals(500, result.getExtruderDStats().getVolume(), 0.1);
    }
    
    /**
     * Test of calculateVolumeAndTime method, of class TimeAndVolumeCalc.
     */
    @Test
    public void testCalculateVolumeAndTime_checkNoNegative()
    {
        System.out.println("testCalculateVolumeAndTime");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 50, 100),
            new ToolDefinition(1, 500, 5000)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults_NoDuration(layers);

        //Add an MCodeNode at the start to see what happens
        allLayerPostProcessResults.get(0).getLayerData().addChildAtStart(new MCodeNode(999));

        TimeAndVolumeCalc timeAndVolumeCalc = new TimeAndVolumeCalc(Head.HeadType.DUAL_MATERIAL_HEAD);
        TimeAndVolumeCalcResult result = timeAndVolumeCalc.calculateVolumeAndTime(allLayerPostProcessResults);

        assertFalse(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(0).getFinishTimeFromStartOfPrint_secs().isPresent());
        assertEquals(1.0, allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(0).getFinishTimeFromStartOfPrint_secs().get(), 0.0);
}

    /**
     * Test of calculateVolumeAndTime method, of class TimeAndVolumeCalc.
     */
    @Test
    public void testCalculateVolumeAndTime_checkFirstMovementHasTime()
    {
        System.out.println("testCalculateVolumeAndTime");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 50, 100),
            new ToolDefinition(1, 500, 5000)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults_NoDuration(layers);

        //Add an MCodeNode at the start to see what happens
        allLayerPostProcessResults.get(0).getLayerData().addChildAtStart(new MCodeNode(999));

        TimeAndVolumeCalc timeAndVolumeCalc = new TimeAndVolumeCalc(Head.HeadType.DUAL_MATERIAL_HEAD);
        TimeAndVolumeCalcResult result = timeAndVolumeCalc.calculateVolumeAndTime(allLayerPostProcessResults);

        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getChildren().get(0).getFinishTimeFromStartOfPrint_secs().isPresent());
    }
    /**
     * Test of calculateVolumeAndTime method, of class TimeAndVolumeCalc.
     */
    @Test
    public void testCalculateVolumeAndTime_checkToolSelectTimes()
    {
        System.out.println("testCalculateVolumeAndTime");
        List<LayerDefinition> layers = new ArrayList<>();
        layers.add(new LayerDefinition(0, new ToolDefinition[]
        {
            new ToolDefinition(0, 50, 100),
            new ToolDefinition(1, 500, 5000)
        }));

        List<LayerPostProcessResult> allLayerPostProcessResults = TestDataGenerator.generateLayerResults_NoDuration(layers);

        //Add an MCodeNode at the start to see what happens
        allLayerPostProcessResults.get(0).getLayerData().addChildAtStart(new MCodeNode(999));

        TimeAndVolumeCalc timeAndVolumeCalc = new TimeAndVolumeCalc(Head.HeadType.DUAL_MATERIAL_HEAD);
        TimeAndVolumeCalcResult result = timeAndVolumeCalc.calculateVolumeAndTime(allLayerPostProcessResults);

        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getFinishTimeFromStartOfPrint_secs().isPresent());
//        assertTrue(allLayerPostProcessResults.get(0).getLayerData().getChildren().get(1).getFinishTimeFromStartOfPrint_secs().get());
    }

}
