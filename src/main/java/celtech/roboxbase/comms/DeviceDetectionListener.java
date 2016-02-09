package celtech.roboxbase.comms;

/**
 *
 * @author Ian
 */
public interface DeviceDetectionListener
{
    public void deviceDetected(DetectedDevice detectedDevice);
    public void deviceNoLongerPresent(DetectedDevice detectedDevice);
}
