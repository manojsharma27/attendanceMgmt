package com.ms.app.attendancemgmt.service;


import android.Manifest;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationAvailability;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
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

import java.util.Arrays;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Response;


public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ServerUpdateResponseHandler {

    GoogleApiClient mLocationClient;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 12121;
    public static final int REQUEST_CODE = 11012;
    public static final String ACTION_LOCATION_BROADCAST = "LocationMonitoringService-LocationBroadcast";
    private TimerTask timerTask;
    private Timer timer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Constants.TAG + this.getClass().getSimpleName(), "Inside onStartCommand");
        if (null == intent) {
            Utility.writePref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_IN);
            Log.w(Constants.TAG, "Service was stopped and automatically restarted by the system.");
            BackgroundTaskHandler.startLocationMonitorServiceBySelf(this, Constants.START_LOC_MONITOR_SERVICE_INTERVAL);
            Log.w(Constants.TAG, "Stopped self. Restarting again via alarm.");
            stopForeground(true);
            stopSelf();
        } else {
            Utility.writePref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_IN);
            createLocationClientAndConnect();
            configureNotificationIntent(intent);
        }
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        Log.i(Constants.TAG, "Inside onCreate");
        super.onCreate();
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
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("Auto punching")
                    .setSmallIcon(R.mipmap.gps_icon)
                    .setLargeIcon(Bitmap.createScaledBitmap(icon, 100, 100, false))
                    .setContentIntent(pendingIntent)
                    .setOngoing(true)
                    .build();

            startForeground(NOTIFICATION_ID_FOREGROUND_SERVICE, notification);
            Utility.writePref(this.getApplicationContext(), Constants.LAST_UPDATE_TO_SERVER_TIME, String.valueOf(System.currentTimeMillis()));
            configureLocationUpdateRequesterTask();
//            Utility.updateLocationServiceStatus(LocationMonitoringService.this.getApplicationContext(), Constants.STARTED);
        } else if (action.equals(Constants.ACTION_STOP_FOREGROUND_LOCATION_SERVICE)) {
            Log.i(Constants.TAG, "Received Stop Foreground Intent");
            stopForeground(true);
            stopLocationUpdateRequesterTask();
            stopSelf();
        }
    }

    private void configureLocationUpdateRequesterTask() {
        timerTask = new TimerTask() {
            @Override
            public void run() {
                requestLocationUpdate();
            }
        };
        timer = new Timer();
        long punchInterval = Utility.getPunchingInterval(this.getApplicationContext());
        long requestInterval = Math.max(punchInterval / 4, Constants.MIN_PUNCH_INTERVAL);
        timer.schedule(timerTask, Constants.FASTEST_LOCATION_INTERVAL, requestInterval);
    }

    private void stopLocationUpdateRequesterTask() {
        if (null != timerTask) {
            timerTask.cancel();
        }
        if (null != timer) {
            timer.cancel();
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
        requestLocationUpdate();
        Log.d(Constants.TAG, "Connected to Google API");
    }

    private void requestLocationUpdate() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.d(Constants.TAG, "== Error On onConnected(). Permission not granted");
            return;
        }
        LocationServices.FusedLocationApi.flushLocations(mLocationClient);
        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, getLocationRequest(), this, Looper.getMainLooper());
    }

    @Override
    public void onConnectionSuspended(int i) {
        createLocationClientAndConnect();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.v(Constants.TAG, "location changed");
            LocationModel locationModel = new LocationModel(location.getLatitude(), location.getLongitude());
            boolean isPunchIntervalElapsed = checkPunchIntervalElapsed();
            if (isPunchIntervalElapsed) {
                sendUpdatesToService(locationModel);
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(Constants.TAG, "Failed to connect to Google API");
    }

    @Override
    public void onDestroy() {
        Log.i(Constants.TAG, "Inside onDestroy");
        if (null != mLocationClient) {
            mLocationClient.disconnect();
        }
        Utility.writePref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_OUT);
        super.onDestroy();
    }

    private void sendUpdatesToService(LocationModel locationModel) {
        Attendance attendance = new Attendance(Utility.readPref(this.getApplicationContext(), Constants.EMP_ID));
        attendance.setDevId(Utility.readPref(this.getApplicationContext(), Constants.DEVICE_ID));
        attendance.setLat(locationModel.getLatitude());
        attendance.setLon(locationModel.getLongitude());
        attendance.setTime(locationModel.getLogTime());

        if (Utility.checkInternetConnected(LocationMonitoringService.this.getApplicationContext())) {
            UpdateAttendance updateAttendance = new UpdateAttendance(LocationMonitoringService.this, attendance);
            updateAttendance.setContext(this.getApplicationContext());
            updateAttendance.register();
        } else {
            // write location updates to file
            FileHandler.writeAttendanceToFile(LocationMonitoringService.this.getApplicationContext(), attendance);
            Log.i(Constants.TAG, "Recorded in file");
        }
    }

    private boolean checkPunchIntervalElapsed() {
        String lastTime = Utility.readPref(this.getApplicationContext(), Constants.LAST_UPDATE_TO_SERVER_TIME);
        long lastUpdateTime = StringUtils.isEmpty(lastTime) ? System.currentTimeMillis() : Long.parseLong(lastTime);
        return System.currentTimeMillis() - lastUpdateTime >= Utility.getPunchingInterval(this.getApplicationContext());
    }

    private LocationRequest getLocationRequest() {
        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(Constants.LOCATION_INTERVAL);
        mLocationRequest.setFastestInterval(Constants.FASTEST_LOCATION_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        return mLocationRequest;
    }

    @Override
    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(Constants.MSG_OK));
        String time = Utility.getTime();
        String successMsg = String.format(Constants.ATTENDANCE_REGISTERED_MSG, time, attendance.getLon(), attendance.getLat());
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        if (isSuccess) {
            Log.i(Constants.TAG, successMsg);
        } else {
            Log.e(Constants.TAG, failedMsg);
            // write location updates to file
            FileHandler.writeAttendanceToFile(LocationMonitoringService.this.getApplicationContext(), attendance);
            Log.i(Constants.TAG, "Recorded in file");
        }
    }

}
