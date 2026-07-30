-- 培训班管理系统建表脚本（H2/MySQL 兼容）
-- 通用字段：id, create_time, update_time, create_by, update_by

CREATE TABLE IF NOT EXISTS student (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(64)  NOT NULL,
    gender          INT          DEFAULT 0,
    phone           VARCHAR(20),
    parent_name     VARCHAR(64),
    parent_phone    VARCHAR(20),
    source          VARCHAR(64),
    consultant      VARCHAR(64),
    tags            VARCHAR(255),
    credit_balance  INT          DEFAULT 0,
    remark          VARCHAR(500),
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(64)  DEFAULT 'system',
    update_by       VARCHAR(64)  DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS course (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    subject         VARCHAR(64),
    age_range       VARCHAR(64),
    total_lessons   INT,
    price           DECIMAL(10,2),
    description     VARCHAR(500),
    create_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by       VARCHAR(64)  DEFAULT 'system',
    update_by       VARCHAR(64)  DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS teacher (
    id                BIGINT AUTO_INCREMENT PRIMARY KEY,
    name              VARCHAR(64) NOT NULL,
    phone             VARCHAR(20),
    subject           VARCHAR(64),
    qualification     VARCHAR(128),
    salary_per_lesson DECIMAL(10,2),
    status            INT         DEFAULT 0,
    create_time       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    update_time       TIMESTAMP   DEFAULT CURRENT_TIMESTAMP,
    create_by         VARCHAR(64) DEFAULT 'system',
    update_by         VARCHAR(64) DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS class_info (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(128) NOT NULL,
    course_id      BIGINT,
    class_type     INT,
    capacity       INT,
    enrolled_count INT          DEFAULT 0,
    teacher_id     BIGINT,
    start_date     DATE,
    end_date       DATE,
    status         INT          DEFAULT 0,
    create_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by      VARCHAR(64)  DEFAULT 'system',
    update_by      VARCHAR(64)  DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS attendance (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    class_id    BIGINT,
    student_id  BIGINT,
    lesson_time TIMESTAMP,
    status      INT,
    remark      VARCHAR(255),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    create_by   VARCHAR(64) DEFAULT 'system',
    update_by   VARCHAR(64) DEFAULT 'system'
);

CREATE TABLE IF NOT EXISTS order_info (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_no     VARCHAR(64) NOT NULL,
    student_id   BIGINT,
    class_id     BIGINT,
    course_id    BIGINT,
    amount       DECIMAL(10,2),
    paid_amount  DECIMAL(10,2),
    pay_method   INT,
    status       INT          DEFAULT 0,
    create_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    create_by    VARCHAR(64)  DEFAULT 'system',
    update_by    VARCHAR(64)  DEFAULT 'system'
);
