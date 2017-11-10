package com.ms.app.attendancemgmt.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.ms.app.attendancemgmt.util.Utility;

import java.sql.Timestamp;

public class RepeatLocUpdateService extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.v(getClass().getName(), "Timed alarm onReceive() started at time: " + new Timestamp(System.currentTimeMillis()).toString());
        Utility.toastMsg(context, "on receive");
        sendLocationUpdate(context);
    }

    private void sendLocationUpdate(Context context) {
        Intent updateIntent = new Intent("NavSysLocationUpdates");
        updateIntent.putExtra("latitude", 10.0d);
        updateIntent.putExtra("longitude", 10.0d);
        Log.v("context", context.getClass().toString());
        context.sendBroadcast(updateIntent);
        Log.v(getClass().getName(), "broadcast msg sent...");
    }
}
