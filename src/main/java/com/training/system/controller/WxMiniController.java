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
 * 微信小程序端接口。
 * 登录鉴权简化：/wx/login 返回模拟 JWT token，后续接口在 URL 参数或 header 里传 studentId
 * （没有接入真正 Spring Security JWT，Demo 用 token=wx-token-{studentId}）。
 */
@RestController
@RequestMapping("/api/wx")
@RequiredArgsConstructor
@Tag(name = "小程序端接口")
@Slf4j
public class WxMiniController {

    private final StudentService studentService;
    private final ClassInfoService classInfoService;
    private final CourseService courseService;
    private final TeacherService teacherService;
    private final JdbcTemplate jdbcTemplate;

    private static final String[] WEEK_DAYS = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ================== 登录（模拟微信 code2session）==================

    @PostMapping("/login")
    @Operation(summary = "小程序登录（模拟微信code登录）")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body) {
        // code: 传手机号或学员姓名（Demo），开发阶段传 1/2/3/4/5 分别对应学员id
        String code = body.getOrDefault("code", "");
        Student s;
        if (StringUtils.hasText(code) && code.matches("\\d+")) {
            s = studentService.getById(Long.parseLong(code));
        } else {
            s = studentService.getOne(new LambdaQueryWrapper<Student>()
                    .like(Student::getPhone, code).or().like(Student::getName, code)
                    .last("LIMIT 1"));
        }
        if (s == null) {
            s = studentService.list().get(0);
        }
        String token = "wx-token-" + s.getId() + "-" + System.currentTimeMillis();
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("studentId", s.getId());
        data.put("name", s.getName());
        return Result.success(data);
    }

    // ================== 学员信息 ==================

    @GetMapping("/student/me")
    @Operation(summary = "当前学员信息")
    public Result<Map<String, Object>> me(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                          @RequestParam(value = "studentId", required = false) String sid) {
        long studentId = parseStudentId(hdSid, sid);
        Student s = studentService.getById(studentId);
        if (s == null) return Result.error("学员不存在");

        // 班级数（通过 order_info 推断）
        Integer classCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(DISTINCT class_id) FROM order_info WHERE student_id = ? AND status = 1",
                Integer.class, studentId);
        // 出勤率
        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ?", Integer.class, studentId);
        Integer present = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status = 0", Integer.class, studentId);
        BigDecimal attendanceRate = BigDecimal.ZERO;
        if (total != null && total > 0) {
            attendanceRate = BigDecimal.valueOf(present).multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(total), 0, RoundingMode.HALF_UP);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("id", s.getId());
        data.put("name", s.getName());
        data.put("gender", s.getGender());
        data.put("genderText", s.getGender() == 1 ? "男" : s.getGender() == 2 ? "女" : "未知");
        data.put("phone", s.getPhone());
        data.put("parentName", s.getParentName());
        data.put("parentPhone", s.getParentPhone());
        data.put("source", s.getSource());
        data.put("tags", s.getTags());
        data.put("creditBalance", s.getCreditBalance());
        data.put("classCount", classCount == null ? 0 : classCount);
        data.put("attendanceRate", attendanceRate.intValue());
        return Result.success(data);
    }

    // ================== 我的班级列表 ==================

    @GetMapping("/classes")
    @Operation(summary = "我所在的班级列表")
    public Result<List<WxClassVO>> myClasses(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                             @RequestParam(value = "studentId", required = false) String sid) {
        long studentId = parseStudentId(hdSid, sid);
        List<Long> classIds = jdbcTemplate.queryForList(
                "SELECT class_id FROM order_info WHERE student_id = ? AND status = 1",
                Long.class, studentId);
        if (CollectionUtils.isEmpty(classIds)) return Result.success(Collections.emptyList());

        Map<Long, Course> courseMap = courseService.list().stream()
                .collect(Collectors.toMap(Course::getId, c -> c));
        Map<Long, Teacher> teacherMap = teacherService.list().stream()
                .collect(Collectors.toMap(Teacher::getId, t -> t));

        List<ClassInfo> list = classInfoService.listByIds(classIds);
        List<WxClassVO> voList = list.stream().map(c -> {
            WxClassVO vo = new WxClassVO();
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
            Teacher teacher = teacherMap.get(c.getTeacherId());
            if (teacher != null) vo.setTeacherName(teacher.getName());
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

    // ================== 我的课表 ==================

    @GetMapping("/schedule")
    @Operation(summary = "我的周课表")
    public Result<List<WxScheduleVO>> schedule(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                               @RequestParam(value = "studentId", required = false) String sid,
                                               @RequestParam(required = false) String weekStart) {
        long studentId = parseStudentId(hdSid, sid);

        // 本周起始日
        LocalDate start;
        if (StringUtils.hasText(weekStart)) {
            start = LocalDate.parse(weekStart, DATE_FMT);
        } else {
            start = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        }

        List<WxClassVO> classes = myClassesInternal(studentId);
        Map<Integer, List<String[]>> weekAssign = buildWeekAssign(classes);

        List<WxScheduleVO> result = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate day = start.plusDays(i);
            WxScheduleVO vo = new WxScheduleVO();
            vo.setWeekDay(WEEK_DAYS[i]);
            vo.setDate(day.format(DATE_FMT));
            List<WxScheduleVO.Lesson> lessons = new ArrayList<>();
            List<String[]> arr = weekAssign.getOrDefault(i, Collections.emptyList());
            for (String[] item : arr) {
                WxScheduleVO.Lesson l = new WxScheduleVO.Lesson();
                l.setClassId(Long.parseLong(item[0]));
                l.setClassName(item[1]);
                l.setSubject(item[2]);
                l.setTeacherName(item[3]);
                l.setRoom(item[4]);
                l.setStartTime(item[5]);
                l.setEndTime(item[6]);
                int status = decideAttendanceStatus(studentId, l.getClassId(), day);
                l.setStatus(status);
                l.setStatusText(new String[]{"已签到", "请假", "未签到", "补课"}[status]);
                lessons.add(l);
            }
            vo.setLessons(lessons);
            result.add(vo);
        }
        return Result.success(result);
    }

    // ================== 今日课程（首页用）==================

    @GetMapping("/today-lessons")
    @Operation(summary = "今日课程列表")
    public Result<List<WxScheduleVO.Lesson>> todayLessons(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                                          @RequestParam(value = "studentId", required = false) String sid) {
        long studentId = parseStudentId(hdSid, sid);
        List<WxScheduleVO> week = scheduleInternal(studentId, null);
        LocalDate today = LocalDate.now();
        String todayStr = today.format(DATE_FMT);
        for (WxScheduleVO d : week) {
            if (todayStr.equals(d.getDate())) return Result.success(d.getLessons());
        }
        return Result.success(Collections.emptyList());
    }

    // ================== 扫码签到 ==================

    @PostMapping("/attendance/checkin")
    @Operation(summary = "扫码签到")
    public Result<Map<String, Object>> checkin(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                               @RequestParam(value = "studentId", required = false) String sid,
                                               @RequestBody Map<String, Object> body) {
        long studentId = parseStudentId(hdSid, sid);
        Long classId = Long.valueOf(body.get("classId").toString());
        String lessonTimeStr = body.get("lessonTime") == null ? null : body.get("lessonTime").toString();
        LocalDateTime lessonTime = StringUtils.hasText(lessonTimeStr)
                ? LocalDateTime.parse(lessonTimeStr.replace(" ", "T"))
                : LocalDateTime.now();

        // 去重：同日同班同学只能签一次
        LocalDate day = lessonTime.toLocalDate();
        Integer exists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND class_id = ? AND DATE(lesson_time) = ?",
                Integer.class, studentId, classId, java.sql.Date.valueOf(day));
        if (exists != null && exists > 0) {
            return Result.error("今日该班级已签到");
        }
        Attendance att = new Attendance();
        att.setStudentId(studentId);
        att.setClassId(classId);
        att.setLessonTime(lessonTime);
        att.setStatus(0);
        att.setRemark("小程序扫码签到");
        jdbcTemplate.update(
                "INSERT INTO attendance(class_id,student_id,lesson_time,status,remark,create_time,update_time,create_by,update_by) VALUES(?,?,?,?,?,?,?,?,?)",
                classId, studentId, java.sql.Timestamp.valueOf(lessonTime), 0, "小程序扫码签到",
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                java.sql.Timestamp.valueOf(LocalDateTime.now()),
                "wx-mini", "wx-mini");
        // 扣课时
        jdbcTemplate.update("UPDATE student SET credit_balance = GREATEST(0,credit_balance-1) WHERE id = ?", studentId);

        Student s = studentService.getById(studentId);
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("name", s.getName());
        res.put("creditBalance", s.getCreditBalance() - 1);
        res.put("time", lessonTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
        return Result.success(res);
    }

    // ================== 我的考勤记录 ==================

    @GetMapping("/attendance/list")
    @Operation(summary = "我的考勤记录")
    public Result<List<WxAttendanceVO>> attendanceList(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                                       @RequestParam(value = "studentId", required = false) String sid,
                                                       @RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        long studentId = parseStudentId(hdSid, sid);
        int offset = (page - 1) * size;
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT a.id,a.class_id,a.lesson_time,a.status,a.remark,c.name AS class_name " +
                        "FROM attendance a LEFT JOIN class_info c ON c.id = a.class_id " +
                        "WHERE a.student_id = ? ORDER BY a.lesson_time DESC LIMIT ? OFFSET ?",
                studentId, size, offset);
        List<WxAttendanceVO> list = rows.stream().map(r -> {
            WxAttendanceVO vo = new WxAttendanceVO();
            vo.setId(((Number) r.get("id")).longValue());
            vo.setClassId(((Number) r.get("class_id")).longValue());
            vo.setClassName((String) r.get("class_name"));
            java.sql.Timestamp ts = (java.sql.Timestamp) r.get("lesson_time");
            vo.setLessonTime(ts == null ? null : ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")));
            int st = ((Number) r.get("status")).intValue();
            vo.setStatus(st);
            vo.setStatusText(new String[]{"出勤", "请假", "旷课", "补课"}[st]);
            vo.setRemark((String) r.get("remark"));
            return vo;
        }).collect(Collectors.toList());
        return Result.success(list);
    }

    // ================== 我的作业/成绩 ==================

    @GetMapping("/homework")
    @Operation(summary = "我的作业/成绩（Demo数据）")
    public Result<List<WxHomeworkVO>> homework(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                               @RequestParam(value = "studentId", required = false) String sid) {
        long studentId = parseStudentId(hdSid, sid);
        List<WxClassVO> classes = myClassesInternal(studentId);
        List<WxHomeworkVO> list = new ArrayList<>();
        String[][] samples = {
                {"第1次课后练习", "Unit1 单词抄写+听力，完成并签字", "92", "100", "1", "书写工整，继续保持"},
                {"单元小测", "第一章知识点闭卷测试", "88", "100", "1", "计算部分需加强"},
                {"第3次课后练习", "第3课生字10个×5遍，组词造句", "0", "100", "0", ""},
                {"绘画作品评分", "主题：我的暑假", "95", "100", "1", "色彩运用很棒！"}
        };
        int ci = 0;
        for (WxClassVO c : classes) {
            for (int k = 0; k < 2 && ci < samples.length; k++) {
                String[] s = samples[ci++];
                WxHomeworkVO vo = new WxHomeworkVO();
                vo.setId((long) (ci * 100 + studentId));
                vo.setClassId(c.getClassId());
                vo.setClassName(c.getClassName());
                vo.setTitle(s[0]);
                vo.setContent(s[1]);
                vo.setScore(Integer.parseInt(s[2]));
                vo.setTotalScore(Integer.parseInt(s[3]));
                vo.setSubmitStatus(Integer.parseInt(s[4]));
                vo.setTeacherComment(s[5]);
                vo.setDeadline(LocalDate.now().plusDays(ci % 3 - 1).toString());
                vo.setSubmitTime("1".equals(s[4]) ? LocalDateTime.now().minusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
                list.add(vo);
            }
        }
        return Result.success(list);
    }

    // ================== 小程序首页统计 ==================

    @GetMapping("/dashboard")
    @Operation(summary = "小程序首页统计卡")
    public Result<Map<String, Object>> dashboard(@RequestHeader(value = "x-student-id", required = false) String hdSid,
                                                 @RequestParam(value = "studentId", required = false) String sid) {
        long studentId = parseStudentId(hdSid, sid);
        Student s = studentService.getById(studentId);
        if (s == null) return Result.error("学员不存在");
        List<WxClassVO> cls = myClassesInternal(studentId);

        Integer total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ?", Integer.class, studentId);
        Integer present = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND status = 0", Integer.class, studentId);
        int rate = total != null && total > 0 ? present * 100 / total : 100;

        List<WxScheduleVO.Lesson> today = todayLessonsInternal(studentId);
        Map<String, Object> data = new HashMap<>();
        data.put("studentName", s.getName());
        data.put("creditBalance", s.getCreditBalance());
        data.put("classCount", cls.size());
        data.put("attendanceRate", rate);
        data.put("todayLessonCount", today.size());
        return Result.success(data);
    }

    // ================== 内部辅助方法 ==================

    private long parseStudentId(String hd, String qp) {
        if (StringUtils.hasText(hd) && hd.matches("\\d+")) return Long.parseLong(hd);
        if (StringUtils.hasText(qp) && qp.matches("\\d+")) return Long.parseLong(qp);
        return 1L; // 默认返回第一个学员
    }

    private List<WxClassVO> myClassesInternal(long studentId) {
        return myClasses(null, String.valueOf(studentId)).getData();
    }

    private List<WxScheduleVO> scheduleInternal(long studentId, String weekStart) {
        return schedule(null, String.valueOf(studentId), weekStart).getData();
    }

    private List<WxScheduleVO.Lesson> todayLessonsInternal(long studentId) {
        return todayLessons(null, String.valueOf(studentId)).getData();
    }

    /**
     * 根据班级，构造每星期几在哪个时间段上什么课。为演示效果，为每个班级固定分配一个周几+时段。
     */
    private Map<Integer, List<String[]>> buildWeekAssign(List<WxClassVO> classes) {
        Map<Integer, List<String[]>> res = new HashMap<>();
        // 班级id%7 决定周几，班级id%4 决定时段
        String[][] slots = {
                {"09:00", "10:30", "教室A201"}, {"10:45", "12:15", "教室B105"},
                {"14:00", "15:30", "教室C302"}, {"19:00", "20:30", "教室A201"}
        };
        int i = 0;
        for (WxClassVO c : classes) {
            int day = ((c.getClassId().intValue() + i) % 7);
            int slot = (c.getClassId().intValue() + i) % 4;
            String[] slotInfo = slots[slot];
            res.computeIfAbsent(day, k -> new ArrayList<>()).add(new String[]{
                    String.valueOf(c.getClassId()),
                    c.getClassName(),
                    c.getSubject() == null ? "" : c.getSubject(),
                    c.getTeacherName() == null ? "" : c.getTeacherName(),
                    slotInfo[2], slotInfo[0], slotInfo[1]
            });
            i++;
        }
        // 周六周日各多加一个周末班效果（如果没有）
        if (!res.containsKey(5)) res.put(5, new ArrayList<>());
        if (!res.containsKey(6)) res.put(6, new ArrayList<>());
        return res;
    }

    private int decideAttendanceStatus(long studentId, long classId, LocalDate day) {
        Integer cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM attendance WHERE student_id = ? AND class_id = ? AND DATE(lesson_time) = ?",
                Integer.class, studentId, classId, java.sql.Date.valueOf(day));
        if (cnt != null && cnt > 0) return 0;
        // 今天之前的日默认未签到=旷课（2），今天及之后默认=未签到（2）
        if (day.isBefore(LocalDate.now())) return 2;
        return 2;
    }
}
