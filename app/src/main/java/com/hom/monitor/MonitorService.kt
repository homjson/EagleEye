package com.hom.monitor

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.hom.monitor.data.model.CpuLoad
import com.hom.monitor.data.model.MemoryInfo
import com.hom.monitor.data.model.ProcessInfo
import com.hom.monitor.data.model.ThermalInfo
import com.hom.monitor.data.model.ThreadInfo

import com.hom.monitor.data.parser.ProcParser
import com.hom.monitor.data.repository.LoadRepository
import com.hom.monitor.data.repository.ProcessRepository
import com.hom.monitor.data.repository.ThreadRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class FloatingType(val label: String) {
    THREAD("线程"),
    LOAD("负载"),
    PROCESS("进程")
}

data class FloatingState(
    val threadVisible: Boolean = false,
    val loadVisible: Boolean = false,
    val processVisible: Boolean = false
)

class MonitorService : Service() {

    private var windowManager: WindowManager? = null
    private val threadRepo = ThreadRepository()
    private val loadRepo = LoadRepository()
    private val processRepo = ProcessRepository()
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val floatViews = mutableMapOf<FloatingType, View>()
    private val floatArgs = mapOf(
        FloatingType.THREAD to WindowArgs(20, 200, 220),
        FloatingType.LOAD to WindowArgs(20, 340, 100),
        FloatingType.PROCESS to WindowArgs(20, 480, 220)
    )

    companion object {
        const val CHANNEL_ID = "eagle_eye_monitor"
        const val NOTIFICATION_ID = 1001

        var instance: MonitorService? = null
            private set

        // 稳定引用 — 数据流始终是同一个对象
        private val _threadList = MutableStateFlow<List<ThreadInfo>>(emptyList())
        val threadList: StateFlow<List<ThreadInfo>> = _threadList.asStateFlow()

        private val _cpuLoad = MutableStateFlow<CpuLoad?>(null)
        val cpuLoad: StateFlow<CpuLoad?> = _cpuLoad.asStateFlow()

        private val _memoryInfo = MutableStateFlow<MemoryInfo?>(null)
        val memoryInfo: StateFlow<MemoryInfo?> = _memoryInfo.asStateFlow()

        private val _processList = MutableStateFlow<List<ProcessInfo>>(emptyList())
        val processList: StateFlow<List<ProcessInfo>> = _processList.asStateFlow()

        private val _thermalInfo = MutableStateFlow<ThermalInfo?>(null)
        val thermalInfo: StateFlow<ThermalInfo?> = _thermalInfo.asStateFlow()

        private val _floatingState = MutableStateFlow(FloatingState())
        val floatingState: StateFlow<FloatingState> = _floatingState.asStateFlow()

        fun start(context: Context) {
            val intent = Intent(context, MonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MonitorService::class.java))
        }

        fun isRunning() = instance != null

        fun toggleFloating(type: FloatingType, show: Boolean) {
            instance?.toggleFloatingWindow(type, show)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startDataCollection()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        instance = null
        serviceScope.cancel()
        FloatingType.entries.forEach { removeFloatingWindow(it) }
        super.onDestroy()
    }

    private fun toggleFloatingWindow(type: FloatingType, show: Boolean) {
        val current = when (type) {
            FloatingType.THREAD -> MonitorService._floatingState.value.threadVisible
            FloatingType.LOAD -> MonitorService._floatingState.value.loadVisible
            FloatingType.PROCESS -> MonitorService._floatingState.value.processVisible
        }
        if (show == current) return

        MonitorService._floatingState.value = when (type) {
            FloatingType.THREAD -> MonitorService._floatingState.value.copy(threadVisible = show)
            FloatingType.LOAD -> MonitorService._floatingState.value.copy(loadVisible = show)
            FloatingType.PROCESS -> MonitorService._floatingState.value.copy(processVisible = show)
        }

        if (show) createFloatingWindow(type) else removeFloatingWindow(type)
    }

    private fun createFloatingWindow(type: FloatingType) {
        if (floatViews.containsKey(type)) return

        val args = floatArgs[type] ?: return
        val density = resources.displayMetrics.density
        val bg = GradientDrawable().apply {
            setColor(0xCC1A1A2E.toInt())
            cornerRadius = 12f * density
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.START
            setPadding((12 * density).toInt(), (6 * density).toInt(), (12 * density).toInt(), (6 * density).toInt())
            background = bg
            minimumWidth = (args.minWidth * density).toInt()
        }

        val titleText = when (type) {
            FloatingType.THREAD -> ""
            FloatingType.LOAD -> "负载"
            FloatingType.PROCESS -> "进程 TOP"
        }
        if (titleText.isNotEmpty()) {
            val titleView = TextView(this).apply {
                text = titleText
                textSize = 10f
                setTextColor(0xFFAAAAAA.toInt())
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = (2 * density).toInt() }
            }
            container.addView(titleView)
        }

        val contentView = TextView(this).apply {
            text = "--"
            textSize = 11f
            setTextColor(0xFFFFFFFF.toInt())
            setLineSpacing(2f, 1f)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        container.addView(contentView)
        container.tag = contentView
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (args.x * density).toInt()
            y = (args.y * density).toInt()
        }

        windowManager?.addView(container, params)
        floatViews[type] = container

        // 拖拽 + 点击
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isDragging = false

        container.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (kotlin.math.abs(dx) > 10 || kotlin.math.abs(dy) > 10) {
                        isDragging = true
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(container, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        val tabIndex = when (type) {
                            FloatingType.LOAD -> 0
                            FloatingType.PROCESS -> 1
                            else -> 0
                        }
                        val intent = Intent(this, MainActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            putExtra("tab_index", tabIndex)
                        }
                        startActivity(intent)
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun removeFloatingWindow(type: FloatingType) {
        floatViews[type]?.let { view ->
            try { windowManager?.removeView(view) } catch (_: Exception) {}
        }
        floatViews.remove(type)
    }

    private fun updateFloatingWindow(type: FloatingType, text: String) {
        val contentView = floatViews[type]?.tag as? TextView ?: return
        contentView.text = text
    }

    /** 统一数据采集 — 唯一数据源 */
    private fun startDataCollection() {
        serviceScope.launch {
            var processPrimed = false

            while (isActive) {
                try {
                    // 线程数据 — 只采集前台 APP 的线程
                    val threads = threadRepo.getForegroundAppThreads()
                    MonitorService._threadList.value = threads

                    // 负载数据 — 无需历史差值，立即发布
                    val cpu = loadRepo.getCpuLoad()
                    val mem = loadRepo.getMemoryInfo()
                    val thermal = ProcParser.parseThermalInfo()
                    MonitorService._cpuLoad.value = cpu
                    MonitorService._memoryInfo.value = mem
                    MonitorService._thermalInfo.value = thermal

                    // 进程数据 — 首轮采样作为基准不发布，次轮开始有 CPU 差值后再发布
                    val processes = processRepo.getProcessList()
                    if (processPrimed) {
                        MonitorService._processList.value = processes
                    } else {
                        processPrimed = true
                    }

                    // 更新悬浮窗
                    withContext(Dispatchers.Main) {
                        if (MonitorService._floatingState.value.threadVisible) {
                            val allThreads = threads.take(10).joinToString("\n") { t ->
                                val coreLabel = if (t.cpuCore >= 0) "CPU%-2d".format(t.cpuCore) else " - "
                                "%5.1f%%  %s  %s".format(t.cpuUsage, coreLabel, t.name)
                            }.ifBlank { "无数据" }
                            val pkgName = threadRepo.foregroundAppName.ifBlank { "前台APP" }
                            updateFloatingWindow(FloatingType.THREAD, "$pkgName\n$allThreads")
                        }
                        if (MonitorService._floatingState.value.loadVisible) {
                            val freqText = cpu.cores.filter { it.online }.joinToString("\n") { c ->
                                val ghz = c.frequency / 1000000f
                                "cpu%d: %s".format(c.index,
                                    if (c.frequency > 0) "%.1fGHz".format(ghz) else "-")
                            }
                            val tempText = buildString {
                                if (thermal.cpuTemp > 0) append("CPU: %.0f℃".format(thermal.cpuTemp))
                                if (thermal.batteryTemp > 0) {
                                    if (isNotEmpty()) append("\n")
                                    append("电池: %.0f℃".format(thermal.batteryTemp))
                                }
                            }
                            updateFloatingWindow(FloatingType.LOAD,
                                "CPU: %.1f%%\n内存: %.1f%%\n$freqText\n$tempText".format(cpu.totalUsage, mem.usagePercent))
                        }
                        if (MonitorService._floatingState.value.processVisible) {
                            val top10 = processes.sortedByDescending { it.cpuUsage }.take(10).joinToString("\n") { p ->
                                "%.1f%% %s".format(p.cpuUsage, p.name.take(22))
                            }.ifEmpty { "无数据" }
                            updateFloatingWindow(FloatingType.PROCESS, top10)
                        }
                    }
                } catch (_: Exception) { }
                delay(2000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Eagle Eye 监视器", NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "监视器后台运行中"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle("Eagle Eye")
        .setContentText("监视器运行中...")
        .setSmallIcon(android.R.drawable.ic_menu_info_details)
        .setOngoing(true)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setContentIntent(
            PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        )
        .build()

    private data class WindowArgs(val x: Int, val y: Int, val minWidth: Int)
}
