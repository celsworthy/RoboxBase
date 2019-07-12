/*
 * Copyright 2015 CEL UK
 */
package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.configuration.fileRepresentation.StylusSettings;
import celtech.roboxbase.utils.BaseEnvironmentConfiguredTest;
import java.io.File;
import java.util.Optional;
import javafx.collections.ObservableList;
import org.junit.Assert;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Before;

/**
 *
 * @author tony
 */
public class StylusSettingsContainerTest extends BaseEnvironmentConfiguredTest
{
    @Test
    public void testLoadStylusSettings()
    {
        ObservableList<StylusSettings> stylusSettings = StylusSettingsContainer.getCompleteSettingsList();
        assertEquals(2, stylusSettings.size());
        
        Optional<StylusSettings> ssOpt = StylusSettingsContainer.getSettingsByName("NotPresent");
        assertTrue(ssOpt.isEmpty());
        ssOpt = StylusSettingsContainer.getSettingsByName("Biro");
        assertTrue(ssOpt.isPresent());
    }    
}
