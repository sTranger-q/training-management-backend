package com.training.system.controller;

import com.training.system.common.BusinessException;
import com.training.system.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 登录接口（MVP 简化版：固定账号，生产应接入 Spring Security + JWT）。
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "登录认证")
public class AuthController {

    @PostMapping("/login")
    @Operation(summary = "登录")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String username = body.getOrDefault("username", "");
        String password = body.getOrDefault("password", "");
        if (!"admin".equals(username) || !"123456".equals(password)) {
            throw new BusinessException(401, "用户名或密码错误");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("token", "mock-token-" + System.currentTimeMillis());
        Map<String, Object> user = new HashMap<>();
        user.put("id", 1L);
        user.put("username", username);
        user.put("name", "系统管理员");
        user.put("roles", new String[]{"admin"});
        data.put("user", user);
        return Result.success(data);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录")
    public Result<Void> logout() {
        return Result.success();
    }
}
