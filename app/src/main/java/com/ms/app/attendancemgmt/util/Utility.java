package com.ms.app.attendancemgmt.util;


import android.content.Context;
import android.widget.Toast;

import com.ms.app.attendancemgmt.data.SampleData;
import com.ms.app.attendancemgmt.model.Employee;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;
import java.util.TimeZone;

public class Utility {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";

    public static String formatDate(String date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        return dateFormat.format(date);
    }

    public static String formatDateForUTC(Date date) {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    public static boolean isPinValid(String pin) {
        for (Employee emp : SampleData.employees()) {
            if (pin.equals(emp.getEmpPin())) {
                return true;
            }
        }
        return false;
    }

    public static Employee searchEmployeeFromPin(String pin) {
        for (Employee emp : SampleData.employees()) {
            if (pin.equals(emp.getEmpPin())) {
                return emp;
            }
        }
        return null;
    }

    public static void toastMsg(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }
}
