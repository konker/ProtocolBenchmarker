package fi.hiit.cultar.protocolbenchmarker.activity;

import android.util.Log;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;
import android.view.View;
import android.view.View.OnClickListener;

import fi.hiit.cultar.protocolbenchmarker.R;

public class PrefsActivity extends PreferenceActivity {
    public static final String TAG = "ProtocolBenchmarker.PreferenceActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.prefs);
        setContentView(R.layout.prefs);

        /*
        Button buttonBack = (Button)findViewById(R.id.buttonBack);
        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(PrefsActivity.TAG, "buttonBack");
                //Intent intent = new Intent(this, MainActivity.class);
                //startActivity(intent);
            }
        });
        */

        Log.d(PrefsActivity.TAG, "onCreate");
    }
}
