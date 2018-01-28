package com.ms.app.attendancemgmt.location.offline;

import android.arch.persistence.room.TypeConverter;

import java.util.Date;

public class RoomDateConverters {

    @TypeConverter
    public static Long toLong(Date date) {
        return null == date ? null : date.getTime();
    }

    @TypeConverter
    public static Date toDate(Long timeStamp) {
        return null == timeStamp ? null : new Date(timeStamp);
    }
}
