package com.ms.app.attendancemgmt.location.offline;


import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Embedded;
import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.arch.persistence.room.TypeConverters;
import android.support.annotation.NonNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ms.app.attendancemgmt.model.Attendance;

@Entity(tableName = "model_entry")
public class ModelEntry {

    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    @NonNull
    @JsonIgnore
    private String entryId;

    @ColumnInfo(name = "upload_timestamp", index = true)
    private long uploadTimestamp;

    @TypeConverters(RoomUploadStatusConverters.class)
    @ColumnInfo(name = "upload_status")
    private UploadStatus uploadStatus;

    @Embedded
    private Attendance attendance;

    public ModelEntry() {
    }

    @Ignore
    public ModelEntry(String entryId, long uploadTimestamp, UploadStatus uploadStatus, Attendance attendance) {
        this.entryId = entryId;
        this.uploadTimestamp = uploadTimestamp;
        this.uploadStatus = uploadStatus;
        this.attendance = attendance;
    }

    public String getEntryId() {
        return entryId;
    }

    public void setEntryId(String entryId) {
        this.entryId = entryId;
    }

    public long getUploadTimestamp() {
        return uploadTimestamp;
    }

    public void setUploadTimestamp(long uploadTimestamp) {
        this.uploadTimestamp = uploadTimestamp;
    }

    public UploadStatus getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(UploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public Attendance getAttendance() {
        return attendance;
    }

    public void setAttendance(Attendance attendance) {
        this.attendance = attendance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ModelEntry that = (ModelEntry) o;

        if (uploadTimestamp != that.uploadTimestamp) return false;
        if (entryId != null ? !entryId.equals(that.entryId) : that.entryId != null) return false;
        return uploadStatus == that.uploadStatus;
    }

    @Override
    public int hashCode() {
        int result = entryId != null ? entryId.hashCode() : 0;
        result = 31 * result + (int) (uploadTimestamp ^ (uploadTimestamp >>> 32));
        result = 31 * result + (uploadStatus != null ? uploadStatus.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("ModelEntry{");
        sb.append("entryId=").append(entryId);
        sb.append(", uploadTimestamp=").append(uploadTimestamp);
        sb.append(", uploadStatus=").append(uploadStatus);
        sb.append(", attendance=").append(attendance);
        sb.append('}');
        return sb.toString();
    }
}
