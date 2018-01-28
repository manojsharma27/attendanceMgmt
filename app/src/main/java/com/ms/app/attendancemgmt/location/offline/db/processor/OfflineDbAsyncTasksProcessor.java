package com.ms.app.attendancemgmt.location.offline.db.processor;

import android.os.AsyncTask;

import com.ms.app.attendancemgmt.location.offline.ModelEntry;

import java.util.List;

/**
 * Uses Async task to read entries from DB
 */

public class OfflineDbAsyncTasksProcessor {

    private DbReadTask dbReadTask;
    private DbWriteTask dbWriteTask;
    private OfflineDBLocationResponseHandler responseHandler;

    public OfflineDbAsyncTasksProcessor(DbWriteTask dbWriteTask) {
        this.dbWriteTask = dbWriteTask;
    }

    public OfflineDbAsyncTasksProcessor(DbReadTask dbReadTask, OfflineDBLocationResponseHandler responseHandler) {
        this.dbReadTask = dbReadTask;
        this.responseHandler = responseHandler;
    }

    public void read() {
        new DbModelEntriesReadAsyncTask().execute(dbReadTask);
    }

    public void write() {
        new DbModelEntriesWriteAsyncTask().execute(dbWriteTask);
    }

    private class DbModelEntriesReadAsyncTask extends AsyncTask<DbReadTask, Void, List<ModelEntry>> {

        @Override
        protected List<ModelEntry> doInBackground(DbReadTask... dbReadTasks) {
            return dbReadTasks[0].process();
        }

        @Override
        protected void onPostExecute(List<ModelEntry> entries) {
            super.onPostExecute(entries);
            responseHandler.handleDbLocationResponse(entries);
        }
    }

    private class DbModelEntriesWriteAsyncTask extends AsyncTask<DbWriteTask, Void, Void> {

        @Override
        protected Void doInBackground(DbWriteTask... dbWriteTasks) {
            dbWriteTasks[0].process();
            return null;
        }
    }
}
