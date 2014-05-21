/**
 * Part of the DroidIBus Project
 *
 * @author Ted S <tass2001@gmail.com>
 * @package net.littlebigisland.droidibus
 *
 */
package net.littlebigisland.droidibus.ibus;

/**
 * Interface to build call backs off of
 */
public interface IBusMessageReceiver{

	// Radio System
	public void onUpdateStation(final String text);
	
	// IKE System
	public void onUpdateRange(final String range);
		
	public void onUpdateOutdoorTemp(final String temp);

	public void onUpdateFuel1(final String mpg);

	public void onUpdateFuel2(final String mpg);

	public void onUpdateAvgSpeed(final String speed);
	
	public void onUpdateTime(final String time);
	
	public void onUpdateDate(final String date);
	
	public void onUpdateSpeed(final String speed);

	public void onUpdateRPM(final String rpm);
	
	public void onUpdateCoolantTemp(final String temp);
	
	public void onUpdateIgnitionSate(final int state);
	
	// Navigation System
	public void onUpdateStreetLocation(final String streetName);
	
	public void onUpdateGPSCoordinates(final String gpsCoordinates);
	
	public void onUpdateLocale(final String cityName);
	
	// Steering Wheel System	
	public void onTrackFwd();
	
	public void onTrackPrev();
	
}