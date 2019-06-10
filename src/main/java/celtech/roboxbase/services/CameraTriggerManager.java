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
import javafx.beans.value.ChangeListener;
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
        
        if (associatedPrinter != null)
        {
            // In case the printer has been seen before, we only want to have one pauseStatusListener
            associatedPrinter.pauseStatusProperty().removeListener(pauseStatusListener);
            associatedPrinter.pauseStatusProperty().addListener(pauseStatusListener);
        }
    }

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
        
        TravelNode returnToPreviousPosition = new TravelNode();
        returnToPreviousPosition.getMovement().setX(layerChangeNode.getMovement().getX());
        returnToPreviousPosition.getMovement().setY(layerChangeNode.getMovement().getY());
        returnToPreviousPosition.getFeedrate().setFeedRate_mmPerMin(moveFeedrate_mm_per_min);

        layerChangeNode.addSiblingAfter(endComment);
        
        if (turnOffHeadLights)
            layerChangeNode.addSiblingAfter(turnHeadLightsOn);
                
        layerChangeNode.addSiblingAfter(returnToPreviousPosition);
        layerChangeNode.addSiblingAfter(selfiePauseNode);
        
        if (outputMoveCommand)
            layerChangeNode.addSiblingAfter(moveBedForward);
        
        if (turnOffHeadLights) 
            layerChangeNode.addSiblingAfter(turnHeadLightsOff);

        layerChangeNode.addSiblingAfter(beginComment);
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
                    
                    ScriptUtils.runScript(BaseConfiguration.getApplicationInstallDirectory(CameraTriggerManager.class) + "takePhoto.sh", jobID);
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
