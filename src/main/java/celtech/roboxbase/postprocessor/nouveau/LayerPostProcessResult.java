package celtech.roboxbase.postprocessor.nouveau;

import celtech.roboxbase.postprocessor.nouveau.nodes.LayerNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.SectionNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.ToolSelectNode;
import java.util.Optional;

/**
 *
 * @author Ian
 */
public class LayerPostProcessResult
{
    private final float eVolume;
    private final float dVolume;
    private final double timeForLayer_secs;
    private final double timeForLayer_cumulative_secs;
    private final LayerNode layerData;
    private Optional<Integer> lastObjectNumber = Optional.empty();
    private int lastFeedrateInForce = -1;
    private final ToolSelectNode lastToolSelectInForce;
    private final ToolSelectNode lastToolSelectOfSameNumber;
    private final double timeUsingLastTool;
    private SectionNode lastSectionNodeInForce = null;

    public LayerPostProcessResult(
            LayerNode layerData,
            float eVolume,
            float dVolume,
            double timeForLayer_secs,
            double timeForLayer_cumulative_secs,
            int lastObjectNumber,
            SectionNode sectionNode,
            ToolSelectNode toolSelectNode,
            ToolSelectNode lastToolSelectOfSameNumber,
            double timeUsingLastTool,
            int lastFeedrateInForce)
    {
        this.layerData = layerData;
        this.eVolume = eVolume;
        this.dVolume = dVolume;
        this.timeForLayer_secs = timeForLayer_secs;
        this.timeForLayer_cumulative_secs = timeForLayer_cumulative_secs;
        this.lastObjectNumber = Optional.of(lastObjectNumber);
        this.lastSectionNodeInForce = sectionNode;
        this.lastToolSelectInForce = toolSelectNode;
        this.lastToolSelectOfSameNumber = lastToolSelectOfSameNumber;
        this.timeUsingLastTool = timeUsingLastTool;
        this.lastFeedrateInForce = lastFeedrateInForce;
    }

    public LayerNode getLayerData()
    {
        return layerData;
    }

    public float getEVolume()
    {
        return eVolume;
    }

    public float getDVolume()
    {
        return dVolume;
    }

    public double getTimeForLayer()
    {
        return timeForLayer_secs;
    }

    public double getTimeForLayer_cumulative_secs()
    {
        return timeForLayer_cumulative_secs;
    }

    public Optional<Integer> getLastObjectNumber()
    {
        return lastObjectNumber;
    }

    public void setLastObjectNumber(int lastObjectNumber)
    {
        this.lastObjectNumber = Optional.of(lastObjectNumber);
    }

    /**
     * This is the last feedrate that the parser saw
     *
     * @param feedrate
     */
    public void setLastFeedrateInForce(int feedrate)
    {
        this.lastFeedrateInForce = feedrate;
    }

    /**
     * This is the last feedrate that the parser saw
     *
     * @return
     */
    public int getLastFeedrateInForce()
    {
        return lastFeedrateInForce;
    }

    public ToolSelectNode getLastToolSelectInForce()
    {
        return lastToolSelectInForce;
    }

    public ToolSelectNode getLastToolSelectOfSameNumber()
    {
        return lastToolSelectOfSameNumber;
    }

    public double getTimeUsingLastTool()
    {
        return timeUsingLastTool;
    }

    public SectionNode getLastSectionNodeInForce()
    {
        return lastSectionNodeInForce;
    }
}
