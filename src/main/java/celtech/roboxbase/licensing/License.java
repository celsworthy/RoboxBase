package celtech.roboxbase.licensing;

import java.time.LocalDate;
import java.util.List;

/**
 * In code representation of the License, created by the {@link LicenseManager}
 * 
 * @author George Salter
 */
public class License {
    
    private final String owner;
    
    private final LicenseType licenseType;
    
    private final LocalDate endDate;
    
    private final List<String> printerIds;
    
    public License(LicenseType licenseType, LocalDate endDate, String owner, List<String> printerIds) {
        this.licenseType = licenseType;
        this.endDate = endDate;
        this.owner = owner;
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
    
    public String getOwner() {
        return owner;
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
    
    public String toShortString() {
        return getFriendlyLicenseType() + " - Expires: " + endDate;
    }
    
    private String buildPrinterIdsString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Associated Printer IDs:");
        printerIds.forEach(id -> {
            stringBuilder.append("\n");
            stringBuilder.append(id);
        });
        return stringBuilder.toString();
    }
    
    @Override
    public String toString() {
        return "License issued to: " + owner + "\n"
                + "License type: " + getFriendlyLicenseType() + "\n"
                + "Expires: " + endDate + "\n"
                + buildPrinterIdsString();
    }
}
