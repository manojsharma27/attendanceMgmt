package com.ms.app.attendancemgmt.service;


import android.content.Context;
import android.os.Environment;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LocationModel;
import com.ms.app.attendancemgmt.util.Constants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileHandler {

    private static final String FILE_NAME = "LocationFetcherData.txt";

    private static File getFile(Context context) {
        File filesDir = context.getFilesDir();
        if (filesDir.exists() && filesDir.isDirectory()) {
            File file = new File(FILE_NAME);
            if (!file.exists()) {
                try {
                    boolean newFile = file.createNewFile();
                    return newFile ? file : null;
                } catch (IOException e) {
                    Log.e(Constants.TAG, "Failed to create new file : " + file.getAbsolutePath());
                }
            } else {
                return file;
            }
        }
        return null;
    }

    private static void writeLocationsToFile(Context context, LocationModel locationModel) {
        ObjectMapper om = new ObjectMapper();
        String modelStr = null;
        try {
            modelStr = om.writeValueAsString(locationModel);
        } catch (JsonProcessingException e) {
        }
        if (StringUtils.isEmpty(modelStr)) {
            return;
        }
        modelStr += "\n";
//        FileOutputStream outputStream = null;
        BufferedWriter bw = null;
        try {
            File file = new File(FILE_NAME);
            if (!file.exists()) {
                boolean created = file.createNewFile();
                if (!created) {
                    Log.e(Constants.TAG, "Failed to create new file : " + FILE_NAME);
                    return;
                }
            }

            FileWriter fw = new FileWriter(file.getAbsoluteFile());
            bw = new BufferedWriter(fw);
            bw.write(modelStr);
            bw.close();
//            outputStream = context.openFileOutput(FILE_NAME, Context.MODE_APPEND);
//            outputStream.write(modelStr.getBytes());
//            outputStream.flush();
//            outputStream.close();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception while writing to file : " + e);
        } finally {
            if (null != bw) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private static List<LocationModel> readLocationsFromFile(Context context) {
        FileInputStream fileInputStream;
        List<String> textList = new ArrayList<>();
        BufferedReader br = null;
        try {
//            fileInputStream = context.openFileInput(FILE_NAME);
//            DataInputStream in = new DataInputStream(fileInputStream);
            br = new BufferedReader(new FileReader(FILE_NAME));
            String line = "";
            while ((line = br.readLine()) != null) {
                textList.add(line);
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Exception while reading from file : " + e);
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }

        ObjectMapper om = new ObjectMapper();
        if (CollectionUtils.isEmpty(textList)) {
            return Collections.emptyList();
        }
        List<LocationModel> models = new ArrayList<>();
        for (String text : textList) {
            try {
                LocationModel locationModel = om.readValue(text, LocationModel.class);
                models.add(locationModel);
            } catch (IOException e) {
            }
        }
        return models;
    }

    public static void writeAttendanceToFile(Context context, Attendance attendance) {
        ObjectMapper om = new ObjectMapper();
        String modelStr = null;
        try {
            modelStr = om.writeValueAsString(attendance);
        } catch (JsonProcessingException e) {
        }
        if (StringUtils.isEmpty(modelStr)) {
            return;
        }
        modelStr += "\n";
        BufferedWriter bw = null;
        File file = getFile(context);
        if (null == file) {
            Log.e(Constants.TAG, "Failed to get file : " + file.getAbsolutePath());
        }
        try {
            FileWriter fw = new FileWriter(file.getAbsoluteFile(), true);
            bw = new BufferedWriter(fw);
            bw.write(modelStr);
            bw.close();
        } catch (Exception e) {
            Log.e(Constants.TAG, "Exception while writing to file : " + e);
        } finally {
            if (null != bw) {
                try {
                    bw.close();
                } catch (IOException e) {
                }
            }
        }
        Log.i(Constants.TAG, "Attendance recorded in file");
    }

    public static List<Attendance> readAttendanceFromFile(Context context) {
        List<String> textList = new ArrayList<>();
        BufferedReader br = null;
        File file = getFile(context);
        if (null == file) {
            Log.e(Constants.TAG, "Failed to get file : " + file.getAbsolutePath());
        }
        try {
            br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                textList.add(line);
            }
        } catch (IOException e) {
            Log.e(Constants.TAG, "Exception while reading from file : " + e);
        } finally {
            if (null != br) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }

        ObjectMapper om = new ObjectMapper();
        if (CollectionUtils.isEmpty(textList)) {
            return Collections.emptyList();
        }
        List<Attendance> attendances = new ArrayList<>();
        for (String text : textList) {
            try {
                Attendance attendance = om.readValue(text, Attendance.class);
                attendances.add(attendance);
            } catch (IOException e) {
            }
        }
        return attendances;
    }

    public static void cleanUp(Context context) {
        File file = new File(FILE_NAME);
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.i(Constants.TAG, "File deleted : " + file.getAbsolutePath());
            } else {
                Log.e(Constants.TAG, "Failed to delete file : " + file.getAbsolutePath());
            }
        }
    }
}
