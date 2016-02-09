package celtech.roboxbase.comms.tx;

import celtech.roboxbase.comms.remote.PrinterIDDataStructure;
import celtech.roboxbase.comms.remote.StringToBase64Encoder;
import java.io.UnsupportedEncodingException;
import javafx.scene.paint.Color;
import static celtech.roboxbase.utils.ColourStringConverter.colourToString;

/**
 *
 * @author ianhudson
 */
public class WritePrinterID extends RoboxTxPacket
{

    public static final int BYTES_FOR_NAME = PrinterIDDataStructure.printerFriendlyNameBytes;
    public static final int BYTES_FOR_FIRST_PAD = 41;
    public static final int BYTES_FOR_SECOND_PAD = 186 - BYTES_FOR_NAME;

    private final char[] firstPad = new char[BYTES_FOR_FIRST_PAD];
    private final char[] secondPad = new char[BYTES_FOR_SECOND_PAD];

    /**
     *
     */
    public WritePrinterID()
    {
        super(TxPacketTypeEnum.WRITE_PRINTER_ID, false, false);
    }

    /**
     *
     * @param byteData
     * @return
     */
    @Override
    public boolean populatePacket(byte[] byteData)
    {
        setMessagePayloadBytes(byteData);
        return false;
    }

    private String model;
    private String edition;
    private String weekOfManufacture;
    private String yearOfManufacture;
    private String poNumber;
    private String serialNumber;
    private String checkByte;
    private String printerFriendlyName;
    private Color colour;

    /**
     *
     * @param model
     * @param edition
     * @param weekOfManufacture
     * @param yearOfManufacture
     * @param poNumber
     * @param serialNumber
     * @param checkByte
     * @param printerFriendlyName
     * @param colour
     */
    public void setIDAndColour(String model, String edition,
            String weekOfManufacture, String yearOfManufacture, String poNumber,
            String serialNumber, String checkByte, String printerFriendlyName,
            Color colour)
    {
        this.model = model;
        this.edition = edition;
        this.weekOfManufacture = weekOfManufacture;
        this.yearOfManufacture = yearOfManufacture;
        this.poNumber = poNumber;
        this.serialNumber = serialNumber;
        this.checkByte = checkByte;
        this.printerFriendlyName = printerFriendlyName;
        this.colour = colour;

        try
        {
            printerFriendlyName = StringToBase64Encoder.encode(printerFriendlyName,
                    BYTES_FOR_NAME);
        } catch (UnsupportedEncodingException ex)
        {
            steno.error("Couldn't encode printer name: " + printerFriendlyName);
            printerFriendlyName = "";
        }

        //The ID is in the first 200 characters
        //The colour is stored in 6 bytes at the end - eg FF FF FF
        StringBuffer payload = new StringBuffer();

        payload.append(String.format("%1$-5s", model));
        payload.append(String.format("%1$-2s", edition));
        payload.append(String.format("%1$-2s", weekOfManufacture));
        payload.append(String.format("%1$-2s", yearOfManufacture));
        payload.append(String.format("%1$-7s", poNumber));
        payload.append(String.format("%1$-4s", serialNumber));
        payload.append(String.format("%1$1s", checkByte));
        payload.append(firstPad);
        payload.append(String.format("%1$" + BYTES_FOR_NAME + "s",
                printerFriendlyName));
        payload.append(secondPad);

        payload.append(colourToString(colour));

        steno.debug("Outputting string of length " + payload.length());
        this.setMessagePayload(payload.toString());
    }

    public void populatePacket(
            String printerUniqueIDIn,
    String printermodelIn,
    String printereditionIn,
    String printerweekOfManufactureIn,
    String printeryearOfManufactureIn,
    String printerpoNumberIn,
    String printerserialNumberIn,
    String printercheckByteIn,
    String printerFriendlyNameIn,
    Color printerColourIn,
    String firmwareVersionIn)
    {
        setIDAndColour(printermodelIn,
                printereditionIn,
                printerweekOfManufactureIn,
                printeryearOfManufactureIn,
                printerpoNumberIn,
                printerserialNumberIn,
                printercheckByteIn,
                printerFriendlyNameIn,
                printerColourIn);
    }

    public String getModel()
    {
        return model;
    }

    public String getEdition()
    {
        return edition;
    }

    public String getWeekOfManufacture()
    {
        return weekOfManufacture;
    }

    public String getYearOfManufacture()
    {
        return yearOfManufacture;
    }

    public String getPoNumber()
    {
        return poNumber;
    }

    public String getSerialNumber()
    {
        return serialNumber;
    }

    public String getCheckByte()
    {
        return checkByte;
    }

    public String getPrinterFriendlyName()
    {
        return printerFriendlyName;
    }

    public Color getColour()
    {
        return colour;
    }

}
