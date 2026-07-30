package com.training.system.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.training.system.entity.ClassInfo;
import com.training.system.vo.ClassInfoVO;

public interface ClassInfoService extends IService<ClassInfo> {

    IPage<ClassInfoVO> pageVO(Integer page, Integer size, String keyword);
}
