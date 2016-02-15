/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.services.printing;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.services.slicer.AbstractSlicerService;
import celtech.roboxbase.services.slicer.SliceResult;
import celtech.roboxbase.services.slicer.SlicerTask;
import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javafx.concurrent.Task;

/**
 * TestSlicerService copies pyramid.gcode into the job directory when Task.call
 * executes.
 *
 * @author tony
 */
public class TestSlicerService extends AbstractSlicerService
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
    public void setPrintableMeshes(PrintableMeshes printableMeshes)
    {
        this.printableMeshes = printableMeshes;
    }

    @Override
    protected Task<SliceResult> createTask()
    {
        return new TestSlicerTask(printJobUUID, printableMeshes,
                printerToUse);
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

    class TestSlicerTask extends SlicerTask
    {

        public TestSlicerTask(String printJobUUID,
                PrintableMeshes printableMeshes,
                Printer printerToUse)
        {
            super(printJobUUID, printableMeshes, printerToUse);
        }

        @Override
        /**
         * Copies pyramid.gcode into the job directory when Task.call executes.
         *
         * @return the standard SliceResult
         */
        protected SliceResult call() throws Exception
        {
            // copy presliced file to user storage project area
            String workingDirectory = BaseConfiguration.getPrintSpoolDirectory()
                    + printJobUUID + File.separator;
            Path destinationFilePath = Paths.get(workingDirectory + printJobUUID
                    + BaseConfiguration.gcodeTempFileExtension);
            URL pyramidGCodeURL = this.getClass().getResource("/pyramid.gcode");
            Path sourceFilePath = Paths.get(pyramidGCodeURL.toURI());
            Files.copy(sourceFilePath, destinationFilePath);
            return new SliceResult(printJobUUID, printableMeshes,
                    printerToUse, true);
        }
    }

}
