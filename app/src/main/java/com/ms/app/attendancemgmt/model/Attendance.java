package com.ms.app.attendancemgmt.model;

import android.location.Geocoder;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.ms.app.attendancemgmt.util.Utility;

import java.util.Date;

/**
 * Model class for marking the attendance of employee
 */
public class Attendance {

    private String empId;
    private String deviceId;
    private double latitude;
    private double longitude;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Utility.DATE_FORMAT, timezone = "UTC")
    private Date markTime;

    public Attendance(String empId) {
        this.empId = empId;
    }

    public String getEmpId() {
        return empId;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Date getMarkTime() {
        return markTime;
    }

    public void setMarkTime(Date markTime) {
        this.markTime = markTime;
    }
}
