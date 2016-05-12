package celtech.roboxbase.postprocessor.nouveau.helpers;

import celtech.roboxbase.postprocessor.nouveau.LayerPostProcessResult;
import celtech.roboxbase.postprocessor.nouveau.nodes.ExtrusionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ToolSelectNode;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ian
 */
public class TestDataGenerator
{
   public static List<LayerPostProcessResult> generateLayerResults(List<LayerDefinition> layerDefinitions)
    {
        List<LayerPostProcessResult> results = new ArrayList<>();
        double startingTimeForLayer = 0;

        for (LayerDefinition layerDefinition : layerDefinitions)
        {
            LayerNode layerNode = generateLayer(startingTimeForLayer, layerDefinition);
            LayerPostProcessResult result = new LayerPostProcessResult(
                    layerNode,
                    0,
                    0,
                    0,
                    0,
                    0,
                    null,
                    null,
                    null,
                    0,
                    0);
            results.add(result);

            startingTimeForLayer += layerNode.getFinishTimeFromStartOfPrint_secs().get();
        }

        return results;
    }

    private static LayerNode generateLayer(double startingTimeForLayer, LayerDefinition layerDefinition)
    {
        LayerNode layerNode = new LayerNode(layerDefinition.getLayerNumber());

        double currentLayerTime = startingTimeForLayer;

        for (ToolDefinition tool : layerDefinition.getTools())
        {
            ToolSelectNode tsNode = new ToolSelectNode();
            tsNode.setToolNumber(tool.getToolNumber());
            tsNode.setEstimatedDuration(tool.getDuration());
            tsNode.setFinishTimeFromStartOfPrint_secs(currentLayerTime + tool.getDuration());

            double durationCountdown = tool.getDuration();
            double decrementValue = 15.0;

            do
            {
                ExtrusionNode exNode = new ExtrusionNode();
                exNode.getExtrusion().setE(1);
                double durationToUse = (durationCountdown > 0) ? durationCountdown : durationCountdown + decrementValue;
                exNode.setFinishTimeFromStartOfPrint_secs(durationToUse + currentLayerTime);
                tsNode.addChildAtStart(exNode);
                durationCountdown -= decrementValue;
            } while (durationCountdown > 0);

            layerNode.addChildAtEnd(tsNode);

            currentLayerTime += tool.getDuration();
        }

        layerNode.setFinishTimeFromStartOfPrint_secs(currentLayerTime);

        return layerNode;
    }    
}
