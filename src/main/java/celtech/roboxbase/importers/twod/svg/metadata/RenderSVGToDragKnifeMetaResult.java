package celtech.roboxbase.importers.twod.svg.metadata;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaPart;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author ianhudson
 */
public class RenderSVGToDragKnifeMetaResult
{
    private List<DragKnifeMetaPart> dragknifemetaparts = new ArrayList<>();
    private final double resultantX;
    private final double resultantY;

    public RenderSVGToDragKnifeMetaResult(double resultantX, double resultantY, List<DragKnifeMetaPart> dragknifemetaparts)
    {
        this.dragknifemetaparts = dragknifemetaparts;
        this.resultantX = resultantX;
        this.resultantY = resultantY;
    }

    public RenderSVGToDragKnifeMetaResult(double resultantX, double resultantY, DragKnifeMetaPart dragknifemetapart)
    {
        this.dragknifemetaparts = new ArrayList();
        this.dragknifemetaparts.add(dragknifemetapart);
        this.resultantX = resultantX;
        this.resultantY = resultantY;
    }

    public List<DragKnifeMetaPart> getDragKnifeMetaParts()
    {
        return dragknifemetaparts;
    }

    public double getResultantX()
    {
        return resultantX;
    }

    public double getResultantY()
    {
        return resultantY;
    }
}
