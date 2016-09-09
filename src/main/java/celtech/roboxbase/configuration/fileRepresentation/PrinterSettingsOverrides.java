package celtech.roboxbase.configuration.fileRepresentation;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.HeadContainer;
import celtech.roboxbase.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile.SupportType;
import celtech.roboxbase.services.slicer.PrintQualityEnumeration;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * PrinterSettings represents the choices made by the user for a project on the
 * Settings panel. It is serialised with the project.
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class PrinterSettingsOverrides
{

    private final Stenographer steno = StenographerFactory.getStenographer(PrinterSettingsOverrides.class.getName());

    private final StringProperty customSettingsName = new SimpleStringProperty();
    private final ObjectProperty<PrintQualityEnumeration> printQuality
            = new SimpleObjectProperty<>(PrintQualityEnumeration.NORMAL);
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    private int brimOverride = 0;
    private float fillDensityOverride = 0;
    private final BooleanProperty printSupportOverride = new SimpleBooleanProperty(false);
    private final BooleanProperty printSupportGapEnabledOverride = new SimpleBooleanProperty(false);
    private final ObjectProperty<SupportType> printSupportTypeOverride = new SimpleObjectProperty<>(SupportType.MATERIAL_1);
    private boolean raftOverride = false;
    private boolean spiralPrintOverride = false;

    public PrinterSettingsOverrides()
    {
        customSettingsName.set("");
        SlicerParametersFile initialParametersFile = SlicerParametersContainer.getInstance().getSettings(
                printQuality.get().getFriendlyName(), HeadContainer.defaultHeadID);
        brimOverride = initialParametersFile.getBrimWidth_mm();
        fillDensityOverride = initialParametersFile.getFillDensity_normalised();
        printSupportTypeOverride.set(SupportType.MATERIAL_1);

        SlicerParametersContainer.addChangesListener(
                new SlicerParametersContainer.SlicerParametersChangesListener()
                {

                    @Override
                    public void whenSlicerParametersSaved(String originalSettingsName,
                            SlicerParametersFile changedParameters)
                    {
                        if (originalSettingsName.equals(customSettingsName.get()))
                        {
                            customSettingsName.set(changedParameters.getProfileName());
                        }
                        toggleDataChanged();
                    }

                    @Override
                    public void whenSlicerParametersDeleted(String settingsName)
                    {
                    }
                });

    }

    private void toggleDataChanged()
    {
        dataChanged.set(dataChanged.not().get());
    }

    public ReadOnlyBooleanProperty getDataChanged()
    {
        return dataChanged;
    }

    public void setPrintQuality(PrintQualityEnumeration value)
    {
        if (printQuality.get() != value)
        {
            printQuality.set(value);
            toggleDataChanged();
        }
    }

    public PrintQualityEnumeration getPrintQuality()
    {
        return printQuality.get();
    }

    public ObjectProperty<PrintQualityEnumeration> printQualityProperty()
    {
        return printQuality;
    }

    public void setSettingsName(String settingsName)
    {
        if (!customSettingsName.get().equals(settingsName))
        {
            customSettingsName.set(settingsName);
            toggleDataChanged();
        }
    }

    public String getSettingsName()
    {
        return customSettingsName.get();
    }

    public StringProperty getSettingsNameProperty()
    {
        return customSettingsName;
    }

    public SlicerParametersFile getSettings(String headType)
    {
        SlicerParametersFile settings = null;
        switch (printQuality.get())
        {
            case DRAFT:
                settings = SlicerParametersContainer.getSettings(
                        BaseConfiguration.draftSettingsProfileName, headType);
                break;
            case NORMAL:
                settings = SlicerParametersContainer.getSettings(
                        BaseConfiguration.normalSettingsProfileName, headType);
                break;
            case FINE:
                settings = SlicerParametersContainer.getSettings(
                        BaseConfiguration.fineSettingsProfileName, headType);
                break;
            case CUSTOM:
                settings = SlicerParametersContainer.getSettings(
                        customSettingsName.get(), headType);
                break;

        }
        if (settings == null)
        {
            return null;
        }
        return applyOverrides(settings);
    }

    /**
     * Standard profiles must have the overrides applied.
     *
     * @param settingsByProfileName
     * @return
     */
    public SlicerParametersFile applyOverrides(SlicerParametersFile settingsByProfileName)
    {
        SlicerParametersFile profileCopy = settingsByProfileName.clone();
        profileCopy.setBrimWidth_mm(brimOverride);
        profileCopy.setFillDensity_normalised(fillDensityOverride);
        profileCopy.setGenerateSupportMaterial(printSupportOverride.get());
        profileCopy.setSupportGapEnabled(printSupportGapEnabledOverride.get());
        profileCopy.setPrintRaft(raftOverride);
        profileCopy.setSpiralPrint(spiralPrintOverride);

        if (spiralPrintOverride)
        {
            profileCopy.setNumberOfPerimeters(1);
        }

        return profileCopy;
    }

    public int getBrimOverride()
    {
        return brimOverride;
    }

    public void setBrimOverride(int brimOverride)
    {
        if (this.brimOverride != brimOverride)
        {
            this.brimOverride = brimOverride;
            toggleDataChanged();
        }
    }

    public float getFillDensityOverride()
    {
        return fillDensityOverride;
    }

    public void setFillDensityOverride(float fillDensityOverride)
    {
        if (this.fillDensityOverride != fillDensityOverride)
        {
            this.fillDensityOverride = fillDensityOverride;
            toggleDataChanged();
        }
    }

    public boolean getPrintSupportOverride()
    {
        return printSupportOverride.get();
    }

    public BooleanProperty getPrintSupportOverrideProperty()
    {
        return printSupportOverride;
    }

    public void setPrintSupportOverride(boolean printSupportOverride)
    {
        this.printSupportOverride.set(printSupportOverride);
        toggleDataChanged();
    }

    public boolean getPrintSupportGapEnabledOverride()
    {
        return printSupportGapEnabledOverride.get();
    }

    public BooleanProperty getPrintSupportGapEnabledOverrideProperty()
    {
        return printSupportGapEnabledOverride;
    }

    public void setPrintSupportGapEnabledOverride(boolean printSupportGapEnabledOverride)
    {
        this.printSupportGapEnabledOverride.set(printSupportGapEnabledOverride);
        toggleDataChanged();
    }

    public SupportType getPrintSupportTypeOverride()
    {
        return printSupportTypeOverride.get();
    }

    public ObjectProperty<SupportType> getPrintSupportTypeOverrideProperty()
    {
        return printSupportTypeOverride;
    }

    public void setPrintSupportTypeOverride(SupportType printSupportTypeOverride)
    {
        if (this.printSupportTypeOverride.get() != printSupportTypeOverride)
        {
            this.printSupportTypeOverride.set(printSupportTypeOverride);
            toggleDataChanged();
        }
    }

    public boolean getRaftOverride()
    {
        return raftOverride;
    }

    public void setRaftOverride(boolean raftOverride)
    {
        if (this.raftOverride != raftOverride)
        {
            this.raftOverride = raftOverride;
            toggleDataChanged();
        }
    }

    public boolean getSpiralPrintOverride()
    {
        return spiralPrintOverride;
    }

    public void setSpiralPrintOverride(boolean spiralPrintOverride)
    {
        if (this.spiralPrintOverride != spiralPrintOverride)
        {
            this.spiralPrintOverride = spiralPrintOverride;
            toggleDataChanged();
        }
    }
}
