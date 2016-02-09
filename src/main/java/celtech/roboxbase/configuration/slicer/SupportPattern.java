package celtech.roboxbase.configuration.slicer;

/**
 *
 * @author Ian
 */
public enum SupportPattern
{

    RECTILINEAR("Rectilinear"),
    RECTILINEAR_GRID("Rectilinear Grid"),
    PILLARS("Pillars"),
    HONEYCOMB("Honeycomb");

    private String displayText;

    private SupportPattern(String displayText)
    {
        this.displayText = displayText;
    }

    @Override
    public String toString()
    {
        return displayText;
    }
}
