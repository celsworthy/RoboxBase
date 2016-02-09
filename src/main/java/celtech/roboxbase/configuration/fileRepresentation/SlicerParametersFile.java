package celtech.roboxbase.configuration.fileRepresentation;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.SlicerType;
import celtech.roboxbase.configuration.slicer.FillPattern;
import celtech.roboxbase.configuration.slicer.NozzleParameters;
import celtech.roboxbase.configuration.slicer.SupportPattern;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Ian
 */
public class SlicerParametersFile
{
    public enum SupportType
    {
        /**
         * Use material 1 as support. Implies all objects printed with material 2.
         */
        MATERIAL_1("supportType.material1"),
        /**
         * Use material 2 as support. Implies all objects printed with material 1.
         */
        MATERIAL_2("supportType.material2");

        String description;

        SupportType(String description)
        {
            this.description = BaseLookup.i18n(description);
        }

        @Override
        public String toString()
        {
            return description;
        }
    }

    private int version = 6;
    private String profileName;
    private String headType;
    private SlicerType slicerOverride;

    /*
     * Extrusion data
     */
    private float firstLayerHeight_mm;
    private float layerHeight_mm;
    private float fillDensity_normalised;
    private FillPattern fillPattern;
    private int fillEveryNLayers;
    private int solidLayersAtTop;
    private int solidLayersAtBottom;
    private int numberOfPerimeters;
    private int brimWidth_mm;
    private boolean spiralPrint;

    /*
     * Nozzle data
     */
    private float firstLayerExtrusionWidth_mm;
    private float perimeterExtrusionWidth_mm;
    private float fillExtrusionWidth_mm;
    private float solidFillExtrusionWidth_mm;
    private float topSolidFillExtrusionWidth_mm;
    private float supportExtrusionWidth_mm;
    private ArrayList<NozzleParameters> nozzleParameters;
    private int firstLayerNozzle;
    private int perimeterNozzle;
    private int fillNozzle;
    private int supportNozzle;
    private int supportInterfaceNozzle;
    private int maxClosesBeforeNozzleReselect;
    private float zHopHeight;
    private float zHopDistance;
            
    /*
     * Support
     */
    private boolean generateSupportMaterial;
    private int supportOverhangThreshold_degrees;
    private int forcedSupportForFirstNLayers = 0;
    private SupportPattern supportPattern;
    private float supportPatternSpacing_mm;
    private int supportPatternAngle_degrees;

    /*
     * Speed settings
     */
    private int firstLayerSpeed_mm_per_s;
    private int perimeterSpeed_mm_per_s;
    private int smallPerimeterSpeed_mm_per_s;
    private int externalPerimeterSpeed_mm_per_s;
    private int fillSpeed_mm_per_s;
    private int solidFillSpeed_mm_per_s;
    private int topSolidFillSpeed_mm_per_s;
    private int supportSpeed_mm_per_s;
    private int bridgeSpeed_mm_per_s;
    private int gapFillSpeed_mm_per_s;
    private int interfaceSpeed_mm_per_s;

    /*
     * Cooling
     */
    private boolean enableCooling;
    private int minFanSpeed_percent;
    private int maxFanSpeed_percent;
    private int bridgeFanSpeed_percent;
    private int disableFanFirstNLayers;
    private int coolIfLayerTimeLessThan_secs;
    private int slowDownIfLayerTimeLessThan_secs;
    private int minPrintSpeed_mm_per_s;

    /*
     * Raft
     */
    private boolean printRaft;
    private float raftBaseLinewidth_mm;
    private float raftAirGapLayer0_mm;
    private int interfaceLayers;
    private float raftBaseThickness_mm = 0.3f;

    private List<PropertyChangeListener> propertyChangeListeners = new ArrayList<>();
    
    @JsonIgnore
    public String getProfileKey() {
        return profileName + "#" + headType;
    }

    public void addPropertyChangeListener(PropertyChangeListener propertyChangeListener)
    {
        propertyChangeListeners.add(propertyChangeListener);
    }

    public void removePropertyChangeListener(PropertyChangeListener propertyChangeListener)
    {
        propertyChangeListeners.remove(propertyChangeListener);
    }

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
        firePropertyChange("version", null, version);
    }

    public String getProfileName()
    {
        return profileName;
    }

    public void setProfileName(String profileName)
    {
        this.profileName = profileName;
        firePropertyChange("profileName", null, profileName);
    }
    
    public String getHeadType()
    {
        return headType;
    }

    public void setHeadType(String headType)
    {
        this.headType = headType;
        firePropertyChange("headType", null, headType);
    }

    public SlicerType getSlicerOverride()
    {
        return slicerOverride;
    }

    public void setSlicerOverride(SlicerType slicerOverride)
    {
        this.slicerOverride = slicerOverride;
        firePropertyChange("slicerOverride", null, slicerOverride);
    }

    public float getFirstLayerHeight_mm()
    {
        return firstLayerHeight_mm;
    }

    public void setFirstLayerHeight_mm(float firstLayerHeight_mm)
    {
        this.firstLayerHeight_mm = firstLayerHeight_mm;
        firePropertyChange("firstLayerHeight_mm", null, firstLayerHeight_mm);
    }

    public float getLayerHeight_mm()
    {
        return layerHeight_mm;
    }

    public void setLayerHeight_mm(float layerHeight_mm)
    {
        this.layerHeight_mm = layerHeight_mm;
        firePropertyChange("layerHeight_mm", null, layerHeight_mm);
    }

    public float getFillDensity_normalised()
    {
        return fillDensity_normalised;
    }

    public void setFillDensity_normalised(float fillDensity_normalised)
    {
        if (this.fillDensity_normalised == fillDensity_normalised)
        {
            return;
        }
        this.fillDensity_normalised = fillDensity_normalised;
        firePropertyChange("fillDensity_normalised", null, fillDensity_normalised);
    }

    public FillPattern getFillPattern()
    {
        return fillPattern;
    }

    public void setFillPattern(FillPattern fillPattern)
    {
        this.fillPattern = fillPattern;
        firePropertyChange("fillPattern", null, fillPattern);
    }

    public int getFillEveryNLayers()
    {
        return fillEveryNLayers;
    }

    public void setFillEveryNLayers(int fillEveryNLayers)
    {
        this.fillEveryNLayers = fillEveryNLayers;
        firePropertyChange("fillEveryNLayers", null, fillEveryNLayers);
    }

    public int getSolidLayersAtTop()
    {
        return solidLayersAtTop;
    }

    public void setSolidLayersAtTop(int solidLayersAtTop)
    {
        this.solidLayersAtTop = solidLayersAtTop;
        firePropertyChange("solidLayersAtTop", null, solidLayersAtTop);
    }

    public int getSolidLayersAtBottom()
    {
        return solidLayersAtBottom;
    }

    public void setSolidLayersAtBottom(int solidLayersAtBottom)
    {
        this.solidLayersAtBottom = solidLayersAtBottom;
        firePropertyChange("solidLayersAtBottom", null, solidLayersAtBottom);
    }

    public int getNumberOfPerimeters()
    {
        return numberOfPerimeters;
    }

    public void setNumberOfPerimeters(int numberOfPerimeters)
    {
        this.numberOfPerimeters = numberOfPerimeters;
        firePropertyChange("numberOfPerimeters", null, numberOfPerimeters);
    }

    public int getBrimWidth_mm()
    {
        return brimWidth_mm;
    }

    public void setBrimWidth_mm(int brimWidth_mm)
    {
        if (this.brimWidth_mm == brimWidth_mm)
        {
            return;
        }
        this.brimWidth_mm = brimWidth_mm;
        firePropertyChange("brimWidth_mm", null, brimWidth_mm);
    }

    public boolean getSpiralPrint()
    {
        return spiralPrint;
    }

    public void setSpiralPrint(boolean spiralPrint)
    {
        this.spiralPrint = spiralPrint;
        firePropertyChange("spiralPrint", null, spiralPrint);
    }

    public float getFirstLayerExtrusionWidth_mm()
    {
        return firstLayerExtrusionWidth_mm;
    }

    public void setFirstLayerExtrusionWidth_mm(float firstLayerExtrusionWidth_mm)
    {
        this.firstLayerExtrusionWidth_mm = firstLayerExtrusionWidth_mm;
        firePropertyChange("firstLayerExtrusionWidth_mm", null, firstLayerExtrusionWidth_mm);
    }

    public float getPerimeterExtrusionWidth_mm()
    {
        return perimeterExtrusionWidth_mm;
    }

    public void setPerimeterExtrusionWidth_mm(float perimeterExtrusionWidth_mm)
    {
        this.perimeterExtrusionWidth_mm = perimeterExtrusionWidth_mm;
        firePropertyChange("perimeterExtrusionWidth_mm", null, perimeterExtrusionWidth_mm);
    }

    public float getFillExtrusionWidth_mm()
    {
        return fillExtrusionWidth_mm;
    }

    public void setFillExtrusionWidth_mm(float fillExtrusionWidth_mm)
    {
        this.fillExtrusionWidth_mm = fillExtrusionWidth_mm;
        firePropertyChange("fillExtrusionWidth_mm", null, fillExtrusionWidth_mm);
    }

    public float getSolidFillExtrusionWidth_mm()
    {
        return solidFillExtrusionWidth_mm;
    }

    public void setSolidFillExtrusionWidth_mm(float solidFillExtrusionWidth_mm)
    {
        this.solidFillExtrusionWidth_mm = solidFillExtrusionWidth_mm;
        firePropertyChange("solidFillExtrusionWidth_mm", null, solidFillExtrusionWidth_mm);
    }

    public float getTopSolidFillExtrusionWidth_mm()
    {
        return topSolidFillExtrusionWidth_mm;
    }

    public void setTopSolidFillExtrusionWidth_mm(float topSolidFillExtrusionWidth_mm)
    {
        this.topSolidFillExtrusionWidth_mm = topSolidFillExtrusionWidth_mm;
        firePropertyChange("topSolidFillExtrusionWidth_mm", null, topSolidFillExtrusionWidth_mm);
    }

    public float getSupportExtrusionWidth_mm()
    {
        return supportExtrusionWidth_mm;
    }

    public void setSupportExtrusionWidth_mm(float supportExtrusionWidth_mm)
    {
        this.supportExtrusionWidth_mm = supportExtrusionWidth_mm;
        firePropertyChange("supportExtrusionWidth_mm", null, supportExtrusionWidth_mm);
    }

    public ArrayList<NozzleParameters> getNozzleParameters()
    {
        return nozzleParameters;
    }

    public void setNozzleParameters(ArrayList<NozzleParameters> nozzleParameters)
    {
        this.nozzleParameters = nozzleParameters;
        firePropertyChange("nozzleParameters", null, nozzleParameters);
    }

    public int getFirstLayerNozzle()
    {
        return firstLayerNozzle;
    }

    public void setFirstLayerNozzle(int firstLayerNozzle)
    {
        this.firstLayerNozzle = firstLayerNozzle;
        firePropertyChange("firstLayerNozzle", null, firstLayerNozzle);
    }

    public int getPerimeterNozzle()
    {
        return perimeterNozzle;
    }

    public void setPerimeterNozzle(int perimeterNozzle)
    {
        this.perimeterNozzle = perimeterNozzle;
        firePropertyChange("perimeterNozzle", null, perimeterNozzle);
    }

    public int getFillNozzle()
    {
        return fillNozzle;
    }

    public void setFillNozzle(int fillNozzle)
    {
        this.fillNozzle = fillNozzle;
        firePropertyChange("fillNozzle", null, fillNozzle);
    }

    public int getSupportNozzle()
    {
        return supportNozzle;
    }

    public void setSupportNozzle(int supportNozzle)
    {
        this.supportNozzle = supportNozzle;
        firePropertyChange("supportNozzle", null, supportNozzle);
    }

    public int getSupportInterfaceNozzle()
    {
        return supportInterfaceNozzle;
    }

    public void setSupportInterfaceNozzle(int supportInterfaceNozzle)
    {
        this.supportInterfaceNozzle = supportInterfaceNozzle;
        firePropertyChange("supportInterfaceNozzle", null, supportInterfaceNozzle);
    }

    public boolean getGenerateSupportMaterial()
    {
        return generateSupportMaterial;
    }

    public void setGenerateSupportMaterial(boolean generateSupportMaterial)
    {
        if (this.generateSupportMaterial == generateSupportMaterial)
        {
            return;
        }
        this.generateSupportMaterial = generateSupportMaterial;
        firePropertyChange("generateSupportMaterial", null, generateSupportMaterial);
    }

    public boolean getPrintRaft()
    {
        return printRaft;
    }

    public void setPrintRaft(boolean printRaft)
    {
        if (this.printRaft == printRaft)
        {
            return;
        }
        this.printRaft = printRaft;
        firePropertyChange("printRaft", null, printRaft);
    }

    public float getRaftBaseLinewidth_mm()
    {
        return raftBaseLinewidth_mm;
    }

    public void setRaftBaseLinewidth_mm(float raftBaseLinewidth_mm)
    {
        if (this.raftBaseLinewidth_mm == raftBaseLinewidth_mm)
        {
            return;
        }
        this.raftBaseLinewidth_mm = raftBaseLinewidth_mm;
        firePropertyChange("raftBaseLinewidth_mm", null, raftBaseLinewidth_mm);
    }

    public float getRaftAirGapLayer0_mm()
    {
        return raftAirGapLayer0_mm;
    }

    public void setRaftAirGapLayer0_mm(float raftAirGapLayer0_mm)
    {
        if (this.raftAirGapLayer0_mm == raftAirGapLayer0_mm)
        {
            return;
        }
        this.raftAirGapLayer0_mm = raftAirGapLayer0_mm;
        firePropertyChange("raftAirGapLayer0_mm", null, raftAirGapLayer0_mm);
    }

    public int getInterfaceLayers()
    {
        return interfaceLayers;
    }

    public void setInterfaceLayers(int interfaceLayers)
    {
        if (this.interfaceLayers == interfaceLayers)
        {
            return;
        }
        this.interfaceLayers = interfaceLayers;
        firePropertyChange("interfaceLayers", null, interfaceLayers);
    }

    public float getRaftBaseThickness_mm()
    {
        return raftBaseThickness_mm;
    }

    public void setRaftBaseThickness_mm(float raftBaseThickness_mm)
    {
        if (this.raftBaseThickness_mm == raftBaseThickness_mm)
        {
            return;
        }
        this.raftBaseThickness_mm = raftBaseThickness_mm;
        firePropertyChange("raftBaseThickness_mm", null, raftBaseThickness_mm);
    }

    public int getSupportOverhangThreshold_degrees()
    {
        return supportOverhangThreshold_degrees;
    }

    public void setSupportOverhangThreshold_degrees(int supportOverhangThreshold_degrees)
    {
        this.supportOverhangThreshold_degrees = supportOverhangThreshold_degrees;
        firePropertyChange("supportOverhangThreshold_degrees", null,
                           supportOverhangThreshold_degrees);
    }

    public int getForcedSupportForFirstNLayers()
    {
        return forcedSupportForFirstNLayers;
    }

    public void setForcedSupportForFirstNLayers(int forcedSupportForFirstNLayers)
    {
        this.forcedSupportForFirstNLayers = forcedSupportForFirstNLayers;
        firePropertyChange("forcedSupportForFirstNLayers", null, forcedSupportForFirstNLayers);
    }

    public SupportPattern getSupportPattern()
    {
        return supportPattern;
    }

    public void setSupportPattern(SupportPattern supportPattern)
    {
        this.supportPattern = supportPattern;
        firePropertyChange("supportPattern", null, supportPattern);
    }

    public float getSupportPatternSpacing_mm()
    {
        return supportPatternSpacing_mm;
    }

    public void setSupportPatternSpacing_mm(float supportPatternSpacing_mm)
    {
        this.supportPatternSpacing_mm = supportPatternSpacing_mm;
        firePropertyChange("supportPatternSpacing_mm", null, supportPatternSpacing_mm);
    }

    public int getSupportPatternAngle_degrees()
    {
        return supportPatternAngle_degrees;
    }

    public void setSupportPatternAngle_degrees(int supportPatternAngle_degrees)
    {
        this.supportPatternAngle_degrees = supportPatternAngle_degrees;
        firePropertyChange("supportPatternAngle_degrees", null, supportPatternAngle_degrees);
    }

    public int getFirstLayerSpeed_mm_per_s()
    {
        return firstLayerSpeed_mm_per_s;
    }

    public void setFirstLayerSpeed_mm_per_s(int firstLayerSpeed_mm_per_s)
    {
        this.firstLayerSpeed_mm_per_s = firstLayerSpeed_mm_per_s;
        firePropertyChange("firstLayerSpeed_mm_per_s", null, firstLayerSpeed_mm_per_s);
    }

    public int getPerimeterSpeed_mm_per_s()
    {
        return perimeterSpeed_mm_per_s;
    }

    public void setPerimeterSpeed_mm_per_s(int perimeterSpeed_mm_per_s)
    {
        this.perimeterSpeed_mm_per_s = perimeterSpeed_mm_per_s;
        firePropertyChange("perimeterSpeed_mm_per_s", null, perimeterSpeed_mm_per_s);
    }

    public int getSmallPerimeterSpeed_mm_per_s()
    {
        return smallPerimeterSpeed_mm_per_s;
    }

    public void setSmallPerimeterSpeed_mm_per_s(int smallPerimeterSpeed_mm_per_s)
    {
        this.smallPerimeterSpeed_mm_per_s = smallPerimeterSpeed_mm_per_s;
        firePropertyChange("smallPerimeterSpeed_mm_per_s", null, smallPerimeterSpeed_mm_per_s);
    }

    public int getExternalPerimeterSpeed_mm_per_s()
    {
        return externalPerimeterSpeed_mm_per_s;
    }

    public void setExternalPerimeterSpeed_mm_per_s(int externalPerimeterSpeed_mm_per_s)
    {
        this.externalPerimeterSpeed_mm_per_s = externalPerimeterSpeed_mm_per_s;
        firePropertyChange("externalPerimeterSpeed_mm_per_s", null, externalPerimeterSpeed_mm_per_s);
    }

    public int getFillSpeed_mm_per_s()
    {
        return fillSpeed_mm_per_s;
    }

    public void setFillSpeed_mm_per_s(int fillSpeed_mm_per_s)
    {
        this.fillSpeed_mm_per_s = fillSpeed_mm_per_s;
        firePropertyChange("fillSpeed_mm_per_s", null, fillSpeed_mm_per_s);
    }

    public int getSolidFillSpeed_mm_per_s()
    {
        return solidFillSpeed_mm_per_s;
    }

    public void setSolidFillSpeed_mm_per_s(int solidFillSpeed_mm_per_s)
    {
        this.solidFillSpeed_mm_per_s = solidFillSpeed_mm_per_s;
        firePropertyChange("solidFillSpeed_mm_per_s", null, solidFillSpeed_mm_per_s);
    }

    public int getTopSolidFillSpeed_mm_per_s()
    {
        return topSolidFillSpeed_mm_per_s;
    }

    public void setTopSolidFillSpeed_mm_per_s(int topSolidFillSpeed_mm_per_s)
    {
        this.topSolidFillSpeed_mm_per_s = topSolidFillSpeed_mm_per_s;
        firePropertyChange("topSolidFillSpeed_mm_per_s", null, topSolidFillSpeed_mm_per_s);
    }

    public int getSupportSpeed_mm_per_s()
    {
        return supportSpeed_mm_per_s;
    }

    public void setSupportSpeed_mm_per_s(int supportSpeed_mm_per_s)
    {
        this.supportSpeed_mm_per_s = supportSpeed_mm_per_s;
        firePropertyChange("supportSpeed_mm_per_s", null, supportSpeed_mm_per_s);
    }

    public int getBridgeSpeed_mm_per_s()
    {
        return bridgeSpeed_mm_per_s;
    }

    public void setBridgeSpeed_mm_per_s(int bridgeSpeed_mm_per_s)
    {
        this.bridgeSpeed_mm_per_s = bridgeSpeed_mm_per_s;
        firePropertyChange("bridgeSpeed_mm_per_s", null, bridgeSpeed_mm_per_s);
    }

    public int getGapFillSpeed_mm_per_s()
    {
        return gapFillSpeed_mm_per_s;
    }

    public void setGapFillSpeed_mm_per_s(int gapFillSpeed_mm_per_s)
    {
        this.gapFillSpeed_mm_per_s = gapFillSpeed_mm_per_s;
        firePropertyChange("gapFillSpeed_mm_per_s", null, gapFillSpeed_mm_per_s);
    }

    public boolean getEnableCooling()
    {
        return enableCooling;
    }

    public void setEnableCooling(boolean enableCooling)
    {
        this.enableCooling = enableCooling;
        firePropertyChange("enableCooling", null, enableCooling);
    }

    public int getMinFanSpeed_percent()
    {
        return minFanSpeed_percent;
    }

    public void setMinFanSpeed_percent(int minFanSpeed_percent)
    {
        this.minFanSpeed_percent = minFanSpeed_percent;
        firePropertyChange("minFanSpeed_percent", null, minFanSpeed_percent);
    }

    public int getMaxFanSpeed_percent()
    {
        return maxFanSpeed_percent;
    }

    public void setMaxFanSpeed_percent(int maxFanSpeed_percent)
    {
        this.maxFanSpeed_percent = maxFanSpeed_percent;
        firePropertyChange("maxFanSpeed_percent", null, maxFanSpeed_percent);
    }

    public int getInterfaceSpeed_mm_per_s()
    {
        return interfaceSpeed_mm_per_s;
    }

    public void setInterfaceSpeed_mm_per_s(int interfaceSpeed_mm_per_s)
    {
        this.interfaceSpeed_mm_per_s = interfaceSpeed_mm_per_s;
        firePropertyChange("interfaceSpeed_mm_per_s", null, interfaceSpeed_mm_per_s);
    }

    public int getBridgeFanSpeed_percent()
    {
        return bridgeFanSpeed_percent;
    }

    public void setBridgeFanSpeed_percent(int bridgeFanSpeed_percent)
    {
        this.bridgeFanSpeed_percent = bridgeFanSpeed_percent;
        firePropertyChange("bridgeFanSpeed_percent", null, bridgeFanSpeed_percent);
    }

    public int getDisableFanFirstNLayers()
    {
        return disableFanFirstNLayers;
    }

    public void setDisableFanFirstNLayers(int disableFanFirstNLayers)
    {
        this.disableFanFirstNLayers = disableFanFirstNLayers;
        firePropertyChange("disableFanFirstNLayers", null, disableFanFirstNLayers);
    }

    public int getCoolIfLayerTimeLessThan_secs()
    {
        return coolIfLayerTimeLessThan_secs;
    }

    public void setCoolIfLayerTimeLessThan_secs(int coolIfLayerTimeLessThan_secs)
    {
        this.coolIfLayerTimeLessThan_secs = coolIfLayerTimeLessThan_secs;
        firePropertyChange("coolIfLayerTimeLessThan_secs", null, coolIfLayerTimeLessThan_secs);
    }

    public int getSlowDownIfLayerTimeLessThan_secs()
    {
        return slowDownIfLayerTimeLessThan_secs;
    }

    public void setSlowDownIfLayerTimeLessThan_secs(int slowDownIfLayerTimeLessThan_secs)
    {
        this.slowDownIfLayerTimeLessThan_secs = slowDownIfLayerTimeLessThan_secs;
        firePropertyChange("slowDownIfLayerTimeLessThan_secs", null,
                           slowDownIfLayerTimeLessThan_secs);
    }

    public int getMinPrintSpeed_mm_per_s()
    {
        return minPrintSpeed_mm_per_s;
    }

    public void setMinPrintSpeed_mm_per_s(int minPrintSpeed_mm_per_s)
    {
        this.minPrintSpeed_mm_per_s = minPrintSpeed_mm_per_s;
        firePropertyChange("minPrintSpeed_mm_per_s", null, minPrintSpeed_mm_per_s);
    }

    public int getMaxClosesBeforeNozzleReselect()
    {
        return maxClosesBeforeNozzleReselect;
    }

    public void setMaxClosesBeforeNozzleReselect(int maxClosesBeforeNozzleReselect)
    {
        this.maxClosesBeforeNozzleReselect = maxClosesBeforeNozzleReselect;
        firePropertyChange("maxClosesBeforeNozzleReselect", null, maxClosesBeforeNozzleReselect);
    }

    public float getzHopHeight()
    {
        return zHopHeight;
    }

    public void setzHopHeight(float zHopHeight)
    {
        this.zHopHeight = zHopHeight;
        firePropertyChange("zHopHeight", null, zHopHeight);
    }

    public float getzHopDistance()
    {
        return zHopDistance;
    }

    public void setzHopDistance(float zHopDistance)
    {
        this.zHopDistance = zHopDistance;
        firePropertyChange("zHopDistance", null, zHopDistance);
    }

    @Override
    public SlicerParametersFile clone()
    {
        SlicerParametersFile clone = new SlicerParametersFile();

        clone.profileName = profileName;
        clone.headType = headType;
        clone.slicerOverride = slicerOverride;

        /*
         * Extrusion data
         */
        clone.firstLayerHeight_mm = firstLayerHeight_mm;
        clone.layerHeight_mm = layerHeight_mm;
        clone.fillDensity_normalised = fillDensity_normalised;
        clone.fillPattern = fillPattern;
        clone.fillEveryNLayers = fillEveryNLayers;
        clone.solidLayersAtTop = solidLayersAtTop;
        clone.solidLayersAtBottom = solidLayersAtBottom;
        clone.numberOfPerimeters = numberOfPerimeters;
        clone.brimWidth_mm = brimWidth_mm;
        clone.spiralPrint = spiralPrint;

        /*
         * Nozzle data
         */
        clone.firstLayerExtrusionWidth_mm = firstLayerExtrusionWidth_mm;
        clone.perimeterExtrusionWidth_mm = perimeterExtrusionWidth_mm;
        clone.fillExtrusionWidth_mm = fillExtrusionWidth_mm;
        clone.solidFillExtrusionWidth_mm = solidFillExtrusionWidth_mm;
        clone.topSolidFillExtrusionWidth_mm = topSolidFillExtrusionWidth_mm;
        clone.supportExtrusionWidth_mm = supportExtrusionWidth_mm;

        clone.nozzleParameters = new ArrayList<>();
        nozzleParameters.stream().forEach(nozzleParameter -> clone.nozzleParameters.add(
            nozzleParameter.clone()));

        clone.firstLayerNozzle = firstLayerNozzle;
        clone.perimeterNozzle = perimeterNozzle;
        clone.fillNozzle = fillNozzle;
        clone.supportNozzle = supportNozzle;
        clone.supportInterfaceNozzle = supportInterfaceNozzle;

        clone.maxClosesBeforeNozzleReselect = maxClosesBeforeNozzleReselect;
        clone.zHopHeight = zHopHeight;
        clone.zHopDistance = zHopDistance;

        /*
         * Support
         */
        clone.generateSupportMaterial = generateSupportMaterial;
        clone.supportOverhangThreshold_degrees = supportOverhangThreshold_degrees;
        clone.forcedSupportForFirstNLayers = forcedSupportForFirstNLayers;
        clone.supportPattern = supportPattern;
        clone.supportPatternSpacing_mm = supportPatternSpacing_mm;
        clone.supportPatternAngle_degrees = supportPatternAngle_degrees;

        /*
         * Speed settings
         */
        clone.firstLayerSpeed_mm_per_s = firstLayerSpeed_mm_per_s;
        clone.perimeterSpeed_mm_per_s = perimeterSpeed_mm_per_s;
        clone.smallPerimeterSpeed_mm_per_s = smallPerimeterSpeed_mm_per_s;
        clone.externalPerimeterSpeed_mm_per_s = externalPerimeterSpeed_mm_per_s;
        clone.fillSpeed_mm_per_s = fillSpeed_mm_per_s;
        clone.solidFillSpeed_mm_per_s = solidFillSpeed_mm_per_s;
        clone.topSolidFillSpeed_mm_per_s = topSolidFillSpeed_mm_per_s;
        clone.supportSpeed_mm_per_s = supportSpeed_mm_per_s;
        clone.bridgeSpeed_mm_per_s = bridgeSpeed_mm_per_s;
        clone.interfaceSpeed_mm_per_s = interfaceSpeed_mm_per_s;
        clone.gapFillSpeed_mm_per_s = gapFillSpeed_mm_per_s;

        /*
         * Cooling
         */
        clone.enableCooling = enableCooling;
        clone.minFanSpeed_percent = minFanSpeed_percent;
        clone.maxFanSpeed_percent = maxFanSpeed_percent;
        clone.bridgeFanSpeed_percent = bridgeFanSpeed_percent;
        clone.disableFanFirstNLayers = disableFanFirstNLayers;
        clone.coolIfLayerTimeLessThan_secs = coolIfLayerTimeLessThan_secs;
        clone.slowDownIfLayerTimeLessThan_secs = slowDownIfLayerTimeLessThan_secs;
        clone.minPrintSpeed_mm_per_s = minPrintSpeed_mm_per_s;

        /*
         * Raft
         */
        clone.raftAirGapLayer0_mm = raftAirGapLayer0_mm;
        clone.raftBaseLinewidth_mm = raftBaseLinewidth_mm;
        clone.raftBaseThickness_mm = raftBaseThickness_mm;
        clone.interfaceLayers = interfaceLayers;
        clone.printRaft = printRaft;

        return clone;
    }

    private void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        for (PropertyChangeListener propertyChangeListener : propertyChangeListeners)
        {
            propertyChangeListener.propertyChange(new PropertyChangeEvent(this, propertyName,
                                                                          oldValue, newValue));
        }
    }
}
