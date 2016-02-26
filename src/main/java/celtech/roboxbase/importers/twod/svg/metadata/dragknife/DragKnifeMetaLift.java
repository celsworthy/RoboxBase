package celtech.roboxbase.importers.twod.svg.metadata.dragknife;

/**
 *
 * @author ianhudson
 */
public class DragKnifeMetaLift extends DragKnifeMetaPart
{
    public DragKnifeMetaLift()
    {
        super(0, 0, 0, 0, null);
    }

    public DragKnifeMetaLift(double startX, double startY, String comment)
    {
        super(startX, startY, 0, 0, comment);
    }

    @Override
    public String renderToGCode()
    {
        String gcodeLine = generateLift(getComment());
        return gcodeLine;
    }
}
