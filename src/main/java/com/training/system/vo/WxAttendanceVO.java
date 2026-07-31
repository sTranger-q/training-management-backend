package com.training.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WxAttendanceVO {
    private Long id;
    private Long classId;
    private String className;
    private String lessonTime;
    private Integer status;
    private String statusText;
    private String remark;
}
