package celtech.roboxbase.utils.Math.packing;

/**
 *
 * @author ianhudson
 */
public interface PackableItem
{
    public double getTotalWidth();
    public double getTotalDepth();
    public void translateFrontLeftTo(double x, double z);
}
