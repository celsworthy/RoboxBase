package celtech.roboxbase.importers.twod.svg.metadata.dragknife;

/**
 *
 * @author ianhudson
 */
public class DragKnifeMetaPlunge extends DragKnifeMetaPart
{
    public DragKnifeMetaPlunge()
    {
        super(0, 0, 0, 0, null);
    }

    public DragKnifeMetaPlunge(double startX, double startY, String comment)
    {
        super(startX, startY, 0, 0, comment);
    }

    @Override
    public String renderToGCode()
    {
        String gcodeLine = generatePlunge(getComment());
        return gcodeLine;
    }
}
