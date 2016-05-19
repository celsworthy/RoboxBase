/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.postprocessor;

import celtech.roboxbase.postprocessor.PrintJobStatistics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.assertEquals;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 *
 * @author tony
 */
public class PrintJobStatisticsTest
{

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void testWriteToFileAndReadBack() throws IOException
    {
        String projectName = "blah";
        String profileName = "blah2";
        double volumeUsed = 100;
        int lineNumberOfFirstExtrusion = 5;
        List<Integer> layerNumberToLineNumber = new ArrayList<>();
        Map<Integer, Double> layerNumberToPredictedDuration_E = new HashMap<>();
        Map<Integer, Double> layerNumberToPredictedDuration_D = new HashMap<>();
        Map<Integer, Double> layerNumberToPredictedDuration_feedrateIndependent = new HashMap<>();

        layerNumberToLineNumber.add(6);
        layerNumberToLineNumber.add(7);
        layerNumberToLineNumber.add(8);

        layerNumberToPredictedDuration_E.put(0, 1.2);
        layerNumberToPredictedDuration_E.put(1, 2.3);
        layerNumberToPredictedDuration_E.put(2, 3.4);

        PrintJobStatistics printJobStatistics = new PrintJobStatistics(
                projectName,
                profileName,
                1,
                lineNumberOfFirstExtrusion,
                volumeUsed,
                0,
                lineNumberOfFirstExtrusion,
                layerNumberToLineNumber,
                layerNumberToPredictedDuration_E,
                layerNumberToPredictedDuration_D,
                layerNumberToPredictedDuration_feedrateIndependent,
                6.9);

        File testFile = temporaryFolder.newFile();
        printJobStatistics.writeToFile(testFile.getAbsolutePath());

        PrintJobStatistics readIntoPrintJobStatistics = PrintJobStatistics.readFromFile(testFile.getAbsolutePath());

        assertEquals(printJobStatistics.getProjectName(),
                readIntoPrintJobStatistics.getProjectName());
        assertEquals(printJobStatistics.getLayerHeight(),
                readIntoPrintJobStatistics.getLayerHeight(), 0.001);
        assertEquals(printJobStatistics.getLayerNumberToLineNumber(),
                readIntoPrintJobStatistics.getLayerNumberToLineNumber());
        assertEquals(printJobStatistics.getLayerNumberToPredictedDuration_E_FeedrateDependent(),
                readIntoPrintJobStatistics.getLayerNumberToPredictedDuration_E_FeedrateDependent());
        assertEquals(printJobStatistics.getLayerNumberToPredictedDuration_D_FeedrateDependent(),
                readIntoPrintJobStatistics.getLayerNumberToPredictedDuration_D_FeedrateDependent());
        assertEquals(printJobStatistics.getLayerNumberToPredictedDuration_FeedrateIndependent(),
                readIntoPrintJobStatistics.getLayerNumberToPredictedDuration_FeedrateIndependent());
        assertEquals(printJobStatistics.getLineNumberOfFirstExtrusion(),
                readIntoPrintJobStatistics.getLineNumberOfFirstExtrusion());
        assertEquals(printJobStatistics.getNumberOfLines(),
                readIntoPrintJobStatistics.getNumberOfLines());
    }

}
