package com.ms.app.attendancemgmt.anotherservice;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MyTestService extends IntentService {
    public MyTestService() {
        super("MyTestService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Do the task here
        Log.i("MyTestService", intent.getStringExtra("foo"));
        Log.i("MyTestService", "" + intent.getLongExtra("repeatation", -99L));
        Log.i("MyTestService", "Service running");
    }
}
