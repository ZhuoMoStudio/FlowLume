package com.zhuomo.flowlume.media

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.zhuomo.flowlume.config.ConfigStore
import java.util.concurrent.TimeUnit

/**
 * 保活自检：周期检查通知监听权限是否被系统回收。
 * NotificationListenerService 由系统绑定，无法 START_STICKY，
 * 采用 WorkManager 周期自检 + 广播补检的组合策略。
 */
class ListenerHealthCheck(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val enabled = NotificationManagerCompatCompat.isListenerEnabled(applicationContext)
        NotificationCenter.emit(if (enabled) ListenerStatus.CONNECTED else ListenerStatus.REVOKED)
        return Result.success()
    }

    companion object {
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ListenerHealthCheck>(6, TimeUnit.HOURS)
                .setInitialDelay(5, TimeUnit.MINUTES)
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "flowlume_listener_health",
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

/** 轻量封装：读取通知使用权列表 */
object NotificationManagerCompatCompat {
    fun isListenerEnabled(context: Context): Boolean {
        val flat = android.provider.Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return flat.split(":").any { it.startsWith(context.packageName) }
    }
}
