package com.ms.app.attendancemgmt.util;

import android.content.Context;

import org.apache.commons.lang3.StringUtils;

/**
 * Holds all constants at one place
 */
public class Constants {
    public static final String EMP_ID = "emp_id";
    public static final String DEVICE_ID = "dev_id";
    public static final String EMP_NAME = "emp_name";
    public static final String HELLO_MSG = "Welcome, %s";
    public static final String TAG = "AttendMgmt_tag";
    public static final String SHARED_PREF_NAME = "com.ms.app.attendancemgmt_preferences";
    public static final String SERVICE_URL_PREF_KEY = "service_url";
    public static final String MASTER_PIN = "9009";
    public static final String APP_TITLE = "Attendance Mgmt";
    public static final String MSG_OK = "OK";
    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String SET_NEXT_ALARM = "setNextLocationUpdateAlarm";
    public static final String EXTRA_LATITUDE = "extra_latitude";
    public static final String EXTRA_LONGITUDE = "extra_longitude";
    public static final String LAST_LATITUDE = "lastLatitude";
    public static final String LAST_LONGITUDE = "lastLongitude";
    public static final String LAST_CAPTURED_TIME = "lastCapturedTime";
    public static final String EXTRA_FETCH_TIME = "extra_fetch_time";
    public static final String PUNCHING_INTERVAL_KEY = "punchingIntervalKey";
    public static final String ATTENDANCE_REGISTERED_MSG = "Attendance registered at %s. \n Loc: (%s,%s)";

    public static final String PUNCH_STATUS = "punchStatus";
    public static final String PUNCHED_IN = "punchedIn";
    public static final String PUNCHED_OUT = "punchedOut";

    //    public static final String TEST_SERVICE_URL = "http://223.196.89.105:8004";
    public static final String AUTHENTICATE_PIN_ENDPOINT = "/verifypin?p=%s";
    public static final String REGISTER_ATTENDANCE_ENDPOINT = "/registerAttendance";

    public static final long FASTEST_LOCATION_INTERVAL = 30 * 1000;
    public static final long LOCATION_INTERVAL = 60 * 1000; // default location interval 1 min
    public static final long MIN_PUNCH_INTERVAL = 30 * 1000; // minimum punch interval set to default
    public static final long MAX_PUNCH_INTERVAL = 60 * 60 * 1000; // max punch interval set to 1 hr
    public static final String ACTION_START_FOREGROUND_LOCATION_SERVICE = "com.ms.app.attendancemgmt.service.locationmonitoringservice.startforeground";
    public static final String ACTION_STOP_FOREGROUND_LOCATION_SERVICE = "com.ms.app.attendancemgmt.service.locationmonitoringservice.stopforeground";
    public static final String LAST_UPDATE_TO_SERVER_TIME = "lastUpdateToServerTimestamp";
}
