package com.ms.app.attendancemgmt.register;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import com.ms.app.attendancemgmt.model.Attendance;

import org.apache.commons.lang3.ArrayUtils;

public class UpdateAttendance {
    private static final String HOST_NAME = "http://www.example.com";
    private Attendance attendance;
    private ProgressBar pbRegAttend;

    public UpdateAttendance(Attendance attendance, ProgressBar pbRegAttend) {
        this.attendance = attendance;
        this.pbRegAttend = pbRegAttend;
    }

    public void register() {

    }

    private class AttendanceRegisterTask extends AsyncTask<Attendance, Integer, Void> {

        @Override
        protected Void doInBackground(Attendance... attendances) {
            if (!ArrayUtils.isEmpty(attendances)) {

            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }
    }
}
