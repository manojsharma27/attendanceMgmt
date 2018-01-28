package com.ms.app.attendancemgmt;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import com.ms.app.attendancemgmt.location.offline.AppDatabase;
import com.ms.app.attendancemgmt.location.offline.ModelEntry;
import com.ms.app.attendancemgmt.location.offline.ModelEntryDao;
import com.ms.app.attendancemgmt.location.offline.UploadStatus;
import com.ms.app.attendancemgmt.model.Attendance;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Date;
import java.util.UUID;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class ModelEntryCrudIT {

    private static final String TEST_EMP_ID = "testEmpId";

    private ModelEntry modelEntry;

    @Test
    public void testCRUD() {
        Context context = InstrumentationRegistry.getTargetContext();

        Attendance attendance = getDummyAttendance();
        String entryId = UUID.randomUUID().toString();
        modelEntry = new ModelEntry(entryId, new Date().getTime(), UploadStatus.PENDING, attendance);

        AppDatabase database = AppDatabase.getDatabase(context);
        ModelEntryDao dao = database.modelEntryDao();

        dao.insert(modelEntry);

        System.out.println("Record created...");

        ModelEntry fetchedEntry = dao.getById(entryId);
        assertNotNull(fetchedEntry);
        Assert.assertEquals(modelEntry, fetchedEntry);
        Assert.assertEquals(UploadStatus.PENDING, modelEntry.getUploadStatus());

        System.out.println("Record fetched...");

        modelEntry.setUploadStatus(UploadStatus.UPLOADED);
        dao.update(modelEntry);

        fetchedEntry = dao.getById(entryId);
        assertNotNull(fetchedEntry);
        Assert.assertEquals(UploadStatus.UPLOADED, modelEntry.getUploadStatus());

        System.out.println("Record uploaded...");

        dao.delete(modelEntry);
        fetchedEntry = dao.getById(entryId);
        assertNull(fetchedEntry);

        System.out.println("Record deleted...");

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
