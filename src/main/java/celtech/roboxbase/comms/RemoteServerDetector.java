package celtech.roboxbase.comms;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
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
    private InetAddress group = null;
    private DatagramSocket s = null;
    private static final ObjectMapper mapper = new ObjectMapper();

    private RemoteServerDetector()
    {
        try
        {
            group = InetAddress.getByName(RemoteDiscovery.multicastAddress);
            s = new DatagramSocket(RemoteDiscovery.clientSocket);
            s.setSoTimeout(500);
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

    public List<DetectedServer> searchForServers()
    {
        List<DetectedServer> newlyDiscoveredServers = new ArrayList<>();

        try
        {
            DatagramPacket hi = new DatagramPacket(RemoteDiscovery.discoverHostsMessage.getBytes("US-ASCII"),
                    RemoteDiscovery.discoverHostsMessage.length(),
                    group, RemoteDiscovery.remoteSocket);

            s.send(hi);

            s.setSoTimeout(2000);

            try
            {
                while (true)
                {
                    byte[] buf = new byte[100];
                    DatagramPacket recv = new DatagramPacket(buf, buf.length);
                    s.receive(recv);

                    String receivedData = new String(Arrays.copyOf(buf, recv.getLength()), "US-ASCII");
                    if (receivedData.equals(RemoteDiscovery.iAmHereMessage))
                    {
                        DetectedServer newServer = new DetectedServer(recv.getAddress());
                        if (newServer.whoAreYou())
                        {
                            newlyDiscoveredServers.add(newServer);
                        }
                    }
                }
            } catch (SocketTimeoutException ex)
            {

            }
        } catch (IOException ex)
        {
            steno.exception("Exception during root scan", ex);
        }

        return newlyDiscoveredServers;
    }
}
