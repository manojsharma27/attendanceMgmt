package com.ms.app.attendancemgmt.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import java.sql.Timestamp;

public class ServiceMgr extends Service {
    public static final int FIRST_RUN = 1000;
    public static final int INTERVAL = 500;
    int REQUEST_CODE = 14122192;
    AlarmManager alarmManager;

    public void onCreate() {
        super.onCreate();
        startService();
        Log.v(getClass().getName(), "onCreate(..)");
    }

    public IBinder onBind(Intent intent) {

        Log.v(getClass().getName(), "onBind(..)");
        return null;
    }

    public void onDestroy() {
        if (this.alarmManager != null) {
            this.alarmManager.cancel(PendingIntent.getBroadcast(getApplicationContext(), this.REQUEST_CODE, new Intent(getApplicationContext(), RepeatLocUpdateService.class), 0));
        }
        Toast.makeText(getApplicationContext(), "Tracking Stopped!", Toast.LENGTH_LONG).show();
        Log.v(getClass().getName(), "Service onDestroy(). Stop AlarmManager at " + new Timestamp(System.currentTimeMillis()).toString());
    }

    private void startService() {
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), this.REQUEST_CODE, new Intent(getApplicationContext(), RepeatLocUpdateService.class), 0);
        this.alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        this.alarmManager.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP, System.currentTimeMillis() + FIRST_RUN, INTERVAL, pendingIntent);
        Toast.makeText(getApplicationContext(), "Tracking Started.", Toast.LENGTH_LONG).show();
        Log.v(getClass().getName(), "AlarmManger started at " + new Timestamp(System.currentTimeMillis()).toString());
    }
}
