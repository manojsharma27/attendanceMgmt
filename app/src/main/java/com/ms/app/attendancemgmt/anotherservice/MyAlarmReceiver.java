package com.ms.app.attendancemgmt.anotherservice;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.util.Log;

import com.ms.app.attendancemgmt.util.Constants;

public class MyAlarmReceiver extends BroadcastReceiver {
    public static final int REQUEST_CODE = 12345;
    public static final String ACTION = "com.ms.app.attendancemgmt.anotherservice.alarm";

    // Triggered by the Alarm periodically (starts the service to run task)
    @Override
    public void onReceive(Context context, Intent intent) {
//        prevTestCode(context);
        long repeatCount = PreferenceManager.getDefaultSharedPreferences(context).getLong("REPEAT_COUNT", 0L);
        String empId = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.EMP_ID, "myempid");
        String devId = PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.DEVICE_ID, "mydevid");

        Intent i = new Intent(context, MyAnotherService.class);
        i.putExtra(Constants.EMP_ID, empId);
        i.putExtra(Constants.DEVICE_ID, devId);
        i.putExtra("repeatation", repeatCount);
        Log.i("MyTestService", "started service from receiver.");
        context.startService(i);

        repeatCount++;
        if (repeatCount > 5) {
            Log.i("MyTestService", "No more tasks..");
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("REPEAT_COUNT", repeatCount).apply();
        Log.i("MyTestService", "Next alarm scheduled.");
        setAnotherAlarm(context);
    }

    private void prevTestCode(Context context) {
        long repeatCount = PreferenceManager.getDefaultSharedPreferences(context).getLong("REPEAT_COUNT", 0L);

        Intent i = new Intent(context, MyTestService.class);
        i.putExtra("foo", "bar");
        i.putExtra("repeatation", repeatCount);
        Log.i("MyTestService", "started service from receiver.");
        context.startService(i);

        Log.i("MyTestService", "This should execute...");
        // repeat alarm for 10 times only
        repeatCount++;
        if (repeatCount > 5) {
            Log.i("MyTestService", "No more tasks..");
            return;
        }

        PreferenceManager.getDefaultSharedPreferences(context).edit().putLong("REPEAT_COUNT", repeatCount).apply();
        Log.i("MyTestService", "Next alarm scheduled.");

        // set another alarm after 1 sec.
        setAnotherAlarm(context);
    }

    private void setAnotherAlarm(Context context) {
        AlarmManager manager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        Intent alarmIntent = new Intent(context, MyAnotherService.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, 0);
        long triggerAtMillis = System.currentTimeMillis() + 1000; // current time + 1 sec
        manager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
    }
}
