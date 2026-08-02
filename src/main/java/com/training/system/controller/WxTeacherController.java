package com.training.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.training.system.common.Result;
import com.training.system.entity.*;
import com.training.system.service.*;
import com.training.system.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 微信小程序教师端接口。
 * 登录鉴权简化：/teacher/login 返回模拟 JWT token，后续接口在 header 里传 x-teacher-id
 */
@RestController
@RequestMapping("/api/wx/teacher")
@RequiredArgsConstructor
@Tag(name = "小程序教师端接口")
@Slf4j
public class WxTeacherController {

    private final TeacherService teacherService;
    private final ClassInfoService classInfoService;
    private final CourseService courseService;
    private final StudentService studentService;
    private final JdbcTemplate jdbcTemplate;

    private static final String[] WEEK_DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // ================== 教师登录 ==================

    @PostMapping("/login")
    @Operation(summary = "教师登录（模拟手机号/姓名登录）")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        String code = body.getOrDefault("code", "");
        Teacher t;
        if (StringUtils.hasText(code) && code.matches("\\d+")) {
            t = teacherService.getById(Long.parseLong(code));
        } else {
            t = teacherService.getOne(new LambdaQueryWrapper<Teacher>()
                    .like(Teacher::getPhone, code).or().like(Teacher::getName, code)
                    .last("LIMIT 1"));
        }
        if (t == null) {
            t = teacherService.list().get(0);
        }
        String token = "wx-teacher-token-" + t.getId() + "-" + System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("teacherId", t.getId());
        data.put("name", t.getName());
        data.put("subject", t.getSubject());
        return Result.success(data);
    }

    // ================== 教师信息 + 首页统计 ==================

    @GetMapping("/me")
    @Operation(summary = "当前教师信息")
    public Result<Map<String, Object>> me(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                          @RequestParam(value = "teacherId", required = false) String tid) {
        long teacherId = parseTeacherId(hdTid, tid);
        Teacher t = teacherService.getById(teacherId);
        if (t == null) return Result.error("教师不存在");

        Map<String, Object> data = new HashMap<>();
        data.put("id", t.getId());
        data.put("name", t.getName());
        data.put("phone", t.getPhone());
        data.put("subject", t.getSubject());
        data.put("qualification", t.getQualification());
        data.put("salaryPerLesson", t.getSalaryPerLesson());
        data.put("status", t.getStatus());
        data.put("statusText", t.getStatus() == 0 ? "在职" : "离职");
        return Result.success(data);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "教师首页统计")
    public Result<Map<String, Object>> dashboard(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                   @RequestParam(value = "teacherId", required = false) String tid) {
        long teacherId = parseTeacherId(hdTid, tid);
        Teacher t = teacherService.getById(teacherId);
        if (t == null) return Result.error("教师不存在");

        // 我的班级数
        List<ClassInfo> myClasses = classInfoService.list(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, teacherId));
        int classCount = myClasses.size();

        // 总学员数（去重）
        Set<Long> studentIds = new HashSet<>();
        for (ClassInfo c : myClasses) {
            List<Long> ids = jdbcTemplate.queryForList(
                    "SELECT DISTINCT student_id FROM order_info WHERE class_id = ? AND status = 1",
                    Long.class, c.getId());
            studentIds.addAll(ids);
        }

        // 本月课时数（考勤记录数）
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        Integer monthLessons = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT DATE(lesson_time)) FROM attendance a " +
                        "JOIN class_info c ON c.id = a.class_id WHERE c.teacher_id = ? AND a.lesson_time >= ?",
                Integer.class, teacherId, java.sql.Date.valueOf(monthStart));

        // 本月课酬
        BigDecimal salaryPerLesson = t.getSalaryPerLesson() == null ? BigDecimal.ZERO : t.getSalaryPerLesson();
        BigDecimal monthSalary = salaryPerLesson.multiply(BigDecimal.valueOf(monthLessons == null ? 0 : monthLessons));

        // 今日课程数
        List<WxTeacherScheduleVO.Lesson> todayLessons = todayLessonsInternal(teacherId);

        Map<String, Object> data = new HashMap<>();
        data.put("teacherName", t.getName());
        data.put("subject", t.getSubject());
        data.put("classCount", classCount);
        data.put("studentCount", studentIds.size());
        data.put("monthLessonCount", monthLessons == null ? 0 : monthLessons);
        data.put("monthSalary", monthSalary);
        data.put("todayLessonCount", todayLessons.size());
        return Result.success(data);
    }

    // ================== 我的班级列表 ==================

    @GetMapping("/classes")
    @Operation(summary = "我的班级列表")
    public Result<List<WxTeacherClassVO>> myClasses(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                     @RequestParam(value = "teacherId", required = false) String tid) {
        long teacherId = parseTeacherId(hdTid, tid);
        List<ClassInfo> list = classInfoService.list(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, teacherId).orderByDesc(ClassInfo::getStatus));

        Map<Long, Course> courseMap = courseService.list().stream()
                .collect(Collectors.toMap(Course::getId, c -> c));

        List<WxTeacherClassVO> voList = list.stream().map(c -> {
            WxTeacherClassVO vo = new WxTeacherClassVO();
            vo.setClassId(c.getId());
            vo.setClassName(c.getName());
            Course course = courseMap.get(c.getCourseId());
            if (course != null) {
                vo.setCourseName(course.getName());
                vo.setSubject(course.getSubject());
                vo.setPrice(course.getPrice());
            }
            vo.setClassType(c.getClassType());
            vo.setClassTypeText(c.getClassType() == 1 ? "一对一" : c.getClassType() == 2 ? "小班" : "大班");
            vo.setCapacity(c.getCapacity());
            vo.setEnrolledCount(c.getEnrolledCount());
            vo.setStartDate(c.getStartDate() == null ? null : c.getStartDate().format(DATE_FMT));
            vo.setEndDate(c.getEndDate() == null ? null : c.getEndDate().format(DATE_FMT));
            vo.setStatus(c.getStatus());
            vo.setStatusText(new String[]{"招生中", "已开课", "已结课", "已停办"}[Math.min(c.getStatus(), 3)]);
            return vo;
        }).collect(Collectors.toList());
        return Result.success(voList);
    }

    // ================== 班级学员名单 ==================

    @GetMapping("/classes/{classId}/students")
    @Operation(summary = "班级学员名单")
    public Result<List<WxTeacherStudentVO>> classStudents(@PathVariable Long classId,
                                                           @RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                           @RequestParam(value = "teacherId", required = false) String tid) {
        long teacherId = parseTeacherId(hdTid, tid);
        // 校验班级归属
        ClassInfo classInfo = classInfoService.getById(classId);
        if (classInfo == null) return Result.error("班级不存在");
        if (classInfo.getTeacherId() == null || classInfo.getTeacherId() != teacherId) {
            return Result.error("无权操作此班级");
        }

        List<Long> studentIds = jdbcTemplate.queryForList(
                "SELECT DISTINCT student_id FROM order_info WHERE class_id = ? AND status = 1",
                Long.class, classId);
        if (CollectionUtils.isEmpty(studentIds)) return Result.success(Collections.emptyList());

        LocalDate today = LocalDate.now();
        List<WxTeacherStudentVO> voList = studentIds.stream().map(sid -> {
            Student s = studentService.getById(sid);
            if (s == null) return null;
            WxTeacherStudentVO vo = new WxTeacherStudentVO();
            vo.setStudentId(s.getId());
            vo.setName(s.getName());
            vo.setGender(s.getGender());
            vo.setGenderText(s.getGender() == 1 ? "男" : s.getGender() == 2 ? "女" : "未知");
            vo.setPhone(s.getPhone());
            vo.setParentName(s.getParentName());
            vo.setParentPhone(s.getParentPhone());
            vo.setTags(s.getTags());
            vo.setCreditBalance(s.getCreditBalance());

            // 查今日考勤
            Integer attStatus = jdbcTemplate.queryForObject(
                    "SELECT status FROM attendance WHERE student_id = ? AND class_id = ? AND DATE(lesson_time) = ? LIMIT 1",
                    Integer.class, sid, classId, java.sql.Date.valueOf(today));
            vo.setAttendanceStatus(attStatus == null ? 2 : attStatus);
            vo.setAttendanceStatusText(new String[]{"出勤", "请假", "未签到", "补课"}[vo.getAttendanceStatus()]);
            return vo;
        }).filter(Objects::nonNull).collect(Collectors.toList());
        return Result.success(voList);
    }

    // ================== 点名（批量考勤） ==================

    @PostMapping("/attendance/checkin")
    @Operation(summary = "批量点名（教师对班级学员点名）")
    public Result<Map<String, Object>> batchCheckin(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                     @RequestParam(value = "teacherId", required = false) String tid,
                                                     @RequestBody Map<String, Object> body) {
        long teacherId = parseTeacherId(hdTid, tid);
        Long classId = Long.valueOf(body.get("classId").toString());
        String lessonTimeStr = body.get("lessonTime") == null ? null : body.get("lessonTime").toString();
        LocalDateTime lessonTime = StringUtils.hasText(lessonTimeStr)
                ? LocalDateTime.parse(lessonTimeStr.replace(" ", "T"))
                : LocalDateTime.now();

        // 校验班级归属
        ClassInfo classInfo = classInfoService.getById(classId);
        if (classInfo == null) return Result.error("班级不存在");
        if (classInfo.getTeacherId() == null || classInfo.getTeacherId() != teacherId) {
            return Result.error("无权操作此班级");
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> attendanceList = (List<Map<String, Object>>) body.getOrDefault("attendanceList", Collections.emptyList());
        if (CollectionUtils.isEmpty(attendanceList)) {
            return Result.error("点名列表为空");
        }

        LocalDate day = lessonTime.toLocalDate();
        int successCount = 0;
        int skipCount = 0;
        for (Map<String, Object> item : attendanceList) {
            Long studentId = Long.valueOf(item.get("studentId").toString());
            Integer status = Integer.valueOf(item.get("status").toString());

            // 去重：同日同班同学只能签一次
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND class_id = ? AND DATE(lesson_time) = ?",
                    Integer.class, studentId, classId, java.sql.Date.valueOf(day));
            if (exists != null && exists > 0) {
                // 更新已有记录
                jdbcTemplate.update(
                        "UPDATE attendance SET status = ?, remark = ? WHERE student_id = ? AND class_id = ? AND DATE(lesson_time) = ?",
                        status, "教师点名", studentId, classId, java.sql.Date.valueOf(day));
            } else {
                jdbcTemplate.update(
                        "INSERT INTO attendance(class_id,student_id,lesson_time,status,remark,create_time,update_time,create_by,update_by) VALUES(?,?,?,?,?,?,?,?,?)",
                        classId, studentId, java.sql.Timestamp.valueOf(lessonTime), status, "教师点名",
                        java.sql.Timestamp.valueOf(LocalDateTime.now()),
                        java.sql.Timestamp.valueOf(LocalDateTime.now()),
                        "wx-teacher", "wx-teacher");
            }
            successCount++;
        }

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("total", attendanceList.size());
        res.put("checkedCount", successCount);
        res.put("time", lessonTime.format(DATETIME_FMT));
        return Result.success(res);
    }

    // ================== 我的课表 ==================

    @GetMapping("/schedule")
    @Operation(summary = "我的周课表")
    public Result<List<WxTeacherScheduleVO>> schedule(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                       @RequestParam(value = "teacherId", required = false) String tid,
                                                       @RequestParam(required = false) String weekStart) {
        long teacherId = parseTeacherId(hdTid, tid);
        LocalDate start;
        if (StringUtils.hasText(weekStart)) {
            start = LocalDate.parse(weekStart, DATE_FMT);
        } else {
            start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        List<WxTeacherClassVO> classes = myClassesInternal(teacherId);
        Map<Integer, List<String[]>> weekAssign = buildWeekAssign(classes);

        List<WxTeacherScheduleVO> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            WxTeacherScheduleVO vo = new WxTeacherScheduleVO();
            vo.setWeekDay(WEEK_DAYS[i]);
            vo.setDate(day.format(DATE_FMT));
            List<WxTeacherScheduleVO.Lesson> lessons = new ArrayList<>();
            List<String[]> arr = weekAssign.getOrDefault(i, Collections.emptyList());
            for (String[] item : arr) {
                WxTeacherScheduleVO.Lesson l = new WxTeacherScheduleVO.Lesson();
                l.setClassId(Long.parseLong(item[0]));
                l.setClassName(item[1]);
                l.setSubject(item[2]);
                l.setRoom(item[3]);
                l.setStartTime(item[4]);
                l.setEndTime(item[5]);
                l.setEnrolledCount(Integer.parseInt(item[6]));
                l.setStatus(day.isBefore(LocalDate.now()) ? 1 : day.isEqual(LocalDate.now()) ? 0 : 2);
                l.setStatusText(new String[]{"今日", "已结束", "待上课"}[l.getStatus()]);
                lessons.add(l);
            }
            vo.setLessons(lessons);
            result.add(vo);
        }
        return Result.success(result);
    }

    // ================== 今日课程 ==================

    @GetMapping("/today-lessons")
    @Operation(summary = "今日课程列表")
    public Result<List<WxTeacherScheduleVO.Lesson>> todayLessons(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                                  @RequestParam(value = "teacherId", required = false) String tid) {
        return Result.success(todayLessonsInternal(parseTeacherId(hdTid, tid)));
    }

    // ================== 作业管理（Demo数据） ==================

    @GetMapping("/homework")
    @Operation(summary = "我布置的作业列表（Demo数据）")
    public Result<List<WxTeacherHomeworkVO>> homework(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                      @RequestParam(value = "teacherId", required = false) String tid) {
        long teacherId = parseTeacherId(hdTid, tid);
        List<WxTeacherClassVO> classes = myClassesInternal(teacherId);
        List<WxTeacherHomeworkVO> list = new ArrayList<>();
        String[][] samples = {
                {"第1次课后练习", "Unit1 单词抄写+听力，完成并签字", "3", "5", "2"},
                {"单元小测", "第一章知识点闭卷测试", "5", "5", "5"},
                {"第3次课后练习", "第3课生字10个×5遍，组词造句", "2", "5", "0"},
                {"绘画作品评分", "主题：我的暑假", "1", "1", "1"}
        };
        int ci = 0;
        for (WxTeacherClassVO c : classes) {
            for (int k = 0; k < 2 && ci < samples.length; k++) {
                String[] s = samples[ci++];
                WxTeacherHomeworkVO vo = new WxTeacherHomeworkVO();
                vo.setId((long) (ci * 100 + teacherId));
                vo.setClassId(c.getClassId());
                vo.setClassName(c.getClassName());
                vo.setTitle(s[0]);
                vo.setContent(s[1]);
                vo.setSubmitCount(Integer.parseInt(s[2]));
                vo.setTotalCount(Integer.parseInt(s[3]));
                vo.setGradedCount(Integer.parseInt(s[4]));
                vo.setDeadline(LocalDate.now().plusDays(ci % 3 - 1).toString());
                vo.setCreateTime(LocalDateTime.now().minusDays(ci).format(DATETIME_FMT));
                list.add(vo);
            }
        }
        return Result.success(list);
    }

    @PostMapping("/homework")
    @Operation(summary = "布置作业（Demo）")
    public Result<Map<String, Object>> createHomework(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                      @RequestParam(value = "teacherId", required = false) String tid,
                                                      @RequestBody Map<String, Object> body) {
        long teacherId = parseTeacherId(hdTid, tid);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id", System.currentTimeMillis());
        res.put("title", body.get("title"));
        res.put("content", body.get("content"));
        res.put("classId", body.get("classId"));
        res.put("deadline", body.get("deadline"));
        res.put("createTime", LocalDateTime.now().format(DATETIME_FMT));
        return Result.success(res);
    }

    // ================== 考勤统计 ==================

    @GetMapping("/attendance/stats")
    @Operation(summary = "考勤统计（按班级）")
    public Result<Map<String, Object>> attendanceStats(@RequestHeader(value = "x-teacher-id", required = false) String hdTid,
                                                        @RequestParam(value = "teacherId", required = false) String tid,
                                                        @RequestParam(defaultValue = "30") int days) {
        long teacherId = parseTeacherId(hdTid, tid);
        LocalDate startDate = LocalDate.now().minusDays(days);

        List<ClassInfo> myClasses = classInfoService.list(new LambdaQueryWrapper<ClassInfo>()
                .eq(ClassInfo::getTeacherId, teacherId));
        List<Map<String, Object>> classStats = new ArrayList<>();
        int totalPresent = 0;
        int totalAbsent = 0;
        int totalLeave = 0;

        for (ClassInfo c : myClasses) {
            Integer present = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attendance WHERE class_id = ? AND status = 0 AND lesson_time >= ?",
                    Integer.class, c.getId(), java.sql.Date.valueOf(startDate));
            Integer leave = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attendance WHERE class_id = ? AND status = 1 AND lesson_time >= ?",
                    Integer.class, c.getId(), java.sql.Date.valueOf(startDate));
            Integer absent = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM attendance WHERE class_id = ? AND status = 2 AND lesson_time >= ?",
                    Integer.class, c.getId(), java.sql.Date.valueOf(startDate));
            Map<String, Object> stat = new HashMap<>();
            stat.put("classId", c.getId());
            stat.put("className", c.getName());
            stat.put("present", present == null ? 0 : present);
            stat.put("leave", leave == null ? 0 : leave);
            stat.put("absent", absent == null ? 0 : absent);
            classStats.add(stat);
            totalPresent += present == null ? 0 : present;
            totalAbsent += absent == null ? 0 : absent;
            totalLeave += leave == null ? 0 : leave;
        }

        int total = totalPresent + totalAbsent + totalLeave;
        BigDecimal attendanceRate = total > 0
                ? BigDecimal.valueOf(totalPresent).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);

        Map<String, Object> data = new HashMap<>();
        data.put("classStats", classStats);
        data.put("totalPresent", totalPresent);
        data.put("totalAbsent", totalAbsent);
        data.put("totalLeave", totalLeave);
        data.put("attendanceRate", attendanceRate.intValue());
        return Result.success(data);
    }

    // ================== 内部辅助方法 ==================

    private long parseTeacherId(String hd, String qp) {
        if (StringUtils.hasText(hd) && hd.matches("\\d+")) return Long.parseLong(hd);
        if (StringUtils.hasText(qp) && qp.matches("\\d+")) return Long.parseLong(qp);
        return 1L;
    }

    private List<WxTeacherClassVO> myClassesInternal(long teacherId) {
        return myClasses(null, String.valueOf(teacherId)).getData();
    }

    private List<WxTeacherScheduleVO.Lesson> todayLessonsInternal(long teacherId) {
        List<WxTeacherScheduleVO> week = scheduleInternal(teacherId, null);
        String todayStr = LocalDate.now().format(DATE_FMT);
        for (WxTeacherScheduleVO d : week) {
            if (todayStr.equals(d.getDate())) return d.getLessons();
        }
        return Collections.emptyList();
    }

    private List<WxTeacherScheduleVO> scheduleInternal(long teacherId, String weekStart) {
        return schedule(null, String.valueOf(teacherId), weekStart).getData();
    }

    /**
     * 根据班级构造周课表（演示效果，固定分配时段）
     */
    private Map<Integer, List<String[]>> buildWeekAssign(List<WxTeacherClassVO> classes) {
        Map<Integer, List<String[]>> res = new HashMap<>();
        String[][] slots = {
                {"09:00", "10:30", "教室A201"}, {"10:45", "12:15", "教室B105"},
                {"14:00", "15:30", "教室C302"}, {"19:00", "20:30", "教室A201"}
        };
        int i = 0;
        for (WxTeacherClassVO c : classes) {
            int day = ((c.getClassId().intValue() + i) % 7);
            int slot = (c.getClassId().intValue() + i) % 4;
            String[] slotInfo = slots[slot];
            res.computeIfAbsent(day, k -> new ArrayList<>()).add(new String[]{
                    String.valueOf(c.getClassId()),
                    c.getClassName(),
                    c.getSubject() == null ? "" : c.getSubject(),
                    slotInfo[2], slotInfo[0], slotInfo[1],
                    String.valueOf(c.getEnrolledCount() == null ? 0 : c.getEnrolledCount())
            });
            i++;
        }
        return res;
    }
}
