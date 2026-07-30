package com.training.system.controller;

import com.training.system.common.Result;
import com.training.system.service.ClassInfoService;
import com.training.system.service.CourseService;
import com.training.system.service.StudentService;
import com.training.system.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 经营看板统计。
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "经营看板")
public class DashboardController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final ClassInfoService classInfoService;
    private final TeacherService teacherService;
    private final JdbcTemplate jdbcTemplate;

    @GetMapping("/stats")
    @Operation(summary = "看板汇总数据")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> data = new HashMap<>();
        data.put("studentCount", studentService.count());
        data.put("courseCount", courseService.count());
        data.put("classCount", classInfoService.count());
        data.put("teacherCount", teacherService.count());

        Double totalRevenue = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(paid_amount),0) FROM order_info WHERE status = 1",
                Double.class);
        data.put("totalRevenue", totalRevenue == null ? 0 : totalRevenue);

        return Result.success(data);
    }
}
