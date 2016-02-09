package celtech.roboxbase.configuration;

/**
 *
 * @author Ian
 */
public enum SlicerType
{

    Slic3r(0), Cura(1);

    private final int enumPosition;

    private SlicerType(int enumPosition)
    {
        this.enumPosition = enumPosition;
    }

    /**
     *
     * @return
     */
    public int getEnumPosition()
    {
        return enumPosition;
    }

    /**
     *
     * @param enumPosition
     * @return
     */
    public static SlicerType fromEnumPosition(int enumPosition)
    {
        SlicerType returnVal = null;

        for (SlicerType value : values())
        {
            if (value.getEnumPosition() == enumPosition)
            {
                returnVal = value;
                break;
            }
        }

        return returnVal;
    }

    @Override
    public String toString()
    {
        return name();
    }
}
