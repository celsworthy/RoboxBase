/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.postprocessor;

import celtech.roboxbase.postprocessor.PrintJobStatistics;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
        List<Double> layerNumberToPredictedDuration = new ArrayList<Double>();

        layerNumberToLineNumber.add(6);
        layerNumberToLineNumber.add(7);
        layerNumberToLineNumber.add(8);

        layerNumberToPredictedDuration.add(1.2);
        layerNumberToPredictedDuration.add(2.3);
        layerNumberToPredictedDuration.add(3.4);

        PrintJobStatistics printJobStatistics = new PrintJobStatistics(
                projectName,
                profileName,
                1,
                lineNumberOfFirstExtrusion,
                volumeUsed,
                0,
                lineNumberOfFirstExtrusion, layerNumberToLineNumber,
                layerNumberToPredictedDuration,
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
        assertEquals(printJobStatistics.getLayerNumberToPredictedDuration(),
                readIntoPrintJobStatistics.getLayerNumberToPredictedDuration());
        assertEquals(printJobStatistics.getLineNumberOfFirstExtrusion(),
                readIntoPrintJobStatistics.getLineNumberOfFirstExtrusion());
        assertEquals(printJobStatistics.getNumberOfLines(),
                readIntoPrintJobStatistics.getNumberOfLines());
    }

}
