package com.drawingviewer.app

import android.app.Application
import android.os.Process
import kotlin.system.exitProcess

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // 全局异常捕获，防止闪退无提示
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            try {
                // 记录异常（简单打印）
                e.printStackTrace()
            } finally {
                // 退出应用
                Process.killProcess(Process.myPid())
                exitProcess(1)
            }
        }
    }
}
