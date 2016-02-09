package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.MaterialType;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.FilamentFileFilter;
import celtech.roboxbase.utils.DeDuplicator;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import javafx.scene.paint.Color;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class FilamentContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(
            FilamentContainer.class.getName());
    private final ObservableList<Filament> appFilamentList = FXCollections.observableArrayList();
    private final ObservableList<Filament> userFilamentList = FXCollections.observableArrayList();
    private final ObservableList<Filament> completeFilamentList = FXCollections.observableArrayList();
    private final ObservableMap<String, Filament> completeFilamentMapByID = FXCollections.observableHashMap();
    private final ObservableMap<String, String> completeFilamentNameByID = FXCollections.observableHashMap();

    public final Filament createNewFilament = new Filament(null, null, null,
            0, 0, 0, 0, 0, 0, 0, 0, Color.ALICEBLUE,
            0, false);
    public static final Filament UNKNOWN_FILAMENT = new Filament("",
            null,
            null,
            1.75f,
            1,
            1,
            0,
            0,
            0,
            0,
            0,
            Color.ALICEBLUE,
            0,
            false);

    private static final String nameProperty = "name";
    private static final String materialProperty = "material";
    private static final String filamentIDProperty = "reelID";
    private static final String diameterProperty = "diameter_mm";
    private static final String costGBPPerKGProperty = "cost_gbp_per_kg";
    private static final String filamentMultiplierProperty = "filament_multiplier";
    private static final String feedRateMultiplierProperty = "feed_rate_multiplier";
    private static final String ambientTempProperty = "ambient_temperature_C";
    private static final String firstLayerBedTempProperty = "first_layer_bed_temperature_C";
    private static final String bedTempProperty = "bed_temperature_C";
    private static final String firstLayerNozzleTempProperty = "first_layer_nozzle_temperature_C";
    private static final String nozzleTempProperty = "nozzle_temperature_C";
    private static final String displayColourProperty = "display_colour";

    public interface FilamentDatabaseChangesListener
    {

        public void whenFilamentChanges(String filamentId);
    }

    private final List<FilamentDatabaseChangesListener> filamentDatabaseChangesListeners = new ArrayList<>();

    public FilamentContainer()
    {
        loadFilamentData();
    }

    public void addFilamentDatabaseChangesListener(FilamentDatabaseChangesListener listener)
    {
        filamentDatabaseChangesListeners.add(listener);
    }

    public void removeFilamentDatabaseChangesListener(FilamentDatabaseChangesListener listener)
    {
        filamentDatabaseChangesListeners.remove(listener);
    }

    private void notifyFilamentDatabaseChangesListeners(String filamentId)
    {
        for (FilamentDatabaseChangesListener listener : filamentDatabaseChangesListeners)
        {
            listener.whenFilamentChanges(filamentId);
        }
    }

    public static String constructFilePath(Filament filament)
    {
        return BaseConfiguration.getUserFilamentDirectory()
                + filament.getFriendlyFilamentName() + "-" + filament.getMaterial().getFriendlyName()
                + BaseConfiguration.filamentFileExtension;
    }

    private void loadFilamentData()
    {
        completeFilamentMapByID.clear();
        completeFilamentNameByID.clear();
        completeFilamentList.clear();
        appFilamentList.clear();
        userFilamentList.clear();

        ArrayList<Filament> filaments = null;

        File applicationFilamentDirHandle = new File(
                BaseConfiguration.getApplicationFilamentDirectory());
        File[] applicationfilaments = applicationFilamentDirHandle.listFiles(
                new FilamentFileFilter());
        if (applicationfilaments != null)
        {
            filaments = ingestFilaments(applicationfilaments, false);
            appFilamentList.addAll(filaments);
            appFilamentList.sort(Filament.BY_MATERIAL.thenComparing(Filament.BY_NAME));
            completeFilamentList.addAll(filaments);
            completeFilamentList.sort(Filament.BY_MATERIAL.thenComparing(Filament.BY_NAME));
        } else
        {
            steno.error("No application filaments found");
        }

        File userFilamentDirHandle = new File(BaseConfiguration.getUserFilamentDirectory());
        File[] userfilaments = userFilamentDirHandle.listFiles(new FilamentFileFilter());
        if (userfilaments != null)
        {
            filaments = ingestFilaments(userfilaments, true);
            completeFilamentList.addAll(filaments);
            completeFilamentList.sort(Filament.BY_MATERIAL.thenComparing(Filament.BY_NAME));
            userFilamentList.addAll(filaments);
            userFilamentList.sort(Filament.BY_MATERIAL.thenComparing(Filament.BY_NAME));
        } else
        {
            steno.info("No user filaments found");
        }
    }

    private ArrayList<Filament> ingestFilaments(File[] filamentFiles, boolean filamentsAreMutable)
    {
        ArrayList<Filament> filamentList = new ArrayList<>();

        for (File filamentFile : filamentFiles)
        {
            try
            {
                Properties filamentProperties = new Properties();
                try (FileInputStream fileInputStream = new FileInputStream(filamentFile))
                {
                    filamentProperties.load(fileInputStream);

                    String name = filamentProperties.getProperty(nameProperty).trim();
                    String filamentID = filamentProperties.getProperty(filamentIDProperty).trim();
                    String material = filamentProperties.getProperty(materialProperty).trim();
                    String diameterString = filamentProperties.getProperty(diameterProperty).trim();
                    String filamentMultiplierString = filamentProperties.getProperty(
                            filamentMultiplierProperty).trim();
                    String feedRateMultiplierString = filamentProperties.getProperty(
                            feedRateMultiplierProperty).trim();
                    String ambientTempString = filamentProperties.getProperty(ambientTempProperty).trim();
                    String firstLayerBedTempString = filamentProperties.getProperty(
                            firstLayerBedTempProperty).trim();
                    String bedTempString = filamentProperties.getProperty(bedTempProperty).trim();
                    String firstLayerNozzleTempString = filamentProperties.getProperty(
                            firstLayerNozzleTempProperty).trim();
                    String nozzleTempString = filamentProperties.getProperty(nozzleTempProperty).trim();
                    String displayColourString = filamentProperties.getProperty(
                            displayColourProperty).trim();
                    // introduced in 1.01.05
                    String costGBPPerKGString = "40";
                    try
                    {
                        costGBPPerKGString = filamentProperties.getProperty(costGBPPerKGProperty).trim();
                    } catch (Exception ex)
                    {
                        steno.warning("No cost per GBP found in filament file");
                    }

                    if (name != null
                            && material != null
                            && filamentID != null
                            && diameterString != null
                            && feedRateMultiplierString != null
                            && filamentMultiplierString != null
                            && ambientTempString != null
                            && firstLayerBedTempString != null
                            && bedTempString != null
                            && firstLayerNozzleTempString != null
                            && nozzleTempString != null
                            && displayColourString != null)
                    {
                        try
                        {
                            float diameter = Float.valueOf(diameterString);
                            float filamentMultiplier = Float.valueOf(filamentMultiplierString);
                            float feedRateMultiplier = Float.valueOf(feedRateMultiplierString);
                            int ambientTemp = Integer.valueOf(ambientTempString);
                            int firstLayerBedTemp = Integer.valueOf(firstLayerBedTempString);
                            int bedTemp = Integer.valueOf(bedTempString);
                            int firstLayerNozzleTemp = Integer.valueOf(firstLayerNozzleTempString);
                            int nozzleTemp = Integer.valueOf(nozzleTempString);
                            Color colour = Color.web(displayColourString);
                            float costGBPPerKG = Float.valueOf(costGBPPerKGString);
                            MaterialType selectedMaterial = MaterialType.valueOf(material);

                            Filament newFilament = new Filament(
                                    name,
                                    selectedMaterial,
                                    filamentID,
                                    diameter,
                                    filamentMultiplier,
                                    feedRateMultiplier,
                                    ambientTemp,
                                    firstLayerBedTemp,
                                    bedTemp,
                                    firstLayerNozzleTemp,
                                    nozzleTemp,
                                    colour,
                                    costGBPPerKG,
                                    filamentsAreMutable);

                            filamentList.add(newFilament);

                            completeFilamentMapByID.put(filamentID, newFilament);
                            completeFilamentNameByID.put(filamentID, name);

                        } catch (IllegalArgumentException ex)
                        {
                            steno.error("Failed to parse filament file "
                                    + filamentFile.getAbsolutePath());
                        }

                    }
                }
            } catch (Exception ex)
            {
                ex.printStackTrace();
                steno.error("Error loading filament " + filamentFile.getAbsolutePath() + " " + ex);
            }
        }

        return filamentList;
    }

    /**
     * Suggest a safe name for a new filament name based on the proposed name.
     */
    public String suggestNonDuplicateName(String proposedName)
    {
        List<String> currentFilamentNames = new ArrayList<>();
        completeFilamentList.stream().forEach((filament) ->
        {
            currentFilamentNames.add(filament.getFriendlyFilamentName());
        });
        return DeDuplicator.suggestNonDuplicateNameCopy(proposedName, currentFilamentNames);
    }

    /**
     * Save the given filament to file, using the friendly name and material
     * type as file name. If a filament already exists of the same filamentID
     * but different file name then delete that file.
     */
    public void saveFilament(Filament filament)
    {
        if (!completeFilamentMapByID.containsKey(filament.getFilamentID()))
        {
            addNewFilament(filament);
        } else
        {
            Filament currentFilamentOfThisID = getFilamentByID(filament.getFilamentID());
            String originalFriendlyNameForFilament = completeFilamentNameByID.get(filament.getFilamentID());
            if (!originalFriendlyNameForFilament.equals(filament.getFriendlyFilamentName()))
            {
                deleteFilamentUsingOldName(currentFilamentOfThisID);
                addNewFilament(filament);
            } else
            {
                saveEditedUserFilament(filament);
            }
        }
        notifyFilamentDatabaseChangesListeners(filament.getFilamentID());
    }

    private void addNewFilament(Filament filament)
    {
        saveEditedUserFilament(filament);
        userFilamentList.add(filament);
        completeFilamentList.add(filament);
        completeFilamentMapByID.put(filament.getFilamentID(), filament);
        completeFilamentNameByID.put(filament.getFilamentID(), filament.getFriendlyFilamentName());
    }

    private void saveEditedUserFilament(Filament filament)
    {
        NumberFormat floatConverter = DecimalFormat.getNumberInstance(Locale.UK);
        floatConverter.setMinimumFractionDigits(3);
        floatConverter.setGroupingUsed(false);

        try
        {
            Properties filamentProperties = new Properties();

            filamentProperties.setProperty(nameProperty, filament.getFriendlyFilamentName());
            filamentProperties.setProperty(materialProperty, filament.getMaterial().name());
            filamentProperties.setProperty(filamentIDProperty, filament.getFilamentID());
            filamentProperties.setProperty(costGBPPerKGProperty, floatConverter.format(
                    filament.getCostGBPPerKG()));
            filamentProperties.setProperty(diameterProperty, floatConverter.format(
                    filament.getDiameter()));
            filamentProperties.setProperty(filamentMultiplierProperty, floatConverter.format(
                    filament.getFilamentMultiplier()));
            filamentProperties.setProperty(feedRateMultiplierProperty, floatConverter.format(
                    filament.getFeedRateMultiplier()));
            filamentProperties.setProperty(ambientTempProperty, String.valueOf(
                    filament.getAmbientTemperature()));
            filamentProperties.setProperty(firstLayerBedTempProperty, String.valueOf(
                    filament.getFirstLayerBedTemperature()));
            filamentProperties.setProperty(bedTempProperty, String.valueOf(
                    filament.getBedTemperature()));
            filamentProperties.setProperty(firstLayerNozzleTempProperty, String.valueOf(
                    filament.getFirstLayerNozzleTemperature()));
            filamentProperties.setProperty(nozzleTempProperty, String.valueOf(
                    filament.getNozzleTemperature()));

            String webColour = String.format("#%02X%02X%02X",
                    (int) (filament.getDisplayColour().getRed() * 255),
                    (int) (filament.getDisplayColour().getGreen() * 255),
                    (int) (filament.getDisplayColour().getBlue() * 255));
            filamentProperties.setProperty(displayColourProperty, webColour);

            String newFilename = constructFilePath(filament);

            File filamentFile = new File(newFilename);
            try (FileOutputStream fileOutputStream = new FileOutputStream(filamentFile))
            {
                filamentProperties.store(fileOutputStream, "Robox data");
            }

        } catch (IOException ex)
        {
            steno.error("Error whilst storing filament file " + filament.getFileName() + " " + ex);
        }
    }

    public void deleteFilament(Filament filament)
    {
        assert (filament.isMutable());
        File filamentToDeleteFile = new File(constructFilePath(filament));
        try
        {
            Files.delete(filamentToDeleteFile.toPath());
            userFilamentList.remove(filament);
            completeFilamentList.remove(filament);
            completeFilamentMapByID.remove(filament.getFilamentID());
            completeFilamentNameByID.remove(filament.getFilamentID());
        } catch (IOException ex)
        {
            steno.error("Error deleting filament: " + constructFilePath(filament));
        }
        notifyFilamentDatabaseChangesListeners(filament.getFilamentID());
    }

    private void deleteFilamentUsingOldName(Filament filament)
    {
        assert (filament.isMutable());
        String oldName = completeFilamentNameByID.get(filament.getFilamentID());
        String path = BaseConfiguration.getUserFilamentDirectory()
                + oldName + "-" + filament.getMaterial().getFriendlyName()
                + BaseConfiguration.filamentFileExtension;
        File filamentToDeleteFile = new File(path);
        try
        {
            Files.delete(filamentToDeleteFile.toPath());
            userFilamentList.remove(filament);
            completeFilamentList.remove(filament);
            completeFilamentMapByID.remove(filament.getFilamentID());
            completeFilamentNameByID.remove(filament.getFilamentID());
        } catch (IOException ex)
        {
            steno.error("Error deleting filament: " + path);
        }
        notifyFilamentDatabaseChangesListeners(filament.getFilamentID());
    }

    public boolean isFilamentIDValid(String filamentID)
    {
        boolean filamentIDIsValid = false;

        if (filamentID != null
                && (filamentID.matches("RBX-[0-9A-Z]{3}-.*")
                || filamentID.matches("^U.*")))
        {
            filamentIDIsValid = true;
        }

        return filamentIDIsValid;
    }

    public boolean isFilamentIDInDatabase(String filamentID)
    {
        boolean filamentIDIsInDatabase = false;

        if (filamentID != null
                && getFilamentByID(filamentID) != null)
        {
            filamentIDIsInDatabase = true;
        }

        return filamentIDIsInDatabase;
    }

    public Filament getFilamentByID(String filamentID)
    {
        Filament returnedFilament = null;

        if (filamentID != null)
        {
            returnedFilament = completeFilamentMapByID.get(filamentID);
            if (returnedFilament == null)
            {
                //Try replacing dashes with underscores...
                returnedFilament = completeFilamentMapByID.get(filamentID.replaceAll("-", "_"));
            }
        }
        return returnedFilament;
    }

    /**
     * Add the filament to the user filament list but do not save it to disk.
     */
    public void addFilamentToUserFilamentList(Filament filament)
    {
        userFilamentList.add(filament);
        completeFilamentList.add(filament);
        completeFilamentMapByID.put(filament.getFilamentID(), filament);
        completeFilamentNameByID.put(filament.getFilamentID(), filament.getFriendlyFilamentName());
    }

    public ObservableList<Filament> getCompleteFilamentList()
    {
        return completeFilamentList;
    }

    public ObservableList<Filament> getUserFilamentList()
    {
        return userFilamentList;
    }

    public ObservableList<Filament> getAppFilamentList()
    {
        return appFilamentList;
    }

    /**
     * For testing only.
     */
    protected void reload()
    {
        loadFilamentData();
    }
}
