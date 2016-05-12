package celtech.roboxbase.postprocessor.nouveau.helpers;

/**
 *
 * @author Ian
 */
public class ToolDefinition
{
    private final int toolNumber;
    private final double duration;

    public ToolDefinition(int toolNumber, double duration)
    {
        this.toolNumber = toolNumber;
        this.duration = duration;
    }

    public int getToolNumber()
    {
        return toolNumber;
    }

    public double getDuration()
    {
        return duration;
    }
}
