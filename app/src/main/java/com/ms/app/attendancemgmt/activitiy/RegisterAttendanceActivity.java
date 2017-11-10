package com.ms.app.attendancemgmt.activitiy;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResult;
import com.google.android.gms.location.LocationSettingsStates;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.anotherservice.MyAlarmReceiver;
import com.ms.app.attendancemgmt.location.LocationUtil.PermissionUtils;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.service.BackgroundTaskHandler;
import com.ms.app.attendancemgmt.service.FileHandler;
import com.ms.app.attendancemgmt.service.LocationMonitoringService;
import com.ms.app.attendancemgmt.service.UpdateLocationToServerBroadcastReceiver;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.Response;

import static android.app.PendingIntent.FLAG_ONE_SHOT;
import static com.ms.app.attendancemgmt.util.Constants.MSG_OK;

public class RegisterAttendanceActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 191;
    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 192;

    private AlertDialog alertDialog;
    private Context context;
    private String deviceId;
    private ProgressBar pbRegAttend;
    private View reg_attend_form;
    private RelativeLayout rlRegAttend;
    private TextView tvTodoMsg;
    private TextView tvRegAttendance;

    private final static int PLAY_SERVICES_REQUEST = 1000;
    private final static int REQUEST_CHECK_SETTINGS = 2000;

    private BackgroundTaskHandler backgroundTaskHandler;
    private TelephonyManager telephonyManager;
    private Location mLastLocation;

    // Google client to interact with Google API

    private GoogleApiClient mGoogleApiClient;

    double latitude;
    double longitude;

    // list of permissions

    ArrayList<String> permissions = new ArrayList<>();
    PermissionUtils permissionUtils;

    boolean isPermissionGranted;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.reg_attend_temp);
        context = RegisterAttendanceActivity.this;
        String empName = this.getIntent().getExtras().getString(Constants.EMP_NAME);
        final String empId = this.getIntent().getExtras().getString(Constants.EMP_ID);
        Utility.saveSharedPref(getApplicationContext(), Constants.EMP_ID, empId);
//        final Employee employee = Utility.searchEmployeeFromPin(empName);
        if (StringUtils.isEmpty(empId)) {
            showEmpNotFoundDialog();
            return;
        }

        backgroundTaskHandler = new BackgroundTaskHandler(context);
        tvTodoMsg = findViewById(R.id.tvTodoMsg);
        tvRegAttendance = findViewById(R.id.tvRegAttendance);
        TextView tvEmpName = findViewById(R.id.tvEmpName);
        tvEmpName.setText(String.format(Constants.HELLO_MSG, empName));
        pbRegAttend = findViewById(R.id.pb_register_attendance);
        pbRegAttend.setVisibility(View.GONE);

        reg_attend_form = findViewById(R.id.reg_attend_form);

        rlRegAttend = findViewById(R.id.rlRegAttendance);
        rlRegAttend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isPunchedIn()) {
                    doPunchOut();
                } else {
                    doPunchIn();
                }
            }
        });
        updatePunchUI(!isPunchedIn());
        setupPermissionUtils();
        if (checkPlayServices()) {
            buildGoogleApiClient();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        final LocationSettingsStates states = LocationSettingsStates.fromIntent(data);
        switch (requestCode) {
            case REQUEST_CHECK_SETTINGS:
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        getLocation();
                        break;
                    case Activity.RESULT_CANCELED:
                        break;
                    default:
                        break;
                }
                break;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(Constants.TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + result.getErrorCode());
    }

    @Override
    public void onConnected(Bundle arg0) {
//        getLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
//        mGoogleApiClient.connect();
    }

    // Permission check functions

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // redirects to utils
        permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void PermissionGranted(int request_code) {
        Log.i("PERMISSION", "GRANTED");
        isPermissionGranted = true;
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {
        Log.i("PERMISSION PARTIALLY", "GRANTED");
    }

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION", "DENIED");
    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION", "NEVER ASK AGAIN");
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
        if (null != alertDialog)
            alertDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPlayServices();
        getLocation();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_register_attendance, menu);
        return true;
    }

    // Code for fetching deviceId

    private void configureTelephonyManager() {
        if (null == telephonyManager) {
            telephonyManager = (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
        }
    }

    private void populateDeviceId() {
        configureTelephonyManager();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
            deviceId = telephonyManager.getDeviceId();
        }

        if (null == deviceId || deviceId.contains("000000")) {
            deviceId = Settings.Secure.getString(getApplicationContext().getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        Utility.toastMsg(this, "DeviceId: " + deviceId);
    }

    private boolean checkAndRequestDeviceIdPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void doRegistration(String empId) {
        getLocation();
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
        } else {
            Utility.toastMsg(context, "Couldn't get the location. Make sure location is enabled on the device");
            return;
        }
        checkAndRequestDeviceIdPermission();
        registerAttendance(empId);
    }

    private void setupPermissionUtils() {
        permissionUtils = new PermissionUtils(RegisterAttendanceActivity.this);
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionUtils.check_permission(permissions, "Need GPS permission for getting your location", 1);
    }

    private void getLocation() {
        if (isPermissionGranted) {
            try {
                mLastLocation = LocationServices.FusedLocationApi
                        .getLastLocation(mGoogleApiClient);
            } catch (SecurityException e) {
                e.printStackTrace();
            }
        }
    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        LocationRequest mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(10000);
        mLocationRequest.setFastestInterval(5000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder()
                .addLocationRequest(mLocationRequest);

        PendingResult<LocationSettingsResult> result =
                LocationServices.SettingsApi.checkLocationSettings(mGoogleApiClient, builder.build());

        result.setResultCallback(new ResultCallback<LocationSettingsResult>() {
            @Override
            public void onResult(LocationSettingsResult locationSettingsResult) {

                final Status status = locationSettingsResult.getStatus();

                switch (status.getStatusCode()) {
                    case LocationSettingsStatusCodes.SUCCESS:
                        // All location settings are satisfied. The client can initialize location requests here
                        getLocation();
                        break;
                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
                        try {
                            // Show the dialog by calling startResolutionForResult(),
                            // and check the result in onActivityResult().
                            status.startResolutionForResult(RegisterAttendanceActivity.this, REQUEST_CHECK_SETTINGS);

                        } catch (IntentSender.SendIntentException e) {
                            // Ignore the error.
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
                        break;
                }
            }
        });


    }

    /**
     * Method to verify google play services on the device
     */

    private boolean checkPlayServices() {

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();

        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleApiAvailability.isUserResolvableError(resultCode)) {
                googleApiAvailability.getErrorDialog(this, resultCode,
                        PLAY_SERVICES_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void showEmpNotFoundDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert:");
        builder.setMessage("Oops! Your details not found.");
        builder.setNeutralButton("Ok", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                finish();
            }
        });
        builder.setCancelable(false);
        alertDialog = builder.create();
        alertDialog.show();
    }

    public void updateProgressBar(int value) {
        pbRegAttend.setProgress(value);
    }

    public void showProgressBar(boolean show) {
        pbRegAttend.setVisibility(show ? View.VISIBLE : View.GONE);
        reg_attend_form.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    private void registerAttendance(String empId) {
        Attendance attendance = new Attendance(empId);
        attendance.setTime(new Date());
        // TODO : get uniqueId for app installation
        attendance.setLat(latitude);
        attendance.setLon(longitude);
        if (null == telephonyManager) {
            checkAndRequestDeviceIdPermission();
            populateDeviceId();
        }
        attendance.setDevId(deviceId);

        // set deviceId and empId in preferences for background update
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.EMP_ID, attendance.getId()).apply();
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(Constants.DEVICE_ID, attendance.getDevId()).apply();

        UpdateAttendance updateAttendance = new UpdateAttendance(this, attendance);
        updateAttendance.setContext(getApplicationContext());
        updateAttendance.register();

        // cant register multiple times
        rlRegAttend.setEnabled(false);
    }

    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(MSG_OK));
        if (!isSuccess) {
            // if registration failed allow registering for multiple times
            rlRegAttend.setEnabled(true);
        }
        String time = Utility.getTime();
        String successMsg = String.format("Attendance registered at %s. \n Loc: (%s,%s)", time, attendance.getLon(), attendance.getLat());
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        Utility.showMessageDialog(RegisterAttendanceActivity.this, isSuccess ? successMsg : failedMsg, isSuccess ? R.mipmap.right : R.mipmap.wrong);
        Utility.toastMsg(context, isSuccess ? successMsg : failedMsg);
    }

    private void showGpsNotEnabledDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Alert:");
        builder.setMessage("GPS is not enabled. Would you like to enable it?");
        builder.setCancelable(false);
        builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                context.startActivity(locationIntent);
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
                Utility.toastMsg(context, "Attendance not registered.");
            }
        });
        alertDialog = builder.create();
        alertDialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mitemLogout:
                finish();
                return true;
            case R.id.mitemStartAutoUpdates:
                backgroundTaskHandler.scheduleUpdateLocationToServerAlarm();
                return true;
            case R.id.mitemStopAutoUpdates:
                backgroundTaskHandler.cancelUpdateLocationToServerAlarm();
                backgroundTaskHandler.stopLocationMonitoringService();
                return true;
            case R.id.mitemReadStoredLocations:
                List<Attendance> attendances = FileHandler.readAttendanceFromFile(getApplicationContext());
                if (CollectionUtils.isEmpty(attendances)) {
                    Utility.toastMsg(getApplicationContext(), "No stored locations to show");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                for (Attendance attendance : attendances) {
                    sb.append(attendance.toString()).append("\n");
                }
                Utility.showMessageDialog(RegisterAttendanceActivity.this, sb.toString());
                return true;
            case R.id.mitemCleanLocations:
                FileHandler.cleanUp(getApplicationContext());
                return true;
        }
        return super.onOptionsItemSelected(item);
    }


    private boolean isPunchedIn() {
        String punchStatus = Utility.readFromSharedPref(getApplicationContext(), Constants.PUNCH_STATUS);
        return StringUtils.equals(Constants.PUNCHED_IN, punchStatus);
    }

    private void doPunchIn() {
        backgroundTaskHandler.scheduleAlarmToStartLocationMonitorService();
        backgroundTaskHandler.scheduleUpdateLocationToServerAlarm();
        Utility.saveSharedPref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_IN);
        // set to punch out
        updatePunchUI(false);
    }

    private void doPunchOut() {
        backgroundTaskHandler.cancelUpdateLocationToServerAlarm();
        backgroundTaskHandler.cancelAlarmToStartLocationMonitorService();
        backgroundTaskHandler.stopLocationMonitoringService();
        Utility.saveSharedPref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_OUT);
        // set to punch in
        updatePunchUI(true);
    }

    private void updatePunchUI(boolean setPunchedIn) {
        String action = setPunchedIn ? "Punch In" : "Punch Out";
        int color = getColor(setPunchedIn ? R.color.colorGreen : R.color.colorOrange);
        String startStop = setPunchedIn ? "start" : "stop";

        tvTodoMsg.setText(String.format("Tap '%s' to %s regular punching in background", action, startStop));
        tvRegAttendance.setText(action);
        tvRegAttendance.setBackgroundColor(color);
        rlRegAttend.setBackgroundColor(color);
    }

}
