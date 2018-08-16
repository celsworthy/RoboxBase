package celtech.roboxbase.services.gcodepreview;

import celtech.roboxbase.configuration.SlicerType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
class GCodePreviewOutputGobbler extends Thread
{

    private final InputStream is;
    private final String type;
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());

    GCodePreviewOutputGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type;
    }

    @Override
    public void run()
    {
        try
        {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                steno.info(">" + line);
            }
        } catch (IOException ioe)
        {
            steno.error(ioe.getMessage());
        }
    }
}
