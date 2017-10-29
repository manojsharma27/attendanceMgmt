package com.ms.app.attendancemgmt.data;

import com.ms.app.attendancemgmt.model.Employee;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Temporary class to hold dummy dataset
 */
public class SampleData {

    public static int EMP_COUNT = 5;
    private static int PinStart = 1000;
    private static String EmpIdPrefix = "BCCI";
    private static List<Employee> employees;
    private static List<String> empNames = Arrays.asList("Suresh Raina", "M. S. Dhoni", "Gautam Gambhir", "Virendra Sehwag", "Hardik Pandya",
            "Virat Kohli", "Harbhajan Singh", "Andrew Flintoff", "Matthew Hayden");

    static {
        employees = new ArrayList<>();
        for (int i = 1; i < EMP_COUNT; i++) {
            int curr = PinStart + i;
            Employee emp = new Employee(EmpIdPrefix + curr, curr + "", empNames.get(i));
            employees.add(emp);
        }
    }

    public static List<Employee> employees() {
        return employees;
    }
}
