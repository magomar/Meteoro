package magomar.meteoro.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import magomar.meteoro.R;
import magomar.meteoro.openweather.OpenWeatherClient;
import magomar.meteoro.openweather.datamodel.Clouds;
import magomar.meteoro.openweather.datamodel.Main;
import magomar.meteoro.openweather.datamodel.WeatherData;
import magomar.meteoro.openweather.datamodel.Wind;
import magomar.meteoro.other.MeteoroPreferences;
import magomar.meteoro.other.Util;

public class MainActivity extends Activity {
	private SharedPreferences preferences;
	private Button updateLocationButton;
	private Button updateWeatherButton;
	private TextView lastLocation;
	private TextView stationField;
	private TextView temperatureField;
	private TextView cloudsField;
	private TextView humidityField;
	private TextView windField;
	private TextView pressureField;
	private TextView weatherField;
	private TextView timeUpdateField;
	private ImageView weatherImage;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		preferences = getSharedPreferences(
				MeteoroPreferences.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
		updateLocationButton = (Button) findViewById(R.id.relocate_button);
		updateWeatherButton = (Button) findViewById(R.id.update_weather_button);
		lastLocation = (TextView) findViewById(R.id.lastLocationInfo);
		stationField = (TextView) findViewById(R.id.station_field);
		timeUpdateField = (TextView) findViewById(R.id.time_update_field);
		temperatureField = (TextView) findViewById(R.id.temperature_field);
		cloudsField = (TextView) findViewById(R.id.clouds_field);
		humidityField = (TextView) findViewById(R.id.humidity_field);
		windField = (TextView) findViewById(R.id.wind_field);
		pressureField = (TextView) findViewById(R.id.pressure_field);
		weatherField = (TextView) findViewById(R.id.weather_field);
		weatherImage = (ImageView) findViewById(R.id.weather_image);
		updateLocationButton.setOnClickListener(new RelocateButtonListener());
		updateWeatherButton.setOnClickListener(new UpdateButtonListener());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	protected void onResume() {
		super.onResume();
		Double location[] = loadLastLocation();
		if (location[0] == null || location[1] == null) {
			return;
		}
		double latitude = location[0];
		double longitude = location[1];
		String latDir = (latitude < 0 ? "S" : "N");
		String longDir = (longitude < 0 ? "W" : "E");

		String latStr = String.format("%.2f",latitude);
		String longStr = String.format("%.2f",longitude);

		lastLocation.setText(latStr + latDir + " " + longStr + longDir);

		Weather task = new Weather();
		task.execute(location);
	}

	private Double[] loadLastLocation() {
		Double[] location = new Double[2];
		String latStr = preferences.getString(MeteoroPreferences.LATITUDE, "");
		String longStr = preferences
				.getString(MeteoroPreferences.LONGITUDE, "");
		if (Util.isDouble(latStr) && Util.isDouble(longStr)) {
			location[0] = Double.parseDouble(latStr);
			location[1] = Double.parseDouble(longStr);
		} else {
			location[0] = location[1] = null;
		}
		return location;
	}

	private final class RelocateButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Intent intent = new Intent(MainActivity.this, UpdateLocation.class);
			startActivity(intent);
		}
	}

	private final class UpdateButtonListener implements OnClickListener {

		@Override
		public void onClick(View v) {
			Context context = MainActivity.this;
			Double location[] = loadLastLocation();
			if (location[0] == null || location[1] == null) {
				Toast.makeText(
						context,
						context.getResources().getString(
								R.string.please_relocate), Toast.LENGTH_LONG)
						.show();
				return;
			}
			long savedTime = preferences.getLong(
					MeteoroPreferences.WEATHER_DATA_TIMESTAMP, 0);
			if (Util.isUpdatable(savedTime)) {
				Weather task = new Weather();
				task.execute(location);
			} else {
				Toast.makeText(context,
						context.getResources().getString(R.string.please_wait),
						Toast.LENGTH_LONG).show();
			}
		}
	}

	private class Weather extends AsyncTask<Double, Void, WeatherData> {

		@Override
		protected WeatherData doInBackground(Double... params) {
			double latitude = params[0];
			double longitude = params[1];
			OpenWeatherClient openWeatherClient = new OpenWeatherClient(
					getApplicationContext());
			WeatherData weatherData;
			try {
				weatherData = openWeatherClient.currentWeatherAroundPoint(
						latitude, longitude);
				return weatherData;
			} catch (IOException e) {
				e.printStackTrace();
			} catch (JSONException e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		protected void onPostExecute(WeatherData result) {
			super.onPostExecute(result);
			setProgressBarIndeterminateVisibility(false);
			stationField.setText(result.getName());
			long savedTime = preferences.getLong(
					MeteoroPreferences.WEATHER_DATA_TIMESTAMP, 0);
			Date savedDate = new Date(savedTime);
			String date = DateFormat.getDateFormat(MainActivity.this).format(
					savedDate);
			String time = DateFormat.getTimeFormat(MainActivity.this).format(
					savedDate);
			timeUpdateField.setText(date + "  " + time);
			Main mainData = result.getMain();
			temperatureField.setText(String.format("%.2f C",
					convertKelvinToCelsius(mainData.getTemp().doubleValue())));
			humidityField
					.setText(String.format("%s%%", mainData.getHumidity()));
			pressureField
					.setText(String.format("%shps", mainData.getPressure()));
			Wind wind = result.getWind();
			windField.setText(String.format("%sm/s (%s)", wind.getSpeed(),
					wind.getDeg()));
			Clouds clouds = result.getClouds();
			cloudsField.setText(String.format("%s%%", clouds.getAll()));
			magomar.meteoro.openweather.datamodel.Weather weather = result
					.getWeather().get(0);
			weatherField.setText(weather.getDescription());
			String icon = weather.getIcon();
			DownloadImageTask task = new DownloadImageTask();
			URL imageURL;
			try {
				imageURL = new URL("http://openweathermap.org/img/w/" + icon
						+ ".png");
				task.execute(imageURL);
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}

		@Override
		protected void onPreExecute() {
			super.onPreExecute();

		}

		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		private double convertKelvinToCelsius(double kelvin) {
			return kelvin - 273.15;
		}
	}

	private class DownloadImageTask extends AsyncTask<URL, Void, Bitmap> {

		protected Bitmap doInBackground(URL... urls) {
			Bitmap mIcon11 = null;
			try {
				InputStream in = urls[0].openStream();
				mIcon11 = BitmapFactory.decodeStream(in);
			} catch (Exception e) {
				Log.e("Error", e.getMessage());
				e.printStackTrace();
			}
			return mIcon11;
		}

		protected void onPostExecute(Bitmap result) {
			weatherImage.setImageBitmap(result);
		}
	}

}
