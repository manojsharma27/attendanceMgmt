package com.ms.app.attendancemgmt.service;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationServices;
import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.location.AddressLocator;
import com.ms.app.attendancemgmt.location.StoredLocationUploader;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LocationModel;
import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Response;

public class LocationMonitoringService extends Service implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        LocationListener, ServerUpdateResponseHandler, android.location.LocationListener {

    GoogleApiClient mLocationClient;
    private static final int REQUEST_PERMISSIONS_REQUEST_CODE = 34;
    private static final int NOTIFICATION_ID_FOREGROUND_SERVICE = 12121;
    public static final int REQUEST_CODE = 11012;
    public static final String ACTION_LOCATION_BROADCAST = "LocationMonitoringService-LocationBroadcast";
    private TimerTask timerTask;
    private Timer timer;
    private StoredLocationUploader storedLocationUploader;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(Constants.TAG + this.getClass().getSimpleName(), "Inside onStartCommand");
        if (null == intent || !startedByActivityOrSelf(intent)) {
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

    private boolean startedByActivityOrSelf(Intent intent) {
        String startedBy = intent.getStringExtra(Constants.STARTED_BY);
        if (StringUtils.isEmpty(startedBy)) {
            return false;
        }

        return startedBy.equals(Constants.ACTIVITY) || startedBy.equals(Constants.SELF);
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
            // init uploader
            storedLocationUploader = new StoredLocationUploader(this.getApplicationContext());
            configureLocationUpdateRequesterTask();
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
                Log.i(Constants.TAG, "Executing task....");
                requestLocationUpdate();
                uploadStoredLocations();
            }
        };
        timer = new Timer();
        long punchInterval = Utility.getPunchingInterval(LocationMonitoringService.this.getApplicationContext());
        long requestInterval = Math.max(punchInterval / 4, Constants.MIN_PUNCH_INTERVAL);
//        requestInterval = TimeUnit.SECONDS.toMillis(30); // this was set for testing purpose
        timer.schedule(timerTask, Constants.FASTEST_LOCATION_INTERVAL, requestInterval);
    }

    private void uploadStoredLocations() {
        if (Utility.checkInternetConnected(LocationMonitoringService.this.getApplicationContext())) {
            storedLocationUploader.checkLocationsAndUpload();
        }
    }

    private void stopLocationUpdateRequesterTask() {
        if (null != timerTask) {
            timerTask.cancel();
        }
        if (null != timer) {
            timer.cancel();
        }
        if (null != storedLocationUploader) {
            storedLocationUploader = null;
        }
        if (null != mLocationClient) {
            mLocationClient.disconnect();
        }
    }

    @NonNull
    private Bundle prepareBundle(Intent intent) {
        Bundle bundle = intent.getExtras();
        if (null == bundle) bundle = new Bundle();
        bundle.putString(Constants.EMP_ID, Utility.readPref(LocationMonitoringService.this.getApplicationContext(), Constants.EMP_ID));
        bundle.putString(Constants.EMP_NAME, Utility.readPref(LocationMonitoringService.this.getApplicationContext(), Constants.EMP_NAME));
        bundle.putString(Constants.DEVICE_ID, Utility.readPref(LocationMonitoringService.this.getApplicationContext(), Constants.DEVICE_ID));
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
//        LocationServices.FusedLocationApi.flushLocations(mLocationClient);
//        LocationServices.FusedLocationApi.requestLocationUpdates(mLocationClient, Utility.getLocationRequest(), this, Looper.getMainLooper());
        LocationManager locationManager = (LocationManager) this.getSystemService(LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.FASTEST_LOCATION_INTERVAL, 5, this);
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
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {
        if (provider.equals(LocationManager.GPS_PROVIDER)) {
            requestLocationUpdate();
        }
    }

    @Override
    public void onProviderDisabled(String provider) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(Constants.TAG, "Failed to connect to Google API");
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.TAG, "Inside onDestroy");
        stopLocationUpdateRequesterTask();
        Utility.writePref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_OUT);
        super.onDestroy();
    }

    private void sendUpdatesToService(LocationModel locationModel) {
        Attendance attendance = new Attendance(Utility.readPref(LocationMonitoringService.this.getApplicationContext(), Constants.EMP_ID));
        attendance.setAddress(AddressLocator.populateAddress(LocationMonitoringService.this.getApplicationContext(), locationModel.getLatitude(), locationModel.getLongitude()));
        attendance.setDevId(Utility.readPref(LocationMonitoringService.this.getApplicationContext(), Constants.DEVICE_ID));
        attendance.setLat(locationModel.getLatitude());
        attendance.setLon(locationModel.getLongitude());
        attendance.setTime(locationModel.getLogTime());

        if (Utility.checkInternetConnected(LocationMonitoringService.this.getApplicationContext())) {
            UpdateAttendance updateAttendance = new UpdateAttendance(LocationMonitoringService.this, attendance);
            updateAttendance.setContext(LocationMonitoringService.this.getApplicationContext());
            updateAttendance.register();
        } else {
            // write location updates to file
            FileHandler.writeAttendanceToFile(LocationMonitoringService.this.getApplicationContext(), attendance);
            Log.i(Constants.TAG, "Recorded in file : " + attendance.toString());
        }
    }

    private boolean checkPunchIntervalElapsed() {
        String lastUpdateStr = Utility.readPref(getApplicationContext(), Constants.LAST_UPDATE_TO_SERVER_TIME);
        if (StringUtils.isEmpty(lastUpdateStr)) {
            return true;
        }
        long lastUpdateTime = Long.parseLong(lastUpdateStr);
//        long lastUpdated = (null == lastUpdateTime) ? System.currentTimeMillis() : lastUpdateTime;
        return Utility.getPunchingInterval(LocationMonitoringService.this.getApplicationContext()) <= System.currentTimeMillis() - lastUpdateTime;
    }

    @Override
    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(Constants.MSG_OK));
        String time = Utility.getTime();
        String successMsg = String.format(Constants.ATTEND_REG_LOC_LOG, time, attendance.getLon(), attendance.getLat());
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        if (isSuccess) {
            Log.i(Constants.TAG, successMsg);
            Utility.writePref(getApplicationContext(), Constants.LAST_UPDATE_TO_SERVER_TIME, String.valueOf(System.currentTimeMillis()));
        } else {
            Log.e(Constants.TAG, failedMsg);
            // write location updates to file
            FileHandler.writeAttendanceToFile(LocationMonitoringService.this.getApplicationContext(), attendance);
            Log.i(Constants.TAG, "Recorded in file");
        }
    }

}
