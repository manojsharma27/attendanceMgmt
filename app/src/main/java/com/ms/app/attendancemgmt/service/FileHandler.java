package com.ms.app.attendancemgmt.service;


import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
        try {
            fileout = new FileOutputStream(new File(getFileName(context)), true);
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
        StringBuilder sb = new StringBuilder();
        try {
            fileIn = new FileInputStream(new File(getFileName(context)));
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

    @NonNull
    private static String getFileName(Context context) {
        return context.getFilesDir() + FILE_NAME;
    }

    public static void cleanUp(Context context) {
        File file = new File(getFileName(context));
        if (file.exists()) {
            boolean deleted = file.delete();
            if (deleted) {
                Log.i(Constants.TAG, "File deleted : " + file.getAbsolutePath());
            } else {
                Log.e(Constants.TAG, "Failed to delete file : " + file.getAbsolutePath());
            }
        }
    }

    public static boolean locationFileExists(Context context) {
        if (null == context) {
            return false;
        }
        File file = new File(getFileName(context));
        return file.exists() && file.isFile();
    }

    public static boolean checkFileModifiedAfter(Context context, long prevTime) {
        File file = new File(getFileName(context));
        if (file.exists()) {
            return file.lastModified() > prevTime;
        }
        return false;
    }
}
