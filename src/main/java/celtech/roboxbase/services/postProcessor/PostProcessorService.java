package celtech.roboxbase.services.postProcessor;

import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.ControllableService;
import celtech.roboxbase.utils.models.PrintableEntity;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Ian
 */
public class PostProcessorService extends Service<GCodePostProcessingResult> implements ControllableService
{

    private String printJobUUID;
    private PrintableEntity printableEntity;
    private Printer printerToUse;

    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    public void setPrintableEntity(PrintableEntity printableEntity)
    {
        this.printableEntity = printableEntity;
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
                printableEntity,
                printerToUse);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
