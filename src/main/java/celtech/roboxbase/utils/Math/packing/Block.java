package celtech.roboxbase.utils.Math.packing;

/**
 *
 * @author Ian
 */
public class Block
{

    private PackableItem packableItem = null;
    private int halfPadding = 0;
    int w = 0;
    int h = 0;
    BinNode fit = null;

    /**
     *
     * @param packableItem
     * @param padding
     */
    public Block(PackableItem packableItem, int padding)
    {
        this.packableItem = packableItem;
        this.halfPadding = padding / 2;
        this.w = (int) packableItem.getTotalWidth() + padding;
        this.h = (int) packableItem.getTotalDepth() + padding;
    }

    /**
     *
     * @return
     */
    public int getW()
    {
        return w;
    }

    /**
     *
     * @param w
     */
    public void setW(int w)
    {
        this.w = w;
    }

    /**
     *
     * @return
     */
    public int getH()
    {
        return h;
    }

    /**
     *
     * @param h
     */
    public void setH(int h)
    {
        this.h = h;
    }

    /**
     *
     * @return
     */
    public BinNode getFit()
    {
        return fit;
    }

    /**
     *
     * @param fit
     */
    public void setFit(BinNode fit)
    {
        this.fit = fit;
    }

    /**
     *
     */
    public void relocate()
    {
        if (fit != null)
        {
            packableItem.translateFrontLeftTo((double) fit.getX() + halfPadding, (double) fit.getY() + halfPadding);
        }
    }

}
