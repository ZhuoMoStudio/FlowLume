package com.zhuomo.flowlume.media

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/**
 * 通知监听服务状态检测与恢复（应用内重启，竞品同款）。
 *
 * 权限存在但服务失活：通过 PackageManager 禁用→启用组件，强制系统解绑后重新绑定，
 * 全程不离开应用（竞品 Diffuse 的做法）。
 * 权限被撤销：才跳转系统「通知使用权」设置页。
 */
object RestartListenerHelper {

    enum class RestartResult { RESTARTED_IN_APP, JUMPED_TO_SETTINGS }

    fun isEnabled(context: Context): Boolean =
        NotificationManagerCompatCompat.isListenerEnabled(context)

    fun restart(context: Context): RestartResult {
        if (!isEnabled(context)) {
            // 权限被撤销 → 引导进入系统「通知使用权」设置页
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            NotificationCenter.emit(ListenerStatus.RESTARTING)
            return RestartResult.JUMPED_TO_SETTINGS
        }

        // 应用内强制重绑：先禁用（系统解绑）→ 再启用（系统重新绑定）
        val component = ComponentName(context, MediaNotificationListener::class.java)
        val pm = context.packageManager
        runCatching {
            pm.setComponentEnabledSetting(
                component,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
            Handler(Looper.getMainLooper()).postDelayed({
                runCatching {
                    pm.setComponentEnabledSetting(
                        component,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            }, 300)
        }.onFailure { e ->
            Log.e(TAG, "in-app rebind failed: ${e.message}")
        }
        NotificationCenter.emit(ListenerStatus.RESTARTING)
        return RestartResult.RESTARTED_IN_APP
    }

    private const val TAG = "RestartNLS"
}
