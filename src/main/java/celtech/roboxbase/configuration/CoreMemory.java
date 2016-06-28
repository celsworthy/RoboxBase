package celtech.roboxbase.configuration;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CoreMemory
{

    private final Stenographer steno = StenographerFactory.getStenographer(CoreMemory.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();
    private final String filename;
    public CoreMemoryData coreMemoryData = null;

    public CoreMemory()
    {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        filename = BaseConfiguration.getApplicationStorageDirectory() + ".CoreMemory.dat";
        File coreMemoryFile = new File(filename);

        if (!coreMemoryFile.exists())
        {
            writeNewCoreMemory(coreMemoryFile);
        } else
        {
            try
            {
                coreMemoryData = mapper.readValue(coreMemoryFile, CoreMemoryData.class);

            } catch (IOException ex)
            {
                steno.exception("Error loading core memory file " + coreMemoryFile.getAbsolutePath() + " - writing new memory", ex);
                writeNewCoreMemory(coreMemoryFile);
            }
        }
    }

    private void writeNewCoreMemory(File coreMemoryFile)
    {
        coreMemoryData = new CoreMemoryData();
        try
        {
            mapper.writeValue(coreMemoryFile, coreMemoryData);
        } catch (IOException ex)
        {
            steno.error("Error trying to write core memory file");
        }
    }

    public void save()
    {

    }
}
