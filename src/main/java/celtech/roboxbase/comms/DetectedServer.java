package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.comms.remote.clear.ListPrintersResponse;
import celtech.roboxbase.comms.remote.StringToBase64Encoder;
import celtech.roboxbase.comms.remote.clear.WhoAreYouResponse;
import celtech.roboxbase.comms.remote.types.SerializableFilament;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
import celtech.roboxbase.configuration.Filament;
import celtech.roboxbase.utils.PercentProgressReceiver;
import celtech.roboxbase.utils.net.MultipartUtility;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
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

    @JsonIgnore
    private final Stenographer steno = StenographerFactory.getStenographer(DetectedServer.class.getName());

    private InetAddress address;
    private final StringProperty name = new SimpleStringProperty("");
    @JsonIgnore
    private final StringProperty serverIP = new SimpleStringProperty("");
    private final StringProperty version = new SimpleStringProperty("");
    private final StringProperty pin = new SimpleStringProperty("1111");
    private final BooleanProperty wasAutomaticallyAdded = new SimpleBooleanProperty(true);

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
    public static final int connectTimeOutShort = 200;
    @JsonIgnore
    public static final int readTimeOutLong = 15000;
    @JsonIgnore
    public static final int connectTimeOutLong = 2000;
    @JsonIgnore
    public static final int maxAllowedPollCount = 5;
    @JsonIgnore
    private static final String LIST_PRINTERS_COMMAND = "/api/discovery/listPrinters";
    @JsonIgnore
    private static final String UPDATE_SYSTEM_COMMAND = "/api/admin/updateSystem";
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
                              .filter(s -> s.getAddress() == address)
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

    public boolean maxPollCountExceeded()
    {
        if (pollCount > maxAllowedPollCount)
        {
            steno.warning("Maximum poll count of " + getName() + " exceeded! Count = " + Integer.toString(pollCount));
            return true;
        }
        else
            return false;
        //return (pollCount > maxAllowedPollCount);
    }

    public boolean incrementPollCount()
    {
        ++pollCount;
        steno.info("Incrementing poll count of " + getName() + " to " + Integer.toString(pollCount));
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

    public String getVersion()
    {
        return version.get();
    }

    public void setVersion(String version)
    {
        if (!version.equals(this.version))
        {
            this.version.set(version);
            dataChanged.set(!dataChanged.get());
        }
    }

    public StringProperty versionProperty()
    {
        return version;
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

        if (serverStatus.get() != ServerStatus.WRONG_VERSION
                && serverStatus.get() != ServerStatus.CONNECTED)
        {
            try
            {
                if (!version.get().equalsIgnoreCase(BaseConfiguration.getApplicationVersion()) &&
                    !(BaseConfiguration.getApplicationVersion().startsWith("tadev") && version.get().startsWith("tadev"))) // Debug hack to allow mismatching development versions to operate.
                {
                    setServerStatus(ServerStatus.WRONG_VERSION);
                    CoreMemory.getInstance().deactivateRoboxRoot(this);
                } else
                {

                    int response = getData(LIST_PRINTERS_COMMAND);
                    if (response == 200)
                    {
                        setServerStatus(ServerStatus.CONNECTED);
                        CoreMemory.getInstance().activateRoboxRoot(this);
                        success = true;
                    } else if (response == 401)
                    {
                        setServerStatus(ServerStatus.WRONG_PIN);
                        CoreMemory.getInstance().deactivateRoboxRoot(this);
                    } else
                    {
                        setServerStatus(ServerStatus.NOT_CONNECTED);
                        CoreMemory.getInstance().deactivateRoboxRoot(this);
                    }
                }
            } catch (IOException ex)
            {
                setServerStatus(ServerStatus.NOT_CONNECTED);
                CoreMemory.getInstance().deactivateRoboxRoot(this);
            }
        }

        return success;
    }

    public void disconnect()
    {
        steno.info("Disconnecting " + name.get());
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

        String url = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + "/api/discovery/whoareyou";

        long t1 = System.currentTimeMillis();
        try
        {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());

            con.setConnectTimeout(connectTimeOutShort);
            con.setReadTimeout(readTimeOutShort);

            int responseCode = con.getResponseCode();

            if (responseCode == 200)
            {
                int availChars = con.getInputStream().available();
                byte[] inputData = new byte[availChars];
                con.getInputStream().read(inputData, 0, availChars);
                response = mapper.readValue(inputData, WhoAreYouResponse.class);

                if (response != null)
                {
                    gotAResponse = true;
                    name.set(response.getName());
                    version.set(response.getServerVersion());
                    serverIP.set(response.getServerIP());
                    pollCount = 0; // Successfull contact, so zero the poll count;
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
                disconnect();
                steno.warning("No response from @ " + address.getHostAddress());
            }
        } catch (java.net.SocketTimeoutException stex)
        {
            long t2 = System.currentTimeMillis();
            steno.error("Timeout whilst asking who are you @ " + address.getHostAddress() + " - time taken = " + Long.toString(t2 - t1));
            disconnect();
        }
        catch (IOException ex)
        {
            steno.error("Error whilst asking who are you @ " + address.getHostAddress());
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

    public boolean upgradeRootSoftware(String path, String filename, PercentProgressReceiver progressReceiver)
    {
        boolean success = false;
        String charset = "UTF-8";
        String requestURL = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + UPDATE_SYSTEM_COMMAND;

        try
        {
            MultipartUtility multipart = new MultipartUtility(requestURL, charset, StringToBase64Encoder.encode("root:" + getPin()));

            File rootSoftwareFile = new File(path + filename);

            // Awkward lambda is to update the last response time whenever the progress bar is updated. This should prevent the server from
            // being removed.
            multipart.addFilePart("name", rootSoftwareFile, (double pp) -> { pollCount = 0; progressReceiver.updateProgressPercent(pp); });

            List<String> response = multipart.finish();
            pollCount = 0; // Successful contact, so zero the poll count;
            
            success = true;
        } catch (IOException ex)
        {
            steno.error("Failure during write of root software: " + ex.getMessage());
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
