package celtech.roboxbase.configuration.slicer;

import static celtech.roboxbase.configuration.SlicerType.*;

import celtech.roboxbase.configuration.SlicerType;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author Ian
 */
public enum SupportPattern
{
    // Slic3r and Cura
    RECTILINEAR("Rectilinear", Arrays.asList(Cura, Slic3r)),
    RECTILINEAR_GRID("Rectilinear Grid", Arrays.asList(Cura, Slic3r)),
    
    // Slic3r only
    PILLARS("Pillars", Arrays.asList(Slic3r)),
    HONEYCOMB("Honeycomb", Arrays.asList(Slic3r)),
    
    // Cura 3 only
    LINES("Lines", Arrays.asList(Cura3)),
    GRID("Grid", Arrays.asList(Cura3)),
    TRIANGLES("Triangles", Arrays.asList(Cura3)),
    CONCENTRIC("Concentric", Arrays.asList(Cura3)),
    ZIGZAG("Zig Zag", Arrays.asList(Cura3)),
    CROSS("Cross", Arrays.asList(Cura3));

    private final String displayText;
    private final List<SlicerType> slicerTypes;

    private SupportPattern(String displayText, List<SlicerType> slicerTypes)
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
     * Returns the SupportPattern enums associated with the slicer.
     * 
     * @param slicerType
     * @return
     */
    public static SupportPattern[] valuesForSlicer(SlicerType slicerType) 
    {
        SupportPattern[] patterns = SupportPattern.values();
        
        SupportPattern[] patternsForSlicer = Arrays.stream(patterns)
                .filter(pattern -> pattern.getSlicerTypes().contains(slicerType))
                .toArray(SupportPattern[]::new);
        
        return patternsForSlicer;
    }
}
