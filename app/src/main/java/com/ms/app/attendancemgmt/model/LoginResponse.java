package com.ms.app.attendancemgmt.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Model class for pin login response:
 * Sample response : {"status":"Success","empId":"ET0001A01","message":"Santosh Thorwat"}
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoginResponse {
    private String status;
    private String empId;
    private String message;

    public LoginResponse() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("LoginResponse{");
        sb.append("status='").append(status).append('\'');
        sb.append(", empId='").append(empId).append('\'');
        sb.append(", message='").append(message).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
