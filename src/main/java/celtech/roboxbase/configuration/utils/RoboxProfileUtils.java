package celtech.roboxbase.configuration.utils;

import celtech.roboxbase.configuration.RoboxProfile;
import celtech.roboxbase.configuration.SlicerType;

/**
 *
 * @author George Salter
 */
public class RoboxProfileUtils 
{
    /**
     * Calculate the raft offset.
     * 
     * @param profileSettings needed to find the total size of the raft
     * @param slicerType the slicer in use
     * @return the raftOffset as a double
     */
    public static double calculateRaftOffset(RoboxProfile profileSettings, SlicerType slicerType)
    {
        double raftOffset = profileSettings.getSpecificFloatSetting("raftBaseThickness_mm")
                        //Raft interface thickness
                        + 0.28
                        //Raft surface layer thickness * surface layers
                        + (profileSettings.getSpecificIntSetting("raftInterfaceLayers")* 0.27)
                        + profileSettings.getSpecificFloatSetting("raftAirGapLayer0_mm");

        return raftOffset;
    }
}
