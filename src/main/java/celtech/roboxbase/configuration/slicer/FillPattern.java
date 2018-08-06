package celtech.roboxbase.configuration.slicer;

import static celtech.roboxbase.configuration.SlicerType.*;

import celtech.roboxbase.configuration.SlicerType;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Ian
 */
public enum FillPattern
{
    LINE("Line", Arrays.asList(Cura, Slic3r)),
    
    RECTILINEAR("Rectilinear", Arrays.asList(Slic3r)),
    HONEYCOMB("Honeycomb", Arrays.asList(Slic3r)),
    
    LINES("Lines", Arrays.asList(Cura3)),
    CONCENTRIC("Concentric", Arrays.asList(Slic3r, Cura3)),
    GRID("Grid", Arrays.asList(Cura3)),
    TRIANGLES("Triangles", Arrays.asList(Cura3)),
    TRIHEXAGON("Tri-Hexagon", Arrays.asList(Cura3)),
    CUBIC("Cubic", Arrays.asList(Cura3)),
    CUBICSUBDIV("Cubic Subdivision", Arrays.asList(Cura3)),
    TETRAHEDRAL("Octet", Arrays.asList(Cura3)),
    QUARTER_CUBIC("Quarter Cubic", Arrays.asList(Cura3)),
    ZIGZAG("Zig Zag", Arrays.asList(Cura3)),
    CROSS("Cross", Arrays.asList(Cura3)),
    CROSS_3D("Cross 3D", Arrays.asList(Cura3));

    private final String displayText;
    private final List<SlicerType> slicerTypes;

    private FillPattern(String displayText, List<SlicerType> slicerTypes)
    {
        this.displayText = displayText;
        this.slicerTypes = slicerTypes;
    }

    @Override
    public String toString()
    {
        return displayText;
    }
    
    public List<SlicerType> getSlicerTypes() 
    {
        return slicerTypes;
    }
    
    /**
     * Returns the FillPattern enums associated with the slicer.
     * 
     * @param slicerType
     * @return
     */
    public static FillPattern[] valuesForSlicer(SlicerType slicerType) 
    {
        FillPattern[] patterns = FillPattern.values();
        
        FillPattern[] patternsForSlicer = Arrays.stream(patterns)
                .filter(pattern -> pattern.getSlicerTypes().contains(slicerType))
                .toArray(FillPattern[]::new);
        
        return patternsForSlicer;
    }
}
