/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.roboxbase.configuration.hardwarevariants;

/**
 * Valid printer models / types are declared here
 *
 * @author alynch
 */
public enum PrinterType {

    ROBOX("RBX01"),
    ROBOX_DUAL("RBX02"),
    BROBOX("RBX10");

    private final String typeCode;

    PrinterType(String typeCode) {
        this.typeCode = typeCode;
    }

    public String getTypeCode() {
        return typeCode;
    }

    public static PrinterType getPrinterTypeForTypeCode(String typeCode) {

        for (PrinterType printerType : PrinterType.values())
        {
            if (printerType.getTypeCode().equalsIgnoreCase(typeCode))
            {
                return printerType;
            }
        }

        throw new RuntimeException("No printer type found for given code: " + typeCode);
    }
}
