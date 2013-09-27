package fi.hiit.cultar.protocolbenchmarker;


public class DataStoreException extends Exception {
    public DataStoreException(String s) {
        super(s);
    }

    public DataStoreException(Throwable t) {
        super(t);
    }

    public DataStoreException(String s, Throwable t) {
        super(s, t);
    }
}
