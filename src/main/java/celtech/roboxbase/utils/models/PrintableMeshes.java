package celtech.roboxbase.utils.models;

import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.services.CameraTriggerData;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class PrintableMeshes extends PrintableEntity
{

    private final List<MeshForProcessing> meshesForProcessing;
    private final List<Boolean> usedExtruders;
    private final List<Integer> extruderForModel;
    private final SlicerParametersFile settings;
    private final PrinterSettingsOverrides printOverrides;
    private final PrintQualityEnumeration printQuality;
    private final SlicerType defaultSlicerType;
    private final Vector3D centreOfPrintedObject;

    public PrintableMeshes(List<MeshForProcessing> meshesForProcessing,
            List<Boolean> usedExtruders,
            List<Integer> extruderForModel,
            String projectName,
            String requiredPrintJobID,
            SlicerParametersFile settings,
            PrinterSettingsOverrides printOverrides,
            PrintQualityEnumeration printQuality,
            SlicerType defaultSlicerType,
            Vector3D centreOfPrintedObject,
            boolean safetyFeaturesRequired,
            boolean cameraEnabled,
            CameraTriggerData cameraTriggerData)
    {
        super(projectName, requiredPrintJobID, safetyFeaturesRequired, cameraEnabled, cameraTriggerData);
        this.meshesForProcessing = meshesForProcessing;
        this.usedExtruders = usedExtruders;
        this.extruderForModel = extruderForModel;
        this.settings = settings;
        this.printOverrides = printOverrides;
        this.printQuality = printQuality;
        this.defaultSlicerType = defaultSlicerType;
        this.centreOfPrintedObject = centreOfPrintedObject;
    }

    public List<MeshForProcessing> getMeshesForProcessing()
    {
        return meshesForProcessing;
    }

    public List<Boolean> getUsedExtruders()
    {
        return usedExtruders;
    }

    public List<Integer> getExtruderForModel()
    {
        return extruderForModel;
    }

    public SlicerParametersFile getSettings()
    {
        return settings;
    }

    public PrinterSettingsOverrides getPrintOverrides()
    {
        return printOverrides;
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality;
    }

    public SlicerType getDefaultSlicerType()
    {
        return defaultSlicerType;
    }

    public Vector3D getCentreOfPrintedObject()
    {
        return centreOfPrintedObject;
    }
}
