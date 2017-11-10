package com.ms.app.attendancemgmt.anotherservice;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.ms.app.attendancemgmt.activitiy.LocationRegisterer;
import com.ms.app.attendancemgmt.util.Constants;

public class AnotherServiceHandlerManager {
    private Context context;
    private Looper looper;
    private static final int LOGIN_FAILED = -1;
    private static final int LOOPER_STARTED = 0;
    private static final int LOGIN_FINISHED = 1;

    private String TAG = "LocationUpdateServiceTag";

    public AnotherServiceHandlerManager(Context context, Looper looper) {
        this.context = context;
        this.looper = looper;
    }

    // Here's the interface that helps LoginService
    // behave as a callback object
    public interface MyCallback {
        void processCallback();
    }

    public void start(final CustomMessage customMessage) {

        final Handler handler = new Handler(looper) {
            @Override
            public void handleMessage(Message msg) {
                handleMyTask((CustomMessage) msg.obj);
            }
        };

        handler.post(new Runnable() {
            @Override
            public void run() {
                Message msg = new Message();
                msg.obj = customMessage;
                handler.sendMessage(msg);
            }
        });

    }

    private void handleMyTask(CustomMessage customMessage) {
        Intent intent = customMessage.getIntent();
        MyCallback callback = customMessage.getMyCallback();
        String devId = intent.getStringExtra(Constants.DEVICE_ID);
        String empId = intent.getStringExtra(Constants.EMP_ID);

        Log.i("MyTestService", "Registering for device:" + devId + ", emp:" + empId);

        LocationRegisterer locationRegisterer = new LocationRegisterer(context);
        locationRegisterer.doRegistration(devId, empId);
        locationRegisterer.setServiceCallback(callback);

        // Do the task here
//        Log.i("MyTestService", intent.getStringExtra("foo"));
        Log.i("MyTestService", "" + intent.getLongExtra("repeatation", -99L));
        Log.i("MyTestService", "Service running");
    }

    public static class CustomMessage {
        private Intent intent;
        private MyCallback myCallback;

        public CustomMessage(Intent intent, MyCallback myCallback) {
            this.intent = intent;
            this.myCallback = myCallback;
        }

        public Intent getIntent() {
            return intent;
        }

        public void setIntent(Intent intent) {
            this.intent = intent;
        }

        public MyCallback getMyCallback() {
            return myCallback;
        }

        public void setMyCallback(MyCallback myCallback) {
            this.myCallback = myCallback;
        }
    }
}
