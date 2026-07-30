package com.training.system.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.training.system.common.Result;
import com.training.system.entity.ClassInfo;
import com.training.system.service.ClassInfoService;
import com.training.system.vo.ClassInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/classes")
@RequiredArgsConstructor
@Tag(name = "班级管理")
public class ClassInfoController {

    private final ClassInfoService classInfoService;

    @GetMapping("/page")
    @Operation(summary = "分页查询班级（含课程名、教师名）")
    public Result<IPage<ClassInfoVO>> page(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword) {
        return Result.success(classInfoService.pageVO(page, size, keyword));
    }

    @GetMapping("/{id}")
    @Operation(summary = "班级详情")
    public Result<ClassInfo> get(@PathVariable Long id) {
        return Result.success(classInfoService.getById(id));
    }

    @PostMapping
    @Operation(summary = "新增班级")
    public Result<Void> add(@Valid @RequestBody ClassInfo classInfo) {
        classInfoService.save(classInfo);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新班级")
    public Result<Void> update(@RequestBody ClassInfo classInfo) {
        classInfoService.updateById(classInfo);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除班级")
    public Result<Void> delete(@PathVariable Long id) {
        classInfoService.removeById(id);
        return Result.success();
    }
}
