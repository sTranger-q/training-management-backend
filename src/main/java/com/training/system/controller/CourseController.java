package com.training.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.system.common.Result;
import com.training.system.entity.Course;
import com.training.system.service.CourseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/courses")
@RequiredArgsConstructor
@Tag(name = "课程管理")
public class CourseController {

    private final CourseService courseService;

    @GetMapping("/page")
    @Operation(summary = "分页查询课程")
    public Result<IPage<Course>> page(@RequestParam(defaultValue = "1") Integer page,
                                      @RequestParam(defaultValue = "10") Integer size,
                                      @RequestParam(required = false) String keyword) {
        Page<Course> p = new Page<>(page, size);
        LambdaQueryWrapper<Course> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(Course::getName, keyword).or().like(Course::getSubject, keyword);
        }
        w.orderByDesc(Course::getCreateTime);
        return Result.success(courseService.page(p, w));
    }

    @GetMapping("/list")
    @Operation(summary = "查询全部课程（下拉用）")
    public Result<?> list() {
        return Result.success(courseService.list());
    }

    @GetMapping("/{id}")
    @Operation(summary = "课程详情")
    public Result<Course> get(@PathVariable Long id) {
        return Result.success(courseService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增课程")
    public Result<Void> add(@Valid @RequestBody Course course) {
        courseService.save(course);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新课程")
    public Result<Void> update(@RequestBody Course course) {
        courseService.updateById(course);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除课程")
    public Result<Void> delete(@PathVariable Long id) {
        courseService.removeById(id);
        return Result.success();
    }
}
