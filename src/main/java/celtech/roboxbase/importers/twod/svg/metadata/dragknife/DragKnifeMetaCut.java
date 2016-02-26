package celtech.roboxbase.importers.twod.svg.metadata.dragknife;

import celtech.roboxbase.importers.twod.svg.SVGConverterConfiguration;

/**
 *
 * @author ianhudson
 */
public class DragKnifeMetaCut extends DragKnifeMetaPart
{
    public DragKnifeMetaCut()
    {
        super(0, 0, 0, 0, null);
    }

    public DragKnifeMetaCut(double startX, double startY, double endX, double endY, String comment)
    {
        super(startX, startY, endX, endY, comment);
    }

    @Override
    public String renderToGCode()
    {
        String gcodeLine = generateXYMove(getEnd().getX(), getEnd().getY(), SVGConverterConfiguration.getInstance().getCuttingFeedrate(), "Cut " + getComment());
        return gcodeLine;
    }
}
