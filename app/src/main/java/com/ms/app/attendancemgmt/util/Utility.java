package com.ms.app.attendancemgmt.util;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.R;
import com.ms.app.attendancemgmt.data.SampleData;
import com.ms.app.attendancemgmt.model.Employee;

import org.apache.commons.lang3.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.ms.app.attendancemgmt.util.Constants.DATE_FORMAT;

public class Utility {

    private static ObjectMapper objectMapper;
    private static final List<String> DEFAULT_PREFS_KEYS = Arrays.asList(Constants.SERVICE_URL_PREF_KEY, Constants.PUNCHING_INTERVAL_KEY);

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

    synchronized public static ObjectMapper getObjectMapper() {
        if (null == objectMapper) {
            objectMapper = new ObjectMapper();
            objectMapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        }
        return objectMapper;
    }

    public static void toastMsg(Context context, String msg) {
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
    }

    public static void writePref(Context context, String key, String value) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(key, value);
        editor.apply();

//        if (DEFAULT_PREFS_KEYS.contains(key)) {
//            PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
//        }
    }

    public static String readPref(Context context, String key) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(Constants.SHARED_PREF_NAME, Context.MODE_PRIVATE);
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
            Log.e(Constants.TAG, "display dialog error.");
        }
    }

    public static void showMessageDialog(Activity activity, String msg) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);

            LayoutInflater inflater = (LayoutInflater) activity
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View layout_Message_dialog = inflater.inflate(
                    R.layout.dialog_message, null);
            builder.setView(layout_Message_dialog);
            builder.setTitle(Constants.APP_TITLE);
            TextView text = layout_Message_dialog.findViewById(R.id.tvDialogMessage);
            text.setVerticalScrollBarEnabled(true);
            text.setHorizontalScrollBarEnabled(true);
            text.setText(msg);
            ImageView image = layout_Message_dialog.findViewById(R.id.imgMessage);
            image.setVisibility(View.GONE);
            builder.setNeutralButton("Ok",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            builder.show();
        } catch (Exception e) {
            Log.e(Constants.TAG, "display dialog error.");
        }
    }

    public static void showGpsNotEnabledDialog(final Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Alert:")
                .setMessage("GPS is not enabled. Would you like to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent locationIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        activity.startActivity(locationIntent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                        Utility.toastMsg(activity, "Attendance not registered.");
                    }
                })
                .create()
                .show();
    }

    public static void loadGetMasterPinDialog(final Activity activity, final MasterPinValidateCallback callback) {
        AlertDialog.Builder dialogMasterPin = new AlertDialog.Builder(
                activity);
        dialogMasterPin.setTitle("Master Pin");
        final EditText txtMasterPin = new EditText(activity);
        txtMasterPin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        txtMasterPin.setLayoutParams(Utility.getLayoutParamsForDialogMsgText());
        dialogMasterPin.setView(txtMasterPin);
        dialogMasterPin.setPositiveButton("Next",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = txtMasterPin.getText().toString().trim();
                        if (!StringUtils.equals(value, Constants.MASTER_PIN)) {
                            Utility.toastMsg(activity,
                                    "Invalid Master Pin.");
                            Utility.showMessageDialog(activity,
                                    "Invalid Master Pin.", R.mipmap.wrong);
                            return;
                        }
                        dialog.cancel();
                        callback.processMasterPinCallback(activity);
                    }
                });

        dialogMasterPin.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        dialog.cancel();
                    }
                });
        dialogMasterPin.show();
    }

    public static LinearLayout.LayoutParams getLayoutParamsForDialogMsgText() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.setMargins(5, 5, 5, 5);
        return layoutParams;
    }

    public static String getServiceUrl(Context context) {
        return readPref(context, Constants.SERVICE_URL_PREF_KEY); // PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.SERVICE_URL_PREF_KEY, null);
    }

    public static String getTime() {
        Calendar cal = Calendar.getInstance();
        String ampm = cal.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
        return String.format("%s:%s %s", cal.get(Calendar.HOUR), cal.get(Calendar.MINUTE), ampm);
    }

    public static boolean ableToAccessInternet(int timeOutInMillis) {
        InetAddress inetAddress = null;
        try {
            Future<InetAddress> future = Executors.newSingleThreadExecutor().submit(new Callable<InetAddress>() {
                @Override
                public InetAddress call() {
                    try {
                        return InetAddress.getByName("google.com");
                    } catch (UnknownHostException e) {
                        return null;
                    }
                }
            });
            inetAddress = future.get(timeOutInMillis, TimeUnit.MILLISECONDS);
            future.cancel(true);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
        }
        boolean connected = inetAddress != null && !inetAddress.equals("");
        Log.d(Constants.TAG, (connected ? "" : "not ") + "able to access internet.");
        return connected;
    }

    public static long getPunchingInterval(Context context) {
        String punchInterval = readPref(context, Constants.PUNCHING_INTERVAL_KEY); // PreferenceManager.getDefaultSharedPreferences(context).getString(Constants.PUNCHING_INTERVAL_KEY, String.valueOf(Constants.MIN_PUNCH_INTERVAL));
        return (StringUtils.isEmpty(punchInterval) ? Constants.MIN_PUNCH_INTERVAL : Long.parseLong(punchInterval));
    }
}
