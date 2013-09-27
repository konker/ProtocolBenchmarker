package fi.hiit.cultar.protocolbenchmarker;

import fi.hiit.cultar.protocolbenchmarker.activity.MainActivity;


//[TODO: implement java.io.Closeable]
public abstract class Benchmark {
    protected MainActivity mMainActivity;
    protected DataStore mDataStore;

    /* enforce the constriuctor which takes a data store */
    private Benchmark(){};
    public Benchmark(MainActivity mainActivity, DataStore dataStore) {
        mMainActivity = mainActivity;
        mDataStore = dataStore;
    }

    public abstract void run();
    public abstract void close();
}
