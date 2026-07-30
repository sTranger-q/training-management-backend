package com.training.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 订单/收费记录。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_info")
public class OrderInfo extends BaseEntity {

    /** 订单号 */
    private String orderNo;

    /** 学员 ID */
    private Long studentId;

    /** 班级 ID */
    private Long classId;

    /** 课程 ID */
    private Long courseId;

    /** 订单金额 */
    private BigDecimal amount;

    /** 实付金额 */
    private BigDecimal paidAmount;

    /** 支付方式：0-微信 1-支付宝 2-现金 3-转账 */
    private Integer payMethod;

    /** 状态：0-待支付 1-已支付 2-已退款 3-已取消 */
    private Integer status;
}
