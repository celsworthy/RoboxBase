package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.DiscoveryResponse;
import celtech.roboxbase.configuration.BaseConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class RemotePrinterDetector extends DeviceDetector
{

    private final Stenographer steno = StenographerFactory.getStenographer("RemotePrinterDetector");

    private boolean initialised = false;
    private InetAddress group = null;
    private DatagramSocket s = null;
    private boolean keepRunning = true;
    private List<DetectedDevice> currentPrinters = new ArrayList<>();
    private static final ObjectMapper mapper = new ObjectMapper();

    public RemotePrinterDetector(DeviceDetectionListener listener)
    {
        super(listener);
        this.setName("RemotePrinterDetector");
    }

    @Override
    public void run()
    {
        if (!initialised)
        {
            try
            {
                group = InetAddress.getByName(RemoteDiscovery.multicastAddress);
                s = new DatagramSocket(RemoteDiscovery.clientSocket);
                s.setSoTimeout(500);
                initialised = true;
            } catch (IOException ex)
            {
                steno.error("Unable to set up remote discovery client");
            }
        }

        while (keepRunning && initialised)
        {
            Set<InetAddress> serverAddresses = new HashSet<>();

            try
            {
                DatagramPacket hi = new DatagramPacket(RemoteDiscovery.discoverHostsMessage.getBytes("US-ASCII"),
                        RemoteDiscovery.discoverHostsMessage.length(),
                        group, RemoteDiscovery.remoteSocket);

                s.send(hi);

                int remainingWaitTime_ms = 1000;

                while (remainingWaitTime_ms > 0)
                {
                    try
                    {
                        s.setSoTimeout(remainingWaitTime_ms);

                        byte[] buf = new byte[100];
                        DatagramPacket recv = new DatagramPacket(buf, buf.length);

                        long startTime = System.currentTimeMillis();

                        s.receive(recv);

                        long endTime = System.currentTimeMillis();

                        remainingWaitTime_ms = remainingWaitTime_ms - (int) (endTime - startTime);

                        if (Arrays.equals(Arrays.copyOf(buf, RemoteDiscovery.iAmHereMessage.getBytes("US-ASCII").length),
                                RemoteDiscovery.iAmHereMessage.getBytes("US-ASCII")))
                        {
                            serverAddresses.add(recv.getAddress());
                        }

                    } catch (SocketTimeoutException ex)
                    {
                        remainingWaitTime_ms = 0;
                    }
                }

                List<DetectedDevice> newlyDetectedPrinters = searchForDevices(serverAddresses);

                List<DetectedDevice> printersToDisconnect = new ArrayList<>();
                List<DetectedDevice> printersToConnect = new ArrayList<>();

                //Deal with disconnections
                currentPrinters.forEach(existingPrinter ->
                {
                    if (!newlyDetectedPrinters.contains(existingPrinter))
                    {
                        printersToDisconnect.add(existingPrinter);
                    }
                });

                //Now new connections
                newlyDetectedPrinters.forEach(newPrinter ->
                {
                    if (!currentPrinters.contains(newPrinter))
                    {
                        printersToConnect.add(newPrinter);
                    }
                });

                for (DetectedDevice printerToDisconnect : printersToDisconnect)
                {
                    steno.info("Disconnecting from " + printerToDisconnect + " as it doesn't seem to be present anymore");
                    deviceDetectionListener.deviceNoLongerPresent(printerToDisconnect);
                    currentPrinters.remove(printerToDisconnect);
                }

                for (DetectedDevice printerToConnect : printersToConnect)
                {
                    steno.info("We have found a new printer " + printerToConnect);
                    currentPrinters.add(printerToConnect);
                    deviceDetectionListener.deviceDetected(printerToConnect);
                }
            } catch (IOException ex)
            {
                steno.error("Unable to query for remote hosts");
            }

            try
            {
                Thread.sleep(1500);
            } catch (InterruptedException ex)
            {
                steno.warning("Interrupted within remote host discovery loop");
            }
        }
    }

    private List<DetectedDevice> searchForDevices(Set<InetAddress> serverAddresses)
    {
        List<DetectedDevice> foundPrinters = new ArrayList<>();

        for (InetAddress address : serverAddresses)
        {
            String url = "http://" + address.getHostAddress() + ":9000/api/discovery";

            try
            {
                URL obj = new URL(url);
                HttpURLConnection con = (HttpURLConnection) obj.openConnection();

                // optional default is GET
                con.setRequestMethod("GET");

                //add request header
                con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());

                con.setConnectTimeout(5000);
                int responseCode = con.getResponseCode();

                if (responseCode == 200)
                {
                    DiscoveryResponse discoveryResponse = mapper.readValue(con.getInputStream(), DiscoveryResponse.class);
                    discoveryResponse.getPrinterIDs().forEach(printerID ->
                    {
                        RemoteDetectedPrinter remotePrinter = new RemoteDetectedPrinter(address, PrinterConnectionType.ROBOX_REMOTE, printerID);
                        foundPrinters.add(remotePrinter);
                    });

//                    steno.info("Got response from @ " + address.getHostAddress() + " : " + discoveryResponse.toString());
                } else
                {
                    steno.warning("No response from @ " + address.getHostAddress());
                }
            } catch (IOException ex)
            {
                steno.error("Error whilst polling for remote printers @ " + address.getHostAddress());
            }

        }

        return foundPrinters;
    }
}
