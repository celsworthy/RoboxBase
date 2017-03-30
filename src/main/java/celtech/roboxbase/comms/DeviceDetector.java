package celtech.roboxbase.comms;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

/**
 *
 * @author Ian
 */
public abstract class DeviceDetector extends Thread
{
    protected final ObservableList<DetectedDevice> currentPrinters = FXCollections.observableArrayList();
    protected final DeviceDetectionListener deviceDetectionListener;
    protected boolean keepRunning = true;

    public DeviceDetector(DeviceDetectionListener listener)
    {
        this.deviceDetectionListener = listener;
        
        this.setDaemon(true);
    }

    public final void shutdownDetector()
    {
        keepRunning = false;
    }

    public enum PrinterConnectionType
    {
        SERIAL,
        ROBOX_REMOTE
    }
    
    public abstract void notifyOfFailedCommsForPrinter(DetectedDevice printerHandle);
}
