/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.postprocessor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 *
 * @author tony
 */
public class PrintJobStatistics
{
    private String printJobID;
    private String projectName;
    private String profileName;
    private float layerHeight;
    private int numberOfLines;
    private double eVolumeUsed;
    private double dVolumeUsed;
    private List<Integer> layerNumberToLineNumber;
    private Map<Integer, Double> layerNumberToPredictedDuration_E_FeedrateDependent;
    private Map<Integer, Double> layerNumberToPredictedDuration_D_FeedrateDependent;
    private Map<Integer, Double> layerNumberToPredictedDuration_FeedrateIndependent;
    private double predictedDuration;
    private int lineNumberOfFirstExtrusion;

    public PrintJobStatistics()
    {
        printJobID = "";
        projectName = "";
        profileName = "";
        layerHeight = 0;
        numberOfLines = 0;
        eVolumeUsed = 0;
        dVolumeUsed = 0;
        lineNumberOfFirstExtrusion = 0;
        layerNumberToLineNumber = null;
        layerNumberToPredictedDuration_E_FeedrateDependent = null;
        layerNumberToPredictedDuration_D_FeedrateDependent = null;
        layerNumberToPredictedDuration_FeedrateIndependent = null;
        predictedDuration = 0;
    }

    public PrintJobStatistics(
            String printJobID,
            String projectName,
            String profileName,
            float layerHeight,
            int numberOfLines,
            double eVolumeUsed,
            double dVolumeUsed,
            int lineNumberOfFirstExtrusion,
            List<Integer> layerNumberToLineNumber,
            Map<Integer, Double> layerNumberToPredictedDuration_E_FeedrateDependent,
            Map<Integer, Double> layerNumberToPredictedDuration_D_FeedrateDependent,
            Map<Integer, Double> layerNumberToPredictedDuration_FeedrateIndependent,
            double predictedDuration
    )
    {
        this.printJobID = printJobID;
        this.projectName = projectName;
        this.profileName = profileName;
        this.layerHeight = layerHeight;
        this.numberOfLines = numberOfLines;
        this.eVolumeUsed = eVolumeUsed;
        this.dVolumeUsed = dVolumeUsed;
        this.lineNumberOfFirstExtrusion = lineNumberOfFirstExtrusion;
        this.layerNumberToLineNumber = layerNumberToLineNumber;
        this.layerNumberToPredictedDuration_E_FeedrateDependent = layerNumberToPredictedDuration_E_FeedrateDependent;
        this.layerNumberToPredictedDuration_D_FeedrateDependent = layerNumberToPredictedDuration_D_FeedrateDependent;
        this.layerNumberToPredictedDuration_FeedrateIndependent = layerNumberToPredictedDuration_FeedrateIndependent;
        this.predictedDuration = predictedDuration;
    }

    public void writeToFile(String statisticsFileLocation) throws IOException
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.writeValue(new File(statisticsFileLocation), this);
    }

    public String getPrintJobID()
    {
        return printJobID;
    }

    public void setPrintJobID(String printJobID)
    {
        this.printJobID = printJobID;
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

    public Map<Integer, Double> getLayerNumberToPredictedDuration_E_FeedrateDependent()
    {
        return layerNumberToPredictedDuration_E_FeedrateDependent;
    }

    public Map<Integer, Double> getLayerNumberToPredictedDuration_D_FeedrateDependent()
    {
        return layerNumberToPredictedDuration_D_FeedrateDependent;
    }

    public Map<Integer, Double> getLayerNumberToPredictedDuration_FeedrateIndependent()
    {
        return layerNumberToPredictedDuration_FeedrateIndependent;
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

    public void setLayerNumberToPredictedDuration_E_FeedrateDependent(Map<Integer, Double> layerNumberToPredictedDuration_E_FeedrateDependent)
    {
        this.layerNumberToPredictedDuration_E_FeedrateDependent = layerNumberToPredictedDuration_E_FeedrateDependent;
    }

    public void setLayerNumberToPredictedDuration_D_FeedrateDependent(Map<Integer, Double> layerNumberToPredictedDuration_D_FeedrateDependent)
    {
        this.layerNumberToPredictedDuration_D_FeedrateDependent = layerNumberToPredictedDuration_D_FeedrateDependent;
    }

    public void setLayerNumberToPredictedDuration_FeedrateIndependent(Map<Integer, Double> layerNumberToPredictedDuration_FeedrateIndependent)
    {
        this.layerNumberToPredictedDuration_FeedrateIndependent = layerNumberToPredictedDuration_FeedrateIndependent;
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
