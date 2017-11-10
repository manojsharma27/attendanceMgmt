package com.ms.app.attendancemgmt.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.data.SampleData;
import com.ms.app.attendancemgmt.model.Employee;

import org.apache.commons.lang3.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class Utility {

    public static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
    private static String SERVICE_URL = StringUtils.EMPTY;

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

    public static void saveSharedPref(Context context, String key, String value) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();
    }

    public static String readFromSharedPref(Context context, String key) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString(key, null);
    }

    /*
     * This method will show Message dailog like Success,Error and warning
	 * message
	 */
    public static void showMessageDialog(Activity activity, String msg, int imgId) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            LayoutInflater inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout_Message_dialog = inflater.inflate(
                    R.layout.dialog_message, null);
            builder.setView(layout_Message_dialog);
            builder.setTitle(Constants.APP_TITLE);
            TextView text = layout_Message_dialog.findViewById(R.id.tvDialogMessage);
            text.setText(msg);
            ImageView image = layout_Message_dialog.findViewById(R.id.imgMessage);
            image.setImageResource(imgId);
            builder.setNeutralButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
        } catch (Exception e) {
            Log.e(Constants.LOG_TAG, "display dialog error.");
        }
    }

    public static void loadPreferences(Context context) {
        SERVICE_URL = readFromSharedPref(context, Constants.SERVICE_URL_PREF_KEY);
    }

    public static String getServiceUrl() {
        return SERVICE_URL;
    }

    public static String getTime() {
        Calendar cal = Calendar.getInstance();
        String ampm = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        return String.format("%s:%s %s", cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), ampm);
    }
}
