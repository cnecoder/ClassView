# 小爱晶的小本本 — 技术文档

## 技术栈

| 层级 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 架构 | MVVM (ViewModel + StateFlow) |
| 数据库 | Room (SQLite) |
| 网络 | Retrofit + OkHttp |
| 后台任务 | WorkManager |
| 闹钟 | AlarmManager (setAlarmClock) |
| 日期 | java.time (JSR-310) + desugaring |
| 最低版本 | Android 10 (API 29) |
| 编译目标 | API 34 |

## 项目结构

```
app/src/main/java/com/example/schedule/
├── MainActivity.kt              # 入口，权限请求，主题状态
├── ScheduleApp.kt               # Application，初始化 DB/网络/Repository
├── data/
│   ├── model/
│   │   ├── Course.kt            # 课程模板实体
│   │   ├── ClassInstance.kt     # 每节课实例实体
│   │   ├── Holiday.kt           # 节假日实体
│   │   └── Alarm.kt             # 闹钟记录实体
│   ├── db/
│   │   ├── AppDatabase.kt       # Room 数据库 (DB v5)
│   │   ├── CourseDao.kt         # 课程 CRUD
│   │   ├── ClassInstanceDao.kt  # 实例查询（按日期/课程）
│   │   ├── HolidayDao.kt        # 节假日查询
│   │   └── AlarmDao.kt          # 闹钟 CRUD
│   ├── remote/
│   │   ├── HolidayApi.kt        # Retrofit 接口 (timor.tech)
│   │   └── HolidayResponse.kt   # API 响应模型
│   └── repository/
│       ├── CourseRepository.kt  # 课程+实例+闹钟仓库
│       └── HolidayRepository.kt # 节假日仓库（缓存+API）
├── ui/
│   ├── theme/
│   │   ├── Color.kt             # 课程颜色 + 生成函数
│   │   ├── Theme.kt             # 动态主题应用
│   │   ├── ThemePresets.kt      # 7 套预设配色
│   │   ├── ThemeManager.kt      # 主题持久化（SharedPreferences）
│   │   └── ThemeEditor.kt       # 自定义主题编辑器
│   └── course/
│       ├── HomeScreen.kt        # 首页三 Tab + 主题切换
│       ├── CourseEditScreen.kt  # 课程编辑表单 + 日历预览
│       └── CourseViewModel.kt   # 课程+实例状态管理
├── alarm/
│   ├── AlarmScheduler.kt        # 闹钟调度（读实例表设闹钟）
│   ├── AlarmReceiver.kt         # 闹钟触发通知
│   ├── AlarmBootReceiver.kt     # 开机重设
│   └── AlarmRescheduleWorker.kt # 每日维护
└── util/
    ├── DateUtils.kt             # 日期解析/格式化/计算
    ├── CourseCalculator.kt      # 日期生成+覆盖应用
    └── DebugLog.kt              # 双写日志（logcat + 文件）
```

## 数据模型

### Course（课程模板）
```
id, name, daysOfWeek("1,3,5"), startTime, endTime,
startDate, endDate, skipHolidays, enableAlarm,
alarmMinutesBefore, alarmRepeatInterval, alarmRepeatCount,
color, note
```

### ClassInstance（每节课实例）
```
id, courseId, date("yyyy-MM-dd"), startTime, endTime,
enableAlarm, alarmMinutesBefore, alarmRepeatInterval, alarmRepeatCount,
manuallyEdited, note
```
- `courseId` + `date` 建立复合索引
- `manuallyEdited = true` 表示与模板不同（日历中单独调整过）

### Holiday（节假日缓存）
```
date("yyyy-MM-dd"), name, isOffDay
```

### Alarm（闹钟记录）
```
id, courseId, triggerTime(ms), courseName,
repeatIntervalMinutes, repeatCount
```

## 核心流程

### 保存课程
```
用户填写模板 → 日历预览中调整 → onSave
  → CourseEditScreen.buildInstances() 生成 List<ClassInstance>
  → ViewModel.saveCourseWithInstances(course, instances)
  → Repository.insert(course) → Repository.saveInstances(id, instances)
  → AlarmScheduler.scheduleForCourse() 从实例表读设闹钟
```

### 编辑课程（恢复日历调整）
```
打开编辑 → CourseEditScreen.reconstructOverrides()
  → 对比模板生成的日期 vs 已有实例的差异
  → 被排除的标记 exclude=true
  → 修改过的标记对应的 DayOverride
  → 恢复 CalendarPreviewSection 的选中状态
```

### 首页展示
```
ViewModel.instances Flow → HomeScreen
  → 周视图：过滤 7 天内的实例，按日期分组展示
  → 月视图：过滤当月实例，按日期分组展示
  → 列表视图：课程列表 → 点击展开全部实例
```

### 主题切换
```
用户选择主题 → ThemeManager.setCurrentTheme(key)
  → SharedPreferences 持久化
  → MainActivity 重组 ScheduleTheme(themeKey)
  → Theme.kt: getPreset(key) 返回 ColorScheme
  → 自定义主题: ThemeManager.loadCustomTheme(key) 从 JSON 重建
```

## 节假日数据
- API: `https://timor.tech/api/holiday/year/{year}`
- 首次启动拉取当年 + 下一年
- Room 本地缓存，每日凌晨 WorkManager 检查更新
- 网络异常时使用缓存，不阻塞课程创建

## 数据库迁移历史
- v1 → v2: Course.dayOfWeek(Int) → daysOfWeek(String)
- v2 → v3: 新增 Course.dayOverrides(JSON)
- v3 → v4: 新增 ClassInstance 表，移除 dayOverrides
- v4 → v5: 移除 Course.restDays 字段
- 使用 `fallbackToDestructiveMigration()` + 异常时 deleteDatabase 重建

## 已知限制
- 系统时钟 App 不显示程序化闹钟（Android 限制）
- Honor 设备 logcat 可能屏蔽 app 日志（DebugLog 双写文件解决）
- 节假日 API 仅覆盖中国法定节假日
