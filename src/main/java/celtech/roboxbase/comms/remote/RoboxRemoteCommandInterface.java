package celtech.roboxbase.comms.remote;

import celtech.roboxbase.comms.CommandInterface;
import celtech.roboxbase.comms.PrinterStatusConsumer;
import celtech.roboxbase.comms.RemoteDetectedPrinter;
import celtech.roboxbase.comms.exceptions.ConnectionLostException;
import celtech.roboxbase.comms.exceptions.RoboxCommsException;
import celtech.roboxbase.comms.rx.RoboxRxPacket;
import celtech.roboxbase.comms.tx.RoboxTxPacket;
import celtech.roboxbase.printerControl.model.Printer;

/**
 *
 * @author Ian Hudson @ Liberty Systems Limited
 */
public class RoboxRemoteCommandInterface extends CommandInterface
{

    private final RemoteClient remoteClient;

    public RoboxRemoteCommandInterface(PrinterStatusConsumer controlInterface,
            RemoteDetectedPrinter printerHandle,
            boolean suppressPrinterIDChecks, int sleepBetweenStatusChecks)
    {
        super(controlInterface, printerHandle, suppressPrinterIDChecks, sleepBetweenStatusChecks);
        this.setName("RemoteCI:" + printerHandle + " " + this.toString());
        remoteClient = new RemoteClient(printerHandle);
    }

    @Override
    protected boolean connectToPrinter()
    {
        remoteClient.connect(printerHandle.getConnectionHandle());
        return true;
    }

    @Override
    protected void disconnectPrinter()
    {
        remoteClient.disconnect(printerHandle.getConnectionHandle());
        controlInterface.disconnected(printerHandle);
        keepRunning = false;
    }

    @Override
    public synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite,
            boolean dontPublishResult) throws RoboxCommsException
    {
        RoboxRxPacket rxPacket = remoteClient.writeToPrinter(printerHandle.getConnectionHandle(), messageToWrite);

        if (!dontPublishResult)
        {
            printerToUse.processRoboxResponse(rxPacket);
        }

        return rxPacket;
    }

//    @Override
//    public synchronized RoboxRxPacket writeToPrinter(RoboxTxPacket messageToWrite,
//            boolean dontPublishResult) throws RoboxCommsException
//    {
//        RoboxRxPacket receivedPacket = null;
//
//        if (commsState == RoboxCommsState.CONNECTED
//                || commsState == RoboxCommsState.CHECKING_FIRMWARE
//                || commsState == RoboxCommsState.CHECKING_ID
//                || commsState == RoboxCommsState.DETERMINING_PRINTER_STATUS)
//        {
//            try
//            {
//                byte[] outputBuffer = messageToWrite.toByteArray();
//
//                
//                remoteClient.writeAndWaitForData(outputBuffer);
//
//                byte[] respCommand = remoteClient.readSerialPort(1);
//
//                RxPacketTypeEnum packetType = RxPacketTypeEnum.getEnumForCommand(respCommand[0]);
//                if (packetType != null)
//                {
//                    if (packetType != messageToWrite.getPacketType().getExpectedResponse())
//                    {
//                        throw new InvalidResponseFromPrinterException(
//                                "Expected response of type "
//                                + messageToWrite.getPacketType().getExpectedResponse().name()
//                                + " and got "
//                                + packetType);
//                    }
//                    steno.trace("Got a response packet back of type: " + packetType.toString());
//                    RoboxRxPacket rxPacketTemplate = RoboxRxPacketFactory.createPacket(packetType);
//                    int packetLength = rxPacketTemplate.packetLength(firmwareVersionInUse);
//
//                    byte[] inputBuffer = null;
//                    if (packetType.containsLengthField())
//                    {
//                        byte[] lengthData = remoteClient.readSerialPort(packetType.
//                                getLengthFieldSize());
//
//                        int payloadSize = Integer.valueOf(new String(lengthData), 16);
//                        if (packetType == RxPacketTypeEnum.LIST_FILES_RESPONSE)
//                        {
//                            payloadSize = payloadSize * 16;
//                        }
//
//                        inputBuffer = new byte[1 + packetType.getLengthFieldSize() + payloadSize];
//                        for (int i = 0; i < packetType.getLengthFieldSize(); i++)
//                        {
//                            inputBuffer[1 + i] = lengthData[i];
//                        }
//
//                        byte[] payloadData = remoteClient.readSerialPort(payloadSize);
//                        for (int i = 0; i < payloadSize; i++)
//                        {
//                            inputBuffer[1 + packetType.getLengthFieldSize() + i] = payloadData[i];
//                        }
//                    } else
//                    {
//                        inputBuffer = new byte[packetLength];
//                        int bytesToRead = packetLength - 1;
//                        byte[] payloadData = remoteClient.readSerialPort(bytesToRead);
//                        for (int i = 0; i < bytesToRead; i++)
//                        {
//                            inputBuffer[1 + i] = payloadData[i];
//                        }
//                    }
//
//                    inputBuffer[0] = respCommand[0];
//
//                    try
//                    {
//                        receivedPacket = RoboxRxPacketFactory.createPacket(inputBuffer, firmwareVersionInUse);
//                        steno.
//                                trace("Got packet of type " + receivedPacket.getPacketType().name());
//
//                        if (!dontPublishResult)
//                        {
//                            printerToUse.processRoboxResponse(receivedPacket);
//                        }
//                    } catch (InvalidCommandByteException ex)
//                    {
//                        steno.error("Command byte of " + String.format("0x%02X", inputBuffer[0])
//                                + " is invalid.");
//                    } catch (UnknownPacketTypeException ex)
//                    {
//                        steno.error("Packet type unknown for command byte "
//                                + String.format("0x%02X", inputBuffer[0]) + " is invalid.");
//                    } catch (UnableToGenerateRoboxPacketException ex)
//                    {
//                        steno.error("A packet that appeared to be of type " + packetType.name()
//                                + " could not be unpacked.");
//                    }
//                } else
//                {
//                    // Attempt to drain the crud from the input
//                    // There shouldn't be anything here but just in case...
//                    byte[] storage = remoteClient.readAllDataOnBuffer();
//
//                    try
//                    {
//                        String received = new String(storage);
//
//                        steno.warning("Invalid packet received from firmware: " + received);
//                    } catch (Exception e)
//                    {
//                        steno.warning(
//                                "Invalid packet received from firmware - couldn't print contents");
//                    }
//
////                    InvalidResponseFromPrinterException exception = new InvalidResponseFromPrinterException("Invalid response - got: " + received);
////                    throw exception;
//                }
//            } catch (LowLevelInterfaceException ex)
//            {
//                steno.exception("Remote comms exception", ex);
//                actionOnCommsFailure();
//            }
//        } else
//        {
//            throw new RoboxCommsException("Invalid state for writing data");
//        }
////        steno.debug("Command Interface send - completed " + messageToWrite.getPacketType());
//        return receivedPacket;
//    }
    private void actionOnCommsFailure() throws ConnectionLostException
    {
        //If we get an exception then abort and treat
        steno.debug("Error during write to printer");
        disconnectPrinter();
        keepRunning = false;
        throw new ConnectionLostException();
    }

    void setPrinterToUse(Printer newPrinter)
    {
        this.printerToUse = newPrinter;
    }

    /**
     *
     * @param sleepMillis
     */
    @Override
    public void setSleepBetweenStatusChecks(int sleepMillis)
    {
        sleepBetweenStatusChecks = sleepMillis;
    }
}
