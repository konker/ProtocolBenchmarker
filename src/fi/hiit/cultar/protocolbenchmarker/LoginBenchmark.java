package fi.hiit.cultar.protocolbenchmarker;

import java.io.IOException;

import fi.hiit.cultar.protocolbenchmarker.activity.MainActivity;

import fi.hiit.cultar.protocol.*;
import fi.hiit.cultar.application.LoginHandler;
import fi.hiit.cultar.application.ApplicationEvent;
import fi.hiit.cultar.application.ApplicationEventListener;


public class LoginBenchmark extends Benchmark {
    private ProtocolClient mProtocolClient;

    public LoginBenchmark(MainActivity mainActivity, DataStore dataStore) {
        super(mainActivity, dataStore);
        mProtocolClient = new ProtocolClient(mainActivity.getHost(), Integer.parseInt(mainActivity.getPort()));
    }

    @Override
    public void run() {
        mMainActivity.activityLogThread("Connecting...");
        mProtocolClient.connect(new ConnectedCallback() {
            @Override
            public void onConnected(boolean success, IOException ex) {
                if (success) {
                    mMainActivity.activityLogThread("CONNECTED: " + success);

                    LoginHandler loginHandler = new LoginHandler(mMainActivity.getUsername(), mMainActivity.getPassword());
                    loginHandler.register(mProtocolClient);
                    loginHandler.addListener(ApplicationEvent.OnLogin, new ApplicationEventListener() {
                        @Override
                        public void update(boolean success, String reason) {
                            if (success) {
                                mMainActivity.activityLogThread("LOGGED IN OK");
                            }
                            else {
                                mMainActivity.activityLogThread("LOGIN FAILED: " + reason);
                            }
                        }
                    });
                    loginHandler.exec();
                }
                else {
                    mMainActivity.activityLogThread("Connection error: " + ex);
                }
            }
        });
    }

    @Override
    public void close() {
        mProtocolClient.close();
    }
}
