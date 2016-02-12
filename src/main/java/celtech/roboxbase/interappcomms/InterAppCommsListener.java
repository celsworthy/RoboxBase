package celtech.roboxbase.interappcomms;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;
import javafx.application.Platform;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class InterAppCommsListener extends Thread
{

    private final Stenographer steno = StenographerFactory.getStenographer(InterAppCommsListener.class.getName());
    private boolean keepRunning = true;
    private final int PORT = 32145;
    private ServerSocket initialServerSocket;
    private Socket serverSocket;

    @Override
    public void run()
    {
        while (keepRunning)
        {
            try
            {
                byte[] buffer = new byte[4096];
                int bufferOffset = 0;
                serverSocket = initialServerSocket.accept();
                DataInputStream is = new DataInputStream(serverSocket.getInputStream());

                while (is.available() > 0)
                {
                    int bytesRead = is.read(buffer, bufferOffset, buffer.length - bufferOffset);
                    bufferOffset += bytesRead;
                }
                
                String input = Arrays.toString(buffer);
                
                steno.info("Was passed data by a sibling:" + input);

                is.close();
                serverSocket.close();
            } catch (IOException ex)
            {
            }

            steno.info("Got a connection from a sibling trying to start up! : ");
        }
    }

    public InterAppStartupStatus letUsBegin(List<String> parameters)
    {
        InterAppStartupStatus status = InterAppStartupStatus.OTHER_ERROR;

        try
        {
            //Bind to localhost adapter with a zero connection queue 
            initialServerSocket = new ServerSocket(PORT, 0, InetAddress.getLocalHost());

            status = InterAppStartupStatus.STARTED_OK;
            this.start();

        } catch (BindException e)
        {
            // If we had any load params then
            steno.info("AutoMaker asked to start but instance is already running.");
            status = InterAppStartupStatus.ALREADY_RUNNING_COULDNT_CONTACT;

            try
            {
                Socket clientSocket = new Socket(InetAddress.getLocalHost(), PORT);
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                StringBuilder paramString = new StringBuilder();
                parameters.forEach(parameter ->
                {
                    paramString.append(parameter);
                });

                String dataToSend = paramString.toString();
                out.printf("%d", dataToSend.length());
                out.printf("%s", dataToSend);
                out.flush();

                clientSocket.close();
                steno.info("Told my sibling about the params I was passed");
                status = InterAppStartupStatus.ALREADY_RUNNING_CONTACT_MADE;
            } catch (IOException ex)
            {
                steno.error("IOException when contacting sibling:" + ex.getMessage());
            } finally
            {
                Platform.exit();
            }
        } catch (IOException e)
        {
            steno.error("Unexpected error whilst attempting to check if another app is running");
            e.printStackTrace();
            Platform.exit();
        }

        return status;
    }

    public void shutdown()
    {
        keepRunning = false;
    }
}
