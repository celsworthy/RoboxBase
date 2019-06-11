package celtech.roboxbase.utils;

import java.io.BufferedReader;
import java.io.IOException;
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

        String scriptOutput = null;

        try
        {
            Process wifiSetupProcess = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(wifiSetupProcess.getInputStream()));
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
}
