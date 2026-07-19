package com.example.schedule.util

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object DebugLog {

    private var logFile: File? = null
    private val fmt = DateTimeFormatter.ofPattern("MM-dd HH:mm:ss")

    fun init(context: Context) {
        logFile = File(context.getExternalFilesDir(null), "debug.log")
        logFile?.parentFile?.mkdirs()
        w("DebugLog", "=== init ===")
    }

    fun w(tag: String, msg: String) {
        val line = "[${LocalDateTime.now().format(fmt)}] $tag: $msg"
        android.util.Log.w("Schedule", line)
        try {
            logFile?.appendText("$line\n")
        } catch (_: Exception) {}
    }

    fun e(tag: String, msg: String, tr: Throwable? = null) {
        val line = "[${LocalDateTime.now().format(fmt)}] E $tag: $msg"
        if (tr != null) {
            val stack = android.util.Log.getStackTraceString(tr)
            android.util.Log.e("Schedule", line + "\n" + stack)
            try { logFile?.appendText("$line\n$stack\n") } catch (_: Exception) {}
        } else {
            android.util.Log.e("Schedule", line)
            try { logFile?.appendText("$line\n") } catch (_: Exception) {}
        }
    }
}
