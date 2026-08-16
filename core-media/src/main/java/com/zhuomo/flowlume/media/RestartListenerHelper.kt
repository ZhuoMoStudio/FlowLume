package com.zhuomo.flowlume.media

import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * 通知监听服务状态检测与恢复。
 * 背景：NotificationListenerService 由系统绑定，无公开 API 强制重绑。
 * 最佳实践：权限被撤销 → 跳转系统「通知使用权」设置页引导重新开启；
 * 权限存在但服务失活 → 尝试 hidden API requestRebind（失败降级为引导手动关闭/开启）。
 */
object RestartListenerHelper {

    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompatCompat.isListenerEnabled(context)

    /**
     * @return 执行说明：是否已跳转系统设置页（true=已跳转，需用户手动操作；false=已发起重绑）
     */
    fun restart(context: Context): Boolean {
        if (!isEnabled(context)) {
            // 未授权 → 引导进入系统「通知使用权」设置页
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            NotificationCenter.emit(ListenerStatus.RESTARTING)
            return true
        }
        // 已授权但可能失活 → 尝试触发系统重绑（隐藏方法，多数 ROM 上有效）
        val rebound = runCatching {
            val nm = context.getSystemService(NotificationManager::class.java)
            val method = NotificationManager::class.java.getMethod(
                "requestRebind", ComponentName::class.java
            )
            method.invoke(nm, ComponentName(context, MediaNotificationListener::class.java))
            true
        }.getOrDefault(false)

        if (!rebound) {
            // 降级：跳设置页，引导「先关闭再开启」
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
        NotificationCenter.emit(ListenerStatus.RESTARTING)
        Log.i(TAG, "restart requested, rebound=$rebound")
        return !rebound
    }

    private const val TAG = "RestartNLS"
}
