package com.ms.app.attendancemgmt;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LoginResponse;
import com.ms.app.attendancemgmt.util.Constants;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class SampleTest {

    @Test
    public void test() throws IOException {
        ObjectMapper om = new ObjectMapper();
        String response = "{\"Status\":\"Success\",\"EmpId\":\"ET0001A01\",\"Message\":\"Santosh Thorwat\"}";
        om.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
        LoginResponse loginResponse = om.readValue(response, LoginResponse.class);
        System.out.println(loginResponse);

        Attendance attendance = new Attendance("testEmpId");
        attendance.setLat(-0.0);
        attendance.setLon(-0.0);
        attendance.setDevId("dummyId");
        attendance.setTime(new Date());
        String json = Utility.getObjectMapper().writeValueAsString(attendance);
        System.out.println(json);

        long longVal = 2 * 60 * 1000;
        String formattedDuration = DurationFormatUtils.formatDuration(longVal, "HH:mm:ss");
        System.out.println(formattedDuration);

        System.out.println(longVal == TimeUnit.MINUTES.toMillis(2));

        String spliter = "{\"devId\":\"21f35eb1efacde82\",\"id\":\"ET0001A01\",\"lat\":40.3404983,\"lon\":-4.5734,\"time\":\"2017-11-12T17:16:10\"}\\|{\"devId\":\"21f35eb1efacde82\",\"id\":\"ET0001A01\",\"lat\":40.3406983,\"lon\":-4.5732483,\"time\":\"2017-11-12T17:16:25\"}\\|{\"devId\":\"21f35eb1efacde82\",\"id\":\"ET0001A01\",\"lat\":40.3408,\"lon\":-4.57331,\"time\":\"2017-11-12T17:16:40\"}\\|{\"devId\":\"21f35eb1efacde82\",\"id\":\"ET0001A01\",\"lat\":40.3415,\"lon\":-4.5737383,\"time\":\"2017-11-12T17:17:44\"}\\|";
        List<String> strings = Arrays.asList(spliter.split("\\\\\\" + Constants.FILE_DELIMITER));
        System.out.println(strings);
    }
}
