package celtech.roboxbase.services.postProcessor;

import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.CameraTriggerData;
import celtech.roboxbase.services.CameraTriggerManager;
import celtech.roboxbase.services.ControllableService;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorService extends Service<GCodePostProcessingResult> implements ControllableService
{

    private String printJobUUID;
    private String nameOfPrint;
    private PrintableMeshes printableMeshes;
    private Printer printerToUse;
    private boolean insertCameraControl;
    private CameraTriggerData cameraTriggerData;

    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    public void setNameOfPrint(String nameOfPrint)
    {
        this.nameOfPrint = nameOfPrint;
    }

    public void setPrintableMeshes(PrintableMeshes printableMeshes)
    {
        this.printableMeshes = printableMeshes;
    }

    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    public void setInsertCameraControl(boolean insertCameraControl)
    {
        this.insertCameraControl = insertCameraControl;
    }

    public void setCameraTriggerData(CameraTriggerData cameraTriggerData)
    {
        this.cameraTriggerData = cameraTriggerData;
    }

    @Override
    protected Task<GCodePostProcessingResult> createTask()
    {
        return new PostProcessorTask(nameOfPrint,
                printJobUUID,
                printableMeshes,
                printerToUse,
                insertCameraControl,
                cameraTriggerData);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
