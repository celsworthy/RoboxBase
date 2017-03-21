package celtech.roboxbase.printerControl.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.paint.Color;

/**
 *
 * @author Ian
 */
public class PrinterIdentity
{

    protected final StringProperty printerUniqueID = new SimpleStringProperty("");
    protected final StringProperty printermodel = new SimpleStringProperty("");
    protected final StringProperty printeredition = new SimpleStringProperty("");
    protected final StringProperty printerweekOfManufacture = new SimpleStringProperty("");
    protected final StringProperty printeryearOfManufacture = new SimpleStringProperty("");
    protected final StringProperty printerpoNumber = new SimpleStringProperty("");
    protected final StringProperty printerserialNumber = new SimpleStringProperty("");
    protected final StringProperty printercheckByte = new SimpleStringProperty("");
    protected final StringProperty printerFriendlyName = new SimpleStringProperty("");
    protected final ObjectProperty<Color> printerColour = new SimpleObjectProperty<>();
    protected final StringProperty firmwareVersion = new SimpleStringProperty();

    private final ChangeListener<String> stringChangeListener = new ChangeListener<String>()
    {
        @Override
        public void changed(
                ObservableValue<? extends String> observable, String oldValue, String newValue)
        {
            updatePrinterUniqueID();
        }
    };

    public PrinterIdentity()
    {
        firmwareVersion.addListener(stringChangeListener);

        printerColour.addListener(new ChangeListener<Color>()
        {

            @Override
            public void changed(
                    ObservableValue<? extends Color> observable, Color oldValue, Color newValue)
            {
                updatePrinterUniqueID();
            }
        });

        printerFriendlyName.addListener(stringChangeListener);
        printerUniqueID.addListener(stringChangeListener);
        printercheckByte.addListener(stringChangeListener);
        printeredition.addListener(stringChangeListener);
        printermodel.addListener(stringChangeListener);
        printerpoNumber.addListener(stringChangeListener);
        printerserialNumber.addListener(stringChangeListener);
        printerweekOfManufacture.addListener(stringChangeListener);
        printeryearOfManufacture.addListener(stringChangeListener);

    }

    public StringProperty printerUniqueIDProperty()
    {
        return printerUniqueID;
    }

    /**
     *
     * @return
     */
    public StringProperty printermodelProperty()
    {
        return printermodel;
    }

    /**
     *
     * @return
     */
    public StringProperty printereditionProperty()
    {
        return printeredition;
    }

    /**
     *
     * @return
     */
    public StringProperty printerweekOfManufactureProperty()
    {
        return printerweekOfManufacture;
    }

    /**
     *
     * @return
     */
    public StringProperty printeryearOfManufactureProperty()
    {
        return printeryearOfManufacture;
    }

    /**
     *
     * @return
     */
    public StringProperty printerpoNumberProperty()
    {
        return printerpoNumber;
    }

    /**
     *
     * @return
     */
    public StringProperty printerserialNumberProperty()
    {
        return printerserialNumber;
    }

    /**
     *
     * @return
     */
    public StringProperty printercheckByteProperty()
    {
        return printercheckByte;
    }

    /**
     *
     * @return
     */
    public StringProperty printerFriendlyNameProperty()
    {
        return printerFriendlyName;
    }

    /**
     *
     * @return
     */
    public final ObjectProperty<Color> printerColourProperty()
    {
        return printerColour;
    }

    /**
     *
     * @return
     */
    public final StringProperty firmwareVersionProperty()
    {
        return firmwareVersion;
    }

    /**
     *
     * @return
     */
    private void updatePrinterUniqueID()
    {
        printerUniqueID.set(printermodel.get()
                + printeredition.get()
                + printerweekOfManufacture.get()
                + printeryearOfManufacture.get()
                + printerpoNumber.get()
                + printerserialNumber.get()
                + printercheckByte.get());
    }

    @Override
    public PrinterIdentity clone()
    {
        PrinterIdentity clone = new PrinterIdentity();
        clone.firmwareVersion.set(firmwareVersion.get());
        clone.printerColour.set(printerColour.get());
        clone.printerFriendlyName.set(printerFriendlyName.get());
        clone.printerUniqueID.set(printerUniqueID.get());
        clone.printercheckByte.set(printercheckByte.get());
        clone.printeredition.set(printeredition.get());
        clone.printermodel.set(printermodel.get());
        clone.printerpoNumber.set(printerpoNumber.get());
        clone.printerserialNumber.set(printerserialNumber.get());
        clone.printerweekOfManufacture.set(printerweekOfManufacture.get());
        clone.printeryearOfManufacture.set(printeryearOfManufacture.get());

        return clone;
    }

    @Override
    public String toString()
    {
        StringBuilder idString = new StringBuilder();
        idString.append(printermodelProperty().get());
        idString.append("-");
        idString.append(printereditionProperty().get());
        idString.append("-");
        idString.append(printerweekOfManufactureProperty().get());
        idString.append(printeryearOfManufactureProperty().get());
        idString.append("-");
        idString.append(printerpoNumberProperty().get());
        idString.append("-");
        idString.append(printerserialNumberProperty().get());
        idString.append("-");
        idString.append(printercheckByteProperty().get());
        
        return idString.toString();
    }
}
