package celtech.roboxbase.importers.twod.svg;

import celtech.roboxbase.importers.twod.svg.metadata.dragknife.DragKnifeMetaPart;
import celtech.roboxbase.printerControl.comms.commands.GCodeMacros;
import celtech.roboxbase.printerControl.comms.commands.MacroLoadException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class DragKnifeToGCodeEngine
{

    private final Stenographer steno = StenographerFactory.getStenographer(DragKnifeToGCodeEngine.class.getName());

    private final String outputFilename;
    private final List<DragKnifeMetaPart> metaparts;

    public DragKnifeToGCodeEngine(String outputURIString, List<DragKnifeMetaPart> metaparts)
    {
        this.outputFilename = outputURIString;
        this.metaparts = metaparts;
    }

    public void generateGCode()
    {
        PrintWriter out = null;
        try
        {
            out = new PrintWriter(new BufferedWriter(new FileWriter(outputFilename)));

            //Add a macro header
            try
            {
                List<String> startMacro = GCodeMacros.getMacroContents("stylus_cut_start", null, false, false, false);
                for (String macroLine : startMacro)
                {
                    out.println(macroLine);
                }
            } catch (MacroLoadException ex)
            {
                steno.exception("Unable to load stylus cut start macro.", ex);
            }

            String renderResult = null;

            for (DragKnifeMetaPart part : metaparts)
            {
                renderResult = part.renderToGCode();
                if (renderResult != null)
                {
                    out.println(renderResult);
                    renderResult = null;
                }
            }

            //Add a macro footer
            try
            {
                List<String> startMacro = GCodeMacros.getMacroContents("stylus_cut_finish", null, false, false, false);
                for (String macroLine : startMacro)
                {
                    out.println(macroLine);
                }
            } catch (MacroLoadException ex)
            {
                steno.exception("Unable to load stylus cut start macro.", ex);
            }
        } catch (IOException ex)
        {
            steno.error("Unable to output SVG GCode to " + outputFilename);
        } finally
        {
            if (out != null)
            {
                out.flush();
                out.close();
            }
        }
    }
}
