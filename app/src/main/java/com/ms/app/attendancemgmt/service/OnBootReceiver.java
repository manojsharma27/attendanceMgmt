package com.ms.app.attendancemgmt.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class OnBootReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        //TODO : modify this if need to start service on boot
//        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
//            context.startService(new Intent(context, ServiceMgr.class));
//            Log.v(getClass().getName(), "Service loaded while device boot.");
//        }
    }
}
