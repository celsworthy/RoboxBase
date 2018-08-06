package celtech.roboxbase.configuration.slicer;

import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 *
 * @author George Salter
 */
public class Cura3ConfigWriter extends SlicerConfigWriter {

    public Cura3ConfigWriter()
    {
        super();
        slicerType = SlicerType.Cura3;
    }
    
    @Override
    protected void outputLine(FileWriter writer, String variableName, boolean value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, int value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, float value) throws IOException
    {
        writer.append(variableName + "=" + threeDPformatter.format(value) + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, String value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, SlicerType value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, Enum value) throws IOException
    {
        writer.append(variableName + "=" + value.name().toLowerCase() + "\n");
    }

    @Override
    protected void outputPrintCentre(FileWriter writer, float centreX, float centreY) throws IOException
    {
    }

    @Override
    protected void outputFilamentDiameter(FileWriter writer, float diameter) throws IOException
    {
        outputLine(writer, "material_diameter", String.format(Locale.UK, "%f", diameter));
    }

    @Override
    void bringDataInBounds(SlicerParametersFile profileData) {
    }
    
}
