package celtech.roboxbase.licensing;

import celtech.roboxbase.configuration.BaseConfiguration;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Optional;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author George Salter
 */
public class NoHardwareLicenseTimer {
    
    private static final Stenographer STENO = StenographerFactory.getStenographer(NoHardwareLicenseTimer.class.getName());
    
    private static final String TIMER_FILE_PATH = BaseConfiguration.getApplicationStorageDirectory() 
                + BaseConfiguration.LICENSE_SUB_PATH + "/timer.lic";
    
    private static NoHardwareLicenseTimer instance;
    
    /**
     * Class is singleton
     * Do not allow instantiation outside of class.
     */
    private NoHardwareLicenseTimer() {}
    
    public static NoHardwareLicenseTimer getInstance() {
        if(instance == null) {
            instance = new NoHardwareLicenseTimer();
        }
        return instance;
    }
    
    public boolean resetNoHardwareLicenseTimer() {
        LocalDate newCutOffDate = LocalDate.now();
        return saveCutoffDate(newCutOffDate);
    }

    
    public boolean hasHardwareBeenCheckedInLast(int days) {
        Optional<LocalDate> lastLicenseCheck = checkDateOfLastHardwareCheck();
        if(lastLicenseCheck.isPresent()) {
            LocalDate cutOffDate = LocalDate.now().minusDays(days);
            return cutOffDate.isBefore(lastLicenseCheck.get());
        } else {
            return false;
        }
    }
    
    private Optional<LocalDate> checkDateOfLastHardwareCheck() {
        File timerFile = new File(TIMER_FILE_PATH);
        if(timerFile.exists()) {
            try {
                byte[] encryptedDate = FileUtils.readFileToByteArray(timerFile);
                Optional<String> unencryptedDateString = unencryptCutOffDate(encryptedDate);
                if(unencryptedDateString.isPresent()) {
                    LocalDate cutOffDate = LocalDate.parse(unencryptedDateString.get(), DateTimeFormatter.ISO_DATE);
                    return Optional.of(cutOffDate);
                }
            } catch (IOException ex) {
                STENO.exception("Exception when reading timer file", ex);
                return Optional.empty();
            }
        }
        
        return Optional.empty();
    }
    
    private boolean saveCutoffDate(LocalDate cutOffDate) {
        File timerFile = new File(TIMER_FILE_PATH);
        byte[] encryptedCuttOffDate = encryptCutOffDate(cutOffDate);
        try {
            FileUtils.writeByteArrayToFile(timerFile, encryptedCuttOffDate);
            return true;
        } catch (IOException ex) {
            STENO.exception("Exception when writing cut off date", ex);
            return false;
        }
    }
    
    private byte[] encryptCutOffDate(LocalDate cutOffDate) {
        final byte[] encodedMessage = cutOffDate.format(DateTimeFormatter.ISO_DATE)
                .getBytes(Charset.forName("UTF-8"));
      
        try {
            final Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();
            
            // generate random IV using block size
            final byte[] ivData = new byte[blockSize];
            final SecureRandom rnd = SecureRandom.getInstance("SHA1PRNG");
            rnd.nextBytes(ivData);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, ivData);

            cipher.init(Cipher.ENCRYPT_MODE, createSecretKeyFromMac(), parameterSpec);

            final byte[] encryptedMessage = cipher.doFinal(encodedMessage);

            // concatenate IV and encrypted message
            final byte[] ivAndEncryptedMessage = new byte[ivData.length
                    + encryptedMessage.length];
            System.arraycopy(ivData, 0, ivAndEncryptedMessage, 0, blockSize);
            System.arraycopy(encryptedMessage, 0, ivAndEncryptedMessage,
                    blockSize, encryptedMessage.length);

            return ivAndEncryptedMessage;
        } catch (GeneralSecurityException | IOException  e) {
            STENO.exception("Unexpected exception while encrypting timer file", e);
            return new byte[0];
        }
    }
    
    private Optional<String> unencryptCutOffDate(byte[] encryptedDate) {
        try 
        {
            final Cipher cipher = Cipher.getInstance("AES/GCM/PKCS5Padding");
            final int blockSize = cipher.getBlockSize();

            // retrieve random IV from start of the received message
            final byte[] ivData = new byte[blockSize];
            System.arraycopy(encryptedDate, 0, ivData, 0, blockSize);
            GCMParameterSpec parameterSpec = new GCMParameterSpec(128, ivData);

            // retrieve the encrypted message itself
            final byte[] encryptedMessage = new byte[encryptedDate.length - blockSize];
            System.arraycopy(encryptedDate, blockSize,
                    encryptedMessage, 0, encryptedMessage.length);

            cipher.init(Cipher.DECRYPT_MODE, createSecretKeyFromMac(), parameterSpec);

            final byte[] encodedMessage = cipher.doFinal(encryptedMessage);

            // concatenate IV and encrypted message
            final String message = new String(encodedMessage, Charset.forName("UTF-8"));

            return Optional.of(message);
        } catch (GeneralSecurityException | IOException ex) {
            STENO.exception("Error occured during decryption of timer file", ex);
            return Optional.empty();
        }
    }
    
    private SecretKey createSecretKeyFromMac() throws NoSuchAlgorithmException, IOException {
        byte[] macAddress = determineMacAddress();
        MessageDigest sha = MessageDigest.getInstance("SHA-1");
        byte[] macAddressKey = sha.digest(macAddress);
        macAddressKey = Arrays.copyOf(macAddressKey, 16);
        SecretKey secretKey = new SecretKeySpec(macAddressKey, "AES");
        return secretKey;
    }
    
    private byte[] determineMacAddress() throws IOException {
        InetAddress ip;	
        ip = InetAddress.getLocalHost();
        NetworkInterface network = NetworkInterface.getByInetAddress(ip);
        return network.getHardwareAddress();	
    }
}
