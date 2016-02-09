package celtech.roboxbase.configuration.fileRepresentation;

import celtech.roboxbase.printerControl.model.Head.HeadType;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ian
 */
public class HeadFile
{

    private int version = 1;
    private String name;
    private String typeCode;
    private HeadType type;

    private List<NozzleHeaterData> nozzleHeaters = new ArrayList<>();
    private List<NozzleData> nozzles = new ArrayList<>();

    public HeadType getType()
    {
        return type;
    }

    public void setType(HeadType type)
    {
        this.type = type;
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getTypeCode()
    {
        return typeCode;
    }

    public void setTypeCode(String typeCode)
    {
        this.typeCode = typeCode;
    }

    public List<NozzleHeaterData> getNozzleHeaters()
    {
        return nozzleHeaters;
    }

    public void setNozzleHeaters(List<NozzleHeaterData> nozzleHeaters)
    {
        this.nozzleHeaters = nozzleHeaters;
    }

    public List<NozzleData> getNozzles()
    {
        return nozzles;
    }

    public void setNozzles(List<NozzleData> nozzles)
    {
        this.nozzles = nozzles;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
