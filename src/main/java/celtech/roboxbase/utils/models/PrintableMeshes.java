package celtech.roboxbase.utils.models;

import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.PrinterSettingsOverrides;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.services.CameraTriggerData;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import java.util.List;
import java.util.Set;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class PrintableMeshes
{

    private final List<MeshForProcessing> meshesForProcessing;
    private final List<Boolean> usedExtruders;
    private final List<Integer> extruderForModel;
    private final String requiredPrintJobID;
    private final SlicerParametersFile settings;
    private final PrinterSettingsOverrides printOverrides;
    private final PrintQualityEnumeration printQuality;
    private final SlicerType defaultSlicerType;
    private final Vector3D centreOfPrintedObject;
    private final boolean safetyFeaturesRequired;
    private final boolean cameraEnabled;
    private final CameraTriggerData cameraTriggerData;

    public PrintableMeshes(List<MeshForProcessing> meshesForProcessing,
            List<Boolean> usedExtruders,
            List<Integer> extruderForModel,
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
        this.meshesForProcessing = meshesForProcessing;
        this.usedExtruders = usedExtruders;
        this.extruderForModel = extruderForModel;
        this.requiredPrintJobID = requiredPrintJobID;
        this.settings = settings;
        this.printOverrides = printOverrides;
        this.printQuality = printQuality;
        this.defaultSlicerType = defaultSlicerType;
        this.centreOfPrintedObject = centreOfPrintedObject;
        this.safetyFeaturesRequired = safetyFeaturesRequired;
        this.cameraEnabled = cameraEnabled;
        this.cameraTriggerData = cameraTriggerData;
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

    public String getRequiredPrintJobID()
    {
        return requiredPrintJobID;
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

    public boolean isSafetyFeaturesRequired()
    {
        return safetyFeaturesRequired;
    }

    public boolean isCameraEnabled()
    {
        return cameraEnabled;
    }

    public CameraTriggerData getCameraTriggerData()
    {
        return cameraTriggerData;
    }
}
