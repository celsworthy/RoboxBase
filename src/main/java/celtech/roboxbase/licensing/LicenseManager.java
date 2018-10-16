package celtech.roboxbase.licensing;

import celtech.roboxbase.ApplicationFeature;
import celtech.roboxbase.BaseLookup;
import celtech.roboxbase.comms.LicenseCheckResult;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.printerControl.model.Printer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.collections.ObservableList;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.bind.DatatypeConverter;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author George Salter
 */
public class LicenseManager {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(LicenseManager.class.getName());
    
    private static final String KEY_TO_THE_CRYPT = "4893AF234EEF124326DDE98ED93284BB";
    
    private static final String END_DATE_KEY = "END_DATE";
    private static final String PRINTER_ID_KEY = "PRINTER_ID";
    private static final String LICENSE_TYPE_KEY = "LICENSE_TYPE";
    
    public LicenseCheckResult checkEncryptedLicenseFileValid(File encryptedLicenseFile, boolean cacheFile) {
        STENO.trace("Begining check of encrypted license file");
        
        boolean licenseFileValid;
        String encryptedText = "";
        
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(encryptedLicenseFile))) {
            StringBuilder stringBuilder = new StringBuilder();
            String line = bufferedReader.readLine();
            while(line != null) {
                stringBuilder.append(line);
                stringBuilder.append("\n");
                line = bufferedReader.readLine();
            }
            encryptedText = stringBuilder.toString();
        } catch (IOException ex) {
            STENO.exception("Unexpected exception while trying to read license file", ex);
        }
        
        String licenseText = decrypt(encryptedText, KEY_TO_THE_CRYPT);
        String[] licenseInfo = licenseText.split("\\r?\\n");
        
        String licenseEndDate = "";
        List<String> printerIds = new ArrayList<>();
        LicenseType licenseType = LicenseType.AUTOMAKER_FREE;
        
        for (String licenseLine : licenseInfo) {
            String[] lineInfo = licenseLine.split(":");
            String licenseInfoKey = lineInfo[0];
            String licenseInfoValue = lineInfo[1];
            
            switch(licenseInfoKey) {
                case END_DATE_KEY:
                    licenseEndDate = licenseInfoValue;
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
        
        boolean licenseStillInDate = checkLicenseActive(licenseEndDate);
        
        ObservableList<Printer> printers = BaseLookup.getConnectedPrinters();
        
        //True if the list of registered printers on the license matches any that are connected.
        boolean printerIdMatch = printers.stream().anyMatch(
                (printer) -> (printerIds.contains(printer.getPrinterIdentity().toString())));
        
        licenseFileValid = printerIdMatch && licenseStillInDate;
        
        if(licenseFileValid) {
            if(cacheFile) {
                cacheLicenseFile(encryptedLicenseFile);
            }
            
            enableApplicationFeaturesBasedOnLicenseType(licenseType);
            
            return LicenseCheckResult.LICENSE_VALID;
        }
        
        return LicenseCheckResult.LICENSE_NOT_VALID;
    }
    
    public LicenseCheckResult checkCachedLicenseFile() {
        File licenseFile = tryAndGetCachedLicenseFile();
        if(licenseFile.exists()) {
            return checkEncryptedLicenseFileValid(licenseFile, false);
        }
        
        return LicenseCheckResult.NO_CACHED_LICENSE;
    }
    
    // TODO: Move this to a shared class
    private static String decrypt(final String ivAndEncryptedMessageBase64, final String symKeyHex) {
        final byte[] symKeyData = DatatypeConverter.parseHexBinary(symKeyHex);

        final byte[] ivAndEncryptedMessage = DatatypeConverter
                .parseBase64Binary(ivAndEncryptedMessageBase64);
        try 
        {
            final Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // create the key
            final SecretKeySpec symKey = new SecretKeySpec(symKeyData, "AES");

            // retrieve random IV from start of the received message
            final byte[] ivData = new byte[blockSize];
            System.arraycopy(ivAndEncryptedMessage, 0, ivData, 0, blockSize);
            final IvParameterSpec iv = new IvParameterSpec(ivData);

            // retrieve the encrypted message itself
            final byte[] encryptedMessage = new byte[ivAndEncryptedMessage.length
                    - blockSize];
            System.arraycopy(ivAndEncryptedMessage, blockSize,
                    encryptedMessage, 0, encryptedMessage.length);

            cipher.init(Cipher.DECRYPT_MODE, symKey, iv);

            final byte[] encodedMessage = cipher.doFinal(encryptedMessage);

            // concatenate IV and encrypted message
            final String message = new String(encodedMessage,
                                              Charset.forName("UTF-8"));

            return message;
        }
        catch (InvalidKeyException e) {
            throw new IllegalArgumentException("key argument does not contain a valid AES key");
        }
        catch (BadPaddingException e) {
            // you'd better know about padding oracle attacks
            return null;
        }
        catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unexpected exception during decryption", e);
        }
    }
    
    private void cacheLicenseFile(File encryptedLicenseFile) {
        File cachedLicense = tryAndGetCachedLicenseFile();
        try {
            Files.copy(encryptedLicenseFile.toPath(), cachedLicense.toPath(), StandardCopyOption.REPLACE_EXISTING);
            STENO.debug("License file cached");
        } catch (IOException ex) {
            STENO.exception("Exception when caching license file", ex);
        }
    }
    
    private boolean checkLicenseActive(String endDate) {
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE;
        LocalDate localDate = LocalDate.now();
        LocalDate licenseEndDate = LocalDate.parse(endDate, dtf);
        return localDate.isBefore(licenseEndDate);
    }
    
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
        } else if(licenseType.equals(LicenseType.AUTOMAKER_FREE)) {
            STENO.info("License type of Automaker Free, enabling standard features");
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.LATEST_CURA_VERSION);
            BaseConfiguration.disableApplicationFeature(ApplicationFeature.GCODE_VISUALISATION);
        }
    }
}
