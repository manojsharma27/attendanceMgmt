package com.ms.app.attendancemgmt.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

import java.util.Date;

import okhttp3.Response;

import static com.ms.app.attendancemgmt.util.Constants.LAST_CAPTURED_TIME;
import static com.ms.app.attendancemgmt.util.Constants.LAST_LATITUDE;
import static com.ms.app.attendancemgmt.util.Constants.LAST_LONGITUDE;
import static com.ms.app.attendancemgmt.util.Constants.MSG_OK;

public class UpdateLocationToServerBroadcastReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 11011;
    public static final String UPDATE_LOCATION_TO_SERVER = "UpdateLocationToServerBroadcastReceiver-updateLocation";

    @Override
    public void onReceive(Context context, Intent intent) {
        String latitude = Utility.readFromSharedPref(context, LAST_LATITUDE);
        String longitude = Utility.readFromSharedPref(context, LAST_LONGITUDE);
        String lastCapturedTime = Utility.readFromSharedPref(context, LAST_CAPTURED_TIME);

        if (StringUtils.isEmpty(latitude) || (Double.parseDouble(latitude) == 0.0)) {
            Log.e(Constants.TAG, "Not updating to server, last_latitude : " + latitude);
            return;
        }

        if (StringUtils.isEmpty(longitude) || (Double.parseDouble(longitude) == 0.0)) {
            Log.e(Constants.TAG, "Not updating to server, last_longitude: " + longitude);
            return;
        }

        String empId = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.EMP_ID, null);
        if (StringUtils.isEmpty(empId)) {
            Log.e(Constants.TAG, "Not updating to server, emp id: " + empId);
            return;
        }

        String devId = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.DEVICE_ID, null);
        if (StringUtils.isEmpty(devId)) {
            Log.e(Constants.TAG, "Not updating to server, device id: " + devId);
            return;
        }

        long lastCapTime = Long.parseLong(lastCapturedTime);

        Attendance attendance = new Attendance(empId);
        attendance.setDevId(devId);
        attendance.setLat(Double.parseDouble(latitude));
        attendance.setLon(Double.parseDouble(longitude));
        attendance.setTime(new Date(lastCapTime));

        UpdateAttendance updateAttendance = new UpdateAttendance(UpdateLocationToServerBroadcastReceiver.this, attendance);
        updateAttendance.setContext(context);
        updateAttendance.register();
//        FileHandler.writeAttendanceToFile(context, attendance);
        scheduleNextAlarm(context);
    }

    private void scheduleNextAlarm(Context context) {
        BackgroundTaskHandler backgroundTaskHandler = new BackgroundTaskHandler(context);
        backgroundTaskHandler.scheduleUpdateLocationToServerAlarm();
    }

    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(MSG_OK));
        String time = Utility.getTime();
        String successMsg = String.format("Attendance registered at %s. \n Loc: (%s,%s)", time, attendance.getLon(), attendance.getLat());
        String failedMsg = "Registration failed.\nUnable to connect to service.";
        if (isSuccess) {
            Log.i(Constants.TAG, successMsg);
        } else {
            Log.e(Constants.TAG, failedMsg);
        }
    }
}
