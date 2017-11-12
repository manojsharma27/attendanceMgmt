package com.ms.app.attendancemgmt.service;


import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LocationModel;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

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
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static android.content.Context.MODE_APPEND;

public class FileHandler {

    private static final String FILE_NAME = "/LocationFetcherData.txt";

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
        FileOutputStream fileout = null;
        OutputStreamWriter outputWriter = null;
        String fileName = context.getFilesDir() + FILE_NAME;
        try {
            fileout = new FileOutputStream(new File(fileName), true);
            outputWriter = new OutputStreamWriter(fileout);
            outputWriter.write(modelStr + Constants.FILE_DELIMITER);
            outputWriter.close();
            Log.i(Constants.TAG, "Attendance recorded in file");
        } catch (Exception e) {
            Log.e(Constants.TAG, "Error saving file. " + e);
        } finally {
            if (null != outputWriter) {
                try {
                    outputWriter.close();
                } catch (IOException e) {
                }
            }
            if (null != fileout) {
                try {
                    fileout.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public static List<Attendance> readAttendanceFromFile(Context context) {
        FileInputStream fileIn = null;
        InputStreamReader reader = null;
        String fileName = context.getFilesDir() + FILE_NAME;
        StringBuilder sb = new StringBuilder();
        try {
            fileIn = new FileInputStream (new File(fileName));
            reader = new InputStreamReader(fileIn);

            char[] inputBuffer = new char[100];
            int charRead;

            while ((charRead = reader.read(inputBuffer)) > 0) {
                // char to string conversion
                String readstring = String.copyValueOf(inputBuffer, 0, charRead);
                sb.append(readstring);
            }
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                }
            }
            if (null != fileIn) {
                try {
                    fileIn.close();
                } catch (IOException e) {
                }
            }
        }

        if (StringUtils.isEmpty(sb.toString())) {
            return Collections.emptyList();
        }
        List<String> textList = Arrays.asList(sb.toString().split("\\" + Constants.FILE_DELIMITER));
        List<Attendance> attendances = new ArrayList<>();
        for (String text : textList) {
            try {
                Attendance attendance = Utility.getObjectMapper().readValue(text, Attendance.class);
                attendances.add(attendance);
            } catch (IOException e) {
            }
        }
        return attendances;
    }

    public static void cleanUp(Context context) {
        String fileName = context.getFilesDir() + FILE_NAME;
        File file = new File(fileName);
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
