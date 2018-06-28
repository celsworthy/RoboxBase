package celtech.roboxbase.services.slicer;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.MachineType;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.utils.models.PrintableMeshes;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.utils.TimeUtils;
import celtech.roboxbase.utils.exporters.AMFOutputConverter;
import celtech.roboxbase.utils.exporters.MeshExportResult;
import celtech.roboxbase.utils.exporters.MeshFileOutputConverter;
import celtech.roboxbase.utils.exporters.STLOutputConverter;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

/**
 *
 * @author ianhudson
 */
public class SlicerTask extends Task<SliceResult> implements ProgressReceiver
{

    private static final Stenographer STENO = StenographerFactory.getStenographer(SlicerTask.class.
            getName());
    private String printJobUUID = null;
    private final PrintableMeshes printableMeshes;
    private final String printJobDirectory;
    private Printer printerToUse = null;

    private static final TimeUtils timeUtils = new TimeUtils();
    private static final String slicerTimerName = "Slicer";

    public SlicerTask(String printJobUUID,
            PrintableMeshes printableMeshes,
            Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.printableMeshes = printableMeshes;
        this.printJobDirectory = BaseConfiguration.getPrintSpoolDirectory() + printJobUUID
                + File.separator;
        this.printerToUse = printerToUse;
        updateProgress(0.0, 100.0);
    }

    public SlicerTask(String printJobUUID,
            PrintableMeshes printableMeshes,
            String printJobDirectory,
            Printer printerToUse)
    {
        this.printJobUUID = printJobUUID;
        this.printableMeshes = printableMeshes;
        this.printJobDirectory = printJobDirectory;
        this.printerToUse = printerToUse;
        updateProgress(0.0, 100.0);
    }

    @Override
    protected SliceResult call() throws Exception
    {
        if (isCancelled())
        {
            return null;
        }

        STENO.debug("slice " + printableMeshes.getSettings().getProfileName());
        updateTitle("Slicer");
        updateMessage("Preparing model for conversion");
        updateProgress(0.0, 100.0);

        return doSlicing(printJobUUID,
                printableMeshes,
                printJobDirectory,
                printerToUse,
                this,
                STENO
        );
    }

    public static SliceResult doSlicing(String printJobUUID,
            PrintableMeshes printableMeshes,
            String printJobDirectory,
            Printer printerToUse,
            ProgressReceiver progressReceiver,
            Stenographer steno)
    {
        steno.debug("Starting slicing");
        String uuidString = new String(printJobUUID);
        timeUtils.timerStart(uuidString, slicerTimerName);

        SlicerType slicerType = printableMeshes.getDefaultSlicerType();
        if (printableMeshes.getSettings().getSlicerOverride() != null)
        {
            slicerType = printableMeshes.getSettings().getSlicerOverride();
        }

        MeshFileOutputConverter outputConverter = null;

        if (slicerType == SlicerType.Slic3r)
        {
            outputConverter = new AMFOutputConverter();
        } else
        {
            outputConverter = new STLOutputConverter();
        }

        MeshExportResult meshExportResult = null;

        // Output multiple files if we are using Cura
        if (printerToUse == null
                || printerToUse.headProperty().get() == null
                || printerToUse.headProperty().get().headTypeProperty().get() == Head.HeadType.SINGLE_MATERIAL_HEAD)
        {
            meshExportResult = outputConverter.outputFile(printableMeshes.getMeshesForProcessing(), printJobUUID, printJobDirectory,
                    true);
        } else
        {
            meshExportResult = outputConverter.outputFile(printableMeshes.getMeshesForProcessing(), printJobUUID, printJobDirectory,
                    false);
        }

        Vector3D centreOfPrintedObject = meshExportResult.getCentre();

        boolean succeeded = sliceFile(printJobUUID, printJobDirectory, slicerType, meshExportResult.getCreatedFiles(), centreOfPrintedObject, progressReceiver, steno);

        timeUtils.timerStop(uuidString, slicerTimerName);
        steno.debug("Slicer Timer Report");
        steno.debug("============");
        steno.debug(slicerTimerName + " " + timeUtils.timeTimeSoFar_ms(uuidString, slicerTimerName) / 1000.0 + " seconds");
        steno.debug("============");

        return new SliceResult(printJobUUID, printableMeshes, printerToUse, succeeded);
    }

    public static boolean sliceFile(String printJobUUID,
            String printJobDirectory,
            SlicerType slicerType,
            List<String> createdMeshFiles,
            Vector3D centreOfPrintedObject,
            ProgressReceiver progressReceiver,
            Stenographer steno)
    {
        boolean succeeded = false;

        String tempGcodeFilename = printJobUUID + BaseConfiguration.gcodeTempFileExtension;

        String configFile = printJobUUID + BaseConfiguration.printProfileFileExtension;
        String jsonSettingsFile = "D:/Dev/RoboxBase/src/main/java/celtech/resources/fdmprinter.def.json"; // PUT THIS IN INSTALLER
        
        MachineType machineType = BaseConfiguration.getMachineType();
        ArrayList<String> commands = new ArrayList<>();

        String windowsSlicerCommand = "";
        String macSlicerCommand = "";
        String linuxSlicerCommand = "";
        String configLoadCommand = "";
        //The next variable is only required for Slic3r
        String printCenterCommand = "";
        String combinedConfigSection = "";
        String verboseOutputCommand = "";
        String progressOutputCommand = "";
        String modelFileCommand = "";
        
        // Used for cura 3 to override th default settings
        String sOverrideOptions = "";

        switch (slicerType)
        {
            case Slic3r:
                windowsSlicerCommand = "\"" + BaseConfiguration.
                        getCommonApplicationDirectory() + "Slic3r\\slic3r.exe\"";
                macSlicerCommand = "Slic3r.app/Contents/MacOS/slic3r";
                linuxSlicerCommand = "Slic3r/bin/slic3r";
                configLoadCommand = "--load";
                combinedConfigSection = configLoadCommand + " \"" + configFile + "\"";
                printCenterCommand = "--print-center";
                break;
            case Cura:
                windowsSlicerCommand = "\"" + BaseConfiguration.
                        getCommonApplicationDirectory() + "Cura\\CuraEngine.exe\"";
                macSlicerCommand = "Cura/CuraEngine";
                linuxSlicerCommand = "Cura/CuraEngine";
                verboseOutputCommand = "-v";
                configLoadCommand = "-c";
                progressOutputCommand = "-p";
                combinedConfigSection = configLoadCommand + " \"" + configFile + "\"";
                break;
            case Cura3:
                windowsSlicerCommand = "\"" + BaseConfiguration.
                        getCommonApplicationDirectory() + "Cura\\Cura3\\CuraEngine.exe\" slice";
                macSlicerCommand = "Cura/CuraEngine";
                linuxSlicerCommand = "Cura/CuraEngine";
                verboseOutputCommand = "-v";
                configLoadCommand = "-j";
                progressOutputCommand = "-p";
                modelFileCommand = " -l";
                combinedConfigSection = configLoadCommand + " \"" + jsonSettingsFile + "\"";
                sOverrideOptions = generateSlicerOverrideOptions(printJobDirectory + configFile);
                break;
        }

        steno.debug("Selected slicer is " + slicerType + " : " + Thread.currentThread().getName());

        switch (machineType)
        {
            case WINDOWS_95:
                commands.add("command.com");
                commands.add("/S");
                commands.add("/C");
                String win95PrintCommand = "\"pushd \""
                        + printJobDirectory
                        + "\" && "
                        + windowsSlicerCommand
                        + " "
                        + verboseOutputCommand
                        + " "
                        + progressOutputCommand
                        + " "
                        + combinedConfigSection
                        + " -o "
                        + "\"" + tempGcodeFilename + "\"";
                for (String fileName : createdMeshFiles)
                {
                    win95PrintCommand += " \"";
                    win95PrintCommand += fileName;
                    win95PrintCommand += "\"";
                }
                win95PrintCommand += " && popd\"";
                commands.add(win95PrintCommand);
                break;
            case WINDOWS:
                commands.add("cmd.exe");
                commands.add("/S");
                commands.add("/C");
                String windowsPrintCommand = "\"pushd \""
                        + printJobDirectory
                        + "\" && "
                        + windowsSlicerCommand
                        + " "
                        + verboseOutputCommand
                        + " "
                        + progressOutputCommand
                        + " "
                        + combinedConfigSection
                        + sOverrideOptions
                        + " -o "
                        + "\"" + tempGcodeFilename + "\"";

                if (!printCenterCommand.equals(""))
                {
                    windowsPrintCommand += " " + printCenterCommand;
                    windowsPrintCommand += " "
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getX())
                            + ","
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getZ());
                }

//                windowsPrintCommand += " *.stl";
                for (String fileName : createdMeshFiles)
                {
                    windowsPrintCommand += modelFileCommand;
                    windowsPrintCommand += " \"";
                    windowsPrintCommand += fileName;
                    windowsPrintCommand += "\"";
                }
                windowsPrintCommand += " && popd\"";
                steno.debug(windowsPrintCommand);
                commands.add(windowsPrintCommand);
                break;
            case MAC:
                commands.add(BaseConfiguration.getCommonApplicationDirectory()
                        + macSlicerCommand);
                if (!verboseOutputCommand.equals(""))
                {
                    commands.add(verboseOutputCommand);
                }
                if (!progressOutputCommand.equals(""))
                {
                    commands.add(progressOutputCommand);
                }
                commands.add(configLoadCommand);
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                if (!printCenterCommand.equals(""))
                {
                    commands.add(printCenterCommand);
                    commands.add(String.format(Locale.UK, "%.3f", centreOfPrintedObject.getX())
                            + ","
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getZ()));
                }
                for (String fileName : createdMeshFiles)
                {
                    commands.add(fileName);
                }
                break;
            case LINUX_X86:
            case LINUX_X64:
                commands.add(BaseConfiguration.getCommonApplicationDirectory()
                        + linuxSlicerCommand);
                if (!verboseOutputCommand.equals(""))
                {
                    commands.add(verboseOutputCommand);
                }
                if (!progressOutputCommand.equals(""))
                {
                    commands.add(progressOutputCommand);
                }
                commands.add(configLoadCommand);
                commands.add(configFile);
                commands.add("-o");
                commands.add(tempGcodeFilename);
                if (!printCenterCommand.equals(""))
                {
                    commands.add(printCenterCommand);
                    commands.add(String.format(Locale.UK, "%.3f", centreOfPrintedObject.getX())
                            + ","
                            + String.format(Locale.UK, "%.3f", centreOfPrintedObject.getZ()));
                }
                for (String fileName : createdMeshFiles)
                {
                    commands.add(fileName);
                }
                break;
            default:
                steno.error("Couldn't determine how to run slicer");
        }

        if (commands.size() > 0)
        {
            steno.debug("Slicer command is " + String.join(" ", commands));
            ProcessBuilder slicerProcessBuilder = new ProcessBuilder(commands);
            if (machineType != MachineType.WINDOWS && machineType != MachineType.WINDOWS_95)
            {
                steno.debug("Set working directory (Non-Windows) to " + printJobDirectory);
                slicerProcessBuilder.directory(new File(printJobDirectory));
            }

            Process slicerProcess = null;
            try
            {
                slicerProcess = slicerProcessBuilder.start();
                // any error message?
                SlicerOutputGobbler errorGobbler = new SlicerOutputGobbler(progressReceiver, slicerProcess.
                        getErrorStream(), "ERROR",
                        slicerType);

                // any output?
                SlicerOutputGobbler outputGobbler = new SlicerOutputGobbler(progressReceiver, slicerProcess.
                        getInputStream(),
                        "OUTPUT", slicerType);

                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                int exitStatus = slicerProcess.waitFor();
                switch (exitStatus)
                {
                    case 0:
                        steno.debug("Slicer terminated successfully ");
                        succeeded = true;
                        break;
                    default:
                        steno.error("Failure when invoking slicer with command line: " + String.join(
                                " ", commands));
                        steno.error("Slicer terminated with unknown exit code " + exitStatus);
                        break;
                }
            } catch (IOException ex)
            {
                steno.error("Exception whilst running slicer: " + ex);
            } catch (InterruptedException ex)
            {
                steno.warning("Interrupted whilst waiting for slicer to complete");
                if (slicerProcess != null)
                {
                    slicerProcess.destroyForcibly();
                }
            }
        } else
        {
            steno.error("Couldn't run slicer - no commands for OS ");
        }

        return succeeded;
    }

    @Override
    public void progressUpdateFromSlicer(String message, float workDone)
    {
        updateMessage(message);
        updateProgress(workDone, 100.0);
    }
    
    /**
     * Turn the roboxprofile into a bunch of -s commands to override the 
     * curaEngine options.
     * 
     * @param configFile the roboxprofile file.
     * @return a String containing all the -s options.
     */
    private static String generateSlicerOverrideOptions(String configFile) {
        
        String overrideOptions = "";
        
        try {
            File configOptions = new File(configFile);
            BufferedReader fileReader = new BufferedReader(new FileReader(configOptions));
            
            String readLine = "";
            
            while((readLine = fileReader.readLine()) != null) {
                if(!readLine.startsWith("#")) {
                    overrideOptions += " -s " + readLine;
                }
            }
        } catch (FileNotFoundException ex) {
            STENO.error("CConfig file: " + configFile + " could not be found.");
            STENO.error(ex.getMessage());
        } catch (IOException ex) {
            STENO.error("Error while reading config file: " + configFile);
            STENO.error(ex.getMessage());
        }
        
        return overrideOptions;
    }
}
