package celtech.roboxbase.licensing;

import java.time.LocalDate;
import java.util.List;

/**
 * In code representation of the License, created by the {@link LicenseManager}
 * 
 * @author George Salter
 */
public class License {
    
    private final LicenseType licenseType;
    
    private final LocalDate endDate;
    
    private final List<String> printerIds;
    
    public License(LicenseType licenseType, LocalDate endDate, List<String> printerIds) {
        this.licenseType = licenseType;
        this.endDate = endDate;
        this.printerIds = printerIds;
    }
    
    public boolean checkLicenseActive() {
        LocalDate localDate = LocalDate.now();
        return localDate.isBefore(endDate);
    }
    
    public boolean containsPrinterId(String printerId) {
        return printerIds.contains(printerId);
    }
    
    public String getFriendlyLicenseType() {
        if(licenseType == LicenseType.AUTOMAKER_PRO) {
            return "AutoMaker Pro";
        } else {
            return "AutoMaker Free";
        } 
    }

    public LicenseType getLicenseType() {
        return licenseType;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public List<String> getPrinterIds() {
        return printerIds;
    }
    
    @Override
    public String toString() {
        return getFriendlyLicenseType() + " - Expires: " + endDate.toString();
    }
}
