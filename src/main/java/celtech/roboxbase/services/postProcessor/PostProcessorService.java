package celtech.roboxbase.services.postProcessor;

import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.CameraTriggerData;
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
    private PrintableMeshes printableMeshes;
    private Printer printerToUse;

    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    public void setPrintableMeshes(PrintableMeshes printableMeshes)
    {
        this.printableMeshes = printableMeshes;
    }

    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    @Override
    protected Task<GCodePostProcessingResult> createTask()
    {
        return new PostProcessorTask(
                printJobUUID,
                printableMeshes,
                printerToUse);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
