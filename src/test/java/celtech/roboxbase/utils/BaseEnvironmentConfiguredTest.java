/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.utils;

import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.appManager.TestSystemNotificationManager;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.datafileaccessors.SlicerParametersContainer;
import celtech.roboxbase.postprocessor.TestGCodeOutputWriter;
import celtech.roboxbase.utils.tasks.TestTaskExecutor;
import java.io.File;
import java.net.URL;
import java.util.Properties;
import javafx.application.Application;
import javafx.stage.Stage;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class BaseEnvironmentConfiguredTest
{

    @Rule
    public TemporaryFolder temporaryUserStorageFolder = new TemporaryFolder();
    public String userStorageFolderPath;

    @Before
    public void setUp()
    {
        Properties testProperties = new Properties();

        testProperties.setProperty("language", "UK");
        URL applicationInstallURL = BaseEnvironmentConfiguredTest.class.getResource("/InstallDir/AutoMaker/");
        userStorageFolderPath = temporaryUserStorageFolder.getRoot().getAbsolutePath()
            + File.separator;
        BaseConfiguration.setInstallationProperties(
            testProperties,
            applicationInstallURL.getFile(),
            userStorageFolderPath);
        
        File filamentDir = new File(userStorageFolderPath
            + BaseConfiguration.filamentDirectoryPath
            + File.separator);
        filamentDir.mkdirs();
        
        new File(userStorageFolderPath
            + BaseConfiguration.printSpoolStorageDirectoryPath
            + File.separator).mkdirs();

        BaseLookup.setupDefaultValues();

        // force initialisation
        URL configURL = BaseEnvironmentConfiguredTest.class.getResource("/Base.configFile.xml");
        System.setProperty("libertySystems.configFile", configURL.getFile());
        String installDir = BaseConfiguration.getApplicationInstallDirectory(
            BaseLookup.class);
        SlicerParametersContainer.getInstance();

        BaseLookup.setTaskExecutor(new TestTaskExecutor());
        BaseLookup.setSystemNotificationHandler(new TestSystemNotificationManager());

        BaseLookup.setPostProcessorOutputWriterFactory(TestGCodeOutputWriter::new);
    }

    public static class AsNonApp extends Application
    {

        @Override
        public void start(Stage primaryStage) throws Exception
        {
            // noop
        }
    }

    public static boolean startedJFX = false;

    @BeforeClass
    public static void initJFX()
    {
        if (!startedJFX)
        {
            Thread t = new Thread("JavaFX Init Thread")
            {
                public void run()
                {
                    Application.launch(AsNonApp.class, new String[0]);
                }
            };
            t.setDaemon(true);
            t.start();
            startedJFX = true;
        }
    }

}
