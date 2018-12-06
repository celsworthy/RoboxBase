package celtech.roboxbase.licensing;

import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.printerControl.model.Head;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterListChangesListener;
import celtech.roboxbase.printerControl.model.Reel;
import com.google.common.io.ByteStreams;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javafx.collections.ObservableList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.codec.binary.Base64;

/**
 *
 * @author George Salter
 */
public class LicenseManager {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(LicenseManager.class.getName());
    
    private static LicenseManager instance;
    //private static final String KEY_TO_THE_CRYPT = "4893AF234EEF124326DDE98ED93284BB";
    
    private static final String OWNER_KEY = "OWNER";
    private static final String END_DATE_KEY = "END_DATE";
    private static final String PRINTER_ID_KEY = "PRINTER_ID";
    private static final String LICENSE_TYPE_KEY = "LICENSE_TYPE";
    
    private static final List<LicenseChangeListener> LICENSE_CHANGE_LISTENERS = new ArrayList<>();
    
    /**
     * Class is singleton
     * Do not allow instantiation outside of class.
     */
    private LicenseManager() {
        BaseLookup.getPrinterListChangesNotifier().addListener(new LicenseValidator());
    }
    
    public static LicenseManager getInstance() {
        if(instance == null) {
            instance = new LicenseManager();
        }
        return instance;
    }
    
    private boolean validateLicense() {
        Optional<License> potentialLicence = readCachedLicenseFile();
        if(potentialLicence.isPresent()) {
            return validateLicense(potentialLicence.get(), true);
        }
        
        return BaseLookup.getSystemNotificationHandler().showSelectLicenseDialogue();
        
        // What to do if license is not valid? Generate free license?
    }
    
    public boolean validateLicense(License license, boolean activateLicense) {
        if(licenseContainsAConnectedPrinter(license)) {
            if (isLicenseFreeVersion(license) || license.checkLicenseActive()) {
                if(activateLicense) {
                    enableApplicationFeaturesBasedOnLicenseType(license.getLicenseType());
                    LICENSE_CHANGE_LISTENERS.forEach(listener -> listener.onLicenseChange(license));
                }
                return true;
            }
        }
        
        return false;
    }
    
    public boolean checkEncryptedLicenseFileValid(File encryptedLicenseFile, boolean cacheFile, boolean activateLicense) {
        boolean licenseFileValid = false;
        Optional<License> potentialLicense = readEncryptedLicenseFile(encryptedLicenseFile);
        if(potentialLicense.isPresent()) {
            licenseFileValid = validateLicense(potentialLicense.get(), activateLicense);
        }
        
        if(licenseFileValid && cacheFile) {
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
    public Optional<License> readCachedLicenseFile() {
        File licenseFile = tryAndGetCachedLicenseFile();
        if(licenseFile.exists()) {
            STENO.debug("Reading cached license file");
            return readEncryptedLicenseFile(licenseFile);
        }
        
        STENO.debug("There is no cached license");
        return Optional.empty();
    }
    
    /**
     * Takes a file and copies it into APPDATA as a License file
     * 
     * @param encryptedLicenseFile file to be cached
     */
    private void cacheLicenseFile(File encryptedLicenseFile) {
        File cachedLicense = tryAndGetCachedLicenseFile();
        try {
            Files.copy(encryptedLicenseFile.toPath(), cachedLicense.toPath(), StandardCopyOption.REPLACE_EXISTING);
            STENO.debug("License file cached");
        } catch (IOException ex) {
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
    private File tryAndGetCachedLicenseFile() {
        File licenseDir = new File(BaseConfiguration.getApplicationStorageDirectory() 
                + BaseConfiguration.LICENSE_SUB_PATH);
        if(!licenseDir.exists()) {
            STENO.debug("License directory does not exist. Creating license directory here: " + licenseDir.getPath());
            licenseDir.mkdir();
        }
        File cachedLicense = new File(licenseDir.getPath() + "/automaker.lic");
        return cachedLicense;
    }
    
    private PublicKey getPublic() {
        try {
            InputStream in = getClass().getResourceAsStream("/celtech/resources/keys/publicKey");
            byte[] keyBytes = ByteStreams.toByteArray(in);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException | IOException ex) {
            STENO.exception("An error occured when getting the public key", ex);
        }
        return null;
    }
    
    public Optional<License> readEncryptedLicenseFile(File encryptedLicenseFile) {
        STENO.trace("Begining read of encrypted license file");
        
        String licenseText;
        
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(encryptedLicenseFile))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            PublicKey publicKey = getPublic();
            while(line != null) {
                stringBuilder.append(decryptLine(line, publicKey));
                stringBuilder.append("\n");
                line = bufferedReader.readLine();
            }
            licenseText = stringBuilder.toString();
        } catch (IOException ex) {
            STENO.exception("Unexpected exception while trying to read license file", ex);
            return Optional.empty();
        }
        
        //byte[] decryptedKey = decryptKey(encryptedKey, getPublic(publicKeyPath));
        //SecretKey symmetricKey = new SecretKeySpec(decryptedKey, 0, decryptedKey.length, "AES");

        //String licenseText = decryptText(encryptedText, symmetricKey);
        
        if(licenseText == null) {
            return Optional.empty();
        }
        String[] licenseInfo = licenseText.split("\\r?\\n");
        
        String owner = "";
        String licenseEndDateString = "";
        List<String> printerIds = new ArrayList<>();
        LicenseType licenseType = LicenseType.AUTOMAKER_FREE;
        
        for (String licenseLine : licenseInfo) {
            String[] lineInfo = licenseLine.split(":");
            String licenseInfoKey = lineInfo[0];
            String licenseInfoValue = lineInfo[1];
            
            switch(licenseInfoKey) {
                case OWNER_KEY:
                    owner = licenseInfoValue;
                    break;
                case END_DATE_KEY:
                    licenseEndDateString = licenseInfoValue;
                    break;
                case PRINTER_ID_KEY:
                    printerIds.add(licenseInfoValue);
                    break;
                case LICENSE_TYPE_KEY:
                    if(licenseInfoValue.equals(LicenseType.AUTOMAKER_FREE.toString())) {
                        licenseType = LicenseType.AUTOMAKER_FREE;
                    } else if(licenseInfoValue.equals(LicenseType.AUTOMAKER_PRO.toString())) {
                        licenseType = LicenseType.AUTOMAKER_PRO;
                    }
            }
        }
        
        LocalDate licenseEndDate = parseDate(licenseEndDateString);
        
        License license = new License(licenseType, licenseEndDate, owner, printerIds);
        STENO.debug("License file read with type of: " + license.getLicenseType());
        return Optional.of(license);
    }
    
//    private byte[] decryptKey(String encryptedKey, PublicKey key) {
//        try {
//            Cipher cipher = Cipher.getInstance("RSA");
//            cipher.init(Cipher.DECRYPT_MODE, key);
//            return cipher.doFinal(Base64.decodeBase64(encryptedKey));
//        } catch (NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException ex) {
//            STENO.exception("Error when obtaining cipher instance.", ex);
//        } catch (InvalidKeyException ex) {
//            STENO.exception("Error occured when decrypting license.", ex);
//        }
//        return null;
//    }
    
    private String decryptLine(String encryptedLine, PublicKey key) {
        try {
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, key);
            return new String(cipher.doFinal(Base64.decodeBase64(encryptedLine)), "UTF-8");
        } catch (NoSuchAlgorithmException 
                | NoSuchPaddingException 
                | IllegalBlockSizeException 
                | BadPaddingException 
                | UnsupportedEncodingException ex) {
            STENO.exception("Error when obtaining cipher instance.", ex);
        } catch (InvalidKeyException ex) {
            STENO.exception("Error occured when decrypting license.", ex);
        }
        return null;
    }
    
//    private static String decryptText(final String ivAndEncryptedMessageBase64, final SecretKey symmetricKey) {
//
//        final byte[] ivAndEncryptedMessage = DatatypeConverter
//                .parseBase64Binary(ivAndEncryptedMessageBase64);
//        try 
//        {
//            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
//            final int blockSize = cipher.getBlockSize();
//
//            // retrieve random IV from start of the received message
//            final byte[] ivData = new byte[blockSize];
//            System.arraycopy(ivAndEncryptedMessage, 0, ivData, 0, blockSize);
//            final IvParameterSpec iv = new IvParameterSpec(ivData);
//
//            // retrieve the encrypted message itself
//            final byte[] encryptedMessage = new byte[ivAndEncryptedMessage.length
//                    - blockSize];
//            System.arraycopy(ivAndEncryptedMessage, blockSize,
//                    encryptedMessage, 0, encryptedMessage.length);
//
//            cipher.init(Cipher.DECRYPT_MODE, symmetricKey, iv);
//
//            final byte[] encodedMessage = cipher.doFinal(encryptedMessage);
//
//            // concatenate IV and encrypted message
//            final String message = new String(encodedMessage, Charset.forName("UTF-8"));
//
//            return message;
//        } catch (GeneralSecurityException ex) {
//            STENO.exception("Error occured during decryption of license file.", ex);
//            return null;
//        }
//    }
        
    /**
     * Turn a date String in the form of yyyy-MM-dd into a {@link LocalDate}
     *
     * @param date date in the form of a String
     * @return
     */
    private LocalDate parseDate(String date) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE;
        LocalDate parsedDate = LocalDate.parse(date, dtf);
        return parsedDate;
    }
    
    /**
     * Check if the a license contains the id of a connected printer
     * 
     * @param license
     * @return 
     */
    private boolean licenseContainsAConnectedPrinter(License license) {
        ObservableList<Printer> printers = BaseLookup.getConnectedPrinters();
        //True if the list of registered printers on the license matches any that are connected.
        boolean printerIdMatch = printers.stream().anyMatch(
                (printer) -> (license.containsPrinterId(printer.getPrinterIdentity().toString())));
        return printerIdMatch;
    }
    
    /**
     * Check if the license is a free license
     * 
     * @param license
     * @return 
     */
    private boolean isLicenseFreeVersion(License license) {
        return license.getLicenseType() == LicenseType.AUTOMAKER_FREE;
    }
    
    /**
     * Enable and disable application features based on the type of license
     * 
     * @param licenseType 
     */
    private void enableApplicationFeaturesBasedOnLicenseType(LicenseType licenseType) {
        if(licenseType.equals(LicenseType.AUTOMAKER_PRO)) {
            STENO.info("License type of Automaker Pro, enabling associated features");
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.LATEST_CURA_VERSION);
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.GCODE_VISUALISATION);
            BaseConfiguration.enableApplicationFeature(ApplicationFeature.OFFLINE_PRINTER);
        } else if(licenseType.equals(LicenseType.AUTOMAKER_FREE)) {
            STENO.info("License type of Automaker Free, enabling standard features");
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.LATEST_CURA_VERSION);
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.GCODE_VISUALISATION);
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.OFFLINE_PRINTER);
        }
    }
    
    public void addLicenseChangeListener(LicenseChangeListener licenseChangeListener) {
        LICENSE_CHANGE_LISTENERS.add(licenseChangeListener);
    }
    
    public void removeLicenseChangeListener(LicenseChangeListener licenseChangeListener) {
        LICENSE_CHANGE_LISTENERS.remove(licenseChangeListener);
    }
        
    /**
     * Interface for a listener that is used when the license has changed
     */
    public static interface LicenseChangeListener {
    
        /**
         * Called when the license has been changed
         * 
         * @param license the new license
         */
        void onLicenseChange(License license);
    }

    /**
     * Listener to validate license when a printer is connected
     */
    private class LicenseValidator implements PrinterListChangesListener {
        
        @Override
        public void whenPrinterAdded(Printer printer) {
            validateLicense();
        }

        @Override
        public void whenPrinterRemoved(Printer printer) {}

        @Override
        public void whenHeadAdded(Printer printer) {}

        @Override
        public void whenHeadRemoved(Printer printer, Head head) {}

        @Override
        public void whenReelAdded(Printer printer, int reelIndex) {}

        @Override
        public void whenReelRemoved(Printer printer, Reel reel, int reelIndex) {}

        @Override
        public void whenReelChanged(Printer printer, Reel reel) {}

        @Override
        public void whenExtruderAdded(Printer printer, int extruderIndex) {}

        @Override
        public void whenExtruderRemoved(Printer printer, int extruderIndex) {}
    }
}
