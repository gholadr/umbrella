package com.thinkful.umbrella;

import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Map;

import me.pushy.sdk.Pushy;


public class MainActivity extends AppCompatActivity
        implements
        OnMapReadyCallback,
        ActivityCompat.
                OnRequestPermissionsResultCallback,
        GoogleApiClient.
                ConnectionCallbacks,
        GoogleApiClient.
                OnConnectionFailedListener,
        LocationListener {
    public static String TAG = "Umbrella";
    protected GoogleApiClient mGoogleApiClient;
    protected Location mCurrentLocation;

    /**
     * Request code for location permission request.
     *
     * @see #onRequestPermissionsResult(int, String[], int[])
     */
    protected static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    /**
     * Flag indicating whether a requested permission has been denied after returning in
     * {@link #onRequestPermissionsResult(int, String[], int[])}.
     */
    private boolean mPermissionDenied = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView textView = (TextView) findViewById(R.id.textView);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        Pushy.listen(this);
        new registerForPushNotificationsAsync().execute();
        SharedPreferences prefA = PreferenceManager.getDefaultSharedPreferences(this);
        //Log.d(TAG, prefA.getString("pushyToken", null));
        String registrationID = prefA.getString("pushyToken", null);
        if (registrationID != null){
            textView.setText(registrationID);
        }

    }

/*    protected void displayNotification(){
        //Build Notification
        int notificationID= 133;
        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher);
        NotificationCompat.Builder nBuilder = new NotificationCompat.Builder(
                this);
        nBuilder.setContentTitle("Notification");
        nBuilder.setContentText("This is a Notification");
        nBuilder.setSmallIcon(R.drawable.zombie_icon_alpha);
        nBuilder.setLargeIcon(largeIcon); //setLargeIcon(R.drawable.ic_launcher);

        nBuilder.setContentIntent(getMainActivityPendingIntent());
        nBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(notificationID, nBuilder.build());
    }*/

/*    protected PendingIntent getMainActivityPendingIntent() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 1234, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        return(pendingIntent);
    }*/
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

    }

    private void initPerms() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission to access the location is missing.
            Log.d(TAG, "permission to access the location is missing");
            //showMissingPermissionError();
            PermissionUtils.requestPermission(this, LOCATION_PERMISSION_REQUEST_CODE,
                    Manifest.permission.ACCESS_FINE_LOCATION, true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            return;
        }

        // Display the missing permission error dialog when the fragments resume.
        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            Log.d(TAG, "perms granted");
        } else mPermissionDenied = true;
    }


    public void showWeatherForCoords() {

        Location mCurrentLocation;
        try {
            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        catch(SecurityException e){
            Log.d(TAG, e.toString());
            return;
        }
        if(mCurrentLocation != null) {
            LatLng mLatLng = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
            Log.d(TAG, mLatLng.toString());
            WebServiceTask mWebServiceTask = new WebServiceTask();
            mWebServiceTask.execute(Double.toString(mLatLng.latitude) + "," + Double.toString(mLatLng.longitude));
        }

    }
    @Override
    public void onConnected(Bundle bundle) {
        initPerms();
        startLocationUpdates();
        showWeatherForCoords();
    }


    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();


    }

    @Override
    public void onResume() {
        super.onResume();
        if (mPermissionDenied) {
            // Permission was not granted, display error dialog.
            initPerms();
            mPermissionDenied = false;
        }else {
            if (mGoogleApiClient.isConnected()) {
                showWeatherForCoords();
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        //stop location updates
        if(mGoogleApiClient.isConnected()) {
            removeLocation();
        }
    }

    protected void startLocationUpdates() {
        if(mPermissionDenied != true) {
            Log.d(TAG, "start location updates");
            LocationRequest mLocationRequest = new LocationRequest();
            mLocationRequest.setInterval(50000);
            mLocationRequest.setPriority(LocationRequest.PRIORITY_NO_POWER);
            //this.set
            try {
                LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            } catch (SecurityException e) {
                Log.d(TAG, e.toString());
            }
        }
    }
    /**
     * Displays a dialog with error message explaining that the location permission is missing.
     */
/*    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }*/

    public void removeLocation(){
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(
                    mGoogleApiClient, this);
        }
    }

    private class registerForPushNotificationsAsync extends AsyncTask<Void, Void, Exception>
    {
        protected Exception doInBackground(Void... params)
        {
            try
            {
                // Acquire a unique registration ID for this device
                String registrationId = Pushy.register(getApplicationContext());

                // Send the registration ID to your backend server and store it for later
                sendRegistrationIdToBackendServer(registrationId);
            }
            catch( Exception exc )
            {
                // Return exc to onPostExecute
                return exc;
            }

            // We're good
            return null;
        }

        @Override
        protected void onPostExecute(Exception exc)
        {
            // Failed?
            if ( exc != null )
            {
                // Show error as toast message
                Toast.makeText(getApplicationContext(), exc.toString(), Toast.LENGTH_LONG).show();
                return;
            }

            // Succeeded, do something to alert the user
        }

        // Example implementation
        void sendRegistrationIdToBackendServer(String registrationId) throws Exception
        {
            // The URL to the function in your backend API that stores registration IDs
            URL sendRegIdRequest = new URL("https://{YOUR_API_HOSTNAME}/register/device?registration_id=" + registrationId);

            // Send the registration ID by executing the GET request
            sendRegIdRequest.openConnection();
        }
    }

    private class WebServiceTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... coords) {
            HttpURLConnection urlConnection = null;
            String useUmbrellaStr = "Don't know, sorry about that.";
            String mUrl = String.format("http://api.wunderground.com/api/739df143b9d824c7/geolookup/forecast/q/%s.json", coords);
            Log.d(TAG, mUrl);
            try {
                URL url = new URL(mUrl);
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();
                useUmbrellaStr = useUmbrella(in);
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
            return useUmbrellaStr;
        }

        @Override
        protected void onPostExecute(String forecast) {
            super.onPostExecute(forecast);
            TextView mTexView = (TextView) findViewById(R.id.hello);
            mTexView.setText(forecast);
        }

        protected String useUmbrella(InputStream in) {
            StringBuilder stringBuilder = new StringBuilder();
            String response = "Don't know, sorry about that.";
            BufferedReader bufferedReader = null;

            try {
                bufferedReader = new BufferedReader(new InputStreamReader(in));
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line + "\n");
                }
                //JSON needs to be parsed here
                Log.i("Returned data", stringBuilder.toString());
                JSONObject forecastJson = new JSONObject(stringBuilder.toString()).getJSONObject("forecast").getJSONObject("simpleforecast").getJSONArray("forecastday").getJSONObject(0);
                JSONObject locationJson = new JSONObject(stringBuilder.toString()).getJSONObject("location");

                response = String.format("Weather for %s, at %s is %s", locationJson.getString("city"), forecastJson.getJSONObject("date").get("pretty"), forecastJson.getString("conditions").toLowerCase());
                return response;

            } catch (Exception e) {
                String sb = e.toString();
                if (sb.length() > 4000) {
                    Log.v(TAG, "sb.length = " + sb.length());
                    int chunkCount = sb.length() / 4000;     // integer division
                    for (int i = 0; i <= chunkCount; i++) {
                        int max = 4000 * (i + 1);
                        if (max >= sb.length()) {
                            Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i));
                        } else {
                            Log.v(TAG, "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max));
                        }
                    }
                } else {
                    Log.v(TAG, sb.toString());
                }
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (final IOException e) {
                        Log.e("PlaceholderFragment", "Error closing stream", e);
                    }
                }
            }
            return response;
        }
    }



}
