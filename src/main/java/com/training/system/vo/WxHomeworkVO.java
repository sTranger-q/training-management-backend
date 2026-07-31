package com.training.system.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WxHomeworkVO {
    private Long id;
    private Long classId;
    private String className;
    private String title;
    private String content;
    private Integer score;
    private Integer totalScore;
    private Integer submitStatus;
    private String teacherComment;
    private String deadline;
    private String submitTime;
}
