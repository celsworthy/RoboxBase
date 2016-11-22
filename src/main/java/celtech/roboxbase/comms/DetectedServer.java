package celtech.roboxbase.comms;

import celtech.roboxbase.comms.remote.Configuration;
import celtech.roboxbase.comms.remote.ListPrintersResponse;
import celtech.roboxbase.comms.remote.StringToBase64Encoder;
import celtech.roboxbase.comms.remote.WhoAreYouResponse;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.configuration.BaseConfiguration;
import celtech.roboxbase.configuration.CoreMemory;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javafx.beans.property.ObjectProperty;
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
    
    private final InetAddress address;
    private StringProperty name = new SimpleStringProperty("");
    private StringProperty version = new SimpleStringProperty("");
    private StringProperty pin = new SimpleStringProperty("1111");
    
    @JsonIgnore
    private static final ObjectMapper mapper = new ObjectMapper();
    
    @JsonIgnore
    private final ObjectProperty<ServerStatus> serverStatus = new SimpleObjectProperty<>(ServerStatus.UNKNOWN);
    
    @JsonIgnore
    private static final String defaultUser = "root";
    
    @JsonIgnore
    private static final String contentPage = "/rootMenu.html";
    
    public enum ServerStatus
    {
        
        UNKNOWN,
        FOUND,
        OK,
        NOT_THERE,
        WRONG_VERSION
    }
    
    public DetectedServer()
    {
        this.address = null;
        this.name = null;
        this.version = null;
    }
    
    public DetectedServer(InetAddress address)
    {
        this.address = address;
    }
    
    public InetAddress getAddress()
    {
        return address;
    }
    
    public String getName()
    {
        return name.get();
    }
    
    public void setName(String name)
    {
        this.name.set(name);
    }
    
    public String getVersion()
    {
        return version.get();
    }
    
    public void setVersion(String version)
    {
        this.version.set(version);
    }
    
    public ServerStatus getServerStatus()
    {
        return serverStatus.get();
    }
    
    @JsonIgnore
    public ObjectProperty<ServerStatus> getServerStatusProperty()
    {
        return serverStatus;
    }
    
    public String getPin()
    {
        return pin.get();
    }
    
    public void setPin(String pin)
    {
        this.pin.set(pin);
    }
    
    public void connect()
    {
        if (version.get().equalsIgnoreCase(BaseConfiguration.getApplicationVersion()))
        {
            try
            {
                postData(contentPage);
                CoreMemory.getInstance().activateRoboxRoot(this);
            } catch (IOException ex)
            {
                serverStatus.set(ServerStatus.NOT_THERE);
            }
        } else
        {
            serverStatus.set(ServerStatus.WRONG_VERSION);
        }
    }
    
    public void disconnect()
    {
        CoreMemory.getInstance().deactivateRoboxRoot(this);
        serverStatus.set(ServerStatus.UNKNOWN);
    }
    
    public boolean whoAmI()
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
                }
            } else
            {
                serverStatus.set(ServerStatus.NOT_THERE);
                steno.warning("No response from @ " + address.getHostAddress());
            }
        } catch (IOException ex)
        {
            steno.error("Error whilst asking who are you @ " + address.getHostAddress());
            serverStatus.set(ServerStatus.NOT_THERE);
        }
        
        return gotAResponse;
    }
    
    public List<DetectedDevice> listAttachedPrinters()
    {
        List<DetectedDevice> detectedDevices = new ArrayList();
        
        String url = "http://" + address.getHostAddress() + ":" + Configuration.remotePort + "/api/discovery/listPrinters";
        
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

//                steno.info("Got a response from server " + this.getName() + " " + listPrintersResponse.getPrinterIDs().size() + " printers attached");
                listPrintersResponse.getPrinterIDs().forEach((printerID) ->
                {
                    RemoteDetectedPrinter detectedPrinter = new RemoteDetectedPrinter(this, DeviceDetector.PrinterConnectionType.ROBOX_REMOTE, printerID);
                    detectedDevices.add(detectedPrinter);
                });
                serverStatus.set(ServerStatus.OK);
            } else
            {
                serverStatus.set(ServerStatus.NOT_THERE);
                steno.warning("No response from @ " + address.getHostAddress());
            }
        } catch (IOException ex)
        {
            serverStatus.set(ServerStatus.NOT_THERE);
            steno.exception("Error whilst polling for remote printers @ " + address.getHostAddress(), ex);
        }
        return detectedDevices;
    }
    
    public void postData(String urlString) throws IOException
    {
        postData(urlString, null, null);
    }
    
    public RoboxRxPacket postData(String urlString, String content, Class<?> expectedResponseClass) throws IOException
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
        int responseCode = con.getResponseCode();
        
        if (responseCode == 200)
        {
            if (expectedResponseClass != null)
            {
                returnvalue = mapper.readValue(con.getInputStream(), expectedResponseClass);
            }
        } else
        {
            steno.warning("Got " + responseCode + " when trying " + urlString);
        }
        
        return (RoboxRxPacket) returnvalue;
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
                .append(name, rhs.name)
                .append(version, rhs.version)
                .isEquals();
    }
    
    @Override
    public String toString()
    {
        return name + "@" + address.getHostAddress() + " v" + version;
    }
}
