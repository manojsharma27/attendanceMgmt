package com.ms.app.attendancemgmt.location.offline;


import android.content.Context;
import android.util.Log;

import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import java.util.Date;
import java.util.List;

public class OfflineLocationHandler {

    private static void writeAttendanceToDB(Context context, Attendance attendance) {
        AppDatabase database = AppDatabase.getDatabase(context);
        ModelEntryDao dao = database.modelEntryDao();
        ModelEntry entry = prepareModelEntry(attendance);
        dao.insert(entry);

        Log.i(Constants.TAG, "OfflineDB: Inserted attendance - " + attendance.toString());
    }

    public static void writeEntryToDB(Context context, ModelEntry modelEntry) {
        AppDatabase database = AppDatabase.getDatabase(context);
        ModelEntryDao dao = database.modelEntryDao();
        modelEntry.setUploadTimestamp(new Date().getTime());
        dao.insert(modelEntry);

        Log.i(Constants.TAG, "OfflineDB: Inserted model entry - " + modelEntry.toString());
    }

    public static ModelEntry prepareModelEntry(Attendance attendance) {
        long currTime = new Date().getTime();
        String entryId = Utility.randomUUID();
        return new ModelEntry(entryId, currTime, UploadStatus.PENDING, attendance);
    }

    public static List<ModelEntry> pendingEntries(Context context) {
        AppDatabase database = AppDatabase.getDatabase(context);
        List<ModelEntry> modelEntries = database.modelEntryDao().entriesWithStatus(UploadStatus.PENDING.getCode());
        return modelEntries;
    }
}
