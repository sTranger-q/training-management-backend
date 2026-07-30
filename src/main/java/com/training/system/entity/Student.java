package com.training.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 学员实体。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("student")
public class Student extends BaseEntity {

    /** 学员姓名 */
    private String name;

    /** 性别：0-未知 1-男 2-女 */
    private Integer gender;

    /** 手机号 */
    private String phone;

    /** 家长姓名 */
    private String parentName;

    /** 家长手机号 */
    private String parentPhone;

    /** 来源 */
    private String source;

    /** 归属顾问 */
    private String consultant;

    /** 标签，逗号分隔 */
    private String tags;

    /** 课时余额 */
    private Integer creditBalance;

    /** 备注 */
    private String remark;
}
