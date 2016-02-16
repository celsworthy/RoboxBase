package celtech.roboxbase.comms.remote;

import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.configuration.BaseConfiguration;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Ian
 */
public class RemoteWebHelper
{

    private final static Stenographer steno = StenographerFactory.getStenographer(RemoteWebHelper.class.getName());
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void postData(String urlString) throws IOException
    {
        postData(urlString, null, null);
    }

    public static RoboxRxPacket postData(String urlString, String content, Class<?> expectedResponseClass) throws IOException
    {
        Object returnvalue = null;

        URL obj = new URL(urlString);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");

        //add request header
        con.setRequestProperty("User-Agent", BaseConfiguration.getApplicationName());

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
}
