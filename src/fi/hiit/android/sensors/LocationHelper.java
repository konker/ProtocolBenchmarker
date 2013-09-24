package fi.hiit.android.sensors;

import android.os.Bundle;
import android.content.Context;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;


public class LocationHelper implements LocationListener {
    private static final int TIME_DELTA_SIGNIFICANT_WINDOW_MS = 1000 * 60 * 2; // 2 minutes

    public interface LocationHelperClient {
        public void setBestLocationEstimate(Location l);
    }

    private LocationHelperClient mClient;
    private LocationManager mLocationManager;
    private Location mBestLocationEstimate;

    public LocationHelper(Context context, LocationHelperClient client) {
        mClient = client;
        mBestLocationEstimate = null;
        mLocationManager = (LocationManager)context.getSystemService(Context.LOCATION_SERVICE);
    }

    public Location getLastKnownLocation() {
        return mLocationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
    }
    public void startLocationUpdates() {
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, this);
    }
    public void stopLocationUpdates() {
        mLocationManager.removeUpdates(this);
    }

    public Location getBestLocationEstimate() {
        return mBestLocationEstimate;
    }
    private void setBestLocationEstimate(Location bestLocationEstimate) {
        mBestLocationEstimate = bestLocationEstimate;
        mClient.setBestLocationEstimate(mBestLocationEstimate);
    }

    /*------------------------------------------------------------------------
     * LocationListener methods {{{
     */
    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location)) {
            setBestLocationEstimate(location);
        }
    }
    @Override
    public void onProviderDisabled(String provider) {
    }
    @Override
    public void onProviderEnabled(String provider) {
    }
    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
    /* }}} */

    public boolean isBetterLocation(Location location) {
        if (mBestLocationEstimate == null) {
            return true;
        }

        // check whether the new location fix is newer
        long timeDelta = location.getTime() - mBestLocationEstimate.getTime();
        boolean isSignificantlyNewer = timeDelta > TIME_DELTA_SIGNIFICANT_WINDOW_MS;
        boolean isSignificantlyOlder = timeDelta < -TIME_DELTA_SIGNIFICANT_WINDOW_MS;
        boolean isNewer = timeDelta > 0;

        if (isSignificantlyNewer) {
            return true;
        }
        else if (isSignificantlyOlder) {
            return false;
        }

        // check accuracy
        int accuracyDelta = (int)(location.getAccuracy() - mBestLocationEstimate.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        boolean isFromSameProvider = isSameProvider(location.getProvider(), mBestLocationEstimate.getProvider());

        if (isMoreAccurate) {
            return true;
        }
        else if (isNewer && !isLessAccurate) {
            return true;
        }
        else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }
    private boolean isSameProvider(String p1, String p2) {
        if (p1 == null) {
            return (p2 == null);
        }
        return p1.equals(p2);
    }
}

