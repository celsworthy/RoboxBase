package celtech.roboxbase.comms;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.MembershipKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class RemoteServerDetector
{

    private final Stenographer steno = StenographerFactory.getStenographer(RemoteServerDetector.class.getName());

    private static RemoteServerDetector instance = null;
    private InetSocketAddress transmitGroup = null;
    private DatagramChannel datagramChannel = null;
    private static final ObjectMapper mapper = new ObjectMapper();
    private MembershipKey multicastKey;
    private static final int MAX_WAIT_TIME_MS = 3000;
    private static final int CYCLE_WAIT_TIME_MS = 500;

    private RemoteServerDetector()
    {
        try
        {
            transmitGroup = new InetSocketAddress(RemoteDiscovery.multicastAddress, RemoteDiscovery.remoteSocket);
            datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET);

            NetworkInterface interf = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            if (interf == null)
                interf = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
            if (interf == null)
            {
                steno.error("Unable to set up remote discovery client - no local host or loopback interface.");
                return;
            }
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(RemoteDiscovery.remoteSocket));
            datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, interf);
            datagramChannel.configureBlocking(false);
    } catch (IOException ex)
        {
            steno.error("Unable to set up remote discovery client");
        }
    }

    public static RemoteServerDetector getInstance()
    {
        if (instance == null)
        {
            instance = new RemoteServerDetector();
        }

        return instance;
    }

    public List<DetectedServer> searchForServers() throws IOException
    {
        List<DetectedServer> newlyDiscoveredServers = new ArrayList<>();

        ByteBuffer sendBuffer = ByteBuffer.wrap(RemoteDiscovery.discoverHostsMessage.getBytes("US-ASCII"));
        datagramChannel.send(sendBuffer, transmitGroup);

        int waitTime = 0;
        while (waitTime < MAX_WAIT_TIME_MS)
        {
            ByteBuffer inputBuffer = ByteBuffer.allocate(100);
            InetSocketAddress inboundAddress = (InetSocketAddress) datagramChannel.receive(inputBuffer);
            if (inboundAddress != null)
            {
                byte[] inputBytes = new byte[100];
                int bytesRead = inputBuffer.position();
                inputBuffer.rewind();
                inputBuffer.get(inputBytes, 0, bytesRead);
                String receivedData = new String(Arrays.copyOf(inputBytes, bytesRead), "US-ASCII");

                if (receivedData.equals(RemoteDiscovery.iAmHereMessage))
                {
                    //steno.info("searchForServers got response from address " + inboundAddress.getAddress());
                    DetectedServer newServer = DetectedServer.createDetectedServer(inboundAddress.getAddress());
                    if (newServer.whoAreYou())
                    {
                        newlyDiscoveredServers.add(newServer);
                    }
                } else
                {
                    steno.warning("Didn't understand the response from remote server with address " + inboundAddress.getAddress() + ". I saw: " + receivedData);
                }
            } else
            {
                try
                {
                    Thread.sleep(CYCLE_WAIT_TIME_MS);
                    waitTime += CYCLE_WAIT_TIME_MS;
                } catch (InterruptedException ex)
                {

                }
            }
        }
        
        return newlyDiscoveredServers;
    }
}
