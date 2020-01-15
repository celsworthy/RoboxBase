package celtech.roboxbase.services.camera;

import celtech.roboxbase.services.printing.SFTPUtils;
import javafx.concurrent.Task;

/**
 *
 * @author George Salter
 */
public class ChangeRemoteCameraSettingsTask extends Task<Boolean> 
{
    private final String serverAddress;
    
    public ChangeRemoteCameraSettingsTask(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }
    
//    private class TransferProgressMonitor implements SftpProgressMonitor
//    {
//        long count = 0;
//        long fileSize = 0;
//        DetectedServer server;
//        
//        public TransferProgressMonitor(DetectedServer server)
//        {
//            this.server = server;
//        }
//
//        @Override
//        public void init(int op, String src, String dest, long fileSize)
//        {
//            this.fileSize = fileSize;
//            this.count = 0;
//            if (server != null)
//                server.resetPollCount();
//            steno.debug("Initialise file transfer: src = \"" + src + "\", dst = \"" + dest + "\", fileSize = " + Long.toString(fileSize));
//        }
//
//        @Override
//        public boolean count(long increment)
//        {
//          count += increment;
//          steno.debug("Transfer progress: count = " + Long.toString(count) + " of " + Long.toString(fileSize));
//          updateProgress((float) count, (float) fileSize);
//          if (server != null)
//                server.resetPollCount();
//          return !isCancelled();
//        }
//        
//        @Override
//        public void end(){
//        }
//    }

    @Override
    protected Boolean call() throws Exception 
    {
        boolean settingsChangeSuccessful = false;
        
        // We need to send the settings from the .camerprofile to the pi/server
        SFTPUtils sftpHelper = new SFTPUtils(serverAddress);
//        SftpProgressMonitor monitor = 
//        String remoteDirectory = BaseConfiguration.getRemoteRootDirectory();
//        if (sftpHelper.transferToRemotePrinter(cameraProfile, remoteDirectory, gcodeFile.getName(), monitor))
        
        
        // Then we need to tell root to run a script e.g. changeCameraSettings.sh
        
        return settingsChangeSuccessful;
    }
}
