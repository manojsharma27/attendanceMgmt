package com.ms.app.attendancemgmt.location;

import android.content.Context;
import android.util.Log;

import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.service.FileHandler;
import com.ms.app.attendancemgmt.util.Constants;

import java.util.List;

import okhttp3.Response;

/**
 * Checks if there are any stored locations when internet is connected
 * If locations are present, uploads them to server one at a time
 */
public class StoredLocationUploader implements ServerUpdateResponseHandler {

    private Context context;
    private volatile int entry_count;
    private volatile int uploadCounter;
    private long uploadStartTime;

    public StoredLocationUploader(Context context) {
        this.context = context;
    }

    public void checkLocationsAndUpload() {
        if (entriesPresent()) {
            List<Attendance> attendanceList = FileHandler.readAttendanceFromFile(context);
            uploadStartTime = System.currentTimeMillis();
            entry_count = attendanceList.size();
            uploadCounter = 0;
            Log.i(Constants.TAG, String.format("Syncing %s entries...", entry_count));
            for (Attendance attendance : attendanceList) {
                UpdateAttendance updateAttendance = new UpdateAttendance(StoredLocationUploader.this, attendance);
                updateAttendance.setContext(context);
                updateAttendance.register();
            }
        }
    }

    @Override
    public void handleRegisterAttendanceResponse(Response response, Attendance attendance) {
        boolean isSuccess = (null != response && response.message().equals(Constants.MSG_OK));
        if (isSuccess) {
            uploadCounter++;
            entry_count--;
            Log.i(Constants.TAG, "Entries uploaded: " + uploadCounter);
            checkEntriesUploaded();
        }
    }

    public boolean entriesPresent() {
        return FileHandler.locationFileExists(context);
    }

    private void checkEntriesUploaded() {
        if (entry_count <= 0) {
            if (FileHandler.locationFileExists(context)) {
                if (!FileHandler.checkFileModifiedAfter(context, uploadStartTime)) {
                    FileHandler.cleanUp(context);
                }
            }
        }
    }
}
