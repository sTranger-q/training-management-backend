package com.training.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.system.entity.Student;
import com.training.system.mapper.StudentMapper;
import com.training.system.service.StudentService;
import org.springframework.stereotype.Service;

@Service
public class StudentServiceImpl extends ServiceImpl<StudentMapper, Student> implements StudentService {
}
