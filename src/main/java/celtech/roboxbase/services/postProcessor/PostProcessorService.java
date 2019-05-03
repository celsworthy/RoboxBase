package celtech.roboxbase.services.postProcessor;

import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
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
    private SlicerType slicerType;

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
    
    public void setSlicerType(SlicerType slicerType) {
        this.slicerType = slicerType;
    }

    @Override
    protected Task<GCodePostProcessingResult> createTask()
    {
        return new PostProcessorTask(
                printJobUUID,
                printableMeshes,
                printerToUse,
                slicerType);
    }

    @Override
    public boolean cancelRun()
    {
        return cancel();
    }

}
