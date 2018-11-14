package celtech.roboxbase.configuration.slicer;

import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;

/**
 *
 * @author Ian
 */
public interface ConfigWriter
{
    public void generateConfigForSlicer(SlicerParametersFile profileData, String destinationFile);
    
    public void setPrintCentre(double x, double y);
}
