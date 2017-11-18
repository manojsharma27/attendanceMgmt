package com.ms.app.attendancemgmt.activitiy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
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
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.location.AddressLocator;
import com.ms.app.attendancemgmt.location.PermissionUtils;
import com.ms.app.attendancemgmt.location.StoredLocationUploader;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.service.BackgroundTaskHandler;
import com.ms.app.attendancemgmt.service.FileHandler;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.MasterPinValidateCallback;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Response;

public class RegisterAttendanceActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback, ServerUpdateResponseHandler {
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
        onNewIntent(getIntent());
        String empName = this.getIntent().getExtras().getString(Constants.EMP_NAME);

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
                if (Utility.isPunchedIn(getApplicationContext())) {
                    doPunchOut();
                } else {
                    doRegistration(Utility.readPref(context, Constants.EMP_ID)); // instant location updates when punchIn clicked.
                    if (null != mGoogleApiClient) {
                        mGoogleApiClient.disconnect();
                    }
                    doPunchIn();
                }
            }
        });
        updatePunchUI(!Utility.isPunchedIn(getApplicationContext()));
        setupPermissionUtils();
        if (checkPlayServices()) {
            buildGoogleApiClient();
            getLocation();
        }
        if (checkAndRequestDeviceIdPermission()) {
            populateDeviceId();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        String empId = this.getIntent().getExtras().getString(Constants.EMP_ID);
        String empName = this.getIntent().getExtras().getString(Constants.EMP_NAME);
        if (StringUtils.isEmpty(empId)) {
            showEmpNotFoundDialog();
            return;
        }
        Utility.writePref(getApplicationContext(), Constants.EMP_ID, empId);
        Utility.writePref(getApplicationContext(), Constants.EMP_NAME, empName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
        getLocation();
    }

    @Override
    public void onConnectionSuspended(int arg0) {
        mGoogleApiClient.connect();
    }

    // Permission check functions

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        // redirects to utils
        permissionUtils.onRequestPermissionsResult(RegisterAttendanceActivity.this, requestCode, permissions, grantResults);
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
        if (null != mGoogleApiClient) {
            mGoogleApiClient.disconnect();
        }
        super.onDestroy();
        if (null != alertDialog)
            alertDialog.dismiss();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkPlayServices()) {
            buildGoogleApiClient();
            getLocation();
        }
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
        Log.d(Constants.TAG, "DeviceId: " + deviceId);
    }

    private boolean checkAndRequestDeviceIdPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }

    private void doRegistration(String empId) {
        checkAndRequestDeviceIdPermission();
        getLocation();
        if (mLastLocation != null) {
            latitude = mLastLocation.getLatitude();
            longitude = mLastLocation.getLongitude();
        } else {
            Utility.toastMsg(context, "Couldn't get the location. Make sure location is enabled on the device");
            return;
        }
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
        if (null != mGoogleApiClient) {
            return;
        }
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        mGoogleApiClient.connect();

        LocationRequest mLocationRequest = Utility.getLocationRequest();
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
        Utility.writePref(context, Constants.EMP_ID, attendance.getId());
        Utility.writePref(context, Constants.DEVICE_ID, attendance.getDevId());

        UpdateAttendance updateAttendance = new UpdateAttendance(RegisterAttendanceActivity.this, attendance);
        updateAttendance.setContext(getApplicationContext());
        updateAttendance.register();
    }

    @Override
    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(Constants.MSG_OK));
        String time = Utility.getTime();
        String address = AddressLocator.populateAddress(this.getApplicationContext(), attendance.getLat(), attendance.getLon());
        String successMsg = String.format(Constants.ATTEND_REG_LOC_MSG, time, address);
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        Utility.showMessageDialog(RegisterAttendanceActivity.this, isSuccess ? successMsg : failedMsg, isSuccess ? R.mipmap.right : R.mipmap.wrong);
        successMsg = String.format(Constants.ATTEND_REG_TOAST_MSG, time);
        Utility.toastMsg(context, isSuccess ? successMsg : failedMsg);

        if (!isSuccess) {
            Log.i(Constants.TAG, "Failed to register to service, so recording in file.");
            FileHandler.writeAttendanceToFile(this.getApplicationContext(), attendance);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.mitemLogout:
                if (Utility.isPunchedIn(getApplicationContext())) {
                    doPunchOut();
                }
                finish();
                return true;
            case R.id.mitemStopAutoUpdates:
                backgroundTaskHandler.stopLocationMonitorService(this);
                backgroundTaskHandler.stopLocationMonitoringService();
                return true;
            case R.id.mitemReadStoredLocations:
                List<Attendance> attendances = FileHandler.readAttendanceFromFile(this.getApplicationContext());
                if (CollectionUtils.isEmpty(attendances)) {
                    Utility.toastMsg(getApplicationContext(), "No stored locations to show");
                    return true;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Latitude, Longitude:\n\n");
                for (Attendance attendance : attendances) {
                    sb.append(String.format("( %s, %s )", attendance.getLat(), attendance.getLon())).append("\n");
                }
                Utility.showMessageDialog(RegisterAttendanceActivity.this, sb.toString());
                return true;
            case R.id.mitemSyncStoredLocations:
                if (!Utility.checkInternetConnected(this.getApplicationContext())) {
                    Utility.toastMsg(getApplicationContext(), "Failed to sync stored locations");
                    return true;
                }
                StoredLocationUploader uploader = new StoredLocationUploader(this.getApplicationContext());
                uploader.checkLocationsAndUpload();
                Utility.toastMsg(this.getApplicationContext(), "Started background sync of stored locations.");
                return true;
            case R.id.mitemCleanLocations:
                FileHandler.cleanUp(this.getApplicationContext());
                return true;
            case R.id.mitemLocUpdateInterval:
                MasterPinValidateCallback callback = new MasterPinValidateCallback() {
                    @Override
                    public void processMasterPinCallback(Activity activity) {
                        loadConfigurePunchIntervalDialog();
                    }
                };
                Utility.loadGetMasterPinDialog(RegisterAttendanceActivity.this, callback);
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Boolean exit = false;

    @Override
    public void onBackPressed() {
        if (Utility.isPunchedIn(getApplicationContext())) {
            super.onBackPressed();
        } else {
            loadLoginActivityAndFinish();
        }
    }

    private void askPressBackAgainToExit() {
        if (exit) {
            doPunchOut();
            loadLoginActivityAndFinish();
        } else {
            Utility.toastMsg(getApplicationContext(), "Press back again to Punch out and exit");
            exit = true;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    exit = false;
                }
            }, 3 * 1000);
        }
    }

    private void loadLoginActivityAndFinish() {
        Intent loginIntent = new Intent(this.getApplicationContext(), LoginActivity.class);
        loginIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(loginIntent);
        finish();
    }

    public static void showOnBackPressedDialog(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Alert:")
                .setMessage("You are about to exit application without punch out. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        activity.startActivity(locationIntent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        Utility.toastMsg(activity, "Attendance not registered.");
                    }
                })
                .create()
                .show();
    }

    private void doPunchIn() {
        backgroundTaskHandler.startLocationMonitorService(RegisterAttendanceActivity.this);
        Utility.writePref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_IN);
        // set to punch out
        updatePunchUI(false);
    }

    private void doPunchOut() {
        backgroundTaskHandler.stopLocationMonitorService(RegisterAttendanceActivity.this);
        Utility.writePref(getApplicationContext(), Constants.PUNCH_STATUS, Constants.PUNCHED_OUT);
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

    protected void loadConfigurePunchIntervalDialog() {
        AlertDialog.Builder dialogSetService = new AlertDialog.Builder(
                RegisterAttendanceActivity.this);
        dialogSetService.setTitle("Punch Interval (in seconds)");
        final EditText txtPunchInterval = new EditText(RegisterAttendanceActivity.this);
        long punchInterval = Utility.getPunchingInterval(RegisterAttendanceActivity.this); //readPref(LoginActivity.this, Constants.SERVICE_URL_PREF_KEY);
        punchInterval = punchInterval / 1000; // since interval is in millis
        txtPunchInterval.setInputType(InputType.TYPE_CLASS_NUMBER);
        txtPunchInterval.setText(String.valueOf(punchInterval));
        txtPunchInterval.setLayoutParams(Utility.getLayoutParamsForDialogMsgText());
        dialogSetService.setView(txtPunchInterval);
        dialogSetService.setPositiveButton("Update",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = txtPunchInterval.getText().toString().trim();

                        if (!StringUtils.isNumeric(value)) {
                            Utility.toastMsg(getApplicationContext(), "Invalid value entered.");
                            Utility.showMessageDialog(RegisterAttendanceActivity.this,
                                    "Invalid value entered.", R.mipmap.wrong);
                            return;
                        }

                        long longVal = Long.valueOf(value) * 1000; // convert to seconds
                        if (longVal < Constants.MIN_PUNCH_INTERVAL || longVal > Constants.MAX_PUNCH_INTERVAL) {
                            Utility.toastMsg(getApplicationContext(), "Entered interval is out of range.");
                            Utility.showMessageDialog(RegisterAttendanceActivity.this,
                                    "Interval value is out of range. Expected range: (1min - 1hr)", R.mipmap.wrong);
                            return;
                        }

                        //TODO: get interval input with some clock based ui.
                        Utility.writePref(getApplicationContext(),
                                Constants.PUNCHING_INTERVAL_KEY, String.valueOf(longVal));
                        String msg = "Punch interval updated to " + value + " sec";
                        Utility.toastMsg(getApplicationContext(), msg);
                        Utility.showMessageDialog(RegisterAttendanceActivity.this, msg, R.mipmap.right);
                    }
                });

        dialogSetService.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.dismiss();
                    }
                });
        dialogSetService.show();
    }

}
