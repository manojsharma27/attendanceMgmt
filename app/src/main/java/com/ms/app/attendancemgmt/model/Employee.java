package com.ms.app.attendancemgmt.model;

/**
 * Model class to keep imployee information
 */
public class Employee {
    private String empId;
    private String empPin;
    private String name;
    private String email;
    private String phone;

    public Employee(String empId, String empPin, String name) {
        this.empId = empId;
        this.empPin = empPin;
        this.name = name;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getEmpPin() {
        return empPin;
    }

    public void setEmpPin(String empPin) {
        this.empPin = empPin;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Employee{");
        sb.append("empId=").append(empId);
        sb.append(", empPin='").append(empPin).append('\'');
        sb.append(", name='").append(name).append('\'');
        sb.append(", email='").append(email).append('\'');
        sb.append(", phone='").append(phone).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
