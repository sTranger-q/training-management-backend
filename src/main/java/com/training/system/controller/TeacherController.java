package com.training.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.training.system.common.Result;
import com.training.system.entity.Teacher;
import com.training.system.service.TeacherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/teachers")
@RequiredArgsConstructor
@Tag(name = "教师管理")
public class TeacherController {

    private final TeacherService teacherService;

    @GetMapping("/page")
    @Operation(summary = "分页查询教师")
    public Result<IPage<Teacher>> page(@RequestParam(defaultValue = "1") Integer page,
                                       @RequestParam(defaultValue = "10") Integer size,
                                       @RequestParam(required = false) String keyword) {
        Page<Teacher> p = new Page<>(page, size);
        LambdaQueryWrapper<Teacher> w = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            w.like(Teacher::getName, keyword)
                    .or().like(Teacher::getPhone, keyword)
                    .or().like(Teacher::getSubject, keyword);
        }
        w.orderByDesc(Teacher::getCreateTime);
        return Result.success(teacherService.page(p, w));
    }

    @GetMapping("/list")
    @Operation(summary = "查询全部教师（下拉用）")
    public Result<?> list() {
        return Result.success(teacherService.list());
    }

    @PostMapping
    @Operation(summary = "新增教师")
    public Result<Void> add(@Valid @RequestBody Teacher teacher) {
        teacherService.save(teacher);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新教师")
    public Result<Void> update(@RequestBody Teacher teacher) {
        teacherService.updateById(teacher);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除教师")
    public Result<Void> delete(@PathVariable Long id) {
        teacherService.removeById(id);
        return Result.success();
    }
}
