/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.postprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author tony
 */
public class PrintJobStatistics
{

    private String projectName;
    private String profileName;
    private float layerHeight;
    private int numberOfLines;
    private double eVolumeUsed;
    private double dVolumeUsed;
    private List<Integer> layerNumberToLineNumber;
    private List<Double> layerNumberToPredictedDuration;
    private double predictedDuration;
    private int lineNumberOfFirstExtrusion;

    public PrintJobStatistics()
    {
        projectName = "";
        profileName = "";
        layerHeight = 0;
        numberOfLines = 0;
        eVolumeUsed = 0;
        dVolumeUsed = 0;
        lineNumberOfFirstExtrusion = 0;
        layerNumberToLineNumber = null;
        layerNumberToPredictedDuration = null;
        predictedDuration = 0;
    }

    public PrintJobStatistics(
            String projectName,
            String profileName,
            float layerHeight,
            int numberOfLines,
            double eVolumeUsed,
            double dVolumeUsed,
            int lineNumberOfFirstExtrusion,
            List<Integer> layerNumberToLineNumber,
            List<Double> layerNumberToPredictedDuration,
            double predictedDuration)
    {
        this.projectName = projectName;
        this.profileName = profileName;
        this.layerHeight = layerHeight;
        this.numberOfLines = numberOfLines;
        this.eVolumeUsed = eVolumeUsed;
        this.dVolumeUsed = dVolumeUsed;
        this.lineNumberOfFirstExtrusion = lineNumberOfFirstExtrusion;
        this.layerNumberToLineNumber = layerNumberToLineNumber;
        this.layerNumberToPredictedDuration = layerNumberToPredictedDuration;
        this.predictedDuration = predictedDuration;
    }

    public void writeToFile(String statisticsFileLocation) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(statisticsFileLocation), this);
    }

    /**
     * @return the numberOfLines
     */
    public int getNumberOfLines()
    {
        return numberOfLines;
    }

    /**
     * @return the volumeUsed for Extruder E
     */
    public double geteVolumeUsed()
    {
        return eVolumeUsed;
    }

    /**
     * @return the volumeUsed for Extruder D
     */
    public double getdVolumeUsed()
    {
        return dVolumeUsed;
    }

    /**
     * @return the layerNumberToLineNumber
     */
    public List<Integer> getLayerNumberToLineNumber()
    {
        return layerNumberToLineNumber;
    }

    /**
     * @return the layerNumberToPredictedDuration
     */
    public List<Double> getLayerNumberToPredictedDuration()
    {
        return layerNumberToPredictedDuration;
    }

    /**
     * @return the lineNumberOfFirstExtrusion
     */
    public int getLineNumberOfFirstExtrusion()
    {
        return lineNumberOfFirstExtrusion;
    }

    /**
     * @return the predictedDuration
     */
    public double getPredictedDuration()
    {
        return predictedDuration;
    }

    public float getLayerHeight()
    {
        return layerHeight;
    }

    public void setLayerHeight(float layerHeight)
    {
        this.layerHeight = layerHeight;
    }

    public String getProjectName()
    {
        return projectName;
    }

    public void setProjectName(String projectName)
    {
        this.projectName = projectName;
    }

    public String getProfileName()
    {
        return profileName;
    }

    public void setProfileName(String profileName)
    {
        this.profileName = profileName;
    }

    public void setNumberOfLines(int numberOfLines)
    {
        this.numberOfLines = numberOfLines;
    }

    public void seteVolumeUsed(double eVolumeUsed)
    {
        this.eVolumeUsed = eVolumeUsed;
    }

    public void setdVolumeUsed(double dVolumeUsed)
    {
        this.dVolumeUsed = dVolumeUsed;
    }

    public void setLayerNumberToLineNumber(List<Integer> layerNumberToLineNumber)
    {
        this.layerNumberToLineNumber = layerNumberToLineNumber;
    }

    public void setLayerNumberToPredictedDuration(List<Double> layerNumberToPredictedDuration)
    {
        this.layerNumberToPredictedDuration = layerNumberToPredictedDuration;
    }

    public void setPredictedDuration(double predictedDuration)
    {
        this.predictedDuration = predictedDuration;
    }

    public void setLineNumberOfFirstExtrusion(int lineNumberOfFirstExtrusion)
    {
        this.lineNumberOfFirstExtrusion = lineNumberOfFirstExtrusion;
    }

    /**
     * Create a PrintJobStatistics and populate it from a saved file
     *
     * @param absolutePath the path of the file to load
     * @return
     * @throws IOException
     */
    public static PrintJobStatistics readFromFile(String absolutePath) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        PrintJobStatistics printJobStatistics = mapper.readValue(new File(
                absolutePath), PrintJobStatistics.class);
        return printJobStatistics;
    }

}
