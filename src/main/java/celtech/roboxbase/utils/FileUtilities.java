package celtech.roboxbase.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author Ian
 */
public class FileUtilities
{

    public static void writeStreamToFile(InputStream is, String localFilename) throws IOException
    {
        FileOutputStream fos = null;

        File localFile = new File(localFilename);
        fos = FileUtils.openOutputStream(localFile);

        try
        {

            byte[] buffer = new byte[4096];              //declare 4KB buffer
            int len;

            //while we have availble data, continue downloading and storing to local file
            while ((len = is.read(buffer)) > 0)
            {
                fos.write(buffer, 0, len);
            }
        } finally
        {
            try
            {
                if (is != null)
                {
                    is.close();
                }
            } finally
            {
                if (fos != null)
                {
                    fos.close();
                }
            }
        }
    }

}
