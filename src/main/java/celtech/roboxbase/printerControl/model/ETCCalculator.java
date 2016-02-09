/*
 * Copyright 2014 CEL UK
 */
package celtech.roboxbase.printerControl.model;

import celtech.roboxbase.printerControl.model.Printer;
import java.util.ArrayList;
import java.util.List;

/**
 * ETCCalculator calculates the ETC (estimated time to complete) of print jobs
 * for a given line number being processed.
 *
 * @author tony
 */
public class ETCCalculator
{

    /**
     * The line number at the top of the given layer. The first element should
     * have a value of 0.
     */
    private final List<Integer> layerNumberToLineNumber;
    private final List<Double> layerNumberToPredictedDuration;
    /**
     * The time taken to get to the start of the given layer. The first element
     * should have a value of 0.
     */
    private final List<Double> layerNumberToTotalPredictedDuration = new ArrayList<>();
    /**
     * The time to print all layers
     */
    double totalPredictedDurationAllLayers;

    /**
     * The estimated number of seconds it takes to heat the bed up by one degree
     */
    protected static int PREDICTED_BED_HEAT_RATE = 2;
    private final Printer printer;

    public ETCCalculator(Printer printer,
            List<Double> layerNumberToPredictedDuration,
            List<Integer> layerNumberToLineNumber)
    {
        this.printer = printer;
        this.layerNumberToLineNumber = layerNumberToLineNumber;
        this.layerNumberToPredictedDuration = layerNumberToPredictedDuration;

        assert (layerNumberToPredictedDuration.get(0) == 0);
        assert (layerNumberToLineNumber.get(0) == 0);

        double totalPredictedDuration = 0;
        for (int i = 0; i < layerNumberToPredictedDuration.size(); i++)
        {
            totalPredictedDuration += layerNumberToPredictedDuration.get(i);
            layerNumberToTotalPredictedDuration.add(i, totalPredictedDuration);
        }
        totalPredictedDurationAllLayers = totalPredictedDuration;
    }

    /**
     * Calculate the ETC based on predicted durations.
     *
     * @return the number of seconds
     */
    public int getETCPredicted(int lineNumber)
    {
        int remainingTimeSeconds = getPredictedRemainingPrintTime(lineNumber);
        remainingTimeSeconds += getBedHeatingTime();
        return remainingTimeSeconds;
    }

    /**
     * Estimate the time to heat the bed up to the target temperature
     *
     * @return the time in seconds
     */
    private int getBedHeatingTime()
    {
        int bedTargetTemperature = printer.getPrinterAncillarySystems().bedTargetTemperatureProperty().get();
        int bedTemperature = printer.getPrinterAncillarySystems().bedTemperatureProperty().get();
        if (bedTemperature < bedTargetTemperature)
        {
            return (bedTargetTemperature - bedTemperature)
                    * PREDICTED_BED_HEAT_RATE;
        } else
        {
            return 0;
        }
    }

    /**
     * Return the predicted remaining print time by calculating the predicted
     * time to reach the current line, and subtract from the predicted total
     * time.
     */
    private int getPredictedRemainingPrintTime(int lineNumber)
    {
        int layerNumber = getCurrentLayerNumberForLineNumber(lineNumber);
        double totalPredictedDurationForCurrentLayer = layerNumberToTotalPredictedDuration.get(
                layerNumber - 1);
        double predictedDurationInNextLayer = getPartialDurationInLayer(
                layerNumber, lineNumber);
        double totalDurationSoFar = totalPredictedDurationForCurrentLayer
                + predictedDurationInNextLayer;
        int remainingTimeSeconds = (int) ((totalPredictedDurationAllLayers
                - totalDurationSoFar));
        return remainingTimeSeconds;
    }

    /**
     * Get the completed layer number for the given line number.
     */
    public int getCurrentLayerNumberForLineNumber(int lineNumber)
    {
        for (int layerNumber = 0; layerNumber < layerNumberToLineNumber.size(); layerNumber++)
        {
            Integer lineNumberForLayer = layerNumberToLineNumber.get(layerNumber);
            if (lineNumberForLayer >= lineNumber)
            {
                return layerNumber + 1;
            }
        }
        throw new RuntimeException(
                "Did not calculate layer number - line number greater"
                + " than total number of lines");
    }

    /**
     * Return the estimated duration to partially print a layer up to the given
     * line number.
     */
    protected double getPartialDurationInLayer(int layerNumber, int lineNumber)
    {
        double numLinesAtStartOfLayer = 0;
        if (layerNumber > 1)
        {
            numLinesAtStartOfLayer = layerNumberToLineNumber.get(layerNumber - 2);
        }
        double progressInThisLayer = lineNumber
                - numLinesAtStartOfLayer;
        if (progressInThisLayer == 0)
        {
            return 0;
        }

        double numLinesAtEndOfLayer = layerNumberToLineNumber.get(layerNumber - 1);
        double durationInLayer = layerNumberToPredictedDuration.get(layerNumber - 1);
        double totalLinesInNextLayer = numLinesAtEndOfLayer
                - numLinesAtStartOfLayer;
        return (progressInThisLayer / totalLinesInNextLayer)
                * durationInLayer;
    }

    /**
     * Return the percentage complete based on the line number reached.
     */
    public double getPercentCompleteAtLine(int lineNumber)
    {
        double percent = (totalPredictedDurationAllLayers
                - getPredictedRemainingPrintTime(lineNumber))
                / totalPredictedDurationAllLayers;

        if (percent < 0)
        {
            percent = 0;
        }

        return percent;
    }

}
