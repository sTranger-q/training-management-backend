package com.training.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 课程实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends BaseEntity {

    /** 课程名称 */
    private String name;

    /** 科目 */
    private String subject;

    /** 适用年龄段 */
    private String ageRange;

    /** 总课时 */
    private Integer totalLessons;

    /** 单价 */
    private BigDecimal price;

    /** 课程描述 */
    private String description;
}
