package magomar.meteoro.openweather;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

import com.google.gson.Gson;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import magomar.meteoro.R;
import magomar.meteoro.openweather.datamodel.WeatherData;
import magomar.meteoro.other.MeteoroPreferences;
import magomar.meteoro.other.Util;

/**
 * @author Mario Gomez
 * 
 */
public class OpenWeatherClient {
	static final String BASE_URL = "http://api.openweathermap.org/data/2.5/";
	static private final String APPID_HEADER = "x-api-key";

	/**
	 * Minimum currentTime interval to perform weather updates, in milliseconds
	 */
	public static final long UPDATE_TIME_INTERVAL = 10 * 60 * 1000;

	private final HttpClient httpClient;
	private final Context context;
	private final SharedPreferences preferences;
	/**
	 * My personal key to access openweathermap.org server
	 */
	private final String openWeatherAPPID = "4edd1aa0a32a3d879f8ca179e63130a7";

	public OpenWeatherClient(Context context) {
		this.httpClient = new DefaultHttpClient();
		this.context = context;
		preferences = context.getSharedPreferences(
				MeteoroPreferences.PREFERENCES_FILENAME, Context.MODE_PRIVATE);
	}

	/**
	 * Obtain current weather around a geographic point of interest
	 * 
	 * @param lat
	 *            is the latitude of the geographic point of interest
	 *            (North/South coordinate)
	 * @param lon
	 *            is the longitude of the geographic point of interest
	 *            (East/West coordinate)
	 * @param cnt
	 *            is the requested number of weather stations to retrieve (the
	 *            actual answer might be less than the requested).
	 * @return the response obtained from the <a
	 *         href="http://api.openweathermap.org">OpenWeatherMap</a> web
	 *         service.
	 * @throws IOException
	 * @throws JSONException
	 */
	public WeatherData currentWeatherAroundPoint(double lat, double lon)
			throws IOException, JSONException {
		long savedTime = preferences.getLong(
				MeteoroPreferences.WEATHER_DATA_TIMESTAMP, 0);
		JSONObject json;
		if (!Util.isUpdatable(savedTime)) {
			json = loadLastWeatherData();
			if (json != null) {
				return unmarshallJson(json, WeatherData.class);
			}
		}
		List<NameValuePair> pairs = new ArrayList<NameValuePair>();
		pairs.add(new BasicNameValuePair(MeteoroPreferences.LATITUDE, Double
				.toString(lat)));
		pairs.add(new BasicNameValuePair(MeteoroPreferences.LONGITUDE, Double
				.toString(lon)));
		String subUrl = MeteoroPreferences.WEATHER_DATA + "?"
				+ URLEncodedUtils.format(pairs, "utf-8");
		json = doQuery(subUrl);
		saveWeatherData(json);

		return unmarshallJson(json, WeatherData.class);
	}

	private JSONObject doQuery(String subUrl) throws JSONException, IOException {
		String responseBody = null;
		HttpGet httpget = new HttpGet(BASE_URL + subUrl);
		if (this.openWeatherAPPID != null) {
			httpget.addHeader(APPID_HEADER, openWeatherAPPID);
		}

		HttpResponse response = this.httpClient.execute(httpget);
		InputStream contentStream = null;
		try {
			StatusLine statusLine = response.getStatusLine();
			if (statusLine == null) {
				throw new IOException(context.getResources().getString(
						R.string.owm_no_response));
			}
			int statusCode = statusLine.getStatusCode();
			if (statusCode < 200 && statusCode >= 300) {
				throw new IOException(context.getResources().getString(
						R.string.owm_status_code)
						+ String.format(" %d: %s", statusCode, statusLine));
			}
			// Retrieve the Entity of the response, if any
			HttpEntity responseEntity = response.getEntity();
			contentStream = responseEntity.getContent();
			Reader isReader = new InputStreamReader(contentStream);
			int contentSize = (int) responseEntity.getContentLength();
			if (contentSize < 0)
				contentSize = 8 * 1024;
			StringWriter strWriter = new StringWriter(contentSize);
			char[] buffer = new char[8 * 1024];
			int n = 0;
			while ((n = isReader.read(buffer)) != -1) {
				strWriter.write(buffer, 0, n);
			}
			responseBody = strWriter.toString();
			contentStream.close();
		} catch (IOException e) {
			throw e;
		} catch (RuntimeException re) {
			httpget.abort();
			throw re;
		} finally {
			if (contentStream != null)
				contentStream.close();
		}
		return new JSONObject(responseBody);
	}

	private JSONObject loadLastWeatherData() {
		String weatherStr = preferences.getString(
				MeteoroPreferences.WEATHER_DATA, null);
		if (weatherStr != null)
			try {
				return new JSONObject(weatherStr);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		return null;
	}

	private void saveWeatherData(JSONObject json) {
		Editor editor = preferences.edit();
		editor.putString(MeteoroPreferences.WEATHER_DATA, json.toString());
		Date date = new Date();
		long currentTime = date.getTime();
		editor.putLong(MeteoroPreferences.WEATHER_DATA_TIMESTAMP, currentTime);
		editor.commit();
	}

	public static <T> T unmarshallJson(JSONObject json, Class<T> objectClass) {
		Gson gson = new Gson();
		T object = gson.fromJson(json.toString(), objectClass);
		return object;
	}
}
