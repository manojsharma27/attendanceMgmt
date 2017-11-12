package com.ms.app.attendancemgmt;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ms.app.attendancemgmt.model.Attendance;
import com.ms.app.attendancemgmt.model.LoginResponse;
import com.ms.app.attendancemgmt.util.Utility;

import org.apache.commons.lang3.time.DurationFormatUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Date;

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
    }
}
