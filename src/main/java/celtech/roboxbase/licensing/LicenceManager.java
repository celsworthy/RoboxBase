package celtech.roboxbase.licensing;

import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.appManager.NotificationType;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.licence.Licence;
import celtech.roboxbase.licence.LicenceType;
import celtech.roboxbase.licence.LicenceUtilities;
import celtech.roboxbase.licence.NoHardwareLicenceTimer;
import celtech.roboxbase.printerControl.model.Printer;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 * Singleton class providing methods for validating and caching licenses and in turn
 * enabling and disabling features in Automaker
 * 
 * @author George Salter
 */
public class LicenceManager
{
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(LicenceManager.class.getName());
    
    private static final int FIFTEEN_DAYS = 15;
    private static final List<LicenceChangeListener> LICENSE_CHANGE_LISTENERS = new ArrayList<>();
    
    private static boolean proLicenceActiveNotified = false;

    private boolean startupTimeElapsed = false;
    
    /**
     * Class is singleton
     * Do not allow instantiation outside of class.
     */
    private LicenceManager() 
    {
        // On first initialisation we start a timer.
        // The TimerTask will call validate licence, which allows AutoMaker to start
        // without licence dialogs until the first wave of printers is connected.
        TimerTask validateLicenceTask = new TimerTask() 
        {    
            @Override
            public void run()
            {
                Platform.runLater(() -> {
                    startupTimeElapsed = true;
                    validateLicence(true);
                });
            }
        };
        
        Timer timer = new Timer("ValidateLicenceTimer", true);
        long delay = 30000L; // 30 seconds
        timer.schedule(validateLicenceTask, delay);
    }
    
    /**
     * Loaded when first accessed by {@link LicenceManager#getInstance()}  providing a
     * thread safe way to lazy initialise the singleton.
     */
    private static class Holder
    {
        private static final LicenceManager INSTANCE = new LicenceManager();
    }
    
    public static LicenceManager getInstance() 
    {
        return Holder.INSTANCE;
    }
    
    public synchronized boolean validateLicence(boolean canDisplayDialogs) 
    {
        boolean valid = false;
        Optional<Licence> potentialLicence = readCachedLicenseFile();
        if(potentialLicence.isPresent()) 
        {
            valid = validateLicense(potentialLicence.get(), true, canDisplayDialogs);
        }
        else
        {
            enableApplicationFeaturesBasedOnLicenseType(LicenceType.AUTOMAKER_FREE);
            Optional<Licence> licenceOption = Optional.empty();
            LICENSE_CHANGE_LISTENERS.forEach(listener -> listener.onLicenceChange(licenceOption));
        }
        
        return valid;
    }
    
    public synchronized boolean validateLicense(Licence license, boolean activateLicense, boolean canDisplayDialogs) 
    {
        NoHardwareLicenceTimer.getInstance().setTimerFilePath(BaseConfiguration.getApplicationStorageDirectory() +
                                                BaseConfiguration.LICENSE_SUB_PATH +
                                                "/timer.lic");
        boolean isLicenseWithoutHardwareAllowed = NoHardwareLicenceTimer.getInstance().hasHardwareBeenCheckedInLast(FIFTEEN_DAYS);
        // I think perhaps we can remove this dialog, a notification should be enough
//        if(!isLicenseWithoutHardwareAllowed && canDisplayDialogs) 
//        {
//            BaseLookup.getSystemNotificationHandler().showConnectLicensedPrinterDialog();
//        }
        
        boolean isAssociatedPrinterConnected = doesLicenseContainAConnectedPrinter(license);
        
        if(isAssociatedPrinterConnected) 
        {
            isLicenseWithoutHardwareAllowed = NoHardwareLicenceTimer.getInstance().resetNoHardwareLicenceTimer();
        }
        
        if(license.checkLicenceInDate() || LicenceUtilities.isLicenceFreeVersion(license))
        {
            // We have a license that can be activated
            if(isAssociatedPrinterConnected || isLicenseWithoutHardwareAllowed || !startupTimeElapsed) 
            {
                if(activateLicense)
                {
                    enableApplicationFeaturesBasedOnLicenseType(license.getLicenceType());
                    Optional<Licence> licenceOption = Optional.of(license);
                    LICENSE_CHANGE_LISTENERS.forEach(listener -> listener.onLicenceChange(licenceOption));
                    
                    if (BaseLookup.getSystemNotificationHandler() != null && !proLicenceActiveNotified)
                    {
                        proLicenceActiveNotified = true;
                        BaseLookup.getSystemNotificationHandler().showInformationNotification(
                                BaseLookup.i18n("notification.licence.licenceTitle"),
                                BaseLookup.i18n("notification.licence.licenceActivated"));
                    }
                }
            }
            // We have a valid license but it is not active
            else
            {
                enableApplicationFeaturesBasedOnLicenseType(LicenceType.AUTOMAKER_FREE);
                Optional<Licence> licenceOption = Optional.of(license);
                LICENSE_CHANGE_LISTENERS.forEach(listener -> listener.onLicenceChange(licenceOption));
                if (BaseLookup.getSystemNotificationHandler() != null)
                {
                    BaseLookup.getSystemNotificationHandler().showDismissableNotification(
                                BaseLookup.i18n("notification.licence.connectLicencedPrinter"),
                                BaseLookup.i18n("notification.postProcessorFailure.dismiss"),
                                NotificationType.CAUTION);
                }
                proLicenceActiveNotified = false;
            }
            
            return true;
        }
        
        if (BaseLookup.getSystemNotificationHandler() != null)
        {
            BaseLookup.getSystemNotificationHandler().showDismissableNotification(
                        BaseLookup.i18n("notification.licence.licenceExpired"),
                        BaseLookup.i18n("notification.postProcessorFailure.dismiss"),
                        NotificationType.CAUTION);
        }
        
        proLicenceActiveNotified = false;
        return false;
    }
    
    public boolean checkEncryptedLicenseFileValid(File encryptedLicenseFile, boolean cacheFile, boolean activateLicense) 
    {
        boolean licenseFileValid = false;
        Optional<Licence> potentialLicense = LicenceUtilities.readEncryptedLicenceFile(encryptedLicenseFile);
        if(potentialLicense.isPresent()) 
        {
            licenseFileValid = validateLicense(potentialLicense.get(), activateLicense, false);
        }
        
        if(licenseFileValid && cacheFile) 
        {
            cacheLicenseFile(encryptedLicenseFile);
        }

        return licenseFileValid;
    }
    
    
    /**
     * If a cached License file exists it will return as an Optional,
     * if not an empty Optional is returned
     * 
     * @return 
     */
    public Optional<Licence> readCachedLicenseFile() {
        
        File licenseFile = tryAndGetCachedLicenseFile();
        if(licenseFile.exists()) 
        {
            STENO.debug("Reading cached license file");
            return LicenceUtilities.readEncryptedLicenceFile(licenseFile);
        }
        
        STENO.debug("There is no cached license");
        return Optional.empty();
    }
    
    /**
     * Takes a file and copies it into APPDATA as a License file
     * 
     * @param encryptedLicenseFile file to be cached
     */
    private void cacheLicenseFile(File encryptedLicenseFile) 
    {
        File cachedLicense = tryAndGetCachedLicenseFile();
        try 
        {
            Files.copy(encryptedLicenseFile.toPath(), cachedLicense.toPath(), StandardCopyOption.REPLACE_EXISTING);
            STENO.debug("License file cached");
        } 
        catch (IOException ex) 
        {
            STENO.exception("Exception when caching license file", ex);
        }
    }
    
    /**
     * Try and find a cached license file. Method will create License directory in 
     * APPDATA if it doesn't already exist. Will return a file with a full path ending
     * /License/automaker.lic although the File may not exist.
     * 
     * @return License File that may not exist
     */
    public File tryAndGetCachedLicenseFile() 
    {
        File licenseDir = new File(BaseConfiguration.getApplicationStorageDirectory() 
                + BaseConfiguration.LICENSE_SUB_PATH);
        if(!licenseDir.exists()) 
        {
            STENO.debug("License directory does not exist. Creating license directory here: " + licenseDir.getPath());
            licenseDir.mkdir();
        }
        File cachedLicense = new File(licenseDir.getPath() + "/automaker.lic");
        return cachedLicense;
    }
    
    /**
     * Check if the license contains the id of a connected printer
     * 
     * @param license
     * @return 
     */
    private boolean doesLicenseContainAConnectedPrinter(Licence license) 
    {
        ObservableList<Printer> printers = BaseLookup.getConnectedPrinters();
        //True if the list of registered printers on the license matches any that are connected.
        boolean printerIdMatch = printers.stream().anyMatch(
                (printer) -> (license.containsPrinterId(printer.getPrinterIdentity().toString())));
        return printerIdMatch;
    }
    
    /**
     * Enable and disable application features based on the type of license
     * 
     * @param licenseType 
     */
    private void enableApplicationFeaturesBasedOnLicenseType(LicenceType licenseType) 
    {
        if(licenseType.equals(LicenceType.AUTOMAKER_PRO)) 
        {
            STENO.info("License type of Automaker Pro, enabling associated features");
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.LATEST_CURA_VERSION);
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.GCODE_VISUALISATION);
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.OFFLINE_PRINTER);
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.PRO_SPLASH_SCREEN);
        } 
        else if(licenseType.equals(LicenceType.AUTOMAKER_FREE)) 
        {
            STENO.info("License type of Automaker Free, enabling standard features");
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.LATEST_CURA_VERSION);
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.GCODE_VISUALISATION);
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.OFFLINE_PRINTER);
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.PRO_SPLASH_SCREEN);
        }
    }

    public static void addLicenceChangeListener(LicenceChangeListener licenceChangeListener) 
    {
        LICENSE_CHANGE_LISTENERS.add(licenceChangeListener);
    }
    
    public static void removeLicenceChangeListener(LicenceChangeListener licenceChangeListener) 
    {
        LICENSE_CHANGE_LISTENERS.remove(licenceChangeListener);
    }
    
    public interface LicenceChangeListener 
    {
        /**
         * Called when the licence has been changed
         * 
         * @param licence the new licence
         */
        void onLicenceChange(Optional<Licence> licence);
    }
}
