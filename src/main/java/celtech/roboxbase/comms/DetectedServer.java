package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.comms.remote.StringToBase64Encoder;
import celtech.roboxbase.comms.remote.clear.ListPrintersResponse;
import celtech.roboxbase.comms.remote.clear.WhoAreYouResponse;
import celtech.roboxbase.comms.remote.types.SerializableFilament;
import celtech.roboxbase.configuration.ApplicationVersion;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.services.printing.SFTPUtils;
import celtech.roboxbase.utils.PercentProgressReceiver;
import celtech.roboxbase.utils.net.MultipartUtility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.jcraft.jsch.SftpProgressMonitor;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 *
 * @author ianhudson
 */
public final class DetectedServer
{
    // Jackson deserializer, so that DetectedServer.createDetectedServer() is used to create
    // new servers, thus ensuring the integrity of the known server list.
    public static class DetectedServerDeserializer extends StdDeserializer<DetectedServer> {
     
        public DetectedServerDeserializer() {
            this(null);
        }

        public DetectedServerDeserializer(Class<?> vc) {
            super(vc);
        }

        @Override
        public DetectedServer deserialize(JsonParser jp, DeserializationContext dc) throws IOException, JsonProcessingException
        {
            ObjectCodec codec = jp.getCodec();
            JsonNode node = codec.readTree(jp);

            String addressText = node.get("address").asText();
            InetAddress address = InetAddress.getByName(addressText);

            DetectedServer server = DetectedServer.createDetectedServer(address);
            
            server.serverIP.set(addressText);
            server.setName(node.get("name").asText());
            server.setVersion(new ApplicationVersion(node.get("version").get("versionString").asText()));
            server.setPin(node.get("pin").asText());
            server.setWasAutomaticallyAdded(node.get("wasAutomaticallyAdded").asBoolean());
            
            return server;
        }
    }

    @JsonIgnore
    private final Stenographer steno = StenographerFactory.getStenographer(DetectedServer.class.getName());

    private InetAddress address;
    private final StringProperty name = new SimpleStringProperty("");
    @JsonIgnore
    private final StringProperty serverIP = new SimpleStringProperty("");
    private final StringProperty pin = new SimpleStringProperty("1111");
    private final BooleanProperty wasAutomaticallyAdded = new SimpleBooleanProperty(true);
    private ListProperty<String> colours = new SimpleListProperty<>();
    
    private ApplicationVersion version;
    
    private List<DetectedDevice> detectedDevices = new ArrayList();

    @JsonIgnore
    private int pollCount = 0;

    @JsonIgnore
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    private final ObjectProperty<ServerStatus> serverStatus = new SimpleObjectProperty<>(ServerStatus.NOT_CONNECTED);

    @JsonIgnore
    public static final String defaultUser = "root";

    @JsonIgnore
    public static final int readTimeOutShort = 1500;
    @JsonIgnore
    public static final int connectTimeOutShort = 300;
    @JsonIgnore
    public static final int readTimeOutLong = 15000;
    @JsonIgnore
    public static final int connectTimeOutLong = 2000;
    @JsonIgnore
    public static final int maxAllowedPollCount = 8;
    @JsonIgnore
    private static final String LIST_PRINTERS_COMMAND = "/api/discovery/listPrinters";
    @JsonIgnore
    private static final String UPDATE_SYSTEM_COMMAND = "/api/admin/updateSystem";
    @JsonIgnore
    private static final String SHUTDOWN_SYSTEM_COMMAND = "/api/admin/shutdown";
    @JsonIgnore
    private static final String SAVE_FILAMENT_COMMAND = "/api/admin/saveFilament";
    @JsonIgnore
    private static final String DELETE_FILAMENT_COMMAND = "/api/admin/deleteFilament";

    @JsonIgnore
    // A server for any given address is created once, on the first create request, and placed on the
    // known server list. Subsequent create requests get the server from the known server list.
    private static final List<DetectedServer> knownServerList = new ArrayList<>();
    
    public enum ServerStatus
    {

        NOT_CONNECTED,
        CONNECTED,
        WRONG_VERSION,
        WRONG_PIN;

        private String getI18NString()
        {
            return "root." + name();
        }
    }

    private DetectedServer()
    {
    }

    private DetectedServer(InetAddress address)
    {
        this.address = address;
    }
    
    public static synchronized DetectedServer createDetectedServer(InetAddress address)
    {
        // This is the only public way to create a DetectedServer. It is synchronized so that
        // it can be called by multiple threads.
        return knownServerList.stream()
                              .filter(s -> s.getAddress().equals(address))
                              .findAny()
                              .orElseGet(() -> {
                                                   DetectedServer ds = new DetectedServer(address);
                                                   knownServerList.add(ds);
                                                   return ds;
                                               });
    }

    public int getPollCount()
    {
        return pollCount;
    }

    public void resetPollCount()
    {
        pollCount = 0;
    }

    public boolean maxPollCountExceeded()
    {
        if (pollCount > maxAllowedPollCount)
        {
            steno.warning("Maximum poll count of \"" + getDisplayName() + "\" exceeded! Count = " + Integer.toString(pollCount));
            return true;
        }
        else
            return false;
        //return (pollCount > maxAllowedPollCount);
    }

    public boolean incrementPollCount()
    {
        ++pollCount;
        steno.info("Incrementing poll count of \"" + getDisplayName() + "\" to " + Integer.toString(pollCount));
        return maxPollCountExceeded();
    }

    public InetAddress getAddress()
    {
        return address;
    }

    public void setAddress(InetAddress address)
    {
        this.address = address;
    }

    public String getName()
    {
        return name.get();
    }
    
    @JsonIgnore
    public String getDisplayName()
    {
        return getName()+ "@" + getServerIP();
    }

    public void setName(String name)
    {
        if (!name.equals(this.name))
        {
            this.name.set(name);
            dataChanged.set(!dataChanged.get());
        }
    }

    public StringProperty nameProperty()
    {
        return name;
    }

    public String getServerIP()
    {
        return address.getHostAddress();
    }
//
//    public void setServerIP(String serverIP)
//    {
//        if (!serverIP.equals(this.serverIP))
//        {
//            this.serverIP.set(serverIP);
//            dataChanged.set(!dataChanged.get());
//        }
//    }
//
//    public StringProperty serverIPProperty()
//    {
//        return serverIP;
//    }

    public ApplicationVersion getVersion()
    {
        return version;
    }

    public void setVersion(ApplicationVersion version)
    {
        if (this.version == null || !version.getVersionString().equals(this.version.getVersionString()))
        {
            this.version = version;
            dataChanged.set(!dataChanged.get());
        }
    }

    public List<String> getColours() 
    {
        return colours.get();
    }

    public void setColours(List<String> colours) 
    {
        
        if (!colours.equals(this.colours))
        {
            this.colours.setAll(colours);
            dataChanged.set(!dataChanged.get());
        }
    }
    
    public ListProperty coloursProperty() 
    {
        return colours;
    }

    public ServerStatus getServerStatus()
    {
        //steno.info("ServerStatus of " + getName() + " == " + this.serverStatus.get().name());
        return serverStatus.get();
    }

    public void setServerStatus(ServerStatus status)
    {
        //steno.info("Updating status of server " + getName() + " to " + status.name());
      
        if (status != serverStatus.get())
        {
            switch (status)
            {
                case CONNECTED:
                    CoreMemory.getInstance().activateRoboxRoot(this);
                    break;
                case WRONG_VERSION:
                    break;
                default:
                    CoreMemory.getInstance().deactivateRoboxRoot(this);
                    break;
            }
            this.serverStatus.set(status);
            dataChanged.set(!dataChanged.get());
        }
    }

    public ObjectProperty<ServerStatus> serverStatusProperty()
    {
        //steno.info("ServerStatus of " + getName() + " == " + this.serverStatus.get().name());
        return serverStatus;
    }

    public String getPin()
    {
        return pin.get();
    }

    public void setPin(String pin)
    {
        if (!pin.equals(this.pin))
        {
            this.pin.set(pin);
            dataChanged.set(!dataChanged.get());
        }
    }

    public StringProperty pinProperty()
    {
        return pin;
    }

    public boolean getWasAutomaticallyAdded()
    {
        return wasAutomaticallyAdded.get();
    }

    public BooleanProperty wasAutomaticallyAddedProperty()
    {
        return wasAutomaticallyAdded;
    }

    public void setWasAutomaticallyAdded(boolean value)
    {
        wasAutomaticallyAdded.set(value);
    }

    public ReadOnlyBooleanProperty dataChangedProperty()
    {
        return dataChanged;
    }

    public boolean connect()
    {
        boolean success = false;

        steno.info("Connecting " + name.get());
        steno.debug("Status = " + serverStatus.get());

        if (serverStatus.get() != ServerStatus.WRONG_VERSION
                && serverStatus.get() != ServerStatus.CONNECTED)
        {
            try
            {
                int response = getData(LIST_PRINTERS_COMMAND);
                if (response == 200)
                {
                    if (!version.getVersionString().equalsIgnoreCase(BaseConfiguration.getApplicationVersion()) &&
                    !(BaseConfiguration.getApplicationVersion().startsWith("tadev") && version.getVersionString().startsWith("tadev"))) // Debug hack to allow mismatching development versions to operate.
                    {
                        steno.debug("Setting status to WRONG_VERSION");
                        setServerStatus(ServerStatus.WRONG_VERSION);
                        CoreMemory.getInstance().deactivateRoboxRoot(this);
                    } else
                    {
                        steno.debug("Setting status to CONNECTED");
                        setServerStatus(ServerStatus.CONNECTED);
                        CoreMemory.getInstance().activateRoboxRoot(this);
                        success = true;
                    }
                } else if (response == 401)
                {
                    steno.debug("Setting status to WRONG_PIN");
                    setServerStatus(ServerStatus.WRONG_PIN);
                    CoreMemory.getInstance().deactivateRoboxRoot(this);
                } else
                {

                    steno.debug("Response = " + Integer.toString(response) + "- setting status to NOT_CONNECTED");
                    setServerStatus(ServerStatus.NOT_CONNECTED);
                    CoreMemory.getInstance().deactivateRoboxRoot(this);
                }
            } catch (IOException ex)
            {
                steno.debug("Caught exception " + ex.toString() + "- setting status to NOT_CONNECTED");
                setServerStatus(ServerStatus.NOT_CONNECTED);
                CoreMemory.getInstance().deactivateRoboxRoot(this);
            }
        }

        return success;
    }

    public void disconnect()
    {
        steno.info("Disconnecting \"" + getDisplayName() + "\"");
        setServerStatus(ServerStatus.NOT_CONNECTED);
        CoreMemory.getInstance().deactivateRoboxRoot(this);
        
        detectedDevices.forEach((device) ->
        {
            steno.info("Disconnecting device " + device.toString());
            RoboxCommsManager.getInstance().disconnected(device);
        });
    }

    public boolean whoAreYou()
    {
        boolean gotAResponse = false;
        WhoAreYouResponse response = null;

        String url = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + "/api/discovery/whoareyou?pc=yes";

        long t1 = System.currentTimeMillis();
        try
        {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
                    //+ BaseConfiguration.getApplicationVersion());

            con.setConnectTimeout(connectTimeOutShort);
            con.setReadTimeout(readTimeOutShort);

            int responseCode = con.getResponseCode();

            if (responseCode == 200)
            {
                pollCount = 0; // Contact! Zero the poll count;
                int availChars = con.getInputStream().available();
                byte[] inputData = new byte[availChars];
                con.getInputStream().read(inputData, 0, availChars);
                response = mapper.readValue(inputData, WhoAreYouResponse.class);

                if (response != null)
                {
                    gotAResponse = true;
                    name.set(response.getName());
                    version = new ApplicationVersion(response.getServerVersion());
                    serverIP.set(response.getServerIP());
                    
                    ObservableList<String> observableList = FXCollections.observableArrayList();
                    List<String> printerColours = response.getPrinterColours();
                    if(printerColours != null) 
                    {
                        observableList = FXCollections.observableArrayList(printerColours);
                    }
                    colours = new SimpleListProperty<>(observableList);
//                    if (!version.get().equalsIgnoreCase(BaseConfiguration.getApplicationVersion()))
//                    {
//                        setServerStatus(ServerStatus.WRONG_VERSION);
//                    }
                } else
                {
                    steno.warning("Got an indecipherable response from " + address.getHostAddress());
                }
            } else
            {
                steno.warning("No response from @ " + address.getHostAddress());
                //disconnect();
            }
        } catch (java.net.SocketTimeoutException stex)
        {
            long t2 = System.currentTimeMillis();
            steno.warning("Timeout whilst asking who are you @ " + address.getHostAddress() + " - time taken = " + Long.toString(t2 - t1));
            //disconnect();
        }
        catch (IOException ex)
        {
            steno.exception("Error whilst asking who are you @ " + address.getHostAddress(), ex);
            disconnect();
        }
        return gotAResponse;
    }

    public List<DetectedDevice> listAttachedPrinters()
    {
        String url = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + LIST_PRINTERS_COMMAND;

        long t1 = System.currentTimeMillis();
        try
        {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
            con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

            con.setConnectTimeout(connectTimeOutShort);
            con.setReadTimeout(readTimeOutShort);
            
            int responseCode = con.getResponseCode();

            if (responseCode == 200)
            {
                int availChars = con.getInputStream().available();
                byte[] inputData = new byte[availChars];
                con.getInputStream().read(inputData, 0, availChars);
                ListPrintersResponse listPrintersResponse = mapper.readValue(inputData, ListPrintersResponse.class);

                List<DetectedDevice> previousDetectedDevices = detectedDevices;
                detectedDevices = new ArrayList();
                // Move any existing devices from the current list to the new list.
                listPrintersResponse.getPrinterIDs().forEach((printerID) ->
                {
                     detectedDevices.add(previousDetectedDevices.stream()
                                                                .filter((d) -> d.getConnectionHandle().equals(printerID) && d.getConnectionType() == DeviceDetector.PrinterConnectionType.ROBOX_REMOTE)
                                                                .findAny()
                                                                .orElse(new RemoteDetectedPrinter(this, DeviceDetector.PrinterConnectionType.ROBOX_REMOTE, printerID)));
                });
                
                // Disconnect any devices that were previously found, but are not in the new list.
                previousDetectedDevices.forEach((device) -> 
                {
                    if (!detectedDevices.contains(device))
                    {
                        steno.info("Disconnecting missing device " + device.getConnectionHandle());
                        RoboxCommsManager.getInstance().disconnected(device);
                    }
                });
                pollCount = 0; // Successful contact, so zero the poll count;
            } else
            {
                disconnect();
                steno.warning("No response from @ " + address.getHostAddress());
            }
        } catch (java.net.SocketTimeoutException ex)
        {
            long t2 = System.currentTimeMillis();
            steno.error("Timeout whilst polling for remote printers @ " + address.getHostAddress() + " - time taken = " + Long.toString(t2 - t1));
            // But don't disconnect.
            //disconnect();
        }
        catch (IOException ex)
        {
            disconnect();
            steno.exception("Error whilst polling for remote printers @ " + address.getHostAddress(), ex);
        }
        return detectedDevices;
    }

    public void postRoboxPacket(String urlString) throws IOException
    {
        postRoboxPacket(urlString, null, null);
    }

    public Object postRoboxPacket(String urlString, String content, Class<?> expectedResponseClass) throws IOException
    {
        Object returnvalue = null;

        URL obj = new URL("http://" + address.getHostAddress() + ":" + Configuration.remotePort + urlString);
        try
        {
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
            con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

            con.setConnectTimeout(connectTimeOutLong);
            con.setReadTimeout(readTimeOutLong);

            if (content != null)
            {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", "" + content.length());
                con.getOutputStream().write(content.getBytes());
            }

            int responseCode = con.getResponseCode();
            pollCount = 0; // Successful contact, so zero the poll count;

            if (responseCode >= 200
                    && responseCode < 300)
            {
                if (expectedResponseClass != null)
                {
                    returnvalue = mapper.readValue(con.getInputStream(), expectedResponseClass);
                }
            } else
            {
                //Raise an error but don't disconnect...
                steno.error("Got " + responseCode + " when trying " + urlString);
            }
        }
        catch (java.net.SocketTimeoutException ex)
        {
            steno.error("Timeout in postRoboxPacket @" + obj.toString() + ", exception message = " + ex.getMessage());
            throw ex;
        }
        return returnvalue;
    }

    public int postData(String urlString, String content) throws IOException
    {
        URL obj = new URL("http://" + address.getHostAddress() + ":" + Configuration.remotePort + urlString);
        int rc = -1;

        try
        {
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("POST");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
            con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

            con.setReadTimeout(readTimeOutLong);
            con.setConnectTimeout(connectTimeOutLong);

            if (content != null)
            {
                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/json");
                con.setRequestProperty("Content-Length", "" + content.length());
                con.getOutputStream().write(content.getBytes());
            }

            rc = con.getResponseCode();
            pollCount = 0; // Successful contact, so zero the poll count;
        }
        catch (java.net.SocketTimeoutException ex)
        {
            steno.error("Timeout in postData @" + obj.toString() + ", exception message = " + ex.getMessage());
            throw ex;
        }
        return rc;
    }

    public int getData(String urlString) throws IOException
    {
        URL obj = new URL("http://" + address.getHostAddress() + ":" + Configuration.remotePort + urlString);
        int rc = -1;
        try
        {
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
            con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

            con.setConnectTimeout(connectTimeOutLong);
            con.setReadTimeout(readTimeOutLong);

            rc = con.getResponseCode();
            pollCount = 0; // Successful contact, so zero the poll count;
        }
        catch (java.net.SocketTimeoutException ex)
        {
            steno.error("Timeout in getData @" + obj.toString() + ", exception message = " + ex.getMessage());
            throw ex;
        }
        return rc;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(21, 31)
                .append(address)
                .toHashCode();
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof DetectedServer))
        {
            return false;
        }
        if (obj == this)
        {
            return true;
        }

        DetectedServer rhs = (DetectedServer) obj;
        return new EqualsBuilder()
                .append(address, rhs.address)
                .isEquals();
    }

    private class TransferProgressMonitor implements SftpProgressMonitor
    {
        long count = 0;
        long fileSize = 0;
        DetectedServer server;
        PercentProgressReceiver progressReceiver;
        
        public TransferProgressMonitor(DetectedServer server, PercentProgressReceiver progressReceiver)
        {
            this.server = server;
            this.progressReceiver = progressReceiver;
        }

        @Override
        public void init(int op, String src, String dest, long fileSize)
        {
            this.fileSize = fileSize;
            this.count = 0;

            server.resetPollCount();
            steno.debug("Initialise file transfer: src = \"" + src + "\", dst = \"" + dest + "\", fileSize = " + Long.toString(fileSize));
        }

        @Override
        public boolean count(long increment)
        {
          count += increment;
          float percentageDone = 50.0f;
          if (fileSize > 0)
            percentageDone = 25.0f + (75.0f * (float) count / (float) fileSize);
          progressReceiver.updateProgressPercent(percentageDone);

          server.resetPollCount();
          steno.debug("Transfer progress: " + Float.toString(percentageDone) + "%");
          return true;
        }
        
        @Override
        public void end(){
        }
    }
    
    public boolean upgradeRootSoftware(String path, String filename, PercentProgressReceiver progressReceiver)
    {
        boolean success = true;
        
        // First try SFTP;
        TransferProgressMonitor monitor = new TransferProgressMonitor(this, progressReceiver);
        SFTPUtils sftpHelper = new SFTPUtils(address.getHostAddress());
        File localFile = new File(path + filename);
        if (sftpHelper.transferToRemotePrinter(localFile, "/tmp", filename, monitor))
        {
            try
            {
                int rc = postData(SHUTDOWN_SYSTEM_COMMAND, null);
                success = (rc == 200);
            }
            catch (java.net.SocketTimeoutException stex)
            {
                success = true;
            }
            catch (IOException ex)
            {
                steno.debug("Exception in shutdown of remote server, server likely shutdown before response: " + ex.getMessage());
            }
        }
        
        if (!success)
        {
            // Try http POST
            String charset = "UTF-8";
            String requestURL = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + UPDATE_SYSTEM_COMMAND;

            try
            {
                long t1 = System.currentTimeMillis();
                MultipartUtility multipart = new MultipartUtility(requestURL, charset, StringToBase64Encoder.encode("root:" + getPin()));

                File rootSoftwareFile = new File(path + filename);
                steno.info("upgradeRootSoftware: uploading file " + path + filename);

                // Awkward lambda is to update the last response time whenever the progress bar is updated. This should prevent the server from
                // being removed.
                multipart.addFilePart("name", rootSoftwareFile,
                                      (double pp) -> 
                                      {
                                          pollCount = 0;
                                          progressReceiver.updateProgressPercent(pp);
                                      });
                long t2 = System.currentTimeMillis();
                steno.debug("upgradeRootSoftware: time to do multipart.addFilePartLong() = " + Long.toString(t2 - t1));

                List<String> response = multipart.finish();

                long t3 = System.currentTimeMillis();            
                steno.debug("upgradeRootSoftware: time to do multipart.finish() = " + Long.toString(t3 - t2) + ", total time = " + Long.toString(t3 - t1));

                success = true;
            } catch (IOException ex)
            {
                steno.error("Failure during write of root software: " + ex.getMessage());
            }
        }

        if (success)
        {
            // Disconnecting here does not clear the user interface, so set the poll count to force the user interface to disconnect.
            disconnect();
            pollCount = maxAllowedPollCount + 1;
        }
        
        return success;
    }

    public void saveFilament(Filament filament)
    {
        try
        {
            SerializableFilament serializableFilament = new SerializableFilament(filament);
            String jsonifiedData = mapper.writeValueAsString(serializableFilament);
            postData(SAVE_FILAMENT_COMMAND, jsonifiedData);
        } catch (IOException ex)
        {
            steno.exception("Failed to save filament to root " + getName(), ex);
        }
    }

    public void deleteFilament(Filament filament)
    {
        try
        {
            SerializableFilament serializableFilament = new SerializableFilament(filament);
            String jsonifiedData = mapper.writeValueAsString(serializableFilament);
            postData(DELETE_FILAMENT_COMMAND, jsonifiedData);
        } catch (IOException ex)
        {
            steno.exception("Failed to delete filament from root " + getName(), ex);
        }
    }

    @Override
    public String toString()
    {
        return name + "@" + address.getHostAddress() + " v" + version;
    }
}
