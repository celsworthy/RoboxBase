/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.roboxbase.utils.Math.packing;

/**
 *
 * @author Ian
 */
public class BinNode
{
    boolean used = false;
    int x = 0;
    int y = 0;
    int w = 0;
    int h = 0;
    BinNode right;
    BinNode down;

    BinNode(int x, int y, int w, int h)
    {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    /**
     *
     * @return
     */
    public boolean isUsed()
    {
        return used;
    }

    /**
     *
     * @param used
     */
    public void setUsed(boolean used)
    {
        this.used = used;
    }

    /**
     *
     * @return
     */
    public int getX()
    {
        return x;
    }

    /**
     *
     * @param x
     */
    public void setX(int x)
    {
        this.x = x;
    }

    /**
     *
     * @return
     */
    public int getY()
    {
        return y;
    }

    /**
     *
     * @param y
     */
    public void setY(int y)
    {
        this.y = y;
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
    public BinNode getRight()
    {
        return right;
    }

    /**
     *
     * @param right
     */
    public void setRight(BinNode right)
    {
        this.right = right;
    }

    /**
     *
     * @return
     */
    public BinNode getDown()
    {
        return down;
    }

    /**
     *
     * @param down
     */
    public void setDown(BinNode down)
    {
        this.down = down;
    }
    
    
}
