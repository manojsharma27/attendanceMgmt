package com.ms.app.attendancemgmt.register;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.ms.app.attendancemgmt.activitiy.RegisterAttendanceActivity;
import com.ms.app.attendancemgmt.location.AddressLocator;
import com.ms.app.attendancemgmt.location.offline.ModelEntry;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UpdateAttendance {
    private ModelEntry modelEntry;
    private ServerUpdateResponseHandler responseHandler;
    private Context context;

    public UpdateAttendance(ServerUpdateResponseHandler responseHandler, ModelEntry modelEntry) {
        this.responseHandler = responseHandler;
        this.modelEntry = modelEntry;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void register() {
        AttendanceRegisterTask task = new AttendanceRegisterTask();
        task.execute(modelEntry);
    }

    private class AttendanceRegisterTask extends AsyncTask<ModelEntry, Integer, RegisterResponse> {

        @Override
        protected RegisterResponse doInBackground(ModelEntry... entries) {
            if (!ArrayUtils.isEmpty(entries) && null != entries[0]) {
                try {
                    ModelEntry modelEntry = entries[0];
                    Attendance attendance = modelEntry.getAttendance();
                    attendance = populateDataIfNeeded(attendance);
                    String json = Utility.getObjectMapper().writeValueAsString(attendance);
                    Log.i(Constants.TAG, "Registering " + json);
                    String finalUrl = Utility.getServiceUrl(context) + Constants.REGISTER_ATTENDANCE_ENDPOINT;
                    OkHttpClient client = new OkHttpClient();
                    RequestBody body = RequestBody.create(MediaType.parse("application/json"), json);
                    Request request = new Request.Builder()
                            .url(finalUrl)
                            .addHeader("Content-Type", "application/json")
                            .post(body)
                            .build();

                    Response response = client.newCall(request).execute();
                    modelEntry.setAttendance(attendance);
                    return new RegisterResponse(response, modelEntry);
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
        protected void onPostExecute(RegisterResponse regResp) {
            super.onPostExecute(regResp);
            if (responseHandler instanceof RegisterAttendanceActivity) {
                ((RegisterAttendanceActivity) responseHandler).showProgressBar(false);
            }
            if (null != regResp) {
                responseHandler.handleRegisterAttendanceResponse(regResp.response, regResp.modelEntry);
            } else {
                responseHandler.handleRegisterAttendanceResponse(null, modelEntry);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
            if (responseHandler instanceof RegisterAttendanceActivity) {
                ((RegisterAttendanceActivity) responseHandler).updateProgressBar(values[0]);
            }
        }
    }

    private class RegisterResponse {
        private Response response;
        private ModelEntry modelEntry;

        private RegisterResponse(Response response, ModelEntry modelEntry) {
            this.response = response;
            this.modelEntry = modelEntry;
        }
    }

    private Attendance populateDataIfNeeded(Attendance attendance) {
        if (StringUtils.isEmpty(attendance.getAddress()) && Utility.checkInternetConnected(context)) {
            Log.i(Constants.TAG, "Address was blank... ");
            String address = AddressLocator.populateAddress(context, attendance.getLat(), attendance.getLon());
            attendance.setAddress(address);
            Log.i(Constants.TAG, "Address set to " + address);
        }

        if (StringUtils.isEmpty(attendance.getId())) {
            String empid = Utility.readPref(context.getApplicationContext(), Constants.EMP_ID);
            attendance.setId(empid);
            Log.i(Constants.TAG, "empId set to " + empid);
        }

        if (StringUtils.isEmpty(attendance.getDevId())) {
            String devId = Utility.readPref(context.getApplicationContext(), Constants.DEVICE_ID);
            attendance.setDevId(devId);
            Log.i(Constants.TAG, "devId set to " + devId);
        }

        return attendance;
    }
}
