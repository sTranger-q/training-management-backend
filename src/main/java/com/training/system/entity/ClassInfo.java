package com.training.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDate;

/**
 * 班级实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("class_info")
public class ClassInfo extends BaseEntity {

    /** 班级名称 */
    private String name;

    /** 关联课程 ID */
    private Long courseId;

    /** 班型：1-一对一 2-小班 3-大班 */
    private Integer classType;

    /** 容量 */
    private Integer capacity;

    /** 已报名人数 */
    private Integer enrolledCount;

    /** 授课教师 ID */
    private Long teacherId;

    /** 开课日期 */
    private LocalDate startDate;

    /** 结课日期 */
    private LocalDate endDate;

    /** 状态：0-招生中 1-已开课 2-已结课 3-已停办 */
    private Integer status;
}
