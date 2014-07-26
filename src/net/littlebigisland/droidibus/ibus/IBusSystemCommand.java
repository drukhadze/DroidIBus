package net.littlebigisland.droidibus.ibus;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.os.Handler;
import android.util.Log;

/**
 * This class represents the base of which all IBus Systems will extend from
 * in order to parse sent/received messages.
 * 
 * Subsequent subclasses implement message handlers for destination systems
 * using this same class. I.e. Source system IKE Extends IBusSystemCommand
 * it's child class, say GlobalBroadcastSystem also implements IBusSystemCommand.
 * This represents an IBus Message's "Source" and "Destination" system architecture.
 * 
 * Please note that it makes the most sense to do received message processing inside the
 * destination implementation HOWEVER, the source system should be the one handling
 * out bound messages to another destination system. For example, BM -> IKE Messages
 * should be initiated from the BM Class, not the IKE class nor a child
 * implementation of the IKE class inside the BM class.
 */
public abstract class IBusSystemCommand {
	
	// The variable that holds a list of interfaces provided by listeners
	private ArrayList<IBusMessageReceiver> mCallbackListeners = new ArrayList<IBusMessageReceiver>();
	// The variable that holds the handler for every thread with an implemented interface
	private ArrayList<Handler> mCallbackHandlers = new ArrayList<Handler>();
	// ArrayList holding the message currently being processed.
	public ArrayList<Byte> currentMessage = null;
	// Map used to map implementation of Destination systems from each Source System 
	public Map<Byte, IBusSystemCommand> IBusDestinationSystems = new HashMap<Byte, IBusSystemCommand>();
	
	/** 
	 * Converts a Byte Coded Decimal to it's String representation
	 * @param bcd The byte to decode
	 * @return 	  The String representation of the param byte
	 */
	public String bcdToStr(byte bcd) {
		StringBuffer strBuff = new StringBuffer();
		
		byte high = (byte) (bcd & 0xf0);
		high >>>= (byte) 4;	
		high = (byte) (high & 0x0f);
		byte low = (byte) (bcd & 0x0f);
		
		strBuff.append(high);
		strBuff.append(low);
		
		return strBuff.toString();
	}
	
	/** 
	 * Decode Parts of an IBus Message from Bytes into a readable String
	 * 
	 * @param msg 		The Message we're decoding
	 * @param startByte	The index of the first byte we are extracting
	 * @param endByte	The index of the last byte we are extracting
	 * @return The message in UTF-8 String formatting
	 */
	public String decodeMessage(ArrayList<Byte> msg, int startByte, int endByte){
		ArrayList<Byte> tempBytes = new ArrayList<Byte>();
		while(startByte <= endByte){
			tempBytes.add(msg.get(startByte));
			startByte++;
		}
		byte[] strByte = new byte[tempBytes.size()];
		for(int i = 0; i < tempBytes.size(); i++){
			strByte[i] = tempBytes.get(i);
		}
		try {
			return new String(strByte, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return "";
		}
	}
	
	/**
	 * Generate CRC for the message
	 */
	public byte genMessageCRC(byte[] msg){
		byte crc = 0x00;
		for(int i = 0; i < msg.length; i++)
			crc = (byte) (crc ^ msg[i]);
		return crc;
	}
	
	/** 
	 * Use the implementations destination system Map to lookup
	 * the instance class that handles the given message. In some instances
	 * it makes to override this method with more implementation specific code.
	 * 
	 * @param msg	The Message to be handled
	 * @throws NoSuchMethodException 
	 */
	public void mapReceived(ArrayList<Byte> msg){
		try{
			IBusDestinationSystems.get((byte) msg.get(2)).mapReceived(msg);
		}catch(NullPointerException npe){
			// Things not in the map throw a NullPointerException
		}
	}
	
	/**
	 * Register the programmers callback implementation with the class.
	 * Introspectively register this receiver to all children in the
	 * `IBusDestinationSystems` Map.
	 * 
	 * @param cb 	Your implementation of the IBusMessageReceiver
	 * 
	 */
	public void registerCallback(IBusMessageReceiver cb, Handler handler){
		mCallbackListeners.add(cb);
		mCallbackHandlers.add(handler);
		for (Object key : IBusDestinationSystems.keySet()){
			IBusDestinationSystems.get(key).registerCallback(cb, handler);
		}
		
	}
	
	/**
	 * Trigger a method callback. Check to see if the callback receiver has been defined, 
	 * if so call the given method.
	 * @param callback The name of the function to trigger
	 * @throws NoSuchMethodException 
	 */
	public void triggerCallback(final String callback){
		Log.d("DroidIBus", "Triggering callback!");
		if(mCallbackListeners.size() > 0){
			Log.d("DroidIBus", "Callback Size > 0!");
			for(int i = 0; i < mCallbackListeners.size(); i++){
				Log.d("DroidIBus", "Callback Loop!");
				final IBusMessageReceiver mCallbackReceiver = mCallbackListeners.get(i);
				Handler mHandler = mCallbackHandlers.get(i);
				Log.d("DroidIBus", "Prior to launching runnable");
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						try{
							Log.d("DroidIBus", "Running callback!");
							final Method cb = mCallbackReceiver.getClass().getMethod(callback);
							cb.invoke(mCallbackReceiver);
						}catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
							Log.d("DroidIBus", "Callback Exception!");
							e.printStackTrace();
						}
					}
				});
			}
		}else{
			Log.d("DroidIBus", "No Callback listeners?");
		}
		Log.d("DroidIBus", "Done with callback!");
	}
	
	/**
	 * Trigger a method callback. Check to see if the callback receiver has been defined, 
	 * if so call the given method with the given parameters (if any). Overloaded 
	 * for different data types.
	 * @param callback The name of the function to trigger
	 * @param value    The string value to pass to the callback
	 */
	public void triggerCallback(final String callback, final String value){
		Log.d("DroidIBus", "Triggering callback!");
		if(mCallbackListeners.size() > 0){
			Log.d("DroidIBus", "Callback Size > 0!");
			for(int i = 0; i < mCallbackListeners.size(); i++){
				Log.d("DroidIBus", "Callback Loop!");
				final IBusMessageReceiver mCallbackReceiver = mCallbackListeners.get(i);
				Handler mHandler = mCallbackHandlers.get(i);
				Log.d("DroidIBus", "Prior to launching runnable");
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						try{
							Log.d("DroidIBus", "Running callback!");
							Method cb = mCallbackReceiver.getClass().getMethod(callback, String.class);
							cb.invoke(mCallbackReceiver, value);
						}catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
							Log.d("DroidIBus", "Callback Exception!");
							e.printStackTrace();
						}
					}
				});
			}
		}else{
			Log.d("DroidIBus", "No Callback listeners?");
		}
		Log.d("DroidIBus", "Done with callback!");
	}
	
	/**
	 * Trigger a method callback. Check to see if the callback receiver has been defined, 
	 * if so call the given method with the given parameters (if any). Overloaded 
	 * for different data types.
	 * @param callback The name of the function to trigger
	 * @param value    The integer value to pass to the callback
	 */	
	public void triggerCallback(final String callback, final int value){
		Log.d("DroidIBus", "Triggering callback!");
		if(mCallbackListeners.size() > 0){
			Log.d("DroidIBus", "Callback Size > 0!");
			for(int i = 0; i < mCallbackListeners.size(); i++){
				Log.d("DroidIBus", "Callback Loop!");
				final IBusMessageReceiver mCallbackReceiver = mCallbackListeners.get(i);
				Handler mHandler = mCallbackHandlers.get(i);
				Log.d("DroidIBus", "Prior to launching runnable");
				mHandler.post(new Runnable(){
					@Override
					public void run() {
						try{
							Log.d("DroidIBus", "Running callback!");
							final Method cb = mCallbackReceiver.getClass().getMethod(callback, int.class);
							cb.invoke(mCallbackReceiver, value);
						}catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException e) {
							Log.d("DroidIBus", "Callback Exception!");
							e.printStackTrace();
						}
					}
				});
			}
		}else{
			Log.d("DroidIBus", "No Callback listeners?");
		}
		Log.d("DroidIBus", "Done with callback!");
	}
}
