package com.training.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.system.common.Result;
import com.training.system.entity.Student;
import com.training.system.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
@Tag(name = "学员管理")
public class StudentController {

    private final StudentService studentService;

    @GetMapping("/page")
    @Operation(summary = "分页查询学员")
    public Result<IPage<Student>> page(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String keyword) {
        Page<Student> p = new Page<>(page, size);
        LambdaQueryWrapper<Student> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(Student::getName, keyword)
                    .or().like(Student::getPhone, keyword)
                    .or().like(Student::getParentPhone, keyword);
        }
        w.orderByDesc(Student::getCreateTime);
        return Result.success(studentService.page(p, w));
    }

    @GetMapping("/{id}")
    @Operation(summary = "学员详情")
    public Result<Student> get(@PathVariable Long id) {
        return Result.success(studentService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增学员")
    public Result<Void> add(@Valid @RequestBody Student student) {
        studentService.save(student);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新学员")
    public Result<Void> update(@RequestBody Student student) {
        studentService.updateById(student);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除学员")
    public Result<Void> delete(@PathVariable Long id) {
        studentService.removeById(id);
        return Result.success();
    }
}
