package com.training.system.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 班级列表 VO（关联课程名、教师名）。
 */
@Data
public class ClassInfoVO {

    private Long id;
    private String name;
    private Long courseId;
    private String courseName;
    private Integer classType;
    private Integer capacity;
    private Integer enrolledCount;
    private Long teacherId;
    private String teacherName;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer status;
    private LocalDateTime createTime;
}
