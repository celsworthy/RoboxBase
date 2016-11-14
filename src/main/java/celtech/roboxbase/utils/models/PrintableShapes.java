
package celtech.roboxbase.utils.models;

import celtech.roboxbase.configuration.fileRepresentation.StylusSettings;
import java.util.List;

/**
 *
 * @author Ian
 */
public class PrintableShapes extends PrintableEntity
{

    private final List<ShapeForProcessing> shapesForProcessing;
    private final StylusSettings stylusSettings;

    public PrintableShapes(List<ShapeForProcessing> shapesForProcessing,
            String projectName,
            String requiredPrintJobID,
            boolean safetyFeaturesRequired,
            StylusSettings stylusSettings)
    {
        super(projectName, requiredPrintJobID, safetyFeaturesRequired, false, null);
        this.shapesForProcessing = shapesForProcessing;
        this.stylusSettings = stylusSettings;
    }

    public List<ShapeForProcessing> getShapesForProcessing()
    {
        return shapesForProcessing;
    }

    public StylusSettings getStylusSettings()
    {
        return stylusSettings;
    }
}
