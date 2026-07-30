package com.training.system.common;

import lombok.Data;

/**
 * 通用分页查询参数。
 */
@Data
public class PageQuery {

    private Integer page = 1;
    private Integer size = 10;
    private String keyword;
}
