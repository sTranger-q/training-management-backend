package com.training.system.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class WxScheduleVO {
    private String weekDay;
    private String date;
    private List<Lesson> lessons;

    @Data
    public static class Lesson {
        private Long classId;
        private String className;
        private String subject;
        private String teacherName;
        private String room;
        private String startTime;
        private String endTime;
        private Integer status;
        private String statusText;
    }
}
