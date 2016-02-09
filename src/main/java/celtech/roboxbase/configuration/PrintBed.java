package celtech.roboxbase.configuration;

import celtech.roboxbase.utils.Math.MathUtils;
import celtech.roboxbase.utils.RectangularBounds;
import javafx.geometry.BoundingBox;
import javafx.geometry.Point3D;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class PrintBed
{

    private final String bedOuterModelName = "bedOuter.j3o";
    private final String bedInnerModelName = "bedInner.j3o";
    private static PrintBed instance = null;
    //The origin of the print volume is the front left corner
    // X is right
    // Y is down
    // Z is into the screen
    public static final float maxPrintableXSize = 210;
    public static final float maxPrintableZSize = 150;
    private static final float maxPrintableYSize = -100;
    private static final Point3D printVolumeMaximums = new Point3D(maxPrintableXSize, 0,
                                                                   maxPrintableZSize);
    private static final Point3D printVolumeMinimums = new Point3D(0, maxPrintableYSize, 0);
    private static final Point3D centre = new Point3D(maxPrintableXSize / 2, maxPrintableYSize / 2,
                                                      maxPrintableZSize / 2);
    private static final Point3D centreZeroHeight = new Point3D(maxPrintableXSize / 2, 0,
                                                                maxPrintableZSize / 2);
    private BoundingBox printVolumeBoundingBox = null;
    private Stenographer steno = null;

    private PrintBed()
    {
        steno = StenographerFactory.getStenographer(this.getClass().getName());

        printVolumeBoundingBox = new BoundingBox(0, 0, 0, maxPrintableXSize, maxPrintableYSize,
                                                 maxPrintableZSize);
        steno.debug("Print volume bounds " + printVolumeBoundingBox);
    }

    /**
     *
     * @return
     */
    public static PrintBed getInstance()
    {
        if (instance == null)
        {
            instance = new PrintBed();
        }

        return instance;
    }

    /**
     *
     * @return
     */
    public static Point3D getPrintVolumeMinimums()
    {
        return printVolumeMinimums;
    }

    /**
     *
     * @return
     */
    public static Point3D getPrintVolumeMaximums()
    {
        return printVolumeMaximums;
    }

    /**
     *
     * @return
     */
    public String getBedOuterModelName()
    {
        return bedOuterModelName;
    }

    /**
     *
     * @return
     */
    public String getBedInnerModelName()
    {
        return bedInnerModelName;
    }

    /**
     *
     * @return
     */
    public static Point3D getPrintVolumeCentre()
    {
        return centre;
    }

    /**
     *
     * @return
     */
    public static Point3D getPrintVolumeCentreZeroHeight()
    {
        return centreZeroHeight;
    }

    /**
     *
     * @return
     */
    public BoundingBox getPrintVolumeBounds()
    {
        return printVolumeBoundingBox;
    }

    /**
     *
     * @param bounds
     * @return
     */
    public static boolean isBiggerThanPrintVolume(RectangularBounds bounds)
    {
        boolean biggerThanPrintArea = false;

        double xSize = bounds.getWidth();
        double ySize = bounds.getHeight();
        double zSize = bounds.getDepth();

        double epsilon = 0.001;

        if (MathUtils.compareDouble(xSize, maxPrintableXSize, epsilon) == MathUtils.MORE_THAN
            || MathUtils.compareDouble(ySize, -maxPrintableYSize, epsilon) == MathUtils.MORE_THAN
            || MathUtils.compareDouble(zSize, maxPrintableZSize, epsilon) == MathUtils.MORE_THAN)
        {
            biggerThanPrintArea = true;
        }

        return biggerThanPrintArea;
    }
}
