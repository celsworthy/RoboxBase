package celtech.roboxbase.services.printing;

import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.remote.RoboxRemoteCommandInterface;
import celtech.roboxbase.postprocessor.PrintJobStatistics;
import celtech.roboxbase.printerControl.comms.commands.GCodeMacros;
import celtech.roboxbase.printerControl.model.Printer;
import celtech.roboxbase.printerControl.model.PrinterException;
import celtech.roboxbase.utils.SystemUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;
import java.util.Scanner;
import javafx.beans.property.IntegerProperty;
import javafx.concurrent.Task;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author ianhudson
 */
public class TransferGCodeToPrinterTask extends Task<GCodePrintResult>
{

    private Printer printerToUse = null;
    private String gcodeFileToPrint = null;
    private String printJobID = null;
    private final Stenographer steno = StenographerFactory.
            getStenographer(this.getClass().getName());
    private IntegerProperty linesInFile = null;
    private boolean printUsingSDCard = true;
    private boolean dontInitiatePrint = false;
    private int startFromSequenceNumber = 0;
    private boolean thisJobCanBeReprinted = false;
    private int lineCounter = 0;
    private int numberOfLines = 0;
    private final PrintJobStatistics printJobStatistics;

    /**
     *
     * @param printerToUse
     * @param modelFileToPrint
     * @param printJobID
     * @param linesInFile
     * @param printUsingSDCard
     * @param startFromSequenceNumber
     * @param thisJobCanBeReprinted
     * @param dontInitiatePrint
     * @param printJobStatistics
     */
    public TransferGCodeToPrinterTask(Printer printerToUse,
            String modelFileToPrint,
            String printJobID,
            IntegerProperty linesInFile,
            boolean printUsingSDCard,
            int startFromSequenceNumber,
            boolean thisJobCanBeReprinted,
            boolean dontInitiatePrint,
            PrintJobStatistics printJobStatistics
    )
    {
        this.printerToUse = printerToUse;
        this.gcodeFileToPrint = modelFileToPrint;
        this.printJobID = printJobID;
        this.linesInFile = linesInFile;
        this.printUsingSDCard = printUsingSDCard;
        this.startFromSequenceNumber = startFromSequenceNumber;
        this.thisJobCanBeReprinted = thisJobCanBeReprinted;
        this.dontInitiatePrint = dontInitiatePrint;
        this.printJobStatistics = printJobStatistics;
        updateProgress(0.0, 100.0);
    }

    @Override
    protected GCodePrintResult call() throws Exception
    {
        GCodePrintResult result = new GCodePrintResult();
        result.setPrintJobID(printJobID);

        boolean gotToEndOK = false;

        updateTitle("GCode Print ID:" + printJobID);
        int sequenceNumber = 0;
        boolean successfulWrite = false;
        File gcodeFile = new File(gcodeFileToPrint);
        FileReader gcodeReader = null;
        Scanner scanner = null;
        numberOfLines = GCodeMacros.countLinesInMacroFile(gcodeFile, ";");
        linesInFile.setValue(numberOfLines);

        steno.info("Beginning transfer of file " + gcodeFileToPrint + " to printer from line "
                + startFromSequenceNumber);
        
        if (printerToUse.getCommandInterface() instanceof RoboxRemoteCommandInterface)
        {
            //We're talking to a remote printer
            //Send the statistics
            ((RoboxRemoteCommandInterface)printerToUse.getCommandInterface()).sendStatistics(printJobStatistics);
        }
        
        //Note that FileReader is used, not File, since File is not Closeable
        try
        {
            gcodeReader = new FileReader(gcodeFile);
            scanner = new Scanner(gcodeReader);

            if (printUsingSDCard && startFromSequenceNumber == 0)
            {
                printerToUse.initialiseDataFileSend(printJobID, thisJobCanBeReprinted);
            }

            printerToUse.resetDataFileSequenceNumber();
            printerToUse.setDataFileSequenceNumberStartPoint(startFromSequenceNumber);

            updateMessage("Transferring data");

            lineCounter = 0;

            final int bufferSize = 512;
            StringBuffer outputBuffer = new StringBuffer(bufferSize);

            while (scanner.hasNextLine() && !isCancelled())
            {
                String line = scanner.nextLine();
                line = line.trim();

                if (GCodeMacros.isMacroExecutionDirective(line))
                {
                    //Put in contents of macro
                    List<String> macroLines = GCodeMacros.getMacroContents(line, printerToUse.headProperty().get().typeCodeProperty().get(), false, false, false);
                    for (String macroLine : macroLines)
                    {
                        outputLine(macroLine);
                    }
                } else
                {
                    outputLine(line);
                }

                if (lineCounter < numberOfLines)
                {
                    updateProgress((float) lineCounter, (float) numberOfLines);
                }
            }
            gotToEndOK = true;
        } catch (FileNotFoundException ex)
        {
            steno.error("Couldn't open gcode file " + gcodeFileToPrint + ": " + ex);
        } catch (RoboxCommsException ex)
        {
            steno.error("Error during print operation - abandoning print " + printJobID + " " + ex.
                    getMessage());
            if (printUsingSDCard)
            {
                try
                {
                    printerToUse.cancel(null);
                } catch (PrinterException exp)
                {
                    steno.error("Error cancelling print - " + exp.getMessage());
                }
            }
            updateMessage("Printing error");
        } finally
        {
            if (scanner != null)
            {
                scanner.close();
            }

            if (gcodeReader != null)
            {
                gcodeReader.close();
            }
        }

        result.setSuccess(gotToEndOK);
        return result;
    }

    private void outputLine(String line) throws RoboxCommsException, DatafileSendNotInitialised
    {
        if (line.equals("") == false && line.startsWith(";") == false)
        {
            line = SystemUtils.cleanGCodeForTransmission(line);
            if (printUsingSDCard)
            {
                steno.trace("Sending data line " + lineCounter + " to printer");
                printerToUse.sendDataFileChunk(line, lineCounter == numberOfLines - 1,
                        true);
                if (startFromSequenceNumber == 0
                        && !dontInitiatePrint
                        && ((printerToUse.getDataFileSequenceNumber() > 1
                        && printerToUse.isPrintInitiated() == false)
                        || (lineCounter == numberOfLines - 1
                        && printerToUse.isPrintInitiated() == false)))
                {
                    //Start printing!
                    printerToUse.initiatePrint(printJobID);
                }
            } else
            {
                printerToUse.sendRawGCode(line, false);
            }
            lineCounter++;
        }
    }

}
