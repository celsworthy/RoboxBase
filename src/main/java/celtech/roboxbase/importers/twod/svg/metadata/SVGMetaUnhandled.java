package celtech.roboxbase.importers.twod.svg.metadata;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaUnhandled;

/**
 *
 * @author ianhudson
 */
public class SVGMetaUnhandled extends SVGMetaPart
{
    private final String message;

    public SVGMetaUnhandled(String message)
    {
        this.message = message;
    }

    @Override
    public RenderSVGToDragKnifeMetaResult renderToDragKnifeMetaParts(double currentX, double currentY)
    {
        DragKnifeMetaUnhandled unhandled = new DragKnifeMetaUnhandled(currentX, currentY, currentX, currentY, message);
        RenderSVGToDragKnifeMetaResult result = new RenderSVGToDragKnifeMetaResult(currentX, currentY, unhandled);
        
        return result;
    }
    
}
