package net.littlebigisland.droidibus.ibus;

import java.util.ArrayList;

public class SteeringWheelSystemCommand extends IBusSystemCommand{
	
	class Radio extends IBusSystemCommand{

		public void mapReceived(ArrayList<Byte> msg) {
			currentMessage = msg;
			if(currentMessage.get(3) == 0x3B){
				switch(currentMessage.get(4)){
					case 0x21: //Fwds Btn
						triggerCallback("onTrackFwd");
						break;
					case 0x28: //Prev Btn
						triggerCallback("onTrackPrev");
						break;
				}
			}
		}
		
	}
	
	class Telephone extends IBusSystemCommand{

		public void mapReceived(ArrayList<Byte> msg) {
			currentMessage = msg;
			if(currentMessage.get(3) == 0x3B && currentMessage.get(4) == (byte) 0xA0){
				triggerCallback("onVoiceBtnPress");
			}else if(currentMessage.get(3) == 0x01){
				triggerCallback("onRTBtnPress");
			}
		}
		
	}
	
	/**
	 * Cstruct - Register destination systems
	 */
	SteeringWheelSystemCommand(){
		IBusDestinationSystems.put(DeviceAddress.Radio.toByte(), new Radio());
		IBusDestinationSystems.put(DeviceAddress.Telephone.toByte(), new Telephone());
	}
}
