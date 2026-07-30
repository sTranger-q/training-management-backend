package com.training.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.system.entity.Teacher;
import com.training.system.mapper.TeacherMapper;
import com.training.system.service.TeacherService;
import org.springframework.stereotype.Service;

@Service
public class TeacherServiceImpl extends ServiceImpl<TeacherMapper, Teacher> implements TeacherService {
}
