package celtech.roboxbase.importers.twod.svg.metadata;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaCut;

/**
 *
 * @author ianhudson
 */
public class SVGMetaLine extends SVGMetaPart
{

    private final double x, y;
    private final boolean isAbsolute;

    public SVGMetaLine(double x, double y, boolean isAbsolute)
    {
        this.x = x;
        this.y = y;
        this.isAbsolute = isAbsolute;
    }

    public double getX()
    {
        return x;
    }

    public double getY()
    {
        return y;
    }

    public boolean isIsAbsolute()
    {
        return isAbsolute;
    }

    @Override
    public RenderSVGToDragKnifeMetaResult renderToDragKnifeMetaParts(double currentX, double currentY)
    {
        double resultantX, resultantY;

        String comment = " Line " + ((isAbsolute == true) ? "ABS" : "REL");

        if (isAbsolute)
        {
            resultantX = x;
            resultantY = y;
        } else
        {
            resultantX = currentX + x;
            resultantY = currentY + y;
        }

        DragKnifeMetaCut cut = new DragKnifeMetaCut(currentX, currentY, resultantX, resultantY, comment);

        RenderSVGToDragKnifeMetaResult result = new RenderSVGToDragKnifeMetaResult(resultantX, resultantY, cut);
        return result;
    }
}
