package celtech.roboxbase.printerControl.comms.commands;

import java.io.File;
import java.io.FilenameFilter;

/**
 *
 * @author Ian
 */
public class FilenameStartsWithFilter implements FilenameFilter
{

    private final String baseFilename;

    public FilenameStartsWithFilter(String baseFilename)
    {
        this.baseFilename = baseFilename.toUpperCase();
    }

    @Override
    public boolean accept(File dir, String name)
    {
        return (name.toUpperCase().startsWith(baseFilename));
    }
}
