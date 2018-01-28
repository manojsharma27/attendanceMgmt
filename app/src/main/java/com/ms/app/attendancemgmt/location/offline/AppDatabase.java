package com.ms.app.attendancemgmt.location.offline;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.arch.persistence.room.migration.Migration;
import android.content.Context;
import android.util.Log;

import com.ms.app.attendancemgmt.util.Constants;

@Database(entities = ModelEntry.class, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public static final String DB_NAME = "modelAttendanceDB.db";
    private static AppDatabase instance;

    public abstract ModelEntryDao modelEntryDao();

    public static synchronized AppDatabase getDatabase(Context context) {
        if (null == instance) {
            instance = Room.databaseBuilder(context, AppDatabase.class, DB_NAME)
                    .addMigrations(MIGRATION_1_2)
                    .build();
        }
        Log.i(Constants.TAG, "DB path : " + context.getDatabasePath(DB_NAME).getAbsolutePath());
        return instance;
    }

    public static ModelEntryDao modelEntryDao(Context context) {
        return AppDatabase.getDatabase(context).modelEntryDao();
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };
}
