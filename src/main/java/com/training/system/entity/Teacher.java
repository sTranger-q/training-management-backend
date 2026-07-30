package com.training.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教师实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teacher")
public class Teacher extends BaseEntity {

    /** 姓名 */
    private String name;

    /** 手机号 */
    private String phone;

    /** 所授科目 */
    private String subject;

    /** 资质 */
    private String qualification;

    /** 课时单价（课酬） */
    private java.math.BigDecimal salaryPerLesson;

    /** 状态：0-在职 1-离职 */
    private Integer status;
}
