package celtech.roboxbase.importers.twod.svg.metadata.dragknife;

import celtech.roboxbase.importers.twod.svg.SVGConverterConfiguration;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author ianhudson
 */
public abstract class DragKnifeMetaPart
{

    private Vector2D start;
    private Vector2D end;
    private String comment;

    private final NumberFormat threeDPformatter;

    public DragKnifeMetaPart(double startX, double startY, double endX, double endY, String comment)
    {
        start = new Vector2D(startX, startY);
        end = new Vector2D(endX, endY);

        threeDPformatter = DecimalFormat.getNumberInstance(Locale.UK);
        threeDPformatter.setMaximumFractionDigits(3);
        threeDPformatter.setGroupingUsed(false);

        this.comment = comment;
    }

    public void setStart(Vector2D start)
    {
        this.start = start;
    }

    public Vector2D getStart()
    {
        return start;
    }

    public void setEnd(Vector2D end)
    {
        this.end = end;
    }

    public Vector2D getEnd()
    {
        return end;
    }

    public void setComment(String comment)
    {
        this.comment = comment;
    }

    public String getComment()
    {
        return comment;
    }

    public abstract String renderToGCode();

    protected String generateXYMove(double xValue, double yValue, int feedrate, String comment)
    {
        String generatedOutput = "G1 X" + threeDPformatter.format(xValue)
                + " Y" + threeDPformatter.format(yValue)
                + " F" + feedrate
                + " ; " + comment;

        return generatedOutput;
    }

    protected String generateXMove(double xValue, int feedrate, String comment)
    {
        String generatedOutput = "G1 X" + threeDPformatter.format(xValue)
                + " F" + feedrate
                + " ; " + comment;

        return generatedOutput;
    }

    protected String generateYMove(double yValue, int feedrate, String comment)
    {
        String generatedOutput = "G1 Y" + threeDPformatter.format(yValue)
                + " F" + SVGConverterConfiguration.getInstance().getTravelFeedrate()
                + " ; " + comment;

        return generatedOutput;
    }

    protected String generatePlunge(String comment)
    {
        String generatedOutput = "G0 Z" + SVGConverterConfiguration.getInstance().getPlungeDepth()
                + " ;Plunge"
                + " ; " + comment;

        return generatedOutput;
    }

    protected String generateLift(String comment)
    {
        String generatedOutput = "G0 Z10;Lift"
                + " ; " + comment;

        return generatedOutput;
    }
}
