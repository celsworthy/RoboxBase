/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.roboxbase.services.gcodepreview;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony
 */
public class GCodePreviewTask extends Task<Boolean> {

    private static final Stenographer steno = StenographerFactory.getStenographer(GCodePreviewTask.class.getName());
    private OutputStream stdInStream;
    
    public GCodePreviewTask()
    {
        this.stdInStream = null;
    }

    public void writeCommand(String command)
    {
        if (this.stdInStream != null)
        {
            try {
                steno.info("Writing command \"" + command + "\"");
                stdInStream.write(command.getBytes());
                stdInStream.write('\n');
                stdInStream.flush();
            } catch (IOException ex) {
                steno.warning("Failed to write command \"" + command + "\": " + ex.getMessage());
            }
        }
    }

    public void loadGCodeFile(String fileName)
    {
        if (this.stdInStream != null)
        {
            StringBuilder command = new StringBuilder();
            command.append("load ");
            command.append(fileName);
            command.trimToSize();
            
            writeCommand(command.toString());
        }
    }

    public void setTopLayer(int topLayer)
    {
        if (this.stdInStream != null)
        {
            StringBuilder command = new StringBuilder();
            command.append("top ");
            command.append(topLayer);
            command.trimToSize();
            
            writeCommand(command.toString());
        }
    }
    
    public void terminatePreview()
    {
        if (this.stdInStream != null)
        {
            String command = "q";
            writeCommand(command.toString());
        }
    }

    @Override
    protected Boolean call() throws Exception {
        Boolean succeeded = false;
        ArrayList<String> commands = new ArrayList<>();
        commands.add("java");
        commands.add("-jar");
        commands.add("D:\\CEL\\Dev\\GCodeViewer\\target\\GCodeViewer.jar");
        commands.add("-DlibertySystems.configFile=\"D:\\CEL\\Dev\\GCodeViewer\\GCodeViewer.configFile.xml\"");
        if (commands.size() > 0)
        {
            steno.debug("GCodePreviewTask command is " + String.join(" ", commands));
            ProcessBuilder previewProcessBuilder = new ProcessBuilder(commands);

            Process previewProcess = null;
            try
            {
                previewProcess = previewProcessBuilder.start();

                GCodePreviewOutputGobbler errorGobbler = new GCodePreviewOutputGobbler(previewProcess.getErrorStream(), "ERROR");
                GCodePreviewOutputGobbler outputGobbler = new GCodePreviewOutputGobbler(previewProcess.getInputStream(), "OUTPUT");
                this.stdInStream =  previewProcess.getOutputStream();
                
                // kick them off
                errorGobbler.start();
                outputGobbler.start();

                int exitStatus = previewProcess.waitFor();
                switch (exitStatus)
                {
                    case 0:
                        steno.debug("GCode previewer terminated successfully ");
                        succeeded = true;
                        break;
                    default:
                        steno.error("Failure when invoking gcode previewer with command line: " + String.join(
                                " ", commands));
                        steno.error("GCode Previewer terminated with unknown exit code " + exitStatus);
                        break;
                }
            } catch (IOException ex)
            {
                steno.error("Exception whilst running gcode previewer: " + ex);
            } catch (InterruptedException ex)
            {
                steno.warning("Interrupted whilst waiting for GCode Previewer to complete");
                if (previewProcess != null)
                {
                    previewProcess.destroyForcibly();
                }
            }
        } else
        {
            steno.error("Couldn't run GCode Previewer - no commands for OS ");
        }
        
        return succeeded;
    }
}
