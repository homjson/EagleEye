
# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 构建命令

本机 JDK 需要指向 Android Studio 自带的 JBR：

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH="$JAVA_HOME/bin:$PATH"
```

```bash
./gradlew assembleDebug     # Debug APK
./gradlew assembleRelease   # Release APK（需 keystore.properties）
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 架构概览

所有数据采集由 `MonitorService`（前台服务）统一驱动，每 2 秒采集一轮，通过 companion object 的 `StateFlow` 发布给各 ViewModel。View 层通过 `collectAsState()` 订阅。

```
/proc + ps → ProcParser → Repository（含差值计算）→ MonitorService._xxx StateFlow → ViewModel → Compose
```

`MonitorService.startDataCollection()` 中进程数据首轮不发布（作基准样本），第二轮起有了 CPU 差值才发布，同时负载和线程数据立即发布，避免冷启动白屏。

## 关键文件

| 文件 | 作用 |
|---|---|
| `MonitorService.kt` | 前台服务，统一采集循环 + 悬浮窗管理，companion 暴露所有 StateFlow |
| `ProcParser.kt` | `/proc` 解析器，所有 shell 命令的入口，通过 `RootHelper.execCmd()` 执行 |
| `ProcessRepository.kt` | 进程 CPU 计算：保存上一轮 `pid→cpuTime` 映射，`cpu% = deltaTicks * 1000 / deltaMs` |
| `ThreadRepository.kt` | 线程 CPU 计算：按 PID 缓存上轮样本，`cpu% = deltaThread / deltaTotal * 100` |
| `MonitorPager.kt` | 底部导航路由，持有 `ProcessMonitorViewModel`（需跨 Tab 传递给详情页） |
| `RootHelper.kt` | libsu 封装，所有 `/proc` 读取和 shell 命令通过它执行 |

## 数据模型

- `ProcessInfo.cpuTime` — utime+stime（clock ticks），供 Repository 做差值计算，不直接展示
- `ProcessInfo.memUsage` — 单位 KB
- `ThreadInfo.cpuTime` — 同上，用于线程级 CPU 差值
- CPU 差值公式：`deltaTicks * 1000f / deltaMs`（基于 CLK_TCK=100）
- 内存解析要点：`ps -A -o pid,args,stat,rss` 的 args 列含空格，必须从行尾解析 rss/stat

## 悬浮窗

`MonitorService` 管理三种悬浮窗（THREAD/LOAD/PROCESS），`FloatingState` 通过 StateFlow 暴露开关状态。拖拽移动，点击跳转对应 Tab。`SettingsDialog` 提供开关 UI。

## 过滤规则

- `parseProcessList()` 中过滤 args 以 `[` 开头的内核线程（如 `[kworker/0:0]`）
- 长时间命令（如 cat 大量文件）用 glob 展开 `cat /proc/[0-9]*/stat` 而非逐个列出路径，避免 shell 命令行长度限制
