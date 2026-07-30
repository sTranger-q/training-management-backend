package com.training.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.system.entity.ClassInfo;
import com.training.system.vo.ClassInfoVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ClassInfoMapper extends BaseMapper<ClassInfo> {

    @Select("SELECT c.id, c.name, c.course_id AS courseId, co.name AS courseName, " +
            "c.class_type AS classType, c.capacity, c.enrolled_count AS enrolledCount, " +
            "c.teacher_id AS teacherId, t.name AS teacherName, " +
            "c.start_date AS startDate, c.end_date AS endDate, c.status, c.create_time AS createTime " +
            "FROM class_info c " +
            "LEFT JOIN course co ON c.course_id = co.id " +
            "LEFT JOIN teacher t ON c.teacher_id = t.id " +
            "WHERE (#{keyword} IS NULL OR #{keyword} = '' OR c.name LIKE CONCAT('%', #{keyword}, '%')) " +
            "ORDER BY c.create_time DESC")
    IPage<ClassInfoVO> pageVO(IPage<ClassInfoVO> page, @Param("keyword") String keyword);
}
