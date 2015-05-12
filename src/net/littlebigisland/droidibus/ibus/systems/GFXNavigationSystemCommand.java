package net.littlebigisland.droidibus.ibus.systems;
import java.util.ArrayList;

import net.littlebigisland.droidibus.ibus.DeviceAddressEnum;
import net.littlebigisland.droidibus.ibus.IBusSystemCommand;

public class GFXNavigationSystemCommand extends IBusSystemCommand{
	
	/**
	 *  Messages from Radio in the trunk to the BoardMonitor
	 */
	class RadioSystem extends IBusSystemCommand{
		
		private final byte stationText = 0x23;
		private final byte metaData = (byte)0xA5;
		
		public void mapReceived(ArrayList<Byte> msg){
			currentMessage = msg;
			
			switch(currentMessage.get(3)){
				case stationText:
					triggerCallback("onUpdateRadioStation", decodeData());
					break;
				case metaData:
					byte metaDataType = currentMessage.get(6);
					String dataText = decodeData();

					switch(metaDataType){
						case 0x41: // Broadcast Type
							triggerCallback("onUpdateRadioBrodcasts", dataText);
							break;
						case 0x04:
						case 0x44: // Stereo Indicators
							triggerCallback("onUpdateRadioStereoIndicator", dataText);
							break;
						case 0x45: // Radio Data System Indicator
							triggerCallback("onUpdateRadioRDSIndicator", dataText);
							break;
						case 0x02: // Program Type
							triggerCallback("onUpdateRadioProgramIndicator", dataText);
							break;
					}
					break;
			}

		}
		
		/**
		 * Iterate through the message and remove all non-ASCII data
		 * 
		 * @return String 	String representation of data
		 */
		private String decodeData(){
			int startByte = (currentMessage.get(3) == stationText) ? 6 : 7; // Radio RDS starts 6 bytes in, metadata at 7
			int endByte = currentMessage.size() - 2; // Skip the CRC
			
			// Remove the padding from the front and back of the message
			while(currentMessage.get(endByte) == 0x20)
				endByte--;
			while(currentMessage.get(startByte) == 0x20)
				startByte++;
			return decodeMessage(currentMessage, startByte, endByte);			
		}

	}
	
	public GFXNavigationSystemCommand(){
		IBusDestinationSystems.put(DeviceAddressEnum.Radio.toByte(), new RadioSystem());
	}

}