package magomar.meteoro.activities;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Toast;
import android.widget.ToggleButton;

import magomar.meteoro.R;
import magomar.meteoro.other.MeteoroPreferences;

public class UpdateLocation extends Activity implements LocationListener {
    // Constants
    static final String DEFAULT_LAT = "39.466667";
    static final String DEFAULT_LONG = "-0.375";
    static final long MIN_TIME = 5000;
    static final float MIN_DISTANCE = 1;
    private static final int MY_PERMISSIONS_REQUEST_ACCESS_LOCATION = 0;
    // GUI elements
    private ToggleButton toggleGPS;
    private ToggleButton toggleNetwork;
    private EditText longitudeField;
    private EditText latitudeField;
    private Button defaultLocationButton;
    private Button lastLocationButton;
    // Other attributes
    private SharedPreferences preferences;
    private LocationManager locationManager;
    private String activeProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);

        toggleGPS = (ToggleButton) findViewById(R.id.toggleGPS);
        toggleNetwork = (ToggleButton) findViewById(R.id.toggleNetwork);
        defaultLocationButton = (Button) findViewById(R.id.useDefaultLocation);
        lastLocationButton = (Button) findViewById(R.id.useLastLocation);

        longitudeField = ((EditText) findViewById(R.id.longitudeField));
        latitudeField = ((EditText) findViewById(R.id.latitudeField));

        preferences = getSharedPreferences(MeteoroPreferences.PREFERENCES_FILENAME,
                Context.MODE_PRIVATE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        toggleNetwork.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton,
                                         boolean isChecked) {
                reconfigure();
            }
        });
        toggleGPS.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton,
                                         boolean isChecked) {
                reconfigure();
            }
        });
        defaultLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                longitudeField.setText(DEFAULT_LONG);
                latitudeField.setText(DEFAULT_LAT);

            }
        });
        lastLocationButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String longStr = preferences.getString(
                        MeteoroPreferences.LONGITUDE, "");
                String latStr = preferences.getString(
                        MeteoroPreferences.LATITUDE, "");
                if ("".equals(longStr))
                    longStr = getResources().getString(R.string.not_available);
                if ("".equals(latStr))
                    latStr = getResources().getString(R.string.not_available);
                longitudeField.setText(longStr);
                latitudeField.setText(latStr);

            }
        });
        obtainProviders();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Editor editor = preferences.edit();
        editor.putString(MeteoroPreferences.LONGITUDE, longitudeField.getText()
                .toString());
        editor.putString(MeteoroPreferences.LATITUDE, latitudeField.getText()
                .toString());
        editor.commit();
        locationManager.removeUpdates(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (activeProvider != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_LOCATION);

                return;
            }
            locationManager.requestLocationUpdates(activeProvider, MIN_TIME, MIN_DISTANCE, this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_ACCESS_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // TODO
                    // permission was granted, yay! Do the task you need to do.

                } else {
                    // TODO
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return true;
    }

    @Override
    public void onLocationChanged(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        String latStr = String.valueOf(latitude);
        String longStr = String.valueOf(longitude);
        latitudeField.setText(latStr);
        longitudeField.setText(longStr);

    }

    @Override
    public void onProviderEnabled(String provider) {
        Toast.makeText(this,
                getResources().getString(R.string.provider_enabled) + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onProviderDisabled(String provider) {
        Toast.makeText(
                this,
                getResources().getString(R.string.provider_disabled) + provider,
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // TODO Auto-generated method stub
    }

    public void obtainProviders() {
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            toggleGPS.setChecked(false);
            toggleGPS.setClickable(false);
        }
        if (!locationManager
                .isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            toggleNetwork.setChecked(false);
            toggleNetwork.setClickable(false);
        }

    }

    public void reconfigure() {
        if (toggleNetwork.isChecked()) {
            Location location = locationManager
                    .getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location != null) {
                onLocationChanged(location);
            }
        }
        if (toggleGPS.isChecked()) {
            activeProvider = LocationManager.GPS_PROVIDER;
        } else if (toggleNetwork.isChecked()) {
            activeProvider = LocationManager.NETWORK_PROVIDER;
        } else
            activeProvider = null;

        if (activeProvider == null) {
            Toast.makeText(this,
                    getResources().getString(R.string.no_provider),
                    Toast.LENGTH_LONG).show();
            return;
        }
        locationManager.requestLocationUpdates(activeProvider, MIN_TIME, MIN_DISTANCE, this);
        String message = getResources().getString(R.string.active_provider)
                + ": " + activeProvider;
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

}
