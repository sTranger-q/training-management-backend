package com.training.system.vo;

import lombok.Data;

/**
 * 教师端 - 作业 VO
 */
@Data
public class WxTeacherHomeworkVO {
    private Long id;
    private Long classId;
    private String className;
    private String title;
    private String content;
    private String deadline;
    private Integer submitCount;
    private Integer totalCount;
    private Integer gradedCount;
    private String createTime;
}
