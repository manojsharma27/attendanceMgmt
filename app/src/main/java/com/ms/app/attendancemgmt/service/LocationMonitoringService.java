package com.ms.app.attendancemgmt.service;


import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.ms.app.attendancemgmt.model.LocationModel;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import static com.ms.app.attendancemgmt.util.Constants.EXTRA_FETCH_TIME;
import static com.ms.app.attendancemgmt.util.Constants.EXTRA_LATITUDE;
import static com.ms.app.attendancemgmt.util.Constants.EXTRA_LONGITUDE;
import static com.ms.app.attendancemgmt.util.Constants.LAST_CAPTURED_TIME;
import static com.ms.app.attendancemgmt.util.Constants.LAST_LATITUDE;
import static com.ms.app.attendancemgmt.util.Constants.LAST_LONGITUDE;
import static com.ms.app.attendancemgmt.util.Constants.TAG;

public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleApiClient mLocationClient;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    public static final int REQUEST_CODE = 11012;
    public static final String ACTION_LOCATION_BROADCAST = "LocationMonitoringService-LocationBroadcast";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createLocationClientAndConnect();
        return START_STICKY;
    }

    private void createLocationClientAndConnect() {
        if (null == mLocationClient) {
            mLocationClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
        if (!mLocationClient.isConnected() || !mLocationClient.isConnecting()) {
            mLocationClient.connect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "== Error On onConnected(). Permission not granted");
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, getLocationRequest(), this);
        Log.d(TAG, "Connected to Google API");
    }

    @Override
    public void onConnectionSuspended(int i) {
        createLocationClientAndConnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.v(TAG, "location changed");
            //Send result to activities
            Utility.saveSharedPref(this, LAST_LATITUDE, String.valueOf(location.getLatitude()));
            Utility.saveSharedPref(this, LAST_LONGITUDE, String.valueOf(location.getLongitude()));
            Utility.saveSharedPref(this, LAST_CAPTURED_TIME, String.valueOf(SystemClock.currentThreadTimeMillis()));
        }
    }

    private LocationRequest getLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    private void sendMessageToUI(String lat, String lng) {
        Log.d(TAG, "Sending info...");

        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_LATITUDE, lat);
        intent.putExtra(EXTRA_LONGITUDE, lng);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    private void sendLocationUpdate(Context context, LocationModel locationModel) {
        Intent intent = new Intent(ACTION_LOCATION_BROADCAST);
        intent.putExtra(EXTRA_LATITUDE, locationModel.getLatitude());
        intent.putExtra(EXTRA_LONGITUDE, locationModel.getLongitude());
        intent.putExtra(EXTRA_FETCH_TIME, Utility.formatDateForUTC(locationModel.getLogTime()));
        context.sendBroadcast(intent);
        Log.v(TAG, "broadcast msg sent...");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");

    }
}
