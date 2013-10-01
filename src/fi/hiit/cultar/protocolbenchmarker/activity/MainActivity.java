package fi.hiit.cultar.protocolbenchmarker.activity;

import android.util.Log;
import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.LinkedList;
import java.util.Timer;
import java.util.TimerTask;
import java.nio.ByteBuffer;

import android.os.Message;
import android.os.Handler;
import android.os.Handler.Callback;
import android.os.AsyncTask;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import android.widget.Toast;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.model.LatLng;

import fi.hiit.cultar.protocol.InstrumentedProtocolClient;
import fi.hiit.cultar.protocol.IInstrumentationListener;
import fi.hiit.cultar.protocol.ConnectionStateCallback;

import fi.hiit.android.sensors.LocationHelper;
import fi.hiit.android.sensors.LocationHelper.LocationHelperClient;

import fi.hiit.cultar.protocolbenchmarker.Benchmark;
import fi.hiit.cultar.protocolbenchmarker.LoginBenchmark;
import fi.hiit.cultar.protocolbenchmarker.DataStore;
import fi.hiit.cultar.protocolbenchmarker.DataStoreException;
import fi.hiit.cultar.protocolbenchmarker.R;


public class MainActivity
                extends Activity
                implements LocationHelperClient, OnSharedPreferenceChangeListener {

    public static final String TAG = "ProtocolBenchmarker";
    public static final String FILE_DIR = "protocol_benchmarker";
    public static final String DATABASE_NAME = "simple_benchmarks.db";

    private SharedPreferences mPrefs;
    private SharedPreferences.Editor mPrefsEditor;

    private boolean mActive;
    private boolean mConnected;
    private long mActiveStartTimeStamp;
    private DataStore mDataStore;
    private InstrumentedProtocolClient mProtocolClient;
    private BenchmarkTask mBenchmarkTask;
    private ConnectTask mConnectTask;
    private List<Benchmark> mBenchmarks;

    private NetworkInfo mNetworkInfo;

    private TextView mTextTimeActive;
    private TextView mTextLocation;
    private GoogleMap mMap;
    private TextView mTextNetworkInformation;
    private TextView mTextActivityLog;
    private Button mButtonStart;
    private Button mButtonConnect;
    private Button mButtonPrefs;

    private LocationHelper mLocationHelper;
    private Location mLocation;

    private ConnectivityManager mConnectivityManager;

    private Timer mTimer;
    private final Handler mActiveTimerHandler = new Handler(new Callback() {
        @Override
        public boolean handleMessage(Message m) {
            mTextTimeActive.setText(formatActiveTime(System.currentTimeMillis() - mActiveStartTimeStamp));
            return false;
        }
    });
    private class ActiveTimerTask extends TimerTask {
        @Override
        public void run() {
            mActiveTimerHandler.sendEmptyMessage(0);
        }
    }

    /*------------------------------------------------------------------------
     * Lifecycle methods {{{
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mPrefsEditor = mPrefs.edit();
        mPrefs.registerOnSharedPreferenceChangeListener(this);

        mActive = false;
        mActiveStartTimeStamp = -1;

        mBenchmarks = new LinkedList<Benchmark>();

        mLocationHelper = new LocationHelper(this, this);
        mConnectivityManager = (ConnectivityManager)
                                getSystemService(Context.CONNECTIVITY_SERVICE);

        // set up UI elements
        mTextTimeActive = (TextView)findViewById(R.id.textTimeActive);

        mTextLocation = (TextView)findViewById(R.id.textLocation);
        mMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        mTextNetworkInformation = (TextView)findViewById(R.id.textNetworkInformation);
        updateNetworkInformation();

        mTextActivityLog = (TextView)findViewById(R.id.textActivityLog);
        activityLogClear();

        mButtonStart = (Button)findViewById(R.id.buttonStart);
        mButtonStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(MainActivity.TAG, "MainActivity.buttonStart clicked");
                setActive(!mActive);
            }
        });

        mButtonConnect = (Button)findViewById(R.id.buttonConnect);
        mButtonConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(MainActivity.TAG, "MainActivity.buttonConnect clicked");
                if (mConnected) {
                    mProtocolClient.close();
                    mButtonConnect.setText(R.string.connect);
                }
                else {
                    mConnectTask = new ConnectTask();
                    mConnectTask.execute();
                    mButtonConnect.setText(R.string.disconnect);
                }
            }
        });

        mButtonPrefs = (Button)findViewById(R.id.buttonPrefs);
        mButtonPrefs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(MainActivity.TAG, "MainActivity.buttonPrefs clicked");
                Intent intent = new Intent(MainActivity.this, PrefsActivity.class);
                startActivity(intent);
            }
        });

        try {
            mDataStore = new DataStore(this,
                                       Environment.getExternalStorageDirectory()
                                            + File.separator + FILE_DIR
                                            + File.separator + DATABASE_NAME,
                                       R.raw.simple_benchmarks);
        }
        catch (DataStoreException ex1) {
            Log.e(TAG, "Could not create DataStore: " + ex1.getMessage());
            activityLog("Could not create DataStore: " + ex1.getMessage());
        }

        mProtocolClient = new InstrumentedProtocolClient();
        mProtocolClient.setHost(getHost());
        mProtocolClient.setPort(getPort());

        // Add IInstrumentationListener which will store data into mDataStore
        mProtocolClient.addInstrumentationListener(new IInstrumentationListener() {
            @Override
            public void updateRead(long ts, long byteCount) {
                //Log.i(TAG, "INSTRUMENTATION READ: " + ts + ": " + byteCount);
                try {
                    mDataStore.write(String.valueOf(mNetworkInfo), String.valueOf(mLocation), String.valueOf(ts), String.valueOf(byteCount), "CLIENT_READ");
                }
                catch (DataStoreException ex1) {
                    Log.e(TAG, "Instrumentation Error: " + ex1.getMessage());
                }
            }

            @Override
            public void updateWrite(long ts, long byteCount) {
                //Log.i(TAG, "INSTRUMENTATION WRITE: " + ts + ": " + byteCount);
                try {
                    mDataStore.write(String.valueOf(mNetworkInfo), String.valueOf(mLocation), String.valueOf(ts), String.valueOf(byteCount), "CLIENT_WRITE");
                }
                catch (DataStoreException ex1) {
                    Log.e(TAG, "Instrumentation Error: " + ex1.getMessage());
                }
            }
        });

        setConnected(false);
        updateConfiguredStatus();

        addBenchmark(new LoginBenchmark(mProtocolClient, this, mDataStore));
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(MainActivity.TAG, "MainActivity.onPause");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(MainActivity.TAG, "MainActivity.onResume");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(MainActivity.TAG, "MainActivity.onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(MainActivity.TAG, "MainActivity.onRestart");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(MainActivity.TAG, "MainActivity.onDestroy");
    }
    /* }}} */

    /*------------------------------------------------------------------------
     * LocationHelperClient methods {{{
     */
    @Override
    public void setBestLocationEstimate(Location location) {
        Log.d(MainActivity.TAG, "setBestLocationEstimate: " + location);
        mLocation = location;
        mTextLocation.setText(mLocation.toString());
        CameraUpdate center = CameraUpdateFactory.newLatLng(new LatLng(location.getLatitude(),
                                                                       location.getLongitude()));
        Log.d(TAG, "CameraUpdate: " + center);
        mMap.moveCamera(center);
    }
    /* }}} */

    /*------------------------------------------------------------------------
     * OnSharedPreferenceChangeListener methods {{{
     */
    public synchronized void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        Log.i(MainActivity.TAG, "onSharedPreferenceChanged");
        if (key.equals(getString(R.string.host))) {
            mProtocolClient.setHost(getHost());
        }
        else if (key.equals(getString(R.string.port))) {
            mProtocolClient.setPort(getPort());
        }
        updateConfiguredStatus();
        return;
    }
    /* }}} */

    public void addBenchmark(Benchmark benchmark) {
        mBenchmarks.add(benchmark);
    }

    public void removeBenchmark(Benchmark benchmark) {
        mBenchmarks.remove(benchmark);
    }

    private void startBenchmarks() {
        Log.i(MainActivity.TAG, "startBenchmarks");
        updateNetworkInformation();

        // Kick off the benchmark background task
        mBenchmarkTask = new BenchmarkTask();
        mBenchmarkTask.execute();
    }

    private void stopBenchmarks() {
        Log.i(MainActivity.TAG, "stopBenchmarks");
        for (Benchmark benchmark : mBenchmarks) {
            benchmark.close();
        }

        if (mBenchmarkTask != null) {
            mBenchmarkTask.cancel(false);
            mBenchmarkTask = null;
        }
    }

    private class BenchmarkTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothings) {
            benchmarkExec();
            return null; // -> Void
        }

        @Override
        protected void onPostExecute(Void result) {
            // do something on UI thread
        }
    }

    private void benchmarkExec() {
        for (Benchmark benchmark : mBenchmarks) {
            Log.d(TAG, "running benchmark: " + benchmark);
            benchmark.run();
        }
    }

    private class ConnectTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... nothings) {
            connectExec();
            return null; // -> Void
        }

        @Override
        protected void onPostExecute(Void result) {
        }
    }

    private void connectExec() {
        activityLogThread("Connecting...");
        mProtocolClient.connect(new ConnectionStateCallback() {
            @Override
            public void onConnected(final boolean success, Exception ex) {
                //[TODO: is there a less clunky way to handle this?]
                if (success) {
                    MainActivity.this.activityLogThread("CONNECTED: " + success);

                }
                else {
                    MainActivity.this.activityLogThread("Connection error: " + ex);
                }

                //[TODO: is there a less clunky way to handle this?]
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        MainActivity.this.setConnected(success);
                    }
                });
            }

            @Override
            public void onDisconnected(Exception ex) {
                if (ex != null) {
                    MainActivity.this.activityLogThread("DISCONNECTED: " + ex.getMessage());
                }
                else {
                    MainActivity.this.activityLogThread("DISCONNECTED");
                }

                //[TODO: is there a less clunky way to handle this?]
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override public void run() {
                        MainActivity.this.setConnected(false);
                    }
                });
            }
        });
    }

    private void setActive(boolean active) {
        mActive = active;

        if (mActive) {
            startLocation();
            startBenchmarks();
            startActiveTimer();
            mButtonStart.setText(R.string.stop);
            //mButtonStart.setBackgroundResource(R.color.stop_red);
        }
        else {
            stopBenchmarks();
            stopLocation();
            stopActiveTimer();
            mButtonStart.setText(R.string.start);
            //mButtonStart.setBackgroundResource(R.color.start_green);
        }
    }

    private void updateNetworkInformation() {
        Log.i(MainActivity.TAG, "updateNetworkInformation");
        mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
        if (mNetworkInfo != null && mNetworkInfo.isConnected()) {
            mTextNetworkInformation.setText(mNetworkInfo.toString());
        }
        else {
            mTextNetworkInformation.setText("Could not get network information");
        }
    }

    private void setConnected(boolean connected) {
        mConnected = connected;
        if (mConnected) {
            mButtonConnect.setText(R.string.disconnect);
            mButtonPrefs.setEnabled(false);
        }
        else {
            mButtonConnect.setText(R.string.connect);
            mButtonPrefs.setEnabled(true);
            setActive(false);
        }
        mButtonStart.setEnabled(mConnected);
    }

    private void updateConfiguredStatus() {
        Log.i(MainActivity.TAG, "updateConfiguredStatus");
        if (isConfigured() && mDataStore != null) {
            mButtonConnect.setEnabled(true);
            activityLog("Host: " + getHost());
            activityLog("Port: " + getPort());
        }
        else if (mDataStore == null) {
            mButtonConnect.setEnabled(false);
            activityLog("No DataStore.");
        }
        else {
            mButtonConnect.setEnabled(false);
            activityLog("Backend server not configured. Please go to Preferences");
        }
    }

    public void activityLogThread(String s) {
        final String log = mTextActivityLog.getText() + s + "\n";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mTextActivityLog.setText(log);
            }
        });
    }

    public void activityLog(String s) {
        mTextActivityLog.setText(mTextActivityLog.getText() + s + "\n");
    }

    public void activityLogClear() {
        mTextActivityLog.setText("");
    }

    private void startLocation() {
        Log.i(MainActivity.TAG, "startLocation");
        mLocationHelper.startLocationUpdates();
        mTextLocation.setText(mLocationHelper.getLastKnownLocation().toString());
    }

    private void stopLocation() {
        Log.i(MainActivity.TAG, "stopLocation");
        mLocationHelper.stopLocationUpdates();
    }

    private void startActiveTimer() {
        Log.i(MainActivity.TAG, "startActiveTimer");
        mActiveStartTimeStamp = System.currentTimeMillis();
        mTimer = new Timer();
        mTimer.schedule(new ActiveTimerTask(), 0, 1000);
    }

    private void stopActiveTimer() {
        Log.i(MainActivity.TAG, "stopActiveTimer");
        if (mTimer != null) {
            mTimer.cancel();
            mTimer.purge();
            mTimer = null;
        }
    }

    private String formatActiveTime(long ms) {
        int secs = (int)(ms/1000) % 60;
        int mins = (int)((ms/60000) % 60);
        int hours = (int)((ms/3600000) % 24);
        return String.format("%02d:%02d:%02d", hours, mins, secs);
    }

    public String getHost() {
        return mPrefs.getString(getString(R.string.host), "");
    }

    public String getPort() {
        return mPrefs.getString(getString(R.string.port), "");
    }

    public String getUsername() {
        return mPrefs.getString(getString(R.string.username), "");
    }

    public String getPassword() {
        return mPrefs.getString(getString(R.string.password), "");
    }

    public boolean isConfigured() {
        //Log.d(MainActivity.TAG, "[" + getHost() + "][" + getPort() + "][" + getUsername() + "][" + getPassword() + "]");
        return (!getHost().equals("") && !getPort().equals("") && !getUsername().equals("") && !getPassword().equals(""));
    }
}


