package fi.hiit.cultar.protocolbenchmarker;

import android.util.Log;
import java.io.IOException;

import fi.hiit.cultar.protocolbenchmarker.activity.MainActivity;

import fi.hiit.cultar.protocol.*;
import fi.hiit.cultar.application.LoginHandler;
import fi.hiit.cultar.application.ApplicationEvent;
import fi.hiit.cultar.application.ApplicationEventListener;


public class LoginBenchmark extends Benchmark {

    private int mCount = 0;

    public LoginBenchmark(ProtocolClient protocolClient, MainActivity mainActivity, DataStore dataStore) {
        super(protocolClient, mainActivity, dataStore);
    }

    @Override
    public void run() {
        Log.d(MainActivity.TAG, "inside benchmark.run: " + this);
        final LoginHandler loginHandler =
            new LoginHandler(mProtocolClient, mMainActivity.getUsername(),
                                              mMainActivity.getPassword());

        mCount = 0;
        loginHandler.addListener(ApplicationEvent.OnLogin, new ApplicationEventListener() {
            @Override
            public void update(boolean success, String reason) {
                if (success) {
                    mMainActivity.activityLogThread("LOGGED IN OK");
                }
                else {
                    mMainActivity.activityLogThread("LOGIN FAILED: " + reason);
                }
                if (LoginBenchmark.this.mCount++ < 10) {
                    loginHandler.exec();
                }
            }
        });

        loginHandler.exec();
    }
}
