/*
 * Command history saving the last N commands sent to the printer,
 * along with the response.
 */
package celtech.roboxbase.comms;

import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.rx.RxPacketTypeEnum;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import celtech.roboxbase.comms.tx.TxPacketTypeEnum;
import celtech.roboxbase.configuration.BaseConfiguration;
import libertysystems.stenographer.Stenographer;
import libertysystems.stenographer.StenographerFactory;

/**
 *
 * @author Tony
 */
public class CommandHistory {

    private static final Stenographer steno = StenographerFactory.getStenographer(CommandHistory.class.getName());
    
    public class CommandAndResponse {
        public RoboxTxPacket command;
        public RoboxRxPacket responsePacket;
        public byte responseCode;
        public byte[] rawResponse;
        public byte[] excess;
        public int count;
    }
    
    private final CommandAndResponse[] circularCommandHistory;
    private final int size;
    private int cchStart = 0;
    private int cchEnd = 0;
    private boolean saveActive = false;
    
    public CommandHistory(int size)
    {
        this.size = size + 1; // One slot is always unused.
        circularCommandHistory = new CommandAndResponse[this.size];
        for (int index = 0; index < this.size; ++index)
            circularCommandHistory[index] = new CommandAndResponse();
    }
    
    public void beginSave(RoboxTxPacket command,  byte responseCode)
    {
        saveActive = true;
        circularCommandHistory[cchEnd].command = command;
        circularCommandHistory[cchEnd].responseCode = responseCode;
        circularCommandHistory[cchEnd].responsePacket = null;
        circularCommandHistory[cchEnd].rawResponse = null;
        circularCommandHistory[cchEnd].excess = null;
        circularCommandHistory[cchEnd].count = 1;        
    }   

    public void appendRawResponse(byte[] rawResponse,  byte[] excess)
    {
        circularCommandHistory[cchEnd].rawResponse = rawResponse;
        circularCommandHistory[cchEnd].excess = excess;
    }   

    public void appendResponsePacket(RoboxRxPacket responsePacket)
    {
        circularCommandHistory[cchEnd].responsePacket = responsePacket;
    }
    
    public void completeSave()
    {
        if (saveActive)
        {
            saveActive = false;
            if (circularCommandHistory[cchEnd].command != null)
            {
                cchEnd = (cchEnd + 1) % size;
                if (cchEnd == cchStart)
                    cchStart = (cchStart + 1) % size;
            }
            else
            {
                circularCommandHistory[cchEnd].command = null;
                circularCommandHistory[cchEnd].responseCode = (byte)0;
                circularCommandHistory[cchEnd].responsePacket = null;
                circularCommandHistory[cchEnd].rawResponse = null;
                circularCommandHistory[cchEnd].excess = null;
            }
        }
    }

    public void dumpHistory()
    {
        completeSave();
        int nCommands = (cchEnd + size - cchStart) % size;
        steno.passthrough("Dump of the last " + nCommands + " commands sent to the printer");
        for (int index = 0; index < nCommands; ++index)
        {
            CommandAndResponse car = circularCommandHistory[index % size];
            StringBuilder output = new StringBuilder();
            output.append("[");
            output.append(index + 1);
            output.append("] =======================================\n");
            if (car.command != null)
            {
                output.append("Command code: ");
                output.append(String.format("0x%02X", car.command.getPacketType().getCommandByte()));
                output.append(" : ");
                output.append(car.command.getPacketType().toString());
                output.append(" -> ");
                output.append(car.command.getPacketType().getExpectedResponse().toString());
                output.append("\n");
                if (car.command.getMessagePayload() != null)
                {
                    output.append("Payload: \"");
                    output.append(car.command.getMessagePayload());
                    output.append("\"\n");
                }
            }
            output.append("Response code = ");
            output.append(String.format("0x%02X", car.responseCode));
            output.append(" : ");
            RxPacketTypeEnum rxPacketType = RxPacketTypeEnum.getEnumForCommand(car.responseCode);
            if (rxPacketType != null)
                output.append(rxPacketType.toString());
            else
                output.append("<UNKNOWN>");
            output.append("\n");
            if (car.responsePacket != null)
            {
                output.append("Payload: \"");
                output.append(car.responsePacket.getMessagePayload());
                output.append("\"\n");
            }
            else if (car.rawResponse != null)
            {
                output.append("Raw response = \n");
                for (int bIndex = 0; bIndex < car.rawResponse.length; ++bIndex)
                    output.append(String.format("0x%02X ", car.rawResponse[bIndex]));
                output.append("\n");
            }
            if (car.excess != null)
            {
                output.append("Excess = \n");
                for (int eIndex = 0; eIndex < car.excess.length; ++eIndex)
                    output.append(String.format("0x%02X ", car.excess[eIndex]));
                output.append("\n");
            }
            output.append("-----------------------------------------\n");
            
            steno.passthrough(output.toString());
        }
    }   
}
