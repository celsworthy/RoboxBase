package celtech.roboxbase.importers.twod.svg.metadata.dragknife;

/**
 *
 * @author ianhudson
 */
public class DragKnifeMetaUnhandled extends DragKnifeMetaPart
{
    private final String comment;

    public DragKnifeMetaUnhandled()
    {
        super(0, 0, 0, 0, null);
        this.comment = null;
    }
    
    public DragKnifeMetaUnhandled(double startX, double startY, double endX, double endY, String comment)
    {
        super(startX, startY, endX, endY, comment);
        this.comment = comment;
    }
    
    public String getComment()
    {
        return comment;
    }
    
    @Override
    public String renderToGCode()
    {
        return "; Unhandled - " + comment;
    }  
}
