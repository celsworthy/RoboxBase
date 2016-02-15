/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.services.slicer;

import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.ControllableService;
import javafx.concurrent.Service;

/**
 *
 * @author tony
 */
public abstract class AbstractSlicerService extends Service<SliceResult> implements
        ControllableService
{
    public abstract void setPrintJobUUID(String printJobUUID);

    public abstract void setPrintableMeshes(PrintableMeshes printableMeshes);

    public abstract void setPrinterToUse(Printer printerToUse);

}
