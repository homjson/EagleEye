package com.hom.monitor

import android.app.Application
import com.topjohnwu.superuser.Shell

class MonitorApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // 初始化 libsu Shell 环境
        Shell.enableVerboseLogging = BuildConfig.DEBUG
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )
    }
}
