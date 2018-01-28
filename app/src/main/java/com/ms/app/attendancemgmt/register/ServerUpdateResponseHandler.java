package com.ms.app.attendancemgmt.register;


import com.ms.app.attendancemgmt.location.offline.ModelEntry;

import okhttp3.Response;

public interface ServerUpdateResponseHandler {

    void handleRegisterAttendanceResponse(Response response, ModelEntry modelEntry);

}
