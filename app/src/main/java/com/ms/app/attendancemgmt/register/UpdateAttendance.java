package com.ms.app.attendancemgmt.register;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.ArrayUtils;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpdateAttendance {
    private Attendance attendance;
    private ServerUpdateResponseHandler responseHandler;
    private Context context;

    public UpdateAttendance(ServerUpdateResponseHandler responseHandler, Attendance attendance) {
        this.responseHandler = responseHandler;
        this.attendance = attendance;
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
                try {
                    String json = Utility.getObjectMapper().writeValueAsString(attendances[0]);
                    Log.i(Constants.TAG, "Registering " + json);
                    String finalUrl = Utility.getServiceUrl(context) + Constants.REGISTER_ATTENDANCE_ENDPOINT;
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    return client.newCall(request).execute();
//                    Thread.sleep(1000);
//                    return new Response.Builder()
//                            .message(Constants.MSG_OK)
//                            .request(request)
//                            .protocol(Protocol.HTTP_1_0)
//                            .code(HttpURLConnection.HTTP_OK)
//                            .build();
                } catch (Exception e) {
                    Log.e(Constants.TAG, "Exception while registering attendance. ", e);
                }
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (responseHandler instanceof RegisterAttendanceActivity) {
                ((RegisterAttendanceActivity) responseHandler).showProgressBar(true);
            }
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            if (responseHandler instanceof RegisterAttendanceActivity) {
                ((RegisterAttendanceActivity) responseHandler).showProgressBar(false);
            }
        }

        @Override
        protected void onPostExecute(Response response) {
            super.onPostExecute(response);
            if (responseHandler instanceof RegisterAttendanceActivity) {
                ((RegisterAttendanceActivity) responseHandler).showProgressBar(false);
            }
            responseHandler.handleRegisterAttendanceResponse(response, attendance);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (responseHandler instanceof RegisterAttendanceActivity) {
                ((RegisterAttendanceActivity) responseHandler).updateProgressBar(values[0]);
            }
        }
    }
}
