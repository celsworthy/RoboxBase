package celtech.roboxbase.utils.models;

import celtech.roboxbase.services.CameraTriggerData;

/**
 *
 * @author Ian
 */
public abstract class PrintableEntity
{

    private final String projectName;
    private final String requiredPrintJobID;
    private final boolean safetyFeaturesRequired;
    private final boolean cameraEnabled;
    private final CameraTriggerData cameraTriggerData;

    public PrintableEntity(String projectName,
            String requiredPrintJobID,
            boolean safetyFeaturesRequired,
            boolean cameraEnabled,
            CameraTriggerData cameraTriggerData)
    {
        this.projectName = projectName;
        this.requiredPrintJobID = requiredPrintJobID;
        this.safetyFeaturesRequired = safetyFeaturesRequired;
        this.cameraEnabled = cameraEnabled;
        this.cameraTriggerData = cameraTriggerData;
    }

    public final String getProjectName()
    {
        return projectName;
    }

    public final String getRequiredPrintJobID()
    {
        return requiredPrintJobID;
    }

    public final boolean isSafetyFeaturesRequired()
    {
        return safetyFeaturesRequired;
    }

    public final boolean isCameraEnabled()
    {
        return cameraEnabled;
    }

    public final CameraTriggerData getCameraTriggerData()
    {
        return cameraTriggerData;
    }
}
