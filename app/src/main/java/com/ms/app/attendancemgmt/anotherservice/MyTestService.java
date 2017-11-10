package com.ms.app.attendancemgmt.anotherservice;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ms.app.attendancemgmt.activitiy.LocationRegisterer;
import com.ms.app.attendancemgmt.util.Constants;

public class MyTestService extends IntentService {

    public MyTestService() {
        super("MyTestService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String devId = intent.getStringExtra(Constants.DEVICE_ID);
        String empId = intent.getStringExtra(Constants.EMP_ID);

        Log.i("MyTestService", "Registering for device:" + devId + ", emp:" + empId);
        doBackgroundLocationRegistration(intent, devId, empId);
        // Do the task here
//        Log.i("MyTestService", intent.getStringExtra("foo"));
        Log.i("MyTestService", "" + intent.getLongExtra("repeatation", -99L));
        Log.i("MyTestService", "Service running");
    }

    private void doBackgroundLocationRegistration(Intent intent, String devId, String empId) {
        LocationRegisterer locationRegisterer = new LocationRegisterer(this);
        locationRegisterer.doRegistration(devId, empId);
    }
}
