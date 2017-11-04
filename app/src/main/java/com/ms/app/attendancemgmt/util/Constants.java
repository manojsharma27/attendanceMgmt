package com.ms.app.attendancemgmt.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Holds all constants at one place
 */

public class Constants {
    public static final String EMP_ID = "emp_id";
    public static final String EMP_NAME = "emp_name";
    public static final String HELLO_NAME = "Hello, %s";
    public static final String LOG_TAG = "AttendMgmt_tag";
    public static final String SHARED_PREF_NAME = "AttendMgmtSharedPref";
    public static final String SERVICE_URL_PREF_KEY = "service_url";
    public static final String MASTER_PIN = "9009";
    public static final String APP_TITLE = "Attendance Mgmt";

//    public static final String TEST_SERVICE_URL = "http://223.196.89.105:8004";
    public static final String AUTHENTICATE_PIN_ENDPOINT = "/verifypin?p=%s";
    public static final String REGISTER_ATTENDANCE_ENDPOINT = "/registerAttendance";
}
