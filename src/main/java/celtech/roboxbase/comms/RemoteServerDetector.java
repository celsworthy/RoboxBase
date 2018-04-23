package celtech.roboxbase.comms;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
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
    private static final int MAX_WAIT_TIME_MS = 2000;
    private static final int CYCLE_WAIT_TIME_MS = 200;

    private RemoteServerDetector()
    {
        try
        {
            NetworkInterface localInterface = null;
            // Look for a local interface with external access. The code used to do the following:
            // 
            //     NetworkInterface localInterface = NetworkInterface.getByInetAddress(InetAddress.getLocalHost());
            //
            // but this failed on some Linux distributions, because InetAddress.getLocalHost() would return 127.0.1.1.
            // This address did not map to a network interface, so localInterface was null.
            //
            // This code, copied from StackOverflow, finds the first interface that is not a loopback or link local address.
            try 
            {
                Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
                while (localInterface == null && networkInterfaces.hasMoreElements()) 
                {
                    NetworkInterface ni = (NetworkInterface) networkInterfaces.nextElement();
                    Enumeration<InetAddress> nias = ni.getInetAddresses();
                    while(nias.hasMoreElements())
                    {
                        InetAddress ia= (InetAddress) nias.nextElement();
                        if (!ia.isLinkLocalAddress() &&
                            !ia.isLoopbackAddress())
                        {
                            localInterface = ni;
                            break;
                        }
                    }
                }
            }
            catch (SocketException e)
            {
                steno.error("Unable to get current IP " + e.getMessage());
            }
    
            if (localInterface == null)
                localInterface = NetworkInterface.getByInetAddress(InetAddress.getLoopbackAddress());
            if (localInterface == null)
            {
                steno.error("Unable to set up remote discovery client - no local host or loopback interface.");
                return;
            }

            transmitGroup = new InetSocketAddress(RemoteDiscovery.multicastAddress, RemoteDiscovery.remoteSocket);
            datagramChannel = DatagramChannel.open(StandardProtocolFamily.INET);
            datagramChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            datagramChannel.bind(new InetSocketAddress(RemoteDiscovery.remoteSocket));
            datagramChannel.setOption(StandardSocketOptions.IP_MULTICAST_IF, localInterface);
            datagramChannel.configureBlocking(false);
        }
        catch (IOException ex)
        {
            steno.error("Unable to set up remote discovery client : " + ex.getMessage());
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
                } else if (receivedData.equals(RemoteDiscovery.discoverHostsMessage))
                {
                    // FIXME On Macs, it seems to receive the discoverHostsMessage (twice) as well as the iAmHereMessage.
                    // Don't know why.
                    steno.debug("Received ?Ello from address " + inboundAddress.getAddress());
                }
                else
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
