package com.ms.app.attendancemgmt.register;


import com.ms.app.attendancemgmt.model.Attendance;

import okhttp3.Response;

public interface ServerUpdateResponseHandler {
    void handleRegisterAttendanceResponse(Response response, Attendance attendance);
}
