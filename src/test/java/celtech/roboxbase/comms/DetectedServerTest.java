/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.roboxbase.comms;

import org.junit.Test;
import com.jcraft.jsch.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.logging.Level;

/**
 *
 * @author admin
 */
public class DetectedServerTest {
    
    private static final String PRIVATE_KEY = "C:/Users/admin/.ssh/id_rsa";
    private static final String USER = "pi";
    private static final String HOST = "192.168.1.149";
    
    @Test
    public void testPublicPrivateKeyConnection() throws FileNotFoundException
    {
        JSch jsch = new JSch();
        try {
           // File knownHosts = new File("src/main/java/celtech/resources/SSH/known_hosts");
           // jsch.setKnownHosts(new FileInputStream(knownHosts));
            jsch.addIdentity(PRIVATE_KEY);
            Session session = jsch.getSession(USER, HOST, 22);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();
            File file = new File("src/main/java/celtech/resources/SSH/test.txt");
            channelSftp.put(new FileInputStream(file), file.getName());
        } catch (SftpException | JSchException ex) {
            java.util.logging.Logger.getLogger(DetectedServerTest.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
