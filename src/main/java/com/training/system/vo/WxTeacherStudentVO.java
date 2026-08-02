package com.training.system.vo;

import lombok.Data;

/**
 * 教师端 - 班级学员 VO
 */
@Data
public class WxTeacherStudentVO {
    private Long studentId;
    private String name;
    private Integer gender;
    private String genderText;
    private String phone;
    private String parentName;
    private String parentPhone;
    private String tags;
    private Integer creditBalance;
    /** 今日考勤状态：0-出勤 1-请假 2-未签到 3-补课 */
    private Integer attendanceStatus;
    private String attendanceStatusText;
}
