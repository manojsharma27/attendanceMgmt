package com.ms.app.attendancemgmt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model class for pin login response:
 * Sample response :
 * {
 * "status": "Success",
 * "empid": "ET0001A01",
 * "empname": "Santosh Thorwat",
 * "interval": 60,
 * "message": "Valid Pin"
 * }
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String empid;
    private String empname;
    private long interval; // in seconds
    private String message;
    private String status;

    public LoginResponse() {
    }

    public String getEmpid() {
        return empid;
    }

    public void setEmpid(String empid) {
        this.empid = empid;
    }

    public String getEmpname() {
        return empname;
    }

    public void setEmpname(String empname) {
        this.empname = empname;
    }

    public long getInterval() {
        return interval;
    }

    public void setInterval(long interval) {
        this.interval = interval;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LoginResponse{");
        sb.append("empid='").append(empid).append('\'');
        sb.append(", empname='").append(empname).append('\'');
        sb.append(", interval=").append(interval);
        sb.append(", message='").append(message).append('\'');
        sb.append(", status='").append(status).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
