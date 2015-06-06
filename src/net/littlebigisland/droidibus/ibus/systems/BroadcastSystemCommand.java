package net.littlebigisland.droidibus.ibus.systems;

import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class BroadcastSystemCommand extends IBusSystemCommand{
    
    /**
     * Messages from IKE to the Broadcast
     */
    class IKESystem extends IBusSystemCommand{
        
        private final byte mOBCData = 0x24;
        /**
         * Handle OBC messages sent from IKE
         * IBus Message: 80 0C FF 24 <System> 00 32 31 3A 31 30 20 20 6E 
         */
        private void OBCData(){
            // Minus two because the array starts at zero
            // and we need to ignore the last byte (XOR Checksum)
            int endByte = currentMessage.size() - 2;
            switch(currentMessage.get(4)){
                case 0x01: // Time
                    String curTime = "";
                    // The time replaces zeros with space when hour is lt 10
                    if(currentMessage.get(endByte) == 0x20){
                        curTime = decodeMessage(
                            currentMessage, 6, endByte - 2
                        ).replace(" ", "0");
                    }else{
                        // AM/PM Support
                        curTime = decodeMessage(currentMessage, 6, endByte);
                    }
                    triggerCallback("onUpdateTime", curTime);
                    break;
                case 0x02: // Date
                    /* Dots are retarded and even though this
                     * is personal preference I'm coding all
                     * The way down here where it doesn't
                     * belong (outside of the view) SUE ME. */
                    triggerCallback(
                        "onUpdateDate",
                        decodeMessage(
                            currentMessage, 6, endByte
                        ).replace(".", "/")
                    );
                    break;
                case 0x03: //Outdoor Temperature
                    // Handle triple digit temperature
                    int startByte = (currentMessage.get(6) != 0x20) ? 6 : 7;
                    // Clean up the ending space
                    if(currentMessage.get(endByte) == 0x20){
                        endByte -= 1;
                    }
                    triggerCallback(
                        "onUpdateOutdoorTemp",
                        decodeMessage(currentMessage, startByte, endByte)
                    );
                    break;
                case 0x04: // Fuel 1
                    triggerCallback(
                        "onUpdateFuel1",
                        decodeMessage(currentMessage, 6, endByte)
                    );
                    break;
                case 0x05: // Fuel 2
                    triggerCallback(
                        "onUpdateFuel2",
                        decodeMessage(currentMessage, 6, endByte)
                    );
                    break;
                case 0x06: // Range
                    triggerCallback(
                        "onUpdateRange",
                        decodeMessage(currentMessage, 6, endByte)
                    );
                    break;
                case 0x0A: // AVG Speed
                    triggerCallback(
                        "onUpdateAvgSpeed",
                        decodeMessage(currentMessage, 6, endByte)
                    );
                    break;
                case 0x07: // Distance
                case 0x08: // Unknown
                case 0x09: // Limit
                case 0x0E: // Timer
                case 0x0F: // AUX Heater 1
                case 0x10: // AUX Heater 2
                    // Not implementing
                    break;
            }
        }
        
        public void mapReceived(ArrayList<Byte> msg){
            currentMessage = msg;
            byte operation = msg.get(3);
            if(operation == mOBCData)
                OBCData();
        }
        
    }
    
    public BroadcastSystemCommand(){
        IBusDestinationSystems.put(
            DeviceAddressEnum.InstrumentClusterElectronics.toByte(), new IKESystem()
        );
    }

}