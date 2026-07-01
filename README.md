
# Eagle Eye

Android 系统监视器，通过读取 `/proc` 文件系统（需 Root 权限）实时展示设备的 CPU 负载、内存使用、进程和线程信息。

## 功能

- **CPU 监控**：总使用率、Load Average（1m/5m/15m）、各核心使用率与频率
- **内存监控**：总量、已用、可用、缓存、Swap 使用情况
- **进程列表**：所有用户态进程，支持按 CPU、内存、名称排序
- **进程详情**：单击进程查看概要信息及线程列表
- **悬浮窗**：可开关的悬浮叠加层，在任意界面显示核心指标
- **前台 APP 线程**：自动识别前台应用并采集其线程 CPU 占用

## 截图

> TODO: 添加截图

## 系统要求

- Android 8.0 (API 26) 及以上
- **Root 权限**（通过 [libsu](https://github.com/topjohnwu/libsu) 读取 `/proc` 文件系统）

## 技术栈

| 模块     | 技术                                          |
| -------- | --------------------------------------------- |
| UI       | Jetpack Compose + Material 3                  |
| 数据采集 | `/proc` 文件系统解析（ps、stat、meminfo 等） |
| Root 调用| libsu                                         |
| 架构     | MVVM（ViewModel + StateFlow）                 |
| 构建     | Kotlin + Gradle KTS                           |

## 项目结构

```
app/src/main/java/com/hom/monitor/
├── data/
│   ├── model/          # 数据模型（CpuLoad, MemoryInfo, ProcessInfo, ThreadInfo）
│   ├── parser/         # /proc 解析器
│   └── repository/     # 数据仓库（含 CPU 差值计算）
├── ui/
│   ├── components/     # 通用组件（CpuCoreGrid, MemoryBar, ThreadList）
│   ├── load/           # 负载页
│   ├── process/        # 进程页 + 详情页
│   ├── thread/         # 线程页
│   └── navigation/     # 导航 + 底部 Tab
├── util/
│   ├── RootHelper.kt       # libsu 封装
│   └── PermissionHelper.kt # 权限检查
├── MonitorService.kt       # 前台服务（数据采集 + 悬浮窗）
└── MainActivity.kt         # 入口
```

## 构建

```bash
./gradlew assembleDebug
```

## 许可

MIT License
