/*
 * Copyright 2015 CEL UK
 */
package celtech.roboxbase.configuration.datafileaccessors;

import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.fileRepresentation.SlicerParametersFile;
import celtech.roboxbase.utils.BaseEnvironmentConfiguredTest;
import java.io.File;
import javafx.collections.ObservableList;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author tony
 */
public class SlicerParametersContainerTest extends BaseEnvironmentConfiguredTest
{

    @Before
    public void clearUserProfileDir()
    {
        String userProfileDir = BaseConfiguration.getUserPrintProfileDirectory();
        File[] files = new File(userProfileDir).listFiles();
        if (files != null && files.length > 0)
        {
            for (File profileFile : files)
            {
                profileFile.delete();
            }
        }
        SlicerParametersContainer.reload();
    }

    @Test
    public void testLoadProfiles()
    {
        SlicerParametersContainer.getInstance();
        assertEquals(18, SlicerParametersContainer.getApplicationProfileList().size());
        assertEquals(0, SlicerParametersContainer.getUserProfileList().size());
    }

    @Test
    public void testCreateNewProfile()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(BaseConfiguration.draftSettingsProfileName, "RBX01-SM");

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);
        assertEquals(0, userProfiles.size());

        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());
        assertEquals(19, completeProfiles.size());

        SlicerParametersFile retrievedProfile = SlicerParametersContainer.getSettings(NEW_NAME, "RBX01-SM");
        assertEquals(retrievedProfile, draftCopy);
    }

    @Test
    public void testCreateNewProfileAndDelete()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(
            BaseConfiguration.draftSettingsProfileName, "RBX01-SM");

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());

        SlicerParametersContainer.deleteUserProfile(NEW_NAME, "RBX01-SM");
        assertEquals(0, userProfiles.size());
        assertEquals(18, completeProfiles.size());
        SlicerParametersFile retrievedProfile = SlicerParametersContainer.getSettings(
            NEW_NAME, "RBX01-SM");
        Assert.assertNull(retrievedProfile);

    }

    @Test
    public void testCreateNewProfileAndChangeAndSave()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(BaseConfiguration.draftSettingsProfileName, "RBX01-SM");

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        draftCopy.setBrimWidth_mm(10);
        SlicerParametersContainer.saveProfile(draftCopy);

        SlicerParametersContainer.reload();
        SlicerParametersFile newEditedProfile = SlicerParametersContainer.getSettings(NEW_NAME, "RBX01-SM");
        assertEquals(10, newEditedProfile.getBrimWidth_mm());
        assertNotSame(draftCopy, newEditedProfile);

    }
    
    @Test
    public void testCreateNewProfileAndChangeNameAndSave()
    {
        String NEW_NAME = "draftCopy1";
        SlicerParametersContainer.getInstance();
        ObservableList<SlicerParametersFile> userProfiles = SlicerParametersContainer.getUserProfileList();
        ObservableList<SlicerParametersFile> completeProfiles = SlicerParametersContainer.getCompleteProfileList();
        SlicerParametersFile draftSlicerParametersFile = SlicerParametersContainer.getSettings(BaseConfiguration.draftSettingsProfileName, "RBX01-SM");

        SlicerParametersFile draftCopy = draftSlicerParametersFile.clone();
        draftCopy.setProfileName(NEW_NAME);

        SlicerParametersContainer.saveProfile(draftCopy);

        draftCopy.setBrimWidth_mm(5);
        String CHANGED_NAME = "draftCopy2";
        draftCopy.setProfileName(CHANGED_NAME);
        SlicerParametersContainer.saveProfile(draftCopy);
        assertEquals(1, userProfiles.size());
        assertEquals(19, completeProfiles.size());

        SlicerParametersContainer.reload();
        assertEquals(1, userProfiles.size());
        assertEquals(19, completeProfiles.size());        
        SlicerParametersFile newEditedProfile = SlicerParametersContainer.getSettings(
            CHANGED_NAME, "RBX01-SM");
        assertEquals(5, newEditedProfile.getBrimWidth_mm());
        assertNotSame(draftCopy, newEditedProfile);

    }    

}
