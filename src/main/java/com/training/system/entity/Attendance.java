package com.training.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 考勤记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("attendance")
public class Attendance extends BaseEntity {

    /** 班级 ID */
    private Long classId;

    /** 学员 ID */
    private Long studentId;

    /** 课次时间 */
    private LocalDateTime lessonTime;

    /** 状态：0-出勤 1-请假 2-旷课 3-补课 */
    private Integer status;

    /** 备注 */
    private String remark;
}
