package com.ms.app.attendancemgmt.model;


import com.fasterxml.jackson.annotation.JsonFormat;
import com.ms.app.attendancemgmt.util.Constants;

import java.util.Date;

public class LocationModel {
    private double latitude;
    private double longitude;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = Constants.DATE_FORMAT, timezone = "UTC")
    private Date logTime;

    public LocationModel(double latitude, double longitude) {
        this(latitude, longitude, new Date());
    }

    public LocationModel(double latitude, double longitude, Date logTime) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.logTime = logTime;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public Date getLogTime() {
        return logTime;
    }

    public void setLogTime(Date logTime) {
        this.logTime = logTime;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LocationModel{");
        sb.append("longitude=").append(longitude);
        sb.append(", latitude=").append(latitude);
        sb.append(", logTime=").append(logTime);
        sb.append('}');
        return sb.toString();
    }
}
