package com.ms.app.attendancemgmt.location.storedupload;

import android.content.Context;
import android.util.Log;

import com.ms.app.attendancemgmt.location.offline.AppDatabase;
import com.ms.app.attendancemgmt.location.offline.ModelEntry;
import com.ms.app.attendancemgmt.location.offline.UploadStatus;
import com.ms.app.attendancemgmt.location.offline.db.processor.DbReadTask;
import com.ms.app.attendancemgmt.location.offline.db.processor.DbWriteTask;
import com.ms.app.attendancemgmt.location.offline.db.processor.OfflineDBLocationResponseHandler;
import com.ms.app.attendancemgmt.location.offline.db.processor.OfflineDbAsyncTasksProcessor;
import com.ms.app.attendancemgmt.register.ServerUpdateResponseHandler;
import com.ms.app.attendancemgmt.register.UpdateAttendance;
import com.ms.app.attendancemgmt.util.Constants;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

import okhttp3.Response;

/**
 * Checks if there are any stored locations when internet is connected
 * If locations are present, uploads them to server one at a time
 */
public class DbStoredLocationUploader extends StoredLocationUploader
        implements ServerUpdateResponseHandler, OfflineDBLocationResponseHandler {


    public DbStoredLocationUploader(Context context) {
        super(context);
    }

    public void checkLocationsAndUpload() {
        requestStoredEntries();
    }

    private void delayUpload() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void handleRegisterAttendanceResponse(Response response, final ModelEntry modelEntry) {
        boolean isSuccess = (null != response && response.message().equals(Constants.MSG_OK));
        if (isSuccess) {
            final ModelEntry toBeDeletedEntry = modelEntry;
            DbWriteTask deleteEntryTask = new DbWriteTask() {
                @Override
                public void process() {
                    AppDatabase database = AppDatabase.getDatabase(context);
                    database.modelEntryDao().delete(toBeDeletedEntry);
                    Log.i(Constants.TAG, "Model entry deleted : " + toBeDeletedEntry);
                }
            };

            OfflineDbAsyncTasksProcessor writer = new OfflineDbAsyncTasksProcessor(deleteEntryTask);
            writer.write();
        }
    }

    public void requestStoredEntries() {
        DbReadTask readEntriesTask = new DbReadTask() {
            @Override
            public List<ModelEntry> process() {
                AppDatabase database = AppDatabase.getDatabase(context);
                List<ModelEntry> modelEntries = database.modelEntryDao().entriesWithStatus(UploadStatus.PENDING.getCode());
                return modelEntries;
            }
        };

        OfflineDbAsyncTasksProcessor reader = new OfflineDbAsyncTasksProcessor(readEntriesTask, DbStoredLocationUploader.this);
        reader.read();
    }

    @Override
    public void handleDbLocationResponse(List<ModelEntry> modelEntries) {
        if (CollectionUtils.isEmpty(modelEntries)) {
            return;
        }

        Log.i(Constants.TAG, String.format("Syncing %s entries...", modelEntries.size()));
        for (ModelEntry entry : modelEntries) {
            UpdateAttendance updateAttendance = new UpdateAttendance(DbStoredLocationUploader.this, entry);
            updateAttendance.setContext(context);
            updateAttendance.register();
            delayUpload();
        }

    }
}
