package com.training.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.training.system.entity.Course;
import com.training.system.mapper.CourseMapper;
import com.training.system.service.CourseService;
import org.springframework.stereotype.Service;

@Service
public class CourseServiceImpl extends ServiceImpl<CourseMapper, Course> implements CourseService {
}
