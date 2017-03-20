package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.comms.remote.clear.ListPrintersResponse;
import celtech.roboxbase.comms.remote.StringToBase64Encoder;
import celtech.roboxbase.comms.remote.clear.WhoAreYouResponse;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
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
    private final StringProperty serverIP = new SimpleStringProperty("");
    private final StringProperty version = new SimpleStringProperty("");
    private final StringProperty pin = new SimpleStringProperty("1111");

    @JsonIgnore
    private final BooleanProperty dataChanged = new SimpleBooleanProperty(false);

    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();

    @JsonIgnore
    private final ObjectProperty<ServerStatus> serverStatus = new SimpleObjectProperty<>(ServerStatus.NOT_CONNECTED);

    @JsonIgnore
    public static final String defaultUser = "root";

    @JsonIgnore
    private static final String LIST_PRINTERS_COMMAND = "/api/discovery/listPrinters";
    @JsonIgnore
    private static final String UPDATE_SYSTEM_COMMAND = "/api/admin/updateSystem";

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

    public DetectedServer()
    {
    }

    public DetectedServer(InetAddress address)
    {
        this.address = address;
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
        return serverIP.get();
    }

    public void setServerIP(String serverIP)
    {
        if (!serverIP.equals(this.serverIP))
        {
            this.serverIP.set(serverIP);
            dataChanged.set(!dataChanged.get());
        }
    }

    public StringProperty serverIPProperty()
    {
        return serverIP;
    }

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
        return serverStatus.get();
    }

    public void setServerStatus(ServerStatus status)
    {
//        steno.info("Updating status of server " + getName() + " to " + status.name());
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

    public ReadOnlyBooleanProperty dataChangedProperty()
    {
        return dataChanged;
    }

    public void connect()
    {
        if (serverStatus.get() != ServerStatus.WRONG_VERSION
                && serverStatus.get() != ServerStatus.CONNECTED)
        {
            try
            {
                if (!version.get().equalsIgnoreCase(BaseConfiguration.getApplicationVersion()))
                {
                    setServerStatus(ServerStatus.WRONG_VERSION);
                } else
                {

                    int response = getData(LIST_PRINTERS_COMMAND);
                    if (response == 200)
                    {
                        setServerStatus(ServerStatus.CONNECTED);
                    } else if (response == 401)
                    {
                        setServerStatus(ServerStatus.WRONG_PIN);
                    } else
                    {
                        setServerStatus(ServerStatus.NOT_CONNECTED);
                    }
                }
            } catch (IOException ex)
            {
                setServerStatus(ServerStatus.NOT_CONNECTED);
            }
        }
    }

    public void disconnect()
    {
        setServerStatus(ServerStatus.NOT_CONNECTED);
    }

    public boolean whoAreYou()
    {
        boolean gotAResponse = false;
        WhoAreYouResponse response = null;

        String url = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + "/api/discovery/whoareyou";

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
                response = mapper.readValue(inputData, WhoAreYouResponse.class);

                if (response != null)
                {
                    gotAResponse = true;
                    name.set(response.getName());
                    version.set(response.getServerVersion());
                    serverIP.set(response.getServerIP());
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
        } catch (IOException ex)
        {
            steno.error("Error whilst asking who are you @ " + address.getHostAddress());
            disconnect();
        }

        return gotAResponse;
    }

    public List<DetectedDevice> listAttachedPrinters()
    {
        List<DetectedDevice> detectedDevices = new ArrayList();

        String url = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + LIST_PRINTERS_COMMAND;

        try
        {
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();

            // optional default is GET
            con.setRequestMethod("GET");

            //add request header
            con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
            con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

            con.setConnectTimeout(5000);
            int responseCode = con.getResponseCode();

            if (responseCode == 200)
            {
                int availChars = con.getInputStream().available();
                byte[] inputData = new byte[availChars];
                con.getInputStream().read(inputData, 0, availChars);
                ListPrintersResponse listPrintersResponse = mapper.readValue(inputData, ListPrintersResponse.class);

                listPrintersResponse.getPrinterIDs().forEach((printerID) ->
                {
                    RemoteDetectedPrinter detectedPrinter = new RemoteDetectedPrinter(this, DeviceDetector.PrinterConnectionType.ROBOX_REMOTE, printerID);
                    detectedDevices.add(detectedPrinter);
                });
            } else
            {
                disconnect();
                steno.warning("No response from @ " + address.getHostAddress());
            }
        } catch (IOException ex)
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
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");

        //add request header
        con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
        con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));
        con.setConnectTimeout(2000);
        con.setReadTimeout(2000);

        if (content != null)
        {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Content-Length", "" + content.length());
            con.getOutputStream().write(content.getBytes());
        }

        int responseCode = con.getResponseCode();
        if (responseCode >= 200
                && responseCode < 300)
        {
            if (expectedResponseClass != null)
            {
                returnvalue = mapper.readValue(con.getInputStream(), expectedResponseClass);
            }
        } else
        {
            steno.error("Got " + responseCode + " when trying " + urlString);
            disconnect();
        }

        return returnvalue;
    }

    public int postData(String urlString, String content) throws IOException
    {
        Object returnvalue = null;

        URL obj = new URL("http://" + address.getHostAddress() + ":" + Configuration.remotePort + urlString);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");

        //add request header
        con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
        con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

        if (content != null)
        {
            con.setDoOutput(true);
            con.setRequestProperty("Content-Type", "application/json");
            con.setRequestProperty("Content-Length", "" + content.length());
            con.getOutputStream().write(content.getBytes());
        }

        con.setConnectTimeout(2000);

        return con.getResponseCode();
    }

    public int getData(String urlString) throws IOException
    {
        Object returnvalue = null;

        URL obj = new URL("http://" + address.getHostAddress() + ":" + Configuration.remotePort + urlString);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        //add request header
        con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());
        con.setRequestProperty("Authorization", "Basic " + StringToBase64Encoder.encode("root:" + getPin()));

        con.setConnectTimeout(2000);

        return con.getResponseCode();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(21, 31)
                .append(address)
                .append(name)
                .append(version)
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
                .append(name.get(), rhs.name.get())
                .append(version.get(), rhs.version.get())
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

            multipart.addFilePart("name", rootSoftwareFile, progressReceiver);

            List<String> response = multipart.finish();
            success = true;
        } catch (IOException ex)
        {
            steno.error("Failure during write of root software: " + ex.getMessage());
        }

        return success;
    }

    @Override
    public String toString()
    {
        return name + "@" + address.getHostAddress() + " v" + version;
    }
}
