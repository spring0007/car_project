package com.awell.utils;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.util.Log;


public class LocationHelper {

    private static final String TAG = "Car_LocationHelper";
    public static final int MSG_GPS_SPEED = 116;

    private static LocationHelper mLocationHelper;
    private LocationManager mLocationManager;
    private Context mContext;
    private int minTime = 1000;
    private int minDistance = 1;

    private LocationHelper(Context context) {
        this.mContext = context;
    }


    public static LocationHelper getInstance(Context context) {
        if (mLocationHelper == null) {
            mLocationHelper = new LocationHelper(context);
        }
        return mLocationHelper;
    }


    public boolean requestLocationUpdate() {
        LocationManager locationManager = getLocationManager();
        if (locationManager == null) {
            return false;
        }

        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, mLocationListener);
        } catch (SecurityException e) {
            e.printStackTrace();
        }

        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            return false;
        }

        return true;
    }

    private LocationManager getLocationManager() {
        if (mLocationManager == null) {
            mLocationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
            if (mLocationListener == null) {
                Log.e(TAG, "requestLocationUpdate: create LocationManager intance fail");
            }
        }
        return mLocationManager;
    }

    public void releaseLocation() {
        Log.d(TAG, "releaseLocation: ");
        if (mLocationManager != null) {
            try {
                mLocationManager.removeUpdates(mLocationListener);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
        mLocationManager = null;
        Log.d(TAG, "releaseLocation: success");
    }

    private LocationListener mLocationListener = new LocationListener() {

        @Override
        public void onLocationChanged(Location location) {
          /*  if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }*/
            Log.i(TAG, "onLocationChanged: float speed=" + location.getSpeed());
            int speed = (int) (location.getSpeed() * 3.6);// m/s ---> km/h
            int speedMile = (int) (speed / 1.6093);// km/h  ---> miles/h
            Log.i(TAG, "onLocationChanged: float speed = " + speed);
            Log.i(TAG, "onLocationChanged: float speedMile = " + speedMile);

            Intent intent = new Intent();
            intent.setAction(CommonData.BROADCAST_GPS_SPEED);
            intent.putExtra(CommonData.FLAG_KM_MILE, "mile");
            intent.putExtra(CommonData.KEY_KM_SPEED, speed);
            intent.putExtra(CommonData.KEY_MILE_SPEED, speedMile);
            mContext.sendBroadcast(intent);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            switch (status) {
                case LocationProvider.AVAILABLE:
                    Log.d(TAG, "gps status: available");
                    break;
                case LocationProvider.OUT_OF_SERVICE:
                    Log.d(TAG, "gps status: out of service");
                    break;
                case LocationProvider.TEMPORARILY_UNAVAILABLE:
                    Log.d(TAG, "gps status: temporarily unavailable");
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "GPS onProviderEnabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "GPS onProviderDisabled");
        }
    };

}
