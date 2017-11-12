package com.ms.app.attendancemgmt.service;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import java.util.Date;

import static android.app.PendingIntent.FLAG_ONE_SHOT;

public class BackgroundTaskHandler {

    private Context context;

    public BackgroundTaskHandler(Context context) {
        this.context = context;
    }
//
//    public void scheduleUpdateLocationToServerAlarm() {
//        Intent intent = new Intent(context.getApplicationContext(), UpdateLocationToServerBroadcastReceiver.class);
//        intent.setAction(UpdateLocationToServerBroadcastReceiver.ACTION_UPDATE_LOCATION_TO_SERVER);
//        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), UpdateLocationToServerBroadcastReceiver.REQUEST_CODE,
//                intent, PendingIntent.FLAG_ONE_SHOT);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        long alarmTime = System.currentTimeMillis() + Utility.getPunchingInterval(context);
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
//        Utility.toastMsg(context.getApplicationContext(), "Location updates to server started.");
//        Log.v(Constants.TAG, "Alarm scheduled for updating location to server : " + new Date(alarmTime).toString());
//    }

    public void startLocationMonitorService(Activity activity) {
        Intent intent = new Intent(activity, LocationMonitoringService.class);
        intent.setAction(Constants.ACTION_START_FOREGROUND_LOCATION_SERVICE);
        activity.startService(intent);
        Utility.toastMsg(context.getApplicationContext(), "Background location updates to server started");
//        PendingIntent pendingIntent = PendingIntent.getService(context.getApplicationContext(), LocationMonitoringService.REQUEST_CODE, intent, FLAG_ONE_SHOT);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        long alarmTime = System.currentTimeMillis();
//        alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarmTime, pendingIntent);
//        Utility.toastMsg(context.getApplicationContext(), "Location monitoring started.");
//        Log.v(Constants.TAG, "Alarm scheduled for location monitor service : " + new Date(alarmTime).toString());
    }

//    public void cancelUpdateLocationToServerAlarm() {
//        Intent intent = new Intent(context.getApplicationContext(), UpdateLocationToServerBroadcastReceiver.class);
//        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), UpdateLocationToServerBroadcastReceiver.REQUEST_CODE,
//                intent, PendingIntent.FLAG_ONE_SHOT);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        alarmManager.cancel(pendingIntent);
//        Utility.toastMsg(context.getApplicationContext(), "Alarm for updating location to server stopped");
//        Log.i(Constants.TAG, "Alarm for updating location to server stopped");
//    }

    public void stopLocationMonitorService(Activity activity) {
        Intent intent = new Intent(activity, LocationMonitoringService.class);
        intent.setAction(Constants.ACTION_STOP_FOREGROUND_LOCATION_SERVICE);
        activity.startService(intent);
        Utility.toastMsg(context.getApplicationContext(), "Background location updates to server stopped");
//        Intent intent = new Intent(context.getApplicationContext(), LocationMonitoringService.class);
//        PendingIntent pendingIntent = PendingIntent.getService(context.getApplicationContext(), LocationMonitoringService.REQUEST_CODE, intent, FLAG_ONE_SHOT);
//        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        alarmManager.cancel(pendingIntent);
//        Utility.toastMsg(context.getApplicationContext(), "Alarm for location monitor service stopped");
//        Log.i(Constants.TAG, "Alarm for location monitor service stopped");
    }

    public void stopLocationMonitoringService() {
        if (isServiceRunning(LocationMonitoringService.class)) {
            ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            Intent intent = new Intent(context.getApplicationContext(), LocationMonitoringService.class);
            context.stopService(intent);
            Utility.toastMsg(context.getApplicationContext(), "Background location updates to server stopped");
            Log.i(Constants.TAG, "Background location monitoring service stopped");
        }
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
