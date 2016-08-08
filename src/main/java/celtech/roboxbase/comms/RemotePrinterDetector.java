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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final Map<InetAddress, DetectedServer> validInServiceServers = new HashMap<>();
    private final Map<InetAddress, DetectedServer> incorrectVersionServers = new HashMap<>();
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
            try
            {
                DatagramPacket hi = new DatagramPacket(RemoteDiscovery.discoverHostsMessage.getBytes("US-ASCII"),
                        RemoteDiscovery.discoverHostsMessage.length(),
                        group, RemoteDiscovery.remoteSocket);

                s.send(hi);

                boolean keepSearching = true;

                Map<InetAddress, DetectedServer> newlyDiscoveredServers = new HashMap<>();

                while (keepSearching)
                {
                    try
                    {
                        s.setSoTimeout(2000);

                        byte[] buf = new byte[100];
                        DatagramPacket recv = new DatagramPacket(buf, buf.length);

                        s.receive(recv);

                        if (Arrays.equals(Arrays.copyOf(buf, RemoteDiscovery.iAmHereMessage.getBytes("US-ASCII").length),
                                RemoteDiscovery.iAmHereMessage.getBytes("US-ASCII")))
                        {
                            DetectedServer detectedServer = searchForDevices(recv.getAddress());
                            newlyDiscoveredServers.put(recv.getAddress(), detectedServer);
                        }
                    } catch (SocketTimeoutException ex)
                    {
                        // We should issue a new request for server callbacks
                        keepSearching = false;

                        // 
                        // Case 1: The server is no longer there
                        //
                        List<InetAddress> serversToRemoveFromIncorrectVersionList = new ArrayList<>();

                        incorrectVersionServers.entrySet().forEach((currentEntry) ->
                        {
                            if (!newlyDiscoveredServers.containsKey(currentEntry.getKey()))
                            {
                                serversToRemoveFromIncorrectVersionList.add(currentEntry.getKey());
                            }
                        });

                        serversToRemoveFromIncorrectVersionList.forEach(address ->
                        {
                            incorrectVersionServers.remove(address);
                        });

                        List<InetAddress> serversToDisconnect = new ArrayList<>();

                        validInServiceServers.entrySet().forEach((currentEntry) ->
                        {
                            if (!newlyDiscoveredServers.containsKey(currentEntry.getKey()))
                            {
                                serversToDisconnect.add(currentEntry.getKey());
                                // Delete all of the printers that were associated with that server
                                currentEntry.getValue().getAttachedPrinters().forEach(printer ->
                                {
                                    disconnectPrinter(printer);
                                });
                                currentEntry.getValue().removeAllPrinters();
                            }
                        });

                        serversToDisconnect.forEach(address ->
                        {
                            validInServiceServers.remove(address);
                        });

                        // Case 2: The server is there but one or more printers have gone
                        newlyDiscoveredServers.entrySet().forEach((newServerEntry) ->
                        {
                            if (validInServiceServers.containsKey(newServerEntry.getKey()))
                            {
                                //Deal with disconnections
                                // In the old list but not the new
                                validInServiceServers.get(newServerEntry.getKey()).getAttachedPrinters().forEach(existingPrinter ->
                                {
                                    if (!newServerEntry.getValue().getAttachedPrinters().contains(existingPrinter))
                                    {
                                        validInServiceServers.get(newServerEntry.getKey()).removeAttachedPrinter(existingPrinter);
                                        disconnectPrinter(existingPrinter);
                                    }
                                });
                            }
                        });

                        // Case 3: The server is there but one or more printers have been added
                        newlyDiscoveredServers.entrySet().forEach((newServerEntry) ->
                        {
                            if (validInServiceServers.containsKey(newServerEntry.getKey()))
                            {
                                //Deal with new connections
                                // In the new list but not the old

                                newServerEntry.getValue().getAttachedPrinters().forEach(newPrinter ->
                                {
                                    if (!validInServiceServers.get(newServerEntry.getKey()).getAttachedPrinters().contains(newPrinter))
                                    {
                                        validInServiceServers.get(newServerEntry.getKey()).addAttachedPrinter(newPrinter);
                                        connectPrinter(newPrinter);
                                    }
                                });
                            }
                        });

                        newlyDiscoveredServers.entrySet().forEach((newServerEntry) ->
                        {
                            if (!validInServiceServers.containsKey(newServerEntry.getKey()))
                            {
                                if (BaseConfiguration.getApplicationVersion().equals(newServerEntry.getValue().getVersion()))
                                {
                                    validInServiceServers.put(newServerEntry.getKey(), newServerEntry.getValue());

                                    // Case 4: This is a newly detected server and the version is correct
                                    newServerEntry.getValue().getAttachedPrinters().forEach(newPrinter ->
                                    {
                                        connectPrinter(newPrinter);
                                    });
                                } else
                                {
                                    // Case 5: This is a newly detected server but the version is wrong
                                    if (!incorrectVersionServers.containsKey(newServerEntry.getKey()))
                                    {
                                        steno.info("Found a server at " + newServerEntry.getKey()
                                                + " but it's version - " + newServerEntry.getValue().getVersion()
                                                + " doesn't match mine - " + BaseConfiguration.getApplicationVersion());
                                        incorrectVersionServers.put(newServerEntry.getKey(), newServerEntry.getValue());
                                    }
                                }
                            }
                        });
                    }
                }
            } catch (IOException ex)
            {
                steno.error("Unable to query for remote hosts");

                // Remove all of the servers and their attached printers
                incorrectVersionServers.clear();

                validInServiceServers.values().forEach(server ->
                {
                    server.getAttachedPrinters().forEach(printer ->
                    {
                        steno.info("Disconnecting from " + printer + " as it doesn't seem to be present anymore");
                        disconnectPrinter(printer);
                    });

                    server.removeAllPrinters();
                });

                validInServiceServers.clear();
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

    private void disconnectPrinter(DetectedDevice printer)
    {
        steno.info("Disconnecting from " + printer + " as it doesn't seem to be present anymore");
        deviceDetectionListener.deviceNoLongerPresent(printer);
    }

    private void connectPrinter(DetectedDevice printer)
    {
        steno.info("We have found a new printer " + printer);
        deviceDetectionListener.deviceDetected(printer);
    }

    private DetectedServer searchForDevices(InetAddress address)
    {
        DetectedServer detectedServer = null;

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
                int availChars = con.getInputStream().available();
                byte[] inputData = new byte[availChars];
                con.getInputStream().read(inputData, 0, availChars);
                DiscoveryResponse discoveryResponse = mapper.readValue(inputData, DiscoveryResponse.class);

                steno.info("Got a response from a server at version: " + discoveryResponse.getServerVersion());
                detectedServer = new DetectedServer(address, discoveryResponse);
            } else
            {
                steno.warning("No response from @ " + address.getHostAddress());
            }
        } catch (IOException ex)
        {
            steno.error("Error whilst polling for remote printers @ " + address.getHostAddress());
            ex.printStackTrace();
        }

        return detectedServer;
    }
}
