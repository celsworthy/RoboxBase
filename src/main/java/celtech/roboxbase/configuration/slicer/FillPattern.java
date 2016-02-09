package celtech.roboxbase.configuration.slicer;

/**
 *
 * @author Ian
 */
public enum FillPattern
{

    LINE("Line"),
    RECTILINEAR("Rectilinear"),
    HONEYCOMB("Honeycomb"),
    CONCENTRIC("Concentric");

    private String displayText;

    private FillPattern(String displayText)
    {
        this.displayText = displayText;
    }

    @Override
    public String toString()
    {
        return displayText;
    }
}
