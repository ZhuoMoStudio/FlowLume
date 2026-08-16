package com.zhuomo.flowlume.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/** 一键重启通知监听服务（解决权限被回收 / 服务失活故障） */
object RestartListenerHelper {

    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompatCompat.isListenerEnabled(context)

    fun restart(context: Context) {
        if (!isEnabled(context)) {
            // 未授权 → 引导进入系统「通知使用权」设置页
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            NotificationCenter.emit(ListenerStatus.RESTARTING)
            return
        }
        // 已授权但服务失活 → 尝试触发系统重绑（API 31+ 隐藏方法，失败降级）
        val rebound = runCatching {
            val nm = context.getSystemService(NotificationManager::class.java)
            val method = NotificationManager::class.java.getMethod(
                "requestRebind", ComponentName::class.java
            )
            method.invoke(nm, ComponentName(context, MediaNotificationListener::class.java))
            true
        }.getOrDefault(false)

        if (!rebound) {
            // 降级：跳设置页引导手动关闭/开启
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        NotificationCenter.emit(ListenerStatus.RESTARTING)
        Log.i(TAG, "restart requested, rebound=$rebound")
    }

    private const val TAG = "RestartNLS"
}
