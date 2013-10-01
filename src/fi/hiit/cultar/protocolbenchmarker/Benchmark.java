package fi.hiit.cultar.protocolbenchmarker;

import fi.hiit.cultar.protocol.ProtocolClient;
import fi.hiit.cultar.protocolbenchmarker.activity.MainActivity;


//[TODO: implement java.io.Closeable]
public abstract class Benchmark {
    protected ProtocolClient mProtocolClient;
    protected MainActivity mMainActivity;
    protected DataStore mDataStore;

    /* enforce the constructor which takes a data store */
    private Benchmark(){};
    public Benchmark(ProtocolClient protocolClient, MainActivity mainActivity, DataStore dataStore) {
        mProtocolClient = protocolClient;
        mMainActivity = mainActivity;
        mDataStore = dataStore;
    }

    public abstract void run();
    public void close() { }
}
