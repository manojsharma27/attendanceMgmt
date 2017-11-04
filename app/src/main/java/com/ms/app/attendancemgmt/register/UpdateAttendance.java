package com.ms.app.attendancemgmt.register;

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.ArrayUtils;

import java.io.IOException;
import java.net.HttpURLConnection;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpdateAttendance {
    private Attendance attendance;
    private RegisterAttendanceActivity regAttendActivity;

    public UpdateAttendance(RegisterAttendanceActivity regAttendActivity, Attendance attendance) {
        this.regAttendActivity = regAttendActivity;
        this.attendance = attendance;
    }

    public void register() {
        AttendanceRegisterTask task = new AttendanceRegisterTask();
        task.execute(attendance);
    }

    private class AttendanceRegisterTask extends AsyncTask<Attendance, Integer, Response> {

        @Override
        protected Response doInBackground(Attendance... attendances) {
            if (!ArrayUtils.isEmpty(attendances)) {
                ObjectMapper om = new ObjectMapper();
                try {
                    String json = om.writeValueAsString(attendances[0]);
                    String finalUrl = Utility.getServiceUrl() + Constants.REGISTER_ATTENDANCE_ENDPOINT;
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

//                    TODO: uncomment following for actual service call
                    return client.newCall(request).execute();
//                    Thread.sleep(1000);
//                    TODO: remove following after integration with actual service
//                    return new Response.Builder()
//                            .message("Registration complete for " + attendances[0].toString())
//                            .request(request)
//                            .protocol(Protocol.HTTP_1_0)
//                            .code(HttpURLConnection.HTTP_OK)
//                            .build();
                } catch (Exception e) {
                    Log.e(Constants.LOG_TAG, "Exception while registering attendance. ", e);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            regAttendActivity.showProgressBar(true);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            regAttendActivity.showProgressBar(false);
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            regAttendActivity.showProgressBar(false);
            regAttendActivity.handleRegisterAttendanceResponse(response, attendance);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            regAttendActivity.updateProgressBar(values[0]);
        }
    }
}
