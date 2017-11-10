package com.ms.app.attendancemgmt.anotherservice;


import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.Log;

public class MyAnotherService extends Service implements AnotherServiceHandlerManager.MyCallback {
    private HandlerThread thread;

    private String TAG = "LocationUpdateServiceTag";

    @Override
    public void onCreate() {
        super.onCreate();

        thread = new HandlerThread(TAG, android.os.Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AnotherServiceHandlerManager.CustomMessage customMessage = new AnotherServiceHandlerManager.CustomMessage(intent, this);
        AnotherServiceHandlerManager serviceHandlerManager = new AnotherServiceHandlerManager(getApplicationContext(), thread.getLooper());
        serviceHandlerManager.start(customMessage);
        Log.i(TAG, "started handler");
        return Service.START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Not bound.
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        thread.quit();
    }

    @Override
    public void processCallback() {
        super.stopSelf();
        Log.i(TAG, "inside callback");
        onDestroy();
    }
}
