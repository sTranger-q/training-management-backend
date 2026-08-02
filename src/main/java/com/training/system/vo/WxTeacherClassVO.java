package com.training.system.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 教师端 - 班级 VO
 */
@Data
public class WxTeacherClassVO {
    private Long classId;
    private String className;
    private String courseName;
    private String subject;
    private Integer classType;
    private String classTypeText;
    private Integer capacity;
    private Integer enrolledCount;
    private String startDate;
    private String endDate;
    private Integer status;
    private String statusText;
    private BigDecimal price;
}
