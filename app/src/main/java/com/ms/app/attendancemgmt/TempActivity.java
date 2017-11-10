package com.ms.app.attendancemgmt;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.ms.app.attendancemgmt.anotherservice.MyAlarmReceiver;
import com.ms.app.attendancemgmt.service.ServiceMgr;
import com.ms.app.attendancemgmt.util.Utility;

public class TempActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_temp);

        Button btnStartServ = findViewById(R.id.btnStartServ);
        btnStartServ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                startService(new Intent(getApplicationContext(), ServiceMgr.class));
                scheduleAlarm();
                Utility.toastMsg(getApplicationContext(), "Service started.");
            }
        });

        Button btnStopServ = findViewById(R.id.btnStopServ);
        btnStopServ.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                stopService(new Intent(getApplicationContext(), ServiceMgr.class));
                cancelAlarm();
                Utility.toastMsg(getApplicationContext(), "Service stopped.");
            }
        });

    }

    // Setup a recurring alarm every half hour
    public void scheduleAlarm() {
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).edit().putLong("REPEAT_COUNT", 0).apply();
        // Construct an intent that will execute the AlarmReceiver
        Intent intent = new Intent(getApplicationContext(), MyAlarmReceiver.class);
        // Create a PendingIntent to be triggered when the alarm goes off
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        // Setup periodic alarm every every half hour from this point onwards
        long firstMillis = System.currentTimeMillis(); // alarm is set right away
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        // First parameter is the type: ELAPSED_REALTIME, ELAPSED_REALTIME_WAKEUP, RTC_WAKEUP
        // Interval can be INTERVAL_FIFTEEN_MINUTES, INTERVAL_HALF_HOUR, INTERVAL_HOUR, INTERVAL_DAY
        long intervalMillis = 1000; // 2sec
//        alarm.setRepeating(AlarmManager.RTC_WAKEUP, firstMillis, intervalMillis, pIntent);


        // experiment 1 : using exact
        alarm.setExact(AlarmManager.RTC_WAKEUP, firstMillis, pIntent);
        Log.i("MyTestService", "Service started");

    }

    public void cancelAlarm() {
        Intent intent = new Intent(getApplicationContext(), MyAlarmReceiver.class);
        final PendingIntent pIntent = PendingIntent.getBroadcast(this, MyAlarmReceiver.REQUEST_CODE,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarm = (AlarmManager) this.getSystemService(Context.ALARM_SERVICE);
        alarm.cancel(pIntent);
        Log.i("MyTestService", "Service stopped");
    }
}
