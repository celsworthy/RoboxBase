package celtech.roboxbase.services;

import celtech.roboxbase.comms.remote.PauseStatus;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.postprocessor.nouveau.nodes.CommentNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.LayerChangeDirectiveNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.MCodeNode;
import celtech.roboxbase.postprocessor.nouveau.nodes.TravelNode;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterException;
import celtech.roboxbase.utils.ScriptUtils;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import libertysystems.configuration.ConfigNotLoadedException;
import libertysystems.configuration.Configuration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class CameraTriggerManager
{
    private static final Stenographer STENO = StenographerFactory.getStenographer(CameraTriggerManager.class.getName());
    
    private static final String APP_SHORT_NAME_ROOT = "Root";
    
    private Printer associatedPrinter = null;
    private static final int moveFeedrate_mm_per_min = 12000;
    private final ScheduledExecutorService scheduledPhoto;
    private final Runnable photoRun;
    private CameraTriggerData triggerData;
    
    private final ChangeListener pauseStatusListener = (observable, oldPauseStatus, newPauseStatus) -> {
        if (newPauseStatus == PauseStatus.SELFIE_PAUSE)
        {
            try 
            {
                if (triggerUSBCamera())
                {
                    associatedPrinter.resume();
                }
            } catch (PrinterException ex)
            {
                STENO.error("Exception whilst resuming");
            }
        }
    };

    public CameraTriggerManager(Printer printer)
    {
        associatedPrinter = printer;
        scheduledPhoto = Executors.newSingleThreadScheduledExecutor();
        photoRun = new Runnable()
        {
            @Override
            public void run()
            {
                STENO.debug("Firing camera");
                String goProURLString = "http://10.5.5.9/camera/SH?t=" + triggerData.getGoProWifiPassword() + "&p=%01";
                fireCameraThroughURL(goProURLString);
            }
        };
        
        if (associatedPrinter != null)
        {
            // In case the printer has been seen before, we only want to have one pauseStatusListener
            associatedPrinter.pauseStatusProperty().removeListener(pauseStatusListener);
            associatedPrinter.pauseStatusProperty().addListener(pauseStatusListener);
        }
    }
    
    private void fireCameraThroughURL(String urlString)
    {
        try
        {
            URL obj = new URL(urlString);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.3; WOW64) AppleWebKit/537.44 (KHTML, like Gecko) JavaFX/8.0 Safari/537.44");

            // optional default is GET
            con.setRequestMethod("GET");
            
            //add request header
            con.setConnectTimeout(500);
            con.setReadTimeout(500);

            int responseCode = con.getResponseCode();

            if (responseCode == 200 && con.getContentLength() > 0)
            {
                STENO.debug("Took picture");
            } else
            {
                STENO.error("Failed to take picture - response was " + responseCode);
            }
        } catch (IOException ex)
        {
            STENO.error("Exception whilst attempting to take GoPro picture");
        }
    }

    private final ChangeListener<Number> cameraTriggerListener = (ObservableValue<? extends Number> observable, Number oldValue, Number newValue) ->
    {
        if (newValue.intValue() > oldValue.intValue())
        {
            triggerCamera();
        }
    };

    public void appendLayerEndTriggerCode(LayerChangeDirectiveNode layerChangeNode, boolean turnOffHeadLights)
    {
        CommentNode beginComment = new CommentNode("Start of camera trigger");
        CommentNode endComment = new CommentNode("End of camera trigger");

        TravelNode moveBedForward = new TravelNode();

        boolean outputMoveCommand = triggerData.isMoveBeforeCapture();

        if (outputMoveCommand)
        {
            int xMoveInt = triggerData.getyMoveBeforeCapture();
            moveBedForward.getMovement().setX(xMoveInt);

            int yMoveInt = triggerData.getyMoveBeforeCapture();
            moveBedForward.getMovement().setY(yMoveInt);

            moveBedForward.getFeedrate().setFeedRate_mmPerMin(moveFeedrate_mm_per_min);
        }

        MCodeNode selfiePauseNode = new MCodeNode(1);
        selfiePauseNode.setCPresent(true);
        MCodeNode turnHeadLightsOff = new MCodeNode(128);
        MCodeNode turnHeadLightsOn = new MCodeNode(129);
        
//        GCodeDirectiveNode dwellWhilePictureTaken = new GCodeDirectiveNode();
//        dwellWhilePictureTaken.setGValue(4);
//        dwellWhilePictureTaken.setSValue(triggerData.getDelayAfterCapture());

        TravelNode returnToPreviousPosition = new TravelNode();
        returnToPreviousPosition.getMovement().setX(layerChangeNode.getMovement().getX());
        returnToPreviousPosition.getMovement().setY(layerChangeNode.getMovement().getY());
        returnToPreviousPosition.getFeedrate().setFeedRate_mmPerMin(moveFeedrate_mm_per_min);

        layerChangeNode.addSiblingAfter(endComment);
        
        if (turnOffHeadLights)
            layerChangeNode.addSiblingAfter(turnHeadLightsOn);
                
        layerChangeNode.addSiblingAfter(returnToPreviousPosition);
        //layerChangeNode.addSiblingAfter(dwellWhilePictureTaken);
        layerChangeNode.addSiblingAfter(selfiePauseNode);
        
        if (outputMoveCommand)
            layerChangeNode.addSiblingAfter(moveBedForward);
        
        if (turnOffHeadLights) 
            layerChangeNode.addSiblingAfter(turnHeadLightsOff);

        layerChangeNode.addSiblingAfter(beginComment);
    }

    public void listenForCameraTrigger()
    {
        STENO.debug("Started listening for camera trigger");
        associatedPrinter.getPrintEngine().progressCurrentLayerProperty().addListener(cameraTriggerListener);
    }

    public void stopListeningForCameraTrigger()
    {
        STENO.debug("Stopped listening for camera trigger");
        associatedPrinter.getPrintEngine().progressCurrentLayerProperty().removeListener(cameraTriggerListener);
    }

    private void triggerCamera()
    {
        STENO.debug("Asked to trigger camera");
        scheduledPhoto.schedule(photoRun, triggerData.getDelayBeforeCapture(), TimeUnit.SECONDS);
    }

    public void setTriggerData(CameraTriggerData triggerData)
    {
        this.triggerData = triggerData;
    }
    
    private boolean triggerUSBCamera()
    {
        boolean resumePrinter;
        
        // If we are talking to a remote printer, return false, root will handle the resume.
        if (associatedPrinter.getCommandInterface() instanceof RoboxRemoteCommandInterface)
        {
            resumePrinter = false;
        } else
        {
            try
            {
                Configuration config = Configuration.getInstance();
                String applicationShortName = config.getString(BaseConfiguration.applicationConfigComponent, "ApplicationShortName", APP_SHORT_NAME_ROOT);
                
                if (applicationShortName.equals(APP_SHORT_NAME_ROOT))
                {
                    // If we are on a root device we can attempt to take a snapshot from the camera before resuming the print.
                    String jobID = "";
                    if (associatedPrinter.getPrintEngine().printJobProperty().get() != null)
                    {
                        jobID = associatedPrinter.getPrintEngine().printJobProperty().get().getJobUUID();
                    }
                    
                    ScriptUtils.runScript("takePhoto.sh", jobID);
//                    String webCamURL = "http://localHost:8101/0/action/snapshot";
//                    fireCameraThroughURL(webCamURL);
                } 
                
                resumePrinter = true;
            } catch (ConfigNotLoadedException ex)
            {
                STENO.error("Configuration not loaded, cannot determine platform type, print will resume with no camera trigger.");
                resumePrinter = true;
            }
        }
        
        return resumePrinter;
    }
}
