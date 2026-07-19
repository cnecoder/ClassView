# 教师排课闹钟 App 实现计划

## 背景

为教师开发一款 Android 排课应用，支持导入课程、智能跳过节假日、自动设置闹钟提醒。

## 技术栈

| 项 | 选择 | 原因 |
|---|------|------|
| 语言 | Kotlin | Android 官方语言 |
| UI | Jetpack Compose + Material 3 | 声明式 UI，代码简洁，现代标准 |
| 架构 | MVVM | Android 官方推荐 |
| 数据库 | Room | SQLite 封装，类型安全 |
| 闹钟 | AlarmManager | 系统级闹钟，精确唤醒 |
| 后台任务 | WorkManager | 周期性重设闹钟 |
| 网络 | Retrofit + OkHttp | 请求节假日 API |
| 最低版本 | Android 10 (API 29) | 用户指定 |

## 节假日 API

使用 `https://timor.tech/api/holiday` 中国节假日 API：
- 免费，无需注册
- 格式：`GET /api/holiday/year/{year}` 获取全年节假日
- 本地缓存到 Room，减少网络请求

## 项目结构

```
app/src/main/java/com/example/schedule/
├── MainActivity.kt              # 入口
├── ScheduleApp.kt               # Application 类
├── data/
│   ├── db/
│   │   ├── AppDatabase.kt       # Room 数据库
│   │   ├── CourseDao.kt         # 课程 DAO
│   │   └── HolidayDao.kt        # 节假日 DAO
│   ├── model/
│   │   ├── Course.kt            # 课程实体
│   │   ├── Alarm.kt             # 闹钟实体
│   │   └── Holiday.kt           # 节假日实体
│   ├── remote/
│   │   ├── HolidayApi.kt        # Retrofit 接口
│   │   └── HolidayResponse.kt   # API 响应模型
│   └── repository/
│       ├── CourseRepository.kt  # 课程仓库
│       └── HolidayRepository.kt # 节假日仓库
├── ui/
│   ├── theme/                   # Material 3 主题
│   ├── course/
│   │   ├── CourseListScreen.kt  # 课程列表
│   │   ├── CourseEditScreen.kt  # 添加/编辑课程
│   │   └── CourseViewModel.kt   # 课程 VM
│   ├── schedule/
│   │   └── ScheduleScreen.kt    # 周/日视图
│   └── alarm/
│       └── AlarmSettings.kt     # 闹钟设置组件
├── alarm/
│   ├── AlarmScheduler.kt        # 闹钟调度器
│   ├── AlarmReceiver.kt         # 闹钟广播接收器
│   └── AlarmBootReceiver.kt     # 开机重设闹钟
└── util/
    ├── DateUtils.kt             # 日期工具
    └── CourseCalculator.kt      # 课程日期计算（核心）
```

## 数据模型设计

### Course（课程）
```kotlin
@Entity
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,                    // 课程名称
    val dayOfWeek: Int,                  // 星期几 (1=周一)
    val startTime: String,               // 开始时间 "HH:mm"
    val endTime: String,                 // 结束时间 "HH:mm"
    val startDate: LocalDate,            // 课程开始日期
    val endDate: LocalDate,              // 课程结束日期
    val repeatWeeks: String,             // 重复周次 "1,2,3,5-18" (第1,2,3,5至18周)
    val skipHolidays: Boolean,           // 是否跳过法定节假日
    val restDays: String,                // 固定休息日 "6,7" (周六日)
    val enableAlarm: Boolean,            // 是否启用闹钟
    val alarmMinutesBefore: Int,         // 提前多少分钟
    val alarmRepeatInterval: Int,        // 重复间隔（分钟）
    val alarmRepeatCount: Int,           // 重复次数
    val color: Int,                      // 课程颜色
    val note: String                     // 备注
)
```

### Holiday（节假日缓存）
```kotlin
@Entity
data class Holiday(
    @PrimaryKey val date: String,        // "2026-01-01"
    val name: String,                    // "元旦"
    val isOffDay: Boolean                // true=放假, false=调休上班
)
```

### Alarm（闹钟记录）
```kotlin
@Entity
data class Alarm(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val courseId: Long,                  // 关联课程
    val triggerTime: Long,               // 触发时间戳
    val label: String                    // 闹钟标签
)
```

## 核心逻辑：CourseCalculator

课程日期计算是整个 App 的核心。算法：

```
输入：课程 (日期+重复规则) + 节假日数据 + 休息日设置
输出：实际需要上课的日期列表

步骤：
1. 从 startDate 到 endDate，按 dayOfWeek 筛选出所有匹配的日期
2. 如果设置了 skipHolidays，过滤掉节假日（查本地缓存+API）
3. 过滤掉固定休息日（如周六日）
4. 处理调休（节假日调休的周末需要上班，不跳过）
5. 如果设置了 repeatWeeks（教学周次），只保留对应周的日期
```

### 教学周次计算
- 以 startDate 所在周为第 1 周
- repeatWeeks 如 "1,2,3,5-18" 表示第 1、2、3、5 到 18 周上课
- 周次 = (当前日期 - startDate) 所在周差 + 1

## 闹钟调度逻辑

### 设置闹钟
1. 用户添加/编辑课程时选择启用闹钟
2. 调用 CourseCalculator 计算出所有实际上课日期
3. 为每个上课日期的 `startTime - alarmMinutesBefore` 创建闹钟
4. 使用 AlarmManager.setAlarmClock() 设置（即使省电模式也能触发）
5. 根据 alarmRepeatInterval 和 alarmRepeatCount 设置后续重复闹钟

### 闹钟触发
1. AlarmReceiver 收到广播
2. 创建通知（NotificationChannel），使用闹钟音量
3. 播放默认闹钟铃声
4. 用户可以关闭/贪睡

### 闹钟维护
- WorkManager 每日凌晨检查：重新设置未来闹钟（处理节假日数据更新）
- 开机后 AlarmBootReceiver 重新注册所有闹钟
- 删除课程时取消对应闹钟

## 节假日数据策略

1. 首次启动：请求当年 + 下一年节假日 API
2. 缓存到 Room 本地数据库
3. 每周检查是否需要更新下一年数据
4. 网络异常时使用本地缓存，不阻塞课程创建

## 实现步骤

### 步骤 1：创建项目骨架
- 用 Android Studio 创建 Kotlin + Compose 项目
- 配置 Gradle 依赖（Room, Retrofit, WorkManager, Navigation）
- 设置 Material 3 主题
- 验证：项目编译通过

### 步骤 2：数据层
- 定义 Room Entity（Course, Holiday, Alarm）
- 实现 DAO
- 实现 AppDatabase
- 实现 Retrofit HolidayApi
- 实现 CourseRepository, HolidayRepository
- 验证：数据库读写测试通过

### 步骤 3：核心计算引擎
- 实现 DateUtils 工具函数
- 实现 CourseCalculator（日期展开+节假日过滤+周次过滤）
- 验证：单元测试覆盖各种场景（跨年、调休、假期重叠等）

### 步骤 4：课程管理 UI
- CourseListScreen：显示所有课程，支持删除
- CourseEditScreen：表单——名称、时间选择器、重复设定、节假日跳过、闹钟设置
- CourseViewModel
- 验证：手动添加/编辑/删除课程

### 步骤 5：闹钟系统
- AlarmScheduler：设置/取消闹钟
- AlarmReceiver：接收闹钟并显示通知
- AlarmBootReceiver：开机重设
- 验证：设置闹钟后系统闹钟 App 中可见

### 步骤 6：节假日集成
- 首次启动拉取节假日数据
- 课程计算时自动应用节假日过滤
- 验证：元旦/国庆等节假日课程被正确跳过

### 步骤 7：最终验证
- 端到端测试：添加课程 → 计算日期 → 设置闹钟 → 收到通知
- 边界测试：跨学期、调休日、无网络等情况

## 验证方式

每步完成后：
1. `./gradlew assembleDebug` 编译通过
2. 在模拟器/真机上运行，手动验证 UI 流程
3. 核心计算逻辑通过单元测试
