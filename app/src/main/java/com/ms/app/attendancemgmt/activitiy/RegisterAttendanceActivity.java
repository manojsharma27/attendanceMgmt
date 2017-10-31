package com.ms.app.attendancemgmt.activitiy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.Employee;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.ArrayUtils;

import java.util.Date;

import okhttp3.Response;

public class RegisterAttendanceActivity extends AppCompatActivity {
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 191;
    private static final int PHONE_STATE_PERMISSION_REQUEST_CODE = 192;
    private TextView tvEmpName;
    private AlertDialog alertDialog;
    private Context context;
    private LocationManager locationManager;
    private TelephonyManager telephonyManager;
    private Location location;
    private String deviceId;
    private ProgressBar pbRegAttend;
    private View register_attendance_form;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_register_attendance);
        context = this;
        String pin = this.getIntent().getExtras().getString(Constants.EMP_PIN);
        final Employee employee = Utility.searchEmployeeFromPin(pin);
        if (null == employee) {
            showEmpNotFoundDialog();
            return;
        }

        tvEmpName = findViewById(R.id.tvEmpName);
        tvEmpName.setText(String.format(Constants.HELLO_NAME, employee.getName()));

        pbRegAttend = findViewById(R.id.pb_register_attendance);
        pbRegAttend.setVisibility(View.GONE);

        register_attendance_form = findViewById(R.id.register_attendance_form);

        Button btnRegAttendance = findViewById(R.id.btnRegisterAttendance);
        btnRegAttendance.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateLocation();
                checkAndRequestDeviceIdPermission();
                registerAttendance(employee);
            }
        });
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
        register_attendance_form.setVisibility(show ? View.GONE : View.VISIBLE);
    }

    @Override
    public void finish() {
        super.finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != alertDialog)
            alertDialog.dismiss();

    }

    private void registerAttendance(Employee emp) {
        Attendance attendance = new Attendance(emp.getEmpId());
        attendance.setTime(new Date());
        // TODO : get uniqueId for app installation
        if (null != location) {
            attendance.setLat(location.getLatitude());
            attendance.setLon(location.getLongitude());
        }
        if (null == telephonyManager) {
            checkAndRequestDeviceIdPermission();
            populateDeviceId();
        }
        attendance.setDevId(deviceId);

        UpdateAttendance updateAttendance = new UpdateAttendance(this, attendance);
        updateAttendance.register();
    }

    public void handleRegisterAttendanceResponse(Response response) {
        Utility.toastMsg(context, response.message());
    }

    private void configureLocationManager() {
        if (null == locationManager) {
            locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        }
    }

    private void updateLocation() {
        configureLocationManager();
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            showGpsNotEnabledDialog();
        }
        CustomLocationListener locationListener = new CustomLocationListener();
        Criteria criteria = new Criteria();
        criteria.setAltitudeRequired(false);
        criteria.setSpeedRequired(false);
        String bestProvider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(bestProvider, 5000, 10, locationListener);
        } else {
            location = locationManager.getLastKnownLocation(bestProvider);
        }
    }

    private boolean checkAndRequestLocationPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // check for requestCode after permission is granted
        if (!ArrayUtils.isEmpty(grantResults) && PackageManager.PERMISSION_GRANTED == grantResults[0]) {
            switch (requestCode) {
                case LOCATION_PERMISSION_REQUEST_CODE:
                    updateLocation();
                    break;
                case PHONE_STATE_PERMISSION_REQUEST_CODE:
                    populateDeviceId();
                    break;
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (checkAndRequestLocationPermission()) {
            updateLocation();
        }
    }

    private class CustomLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location loc) {
            if (null != loc) {
                location = loc;
            }
        }

        @Override
        public void onStatusChanged(String s, int i, Bundle bundle) {

        }

        @Override
        public void onProviderEnabled(String s) {

        }

        @Override
        public void onProviderDisabled(String s) {

        }
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
        } else {
            deviceId = null;
        }
    }

    private boolean checkAndRequestDeviceIdPermission() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.READ_PHONE_STATE}, PHONE_STATE_PERMISSION_REQUEST_CODE);
            return false;
        }
        return true;
    }
}
