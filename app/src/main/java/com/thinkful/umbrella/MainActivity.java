package com.thinkful.umbrella;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.JsonReader;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        WebServiceTask mWebServiceTask = new WebServiceTask();
        mWebServiceTask.execute("David");
       FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

    }

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
    private class WebServiceTask extends AsyncTask<String, String, String> {

        @Override
        protected String doInBackground(String... params) {
            return "Hello "+params[0];
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            HttpURLConnection urlConnection = null;
            String useUmbrellaStr = "Don't know, sorry about that.";
            try {
                URL url = new URL("http://api.wunderground.com/api/739df143b9d824c7/geolookup/forecast/q/10.811188,106.770847.json");
                urlConnection = (HttpURLConnection) url.openConnection();
                InputStream in = urlConnection.getInputStream();

                useUmbrellaStr = useUmbrella(in);
                TextView mTexView = (TextView) findViewById(R.id.hello);
                mTexView.setText(useUmbrellaStr);

            }
            catch(IOException  e){
                Log.e(getResources().getString(R.string.app_name), e.toString());
            }
            finally{
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
    }

    protected String useUmbrella(InputStream in) {
        StringBuilder stringBuilder = new StringBuilder();
        BufferedReader bufferedReader = null;

        try {
            bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line;

            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line + "\n");
            }
            //JSON needs to be parsed here
            Log.i("Returned data", stringBuilder.toString());
            JSONObject forecastJson = new JSONObject(stringBuilder.toString()).getJSONObject("forecast").getJSONObject("simpleforecast");
            JSONArray weatherArray = forecastJson.getJSONArray("forecastday");
            JSONObject todayWeather = weatherArray.getJSONObject(0);
            Log.d("umbrella", todayWeather.toString());
            if(todayWeather.has("rain") == true){
                return "rain!";
            }
            else{
                return "no rain!";
            }
        } catch (Exception e) {
            String sb = e.toString();
            if (sb.length() > 4000) {
                Log.v("umbrella", "sb.length = " + sb.length());
                int chunkCount = sb.length() / 4000;     // integer division
                for (int i = 0; i <= chunkCount; i++) {
                    int max = 4000 * (i + 1);
                    if (max >= sb.length()) {
                        Log.v("umbrella", "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i));
                    } else {
                        Log.v("umbrella", "chunk " + i + " of " + chunkCount + ":" + sb.substring(4000 * i, max));
                    }
                }
            } else {
                Log.v("umbrella", sb.toString());
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
        return "Don't know, sorry about that.";
    }
}
