package com.training.system.vo;

import lombok.Data;

/**
 * 教师端 - 课表 VO
 */
@Data
public class WxTeacherScheduleVO {
    private String weekDay;
    private String date;
    private java.util.List<Lesson> lessons;

    @Data
    public static class Lesson {
        private Long classId;
        private String className;
        private String subject;
        private String startTime;
        private String endTime;
        private String room;
        private Integer enrolledCount;
        private Integer status;
        private String statusText;
    }
}
