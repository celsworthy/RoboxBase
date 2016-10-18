package celtech.roboxbase.configuration;

import celtech.roboxbase.comms.DetectedServer;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CoreMemory
{

    private final Stenographer steno = StenographerFactory.getStenographer(CoreMemory.class.getName());
    private static CoreMemory instance = null;
    private static final ObjectMapper mapper = new ObjectMapper();
    private final String filename;
    public CoreMemoryData coreMemoryData = null;
    private File coreMemoryFile = null;

    private CoreMemory()
    {
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        mapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);

        filename = BaseConfiguration.getApplicationStorageDirectory() + ".CoreMemory.dat";
        coreMemoryFile = new File(filename);

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
                steno.error("Error loading core memory file " + coreMemoryFile.getAbsolutePath() + " - writing new memory");
                writeNewCoreMemory(coreMemoryFile);
            }
        }
    }

    public static CoreMemory getInstance()
    {
        if (instance == null)
        {
            instance = new CoreMemory();
        }

        return instance;
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

    private void writeCoreMemory()
    {
        try
        {
            mapper.writeValue(coreMemoryFile, coreMemoryData);
        } catch (IOException ex)
        {
            steno.error("Error trying to write core memory file");
        }
    }

    public List<DetectedServer> getActiveRoboxRoots()
    {
        return coreMemoryData.getActiveRoboxRoots();
    }

    public void clearActiveRoboxRoots()
    {
        coreMemoryData.getActiveRoboxRoots().clear();
        writeCoreMemory();
    }

    public void activateRoboxRoot(DetectedServer server)
    {
        coreMemoryData.getActiveRoboxRoots().add(server);
        writeCoreMemory();
    }

    public void deactivateRoboxRoot(DetectedServer server)
    {
        coreMemoryData.getActiveRoboxRoots().remove(server);
        writeCoreMemory();
    }

    public float getLastPrinterFirmwareVersion()
    {
        return coreMemoryData.getLastPrinterFirmwareVersion();
    }

    public void setLastPrinterFirmwareVersion(float firmwareVersionInUse)
    {
        coreMemoryData.setLastPrinterFirmwareVersion(firmwareVersionInUse);
        writeCoreMemory();
    }

    public String getLastPrinterSerial()
    {
        return coreMemoryData.getLastPrinterSerial();
    }

    public void setLastPrinterSerial(String printerIDToUse)
    {
        coreMemoryData.setLastPrinterSerial(printerIDToUse);
        writeCoreMemory();
    }
}
