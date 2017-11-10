package com.ms.app.attendancemgmt.register;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.service.UpdateLocationToServerBroadcastReceiver;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.ArrayUtils;

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
    private UpdateLocationToServerBroadcastReceiver updateLocationToServerBroadcastReceiver;
    private boolean uiBasedRequest;
    private Context context;

    public UpdateAttendance(RegisterAttendanceActivity regAttendActivity, Attendance attendance) {
        this.regAttendActivity = regAttendActivity;
        this.attendance = attendance;
        uiBasedRequest = true;
    }

    public UpdateAttendance(UpdateLocationToServerBroadcastReceiver updateLocationToServerBroadcastReceiver, Attendance attendance) {
        this.updateLocationToServerBroadcastReceiver = updateLocationToServerBroadcastReceiver;
        this.attendance = attendance;
        uiBasedRequest = false;
    }

    public void setContext(Context context) {
        this.context = context;
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
                    String finalUrl = Utility.getServiceUrl(context) + Constants.REGISTER_ATTENDANCE_ENDPOINT;
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

//                    TODO: uncomment following for actual service call
//                    return client.newCall(request).execute();
                    Thread.sleep(1000);
//                    TODO: remove following after integration with actual service
                    return new Response.Builder()
                            .message(Constants.MSG_OK)
                            .request(request)
                            .protocol(Protocol.HTTP_1_0)
                            .code(HttpURLConnection.HTTP_OK)
                            .build();
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception while registering attendance. ", e);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (uiBasedRequest) {
                regAttendActivity.showProgressBar(true);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (uiBasedRequest) {
                regAttendActivity.showProgressBar(false);
            }
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            if (uiBasedRequest) {
                regAttendActivity.showProgressBar(false);
                regAttendActivity.handleRegisterAttendanceResponse(response, attendance);
            } else {
                updateLocationToServerBroadcastReceiver.handleRegisterAttendanceResponse(response, attendance);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (uiBasedRequest) {
                regAttendActivity.updateProgressBar(values[0]);
            }
        }
    }
}
