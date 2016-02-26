package celtech.roboxbase.importers.twod.svg.metadata;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaCut;
import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaPart;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.twod.Vector2D;

/**
 *
 * @author ianhudson
 */
public class SVGMetaPolygon extends SVGMetaPart
{

    private List<Vector2D> points = new ArrayList<>();

    public SVGMetaPolygon()
    {
    }
    
    public void addPoint(double x, double y)
    {
        points.add(new Vector2D(x, y));
    }
    
    public List<Vector2D> getPoints()
    {
        return points;
    }

    @Override
    public RenderSVGToDragKnifeMetaResult renderToDragKnifeMetaParts(double currentX, double currentY)
    {
        double resultantX = currentX, resultantY = currentY;
        List<DragKnifeMetaPart> cuts = new ArrayList<>();

        for (Vector2D point : points)
        {
            DragKnifeMetaCut cut = new DragKnifeMetaCut(resultantX, resultantY, point.getX(), point.getY(), "Poly");
            cuts.add(cut);
            resultantX = point.getX();
            resultantY = point.getY();
        }
        
        RenderSVGToDragKnifeMetaResult result = new RenderSVGToDragKnifeMetaResult(resultantX, resultantY, cuts);
        return result;
    }
}
