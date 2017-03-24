package magomar.meteoro.other;

import java.util.Date;

import magomar.meteoro.openweather.OpenWeatherClient;

/**
 * @author Mario Gomez
 * 
 */
public class Util {
	public static boolean isDouble(String s) {
		try {
			Double.parseDouble(s);
		} catch (NumberFormatException e) {
			return false;
		}
		return true;
	}

	public static boolean isUpdatable(long savedTime) {
		long currentTime = new Date().getTime();
		return currentTime - savedTime > OpenWeatherClient.UPDATE_TIME_INTERVAL;
	}
}
