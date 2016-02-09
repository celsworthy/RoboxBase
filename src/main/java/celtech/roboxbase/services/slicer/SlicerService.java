package celtech.roboxbase.services.slicer;

import celtech.roboxbase.printerControl.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import javafx.concurrent.Task;

/**
 *
 * @author ianhudson
 */
public class SlicerService extends AbstractSlicerService
{

    private String printJobUUID = null;
    private PrintableMeshes printableMeshes;
    private Printer printerToUse = null;

    /**
     *
     * @param printJobUUID
     */
    @Override
    public void setPrintJobUUID(String printJobUUID)
    {
        this.printJobUUID = printJobUUID;
    }

    @Override
    public void setPrintableMeshes(PrintableMeshes printableMeshes)
    {
        this.printableMeshes = printableMeshes;
    }
    
    /**
     *
     * @param printerToUse
     */
    @Override
    public void setPrinterToUse(Printer printerToUse)
    {
        this.printerToUse = printerToUse;
    }

    @Override
    protected Task<SliceResult> createTask()
    {
        return new SlicerTask(printJobUUID, printableMeshes, printerToUse);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean cancelRun()
    {
        return cancel();
    }
}
