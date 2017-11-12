package com.ms.app.attendancemgmt.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LocationModel;
import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;
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

public class UpdateLocationToServerBroadcastReceiver extends BroadcastReceiver implements ServerUpdateResponseHandler {
    public static final int REQUEST_CODE = 11011;
    public static final String ACTION_UPDATE_LOCATION_TO_SERVER = "UpdateLocationToServerBroadcastReceiver-updateLocation";
    private static LocationModel locationModel;

    @Override
    public IBinder peekService(Context myContext, Intent service) {
        return super.peekService(myContext, service);
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.i(Constants.TAG, "broadcast msg received");
        if (null == intent.getAction()) {
            return;
        }
        if (intent.getAction().equals(LocationMonitoringService.ACTION_LOCATION_BROADCAST)) {
            // this is just location broadcast.
            String extraLat = intent.getStringExtra(Constants.EXTRA_LATITUDE);
            String extraLong = intent.getStringExtra(Constants.EXTRA_LONGITUDE);
            String extraTime = intent.getStringExtra(Constants.EXTRA_FETCH_TIME);
            Utility.writePref(context, Constants.LAST_LATITUDE, extraLat);
            Utility.writePref(context, Constants.LAST_LONGITUDE, extraLong);
            Utility.writePref(context, Constants.LAST_CAPTURED_TIME, extraTime);
            double lat = (StringUtils.isEmpty(extraLat) ? 0.0 : Double.parseDouble(extraLat));
            double lon = (StringUtils.isEmpty(extraLong) ? 0.0 : Double.parseDouble(extraLong));
            long time = (StringUtils.isEmpty(extraTime) ? 0l : Long.parseLong(extraTime));
            if (lat == 0.0 || lon == 0.0 || time == 0) {
                return;
            }
            locationModel = new LocationModel(lat, lon, new Date(time));
            Log.i(Constants.TAG, "last location updated.");
            return;
        }

        if (intent.getAction().equals(ACTION_UPDATE_LOCATION_TO_SERVER)) {

            String latitude = Utility.readPref(context, LAST_LATITUDE);
            String longitude = Utility.readPref(context, LAST_LONGITUDE);
            String lastCapturedTime = Utility.readPref(context, LAST_CAPTURED_TIME);

            scheduleNextAlarm(context);

            if (!isValidLocation(latitude, longitude)) {
                if (null != locationModel) {
                    Log.i(Constants.TAG, "getting location from model");
                    latitude = locationModel.getLatitude() + "";
                    longitude = locationModel.getLongitude() + "";
                    lastCapturedTime = locationModel.getLogTime().getTime() + "";
                }
            }

            String empId = Utility.readPref(context, Constants.EMP_ID); //PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.EMP_ID, null);
            if (StringUtils.isEmpty(empId)) {
                Log.e(Constants.TAG, "Not updating to server, emp id: " + empId);
                return;
            }

            String devId = Utility.readPref(context, Constants.DEVICE_ID); //PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.DEVICE_ID, null);
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

            UpdateAttendance updateAttendance = new UpdateAttendance(this, attendance);
            updateAttendance.setContext(context);
            updateAttendance.register();
        }
    }

    private boolean isValidLocation(String latitude, String longitude) {
        if (StringUtils.isEmpty(latitude) || (Double.parseDouble(latitude) == 0.0)) {
            Log.e(Constants.TAG, "Not updating to server, last_latitude : " + latitude);
            return false;
        }

        if (StringUtils.isEmpty(longitude) || (Double.parseDouble(longitude) == 0.0)) {
            Log.e(Constants.TAG, "Not updating to server, last_longitude: " + longitude);
            return false;
        }
        return true;
    }

    private void scheduleNextAlarm(Context context) {
        BackgroundTaskHandler backgroundTaskHandler = new BackgroundTaskHandler(context);
//        backgroundTaskHandler.scheduleUpdateLocationToServerAlarm();
    }

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
