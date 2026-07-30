package com.training.system.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.system.entity.ClassInfo;
import com.training.system.mapper.ClassInfoMapper;
import com.training.system.service.ClassInfoService;
import com.training.system.vo.ClassInfoVO;
import org.springframework.stereotype.Service;

@Service
public class ClassInfoServiceImpl extends ServiceImpl<ClassInfoMapper, ClassInfo> implements ClassInfoService {

    @Override
    public IPage<ClassInfoVO> pageVO(Integer page, Integer size, String keyword) {
        Page<ClassInfoVO> p = new Page<>(page, size);
        return baseMapper.pageVO(p, keyword);
    }
}
