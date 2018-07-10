package celtech.roboxbase.configuration.slicer;

import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 *
 * @author Ian
 */
public class CuraConfigWriter extends SlicerConfigWriter
{

    public CuraConfigWriter()
    {
        super();
        slicerType = SlicerType.Cura;
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, boolean value) throws IOException
    {
        int valueToWrite = (value) ? 1 : 0;
        writer.append(variableName + " = " + valueToWrite + "\n");
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
    protected void outputLine(FileWriter writer, String variableName, FillPattern value) throws IOException
    {
        writer.append(variableName + "=" + value + "\n");
    }

    @Override
    protected void outputLine(FileWriter writer, String variableName, SupportPattern value) throws IOException
    {
        int supportType = 0;

        switch (value)
        {
            case RECTILINEAR:
                supportType = 1;
                break;
            case RECTILINEAR_GRID:
                supportType = 0;
                break;
        }
        writer.append(variableName + "=" + supportType + "\n");
    }

    @Override
    protected void outputPrintCentre(FileWriter writer, float centreX, float centreY) throws IOException
    {
    }

    @Override
    protected void outputFilamentDiameter(FileWriter writer, float diameter) throws IOException
    {
        outputLine(writer, "filamentDiameter", String.format(Locale.UK, "%d",
                                                             (int) (diameter * 1000)));
    }

    @Override
    void bringDataInBounds(SlicerParametersFile profileData)
    {
    }

    @Override
    SlicerType getSlicerType() {
        return SlicerType.Cura;
    }
    
    
}
