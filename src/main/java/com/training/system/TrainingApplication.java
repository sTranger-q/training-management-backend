package com.training.system;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 培训班管理系统启动类。
 * 对应设计报告：模块化单体 + 分层架构（controller/service/entity/mapper）。
 */
@SpringBootApplication
@MapperScan("com.training.system.mapper")
public class TrainingApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrainingApplication.class, args);
    }
}
