package celtech.roboxbase.postprocessor.nouveau.nodes;

import celtech.roboxbase.postprocessor.nouveau.nodes.providers.Renderable;

/**
 *
 * @author Ian
 */
public class ToolSelectNode extends GCodeEventNode implements Renderable
{

    private int toolNumber = -1;
    private boolean outputSuppressed = false;
    private double estimatedDuration = 0;

    public int getToolNumber()
    {
        return toolNumber;
    }

    public void setToolNumber(int toolNumber)
    {
        this.toolNumber = toolNumber;
    }

    public void suppressNodeOutput(boolean suppress)
    {
        outputSuppressed = suppress;
    }
    
    public boolean isNodeOutputSuppressed()
    {
        return outputSuppressed;
    }

    public void setEstimatedDuration(double estimatedDuration)
    {
        this.estimatedDuration = estimatedDuration;
    }

    public double getEstimatedDuration()
    {
        return estimatedDuration;
    }

    @Override
    public String renderForOutput()
    {
        String stringToReturn = "";

        if (!outputSuppressed)
        {
            stringToReturn += "T" + getToolNumber();
            stringToReturn += getCommentText();
            stringToReturn += " ; Tool Node duration: " + getEstimatedDuration();
        }

        return stringToReturn;
    }
}
