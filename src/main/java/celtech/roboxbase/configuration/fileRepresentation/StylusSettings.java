package celtech.roboxbase.configuration.fileRepresentation;

/**
 *
 * @author Ian
 */
public class StylusSettings
{
    public static enum StylusType
    {
        CUTTER,
        PEN
    }
    
    private StylusType stylusType = null;
    private float penThickness = 0.2f;
    private float materialThickness = 0.1f;
    private float bladeOffset = 0.2f;
    private int cuttingPasses = 1;

    public StylusType getStylusType()
    {
        return stylusType;
    }

    public void setStylusType(StylusType stylusType)
    {
        this.stylusType = stylusType;
    }
    
    public float getPenThickness()
    {
        return penThickness;
    }

    public void setPenThickness(float penThickness)
    {
        this.penThickness = penThickness;
    }

    public float getMaterialThickness()
    {
        return materialThickness;
    }

    public void setMaterialThickness(float materialThickness)
    {
        this.materialThickness = materialThickness;
    }

    public float getBladeOffset()
    {
        return bladeOffset;
    }

    public void setBladeOffset(float bladeOffset)
    {
        this.bladeOffset = bladeOffset;
    }

    public int getCuttingPasses()
    {
        return cuttingPasses;
    }

    public void setCuttingPasses(int cuttingPasses)
    {
        this.cuttingPasses = cuttingPasses;
    }
}
