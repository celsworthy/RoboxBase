package celtech.roboxbase.postprocessor;

import celtech.roboxbase.postprocessor.events.GCodeParseEvent;
import java.io.IOException;
import java.util.regex.Pattern;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class GCodeRoboxiser2 extends GCodeRoboxisingEngine
{

    private final Stenographer steno = StenographerFactory.getStenographer(
            GCodeRoboxiser2.class.getName());

    private final Pattern passThroughPattern = Pattern
            .compile("\\b(?:M106 S[0-9.]+|M107|G[0-9]{1,}.*|M[0-9]{2,})(?:[\\s]*;.*)?");

    private final Pattern removePattern = Pattern
            .compile(
                    "\\b(?:M104 S[0-9.]+(?:\\sT[0-9]+)?|M109 S[0-9.]+(?:\\sT[0-9]+)?|M107)(?:[\\s]*;.*)?");

    private final PostProcessingBuffer postProcessingBuffer = new PostProcessingBuffer();

    private int layerCounter = 1;
    
    @Override
    public void processEvent(GCodeParseEvent event) throws PostProcessingError
    {
        if (parseLayer(event))
        {
            steno.info("Layer complete");
            layer++;
        }        
        
//        
//        
//        if (event instanceof RetractEvent)
//        {
//            mungeTheBuffer();
//        } else if (event instanceof EndOfFileEvent)
//        {
//            try
//            {
//                postProcessingBuffer.closeNozzle("End of file", outputWriter);
//                postProcessingBuffer.emptyBufferToOutput(outputWriter);
//
//                try
//                {
//                    outputWriter.writeOutput(";\n; Post print gcode\n");
//                    for (String macroLine : GCodeMacros.getMacroContents("after_print"))
//                    {
//                        outputWriter.writeOutput(macroLine + "\n");
//                    }
//                    outputWriter.writeOutput("; End of Post print gcode\n");
//                } catch (IOException ex)
//                {
//                    throw new PostProcessingError("IO Error whilst writing post-print gcode to file: "
//                            + ex.getMessage());
//                } catch (MacroLoadException ex)
//                {
//                    throw new PostProcessingError(
//                            "Error whilst writing post-print gcode to file - couldn't add after print footer due to circular macro reference");
//                }
//
//                outputWriter.writeOutput(event.renderForOutput());
//                outputWriter.flush();
//            } catch (IOException ex)
//            {
//                steno.error("Error writing event to file");
//                ex.printStackTrace();
//            }
//        } else
//        {
//            postProcessingBuffer.add(event);
//        }
    }

    @Override
    public void unableToParse(String line)
    {
        try
        {
            if ((removePattern.matcher(line)).matches())
            {
                steno.info("Removed " + line);
                outputWriter.writeOutput("; Removed: " + line);
            } else if ((passThroughPattern.matcher(line)).matches())
            {
                outputWriter.writeOutput(line);
                outputWriter.newLine();
            } else
            {
                steno.warning("Unable to parse " + line);
                outputWriter.writeOutput("; >>>ERROR PARSING: " + line);
                outputWriter.newLine();
            }
        } catch (IOException ex)
        {
            steno.error("Parse error - " + line);
        }
    }

    private void mungeTheBuffer()
    {
        try
        {
            postProcessingBuffer.openNozzleFullyBeforeExtrusion();
//            postProcessingBuffer.closeNozzleFullyAfterExtrusion();
            postProcessingBuffer.emptyBufferToOutput(outputWriter);
        } catch (IOException ex)
        {
            steno.error("IO Exception whilst writing buffer contents");
        }
    }

    private boolean parseLayer(GCodeParseEvent event)
    {
        return false;
    }
}
