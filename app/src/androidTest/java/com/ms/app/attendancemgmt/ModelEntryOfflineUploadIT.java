package com.ms.app.attendancemgmt;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.ms.app.attendancemgmt.location.offline.AppDatabase;
import com.ms.app.attendancemgmt.location.offline.ModelEntry;
import com.ms.app.attendancemgmt.location.offline.ModelEntryDao;
import com.ms.app.attendancemgmt.location.offline.OfflineLocationHandler;
import com.ms.app.attendancemgmt.location.offline.UploadStatus;
import com.ms.app.attendancemgmt.model.Attendance;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ModelEntryOfflineUploadIT {

    private static final String TEST_EMP_ID = "testEmpId";

    @Test
    public void testFetchUploadedEntries() {
        ModelEntry pme1 = OfflineLocationHandler.prepareModelEntry(getDummyAttendance());
        ModelEntry pme2 = OfflineLocationHandler.prepareModelEntry(getDummyAttendance());
        ModelEntry ume1 = OfflineLocationHandler.prepareModelEntry(getDummyAttendance());
        ume1.setUploadStatus(UploadStatus.UPLOADED);
        ModelEntry ume2 = OfflineLocationHandler.prepareModelEntry(getDummyAttendance());
        ume2.setUploadStatus(UploadStatus.UPLOADED);

        Context context = InstrumentationRegistry.getTargetContext();

        AppDatabase database = AppDatabase.getDatabase(context);
        ModelEntryDao dao = database.modelEntryDao();
        dao.insert(pme1);
        dao.insert(pme2);
        dao.insert(ume1);
        dao.insert(ume2);

        List<ModelEntry> uploadedEntries = dao.entriesWithStatus(UploadStatus.UPLOADED.getCode());
        assertNotNull(uploadedEntries);
        List<String> entryIds = new ArrayList<>();
        for (ModelEntry entry : uploadedEntries) {
            entryIds.add(entry.getEntryId());
        }

        assertTrue(entryIds.contains(ume1.getEntryId()));
        assertTrue(entryIds.contains(ume2.getEntryId()));

        dao.delete(pme1);
        dao.delete(pme2);
        dao.delete(ume1);
        dao.delete(ume2);
        database.close();
    }

    @NonNull
    private Attendance getDummyAttendance() {
        Attendance attendance = new Attendance(TEST_EMP_ID);
        attendance.setLat(-0.0);
        attendance.setLon(-0.0);
        attendance.setDevId("dummyId");
        attendance.setTime(new Date());
        return attendance;
    }

}
