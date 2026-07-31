package com.training.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WxClassVO {
    private Long classId;
    private String className;
    private String courseName;
    private String subject;
    private Integer classType;
    private String classTypeText;
    private String teacherName;
    private Integer capacity;
    private Integer enrolledCount;
    private String startDate;
    private String endDate;
    private Integer status;
    private String statusText;
    private BigDecimal price;
}
