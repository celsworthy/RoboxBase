/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celtech.roboxbase.services.slicer;

import java.io.IOException;

/**
 *
 * @author ianhudson
 */
public class SlicedFile
{

    private String UUIDString = null;
    private final String UUIDStringTag = "fileUUID";

    /**
     *
     */
    public SlicedFile()
    {
    }
    
    /**
     *
     * @param UUIDString
     */
    public SlicedFile(String UUIDString)
    {
        this.UUIDString = UUIDString;
    }
    
    /**
     *
     * @return
     */
    public String getUUIDString()
    {
        return UUIDString;
    }

    /**
     *
     * @param UUIDString
     */
    public void setUUIDString(String UUIDString)
    {
        this.UUIDString = UUIDString;
    }
}
