package com.ms.app.attendancemgmt.location.offline;

import android.arch.persistence.room.TypeConverter;

public class RoomUploadStatusConverters {

    @TypeConverter
    public static int toCode(UploadStatus uploadStatus) {
        return uploadStatus.getCode();
    }

    @TypeConverter
    public static UploadStatus toStatus(int code) {
        for (UploadStatus status : UploadStatus.values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }
}
