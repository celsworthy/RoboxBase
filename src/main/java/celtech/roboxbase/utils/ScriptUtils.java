package celtech.roboxbase.utils;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian & George Salter
 */
public class ScriptUtils 
{
    public static final Stenographer STENO = StenographerFactory.getStenographer(ScriptUtils.class.getName());
    
    public static String runScript(String pathToScript, String... parameters)
    {
        List<String> command = new ArrayList<>();
        command.add(pathToScript);

        command.addAll(Arrays.asList(parameters));

        ProcessBuilder builder = new ProcessBuilder(command);
        STENO.info("builder = " + builder.toString());

        String scriptOutput = null;

        try
        {
            Process scriptProcess = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(scriptProcess.getInputStream()));
            StringBuilder stringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null)
            {
                if (stringBuilder.length() > 0)
                {
                    stringBuilder.append(System.getProperty("line.separator"));
                }
                stringBuilder.append(line);
            }

            scriptOutput = stringBuilder.toString();
        } catch (IOException ex)
        {
            STENO.error("Error " + ex);
        }

        return scriptOutput;
    }

    public static byte[] runScriptB(String pathToScript, String... parameters)
    {
        List<String> command = new ArrayList<>();
        command.add(pathToScript);

        command.addAll(Arrays.asList(parameters));

        ProcessBuilder builder = new ProcessBuilder(command);

        ByteArrayOutputStream scriptOutput = new ByteArrayOutputStream();

       
        STENO.info("Reading script output");
        try {
            Process scriptProcess = builder.start();
            InputStream iStream = scriptProcess.getInputStream();
            byte[] data = new byte[1024];
            int bytesRead = iStream.read(data);
            while(bytesRead != -1) {
                STENO.info("Read " + bytesRead + " from input stream");
                scriptOutput.write(data, 0, bytesRead);
                bytesRead = iStream.read(data);
            }
            STENO.info("Read total of " + scriptOutput.size() + "bytes from input stream");
        } 
        catch (IOException ex)
        {
            STENO.error("Error " + ex);
        }

        return scriptOutput.toByteArray();
    }
}