package celtech.roboxbase.services.gcodepreview;

import celtech.roboxbase.configuration.SlicerType;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Scanner;
import javafx.beans.property.IntegerProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
class GCodePreviewOutputGobbler extends Thread
{
    private static final int MAX_LAYER_COUNT = 10000;
    private final InputStream is;
    private final String type;
    private final Stenographer steno = StenographerFactory.getStenographer(this.getClass().getName());
    private IntegerProperty layerCountProperty = null;
    private int layerCount = 0;

    GCodePreviewOutputGobbler(InputStream is, String type)
    {
        this.is = is;
        this.type = type.trim();
    }

    void setLayerCountProperty(IntegerProperty layerCountProperty)
    {
        this.layerCountProperty = layerCountProperty;
        this.layerCountProperty.set(layerCount);
    }

    @Override
    public void run()
    {
        try
        {
            boolean isOutputStream = this.type.equalsIgnoreCase("output");
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null)
            {
                if (isOutputStream)
                {
                    steno.info("> " + line);
                    Scanner lineScanner = new Scanner(line);
                    if (lineScanner.hasNext()) {
                        String commandWord = lineScanner.next().toLowerCase();
                        switch (commandWord)
                        {
                            case "layercount":
                                if (lineScanner.hasNextInt()) {
                                    int value = lineScanner.nextInt();
                                    if (value > 0 && value < MAX_LAYER_COUNT) {
                                        layerCount = value;
                                        if (layerCountProperty != null)
                                            layerCountProperty.set(layerCount);                                
                                    }
                                }
                                break;
                            default:
                                break;
                        }
                    }
                }
                else
                {
                    steno.warning("!!> " + line);
                }
            }
        } catch (IOException ioe)
        {
            steno.error(ioe.getMessage());
        }
    }
}
