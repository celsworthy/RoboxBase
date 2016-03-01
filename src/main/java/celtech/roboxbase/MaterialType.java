package celtech.roboxbase;

/**
 *
 * @author Ian
 */
public enum MaterialType
{

    /**
     *
     */
    ABS("ABS", 1.04),
    /**
     *
     */
    PLA("PLA", 1.25),
    /**
     *
     */
    NYL("Nylon", 1.134),
    /**
     *
     */
    HIP("HIPS", 1.134),
    /**
     *
     */
    SPC("Special", 1.0),
    /**
     *
     */
    PET("CO-PET", 1.27),
    /**
     *
     */
    TPU("TPU", 1.2),
    /**
     *
     */
    PCP("PCP", 1.19);

    private String friendlyName;
    /**
     * Approximate material density in g / cm^3.
     */
    private double density;

    private MaterialType(String friendlyName, Double density)
    {
        this.friendlyName = friendlyName;
        this.density = density;
    }

    /**
     *
     * @return
     */
    public String getFriendlyName()
    {
        return friendlyName;
    }

    public double getDensity()
    {
        return density;
    }

    /**
     *
     * @return
     */
    @Override
    public String toString()
    {
        return friendlyName;
    }
}
