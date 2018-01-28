package com.ms.app.attendancemgmt.model;

import android.arch.persistence.room.ColumnInfo;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.TypeConverters;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ms.app.attendancemgmt.location.offline.RoomDateConverters;
import com.ms.app.attendancemgmt.util.Constants;

import java.util.Date;

/**
 * Model class for marking the attendance of employee
 */
public class Attendance {

    private String address;
    private String id;

    @ColumnInfo(name = "dev_id")
    private String devId;

    private double lat;
    private double lon;

    @TypeConverters(RoomDateConverters.class)
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DATE_FORMAT, timezone = "UTC")
    private Date time;

    public Attendance() {
    }

    @Ignore
    public Attendance(String id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getDevId() {
        return devId;
    }

    public void setDevId(String devId) {
        this.devId = devId;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Attendance{");
        sb.append("address='").append(address).append('\'');
        sb.append(", id='").append(id).append('\'');
        sb.append(", devId='").append(devId).append('\'');
        sb.append(", lat=").append(lat);
        sb.append(", lon=").append(lon);
        sb.append(", time=").append(time);
        sb.append('}');
        return sb.toString();
    }
}
