package com.ms.app.attendancemgmt.service;


import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.activitiy.LoginActivity;
import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LocationModel;
import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

import okhttp3.Response;

import static com.ms.app.attendancemgmt.util.Constants.EXTRA_FETCH_TIME;
import static com.ms.app.attendancemgmt.util.Constants.EXTRA_LATITUDE;
import static com.ms.app.attendancemgmt.util.Constants.EXTRA_LONGITUDE;
import static com.ms.app.attendancemgmt.util.Constants.MSG_OK;
import static com.ms.app.attendancemgmt.util.Constants.TAG;

public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ServerUpdateResponseHandler {

    GoogleApiClient mLocationClient;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 12121;
    public static final int REQUEST_CODE = 11012;
    public static final String ACTION_LOCATION_BROADCAST = "LocationMonitoringService-LocationBroadcast";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        createLocationClientAndConnect();
        configureNotificationIntent(intent);
        return START_STICKY;
    }

    private void configureNotificationIntent(Intent intent) {
        String action = intent.getAction();
        if (StringUtils.isEmpty(action)) {
            return;
        }
        if (action.equals(Constants.ACTION_START_FOREGROUND_LOCATION_SERVICE)) {

            Intent notificationIntent = new Intent(this.getApplicationContext(), RegisterAttendanceActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

            Bundle bundle = prepareBundle(intent);
            notificationIntent.putExtras(bundle);
            PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(), 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap icon = BitmapFactory.decodeResource(getResources(), R.mipmap.location_icon);

            Notification notification = new Notification.Builder(this.getApplicationContext())
                    .setContentTitle("PunchIt")
                    .setContentText("Auto punching")
                    .setSmallIcon(R.mipmap.gps_icon)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 100, 100, false))
                    .setContentIntent(pendingIntent)
                    .build();

            startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, notification);
            Utility.writePref(this.getApplicationContext(), Constants.LAST_UPDATE_TO_SERVER_TIME, String.valueOf(System.currentTimeMillis()));

        } else if (action.equals(Constants.ACTION_STOP_FOREGROUND_LOCATION_SERVICE)) {
            Log.i(Constants.TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopSelf();
        }
    }

    @NonNull
    private Bundle prepareBundle(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (null == bundle) bundle = new Bundle();
        bundle.putString(Constants.EMP_ID, Utility.readPref(this.getApplicationContext(), Constants.EMP_ID));
        bundle.putString(Constants.EMP_NAME, Utility.readPref(this.getApplicationContext(), Constants.EMP_NAME));
        bundle.putString(Constants.DEVICE_ID, Utility.readPref(this.getApplicationContext(), Constants.DEVICE_ID));
        return bundle;
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
            LocationModel locationModel = new LocationModel(location.getLatitude(), location.getLongitude());
//            sendLocationUpdate(this, locationModel);
            boolean isPunchIntervalElapsed = checkPunchIntervalElapsed();
            if (isPunchIntervalElapsed) {
                sendUpdatesToService(locationModel);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Failed to connect to Google API");
    }

    @Override
    public void onDestroy() {
        if (null != mLocationClient) {
            mLocationClient.disconnect();
        }
        super.onDestroy();
    }

    private void sendUpdatesToService(LocationModel locationModel) {
        Attendance attendance = new Attendance(Utility.readPref(this.getApplicationContext(), Constants.EMP_ID));
        attendance.setDevId(Utility.readPref(this.getApplicationContext(), Constants.DEVICE_ID));
        attendance.setLat(locationModel.getLatitude());
        attendance.setLon(locationModel.getLongitude());
        attendance.setTime(locationModel.getLogTime());

        UpdateAttendance updateAttendance = new UpdateAttendance(LocationMonitoringService.this, attendance);
        updateAttendance.setContext(this.getApplicationContext());
        updateAttendance.register();
    }

    private boolean checkPunchIntervalElapsed() {
        String lastTime = Utility.readPref(this.getApplicationContext(), Constants.LAST_UPDATE_TO_SERVER_TIME);
        long lastUpdateTime = StringUtils.isEmpty(lastTime) ? System.currentTimeMillis() : Long.parseLong(lastTime);
        return System.currentTimeMillis() - lastUpdateTime >= Utility.getPunchingInterval(this.getApplicationContext());
    }

    private LocationRequest getLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Utility.getPunchingInterval(this));
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
        intent.putExtra(EXTRA_FETCH_TIME, locationModel.getLogTime());
        context.sendBroadcast(intent);
        Log.v(TAG, "broadcast msg sent...");
    }

    @Override
    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(MSG_OK));
        String time = Utility.getTime();
        String successMsg = String.format(Constants.ATTENDANCE_REGISTERED_MSG, time, attendance.getLon(), attendance.getLat());
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        if (isSuccess) {
            Log.i(Constants.TAG, successMsg);
        } else {
            Log.e(Constants.TAG, failedMsg);
        }
    }
}
