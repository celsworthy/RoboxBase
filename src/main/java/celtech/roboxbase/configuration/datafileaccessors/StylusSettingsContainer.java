package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.HeadFileFilter;
import celtech.roboxbase.configuration.fileRepresentation.HeadFile;
import celtech.roboxbase.configuration.fileRepresentation.StylusSettings;
import celtech.roboxbase.printerControl.model.Head.HeadType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.IntStream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class StylusSettingsContainer
{

    private static final Stenographer steno = StenographerFactory.getStenographer(StylusSettingsContainer.class.getName());
    private static StylusSettingsContainer instance = null;
    private final ObservableList<StylusSettings> completeStylusSettingsList;
    private final ObservableMap<String, StylusSettings> completeStylusSettingsMap = FXCollections.observableHashMap();
    private final ObjectMapper mapper = new ObjectMapper();

    private StylusSettingsContainer()
    {
        FileFilter filter = (p -> p.getName().endsWith(BaseConfiguration.stylusSettingsFileExtension));
        File settingsDirHandle = new File(BaseConfiguration.getApplicationStylusSettingsDirectory());
        File[] stylusSettingFiles = settingsDirHandle.listFiles(filter);
        ingestSettingsFiles(stylusSettingFiles);
        settingsDirHandle = new File(BaseConfiguration.getUserStylusSettingsDirectory());
        stylusSettingFiles = settingsDirHandle.listFiles(filter);
        ingestSettingsFiles(stylusSettingFiles);
        completeStylusSettingsList = FXCollections.observableArrayList(completeStylusSettingsMap.values());
        Collections.sort(completeStylusSettingsList, (s1, s2) -> s1.getName().compareToIgnoreCase(s2.getName()));
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    private void ingestSettingsFiles(File[] settingsFiles)
    {
        if (settingsFiles != null)
        {
            for (File settingsFile : settingsFiles)
            {
                try
                {
                    StylusSettings settingsData = mapper.readValue(settingsFile, StylusSettings.class);
                    completeStylusSettingsMap.put(settingsData.getName(), settingsData);

                } catch (IOException ex)
                {
                    steno.error("Error loading stylus settings from " + settingsFile.getAbsolutePath());
                }
            }
        }
    }

    private static void createInstance()
    {
        if (instance == null)
            instance = new StylusSettingsContainer();
    }

    public static StylusSettingsContainer getInstance()
    {
        createInstance();
        return instance;
    }

    public static Optional<StylusSettings> getSettingsByName(String settingsName)
    {
        createInstance();
        StylusSettings namedSettings = instance.completeStylusSettingsMap.getOrDefault(settingsName, null);
        return Optional.ofNullable(namedSettings);
    }

    public static ObservableList<StylusSettings> getCompleteSettingsList()
    {
        createInstance();
        return instance.completeStylusSettingsList;
    }
    
    private String sanitizeFilename(String inputName)
    {
        return inputName.replaceAll("[^a-zA-Z0-9-_\\.]", "_");
    }
    
    public void saveSettings(StylusSettings settingsData)
    {
        checkUserDirectory();
        writeSettingsToFile(settingsData);
        String sName = settingsData.getName();
        completeStylusSettingsMap.put(sName, settingsData);
        int comparison = -1;
        int sIndex = -1;
        for (sIndex = 0; comparison < 0 && sIndex < completeStylusSettingsList.size(); ++sIndex)
            comparison = sName.compareToIgnoreCase(completeStylusSettingsList.get(sIndex).getName());
        if (comparison == 0)
            completeStylusSettingsList.set(sIndex, settingsData);
        else if (comparison > 0)
            completeStylusSettingsList.add(sIndex, settingsData);
        else
            completeStylusSettingsList.add(settingsData);
    }

    public void saveAllSettings()
    {
        checkUserDirectory();
        completeStylusSettingsList.stream()
                                  .filter(StylusSettings::isModified)
                                  .forEach(ss -> writeSettingsToFile(ss));
    }

    private void checkUserDirectory()
    {
        String userDirectory = BaseConfiguration.getUserStylusSettingsDirectory();
        File dirHandle = new File(userDirectory);
        if (!dirHandle.exists())
            dirHandle.mkdirs();
    }

    private void writeSettingsToFile(StylusSettings settingsData)
    {
        String userFilePath = BaseConfiguration.getUserStylusSettingsDirectory()
                                + File.separator
                                + sanitizeFilename(settingsData.getName());
        try
        {
            File userFile = new File(userFilePath);
            mapper.writeValue(userFile, settingsData);
        }
        catch (IOException ex)
        {
            steno.error("Error trying to user stylus settings to \"" + userFilePath + "\"");
        }
    }
}
