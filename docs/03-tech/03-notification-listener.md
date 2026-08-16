# 技术文档 03 · NotificationListenerService 模块与后台保活

## 1. 模块职责
- 监听系统媒体通知，提取歌曲信息与**专辑封面 Bitmap**；
- 判定播放/暂停状态，通过 `ArtBus` 广播封面更新；
- 提供权限状态检测与「一键重启监听服务」能力。

## 2. 核心实现（Kotlin 骨架）

```kotlin
// :core-media 模块
class MediaNotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "Listener connected")
        NotificationCenter.status.emit(ListenerStatus.CONNECTED)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        val notif = sbn.notification ?: return
        if (sbn.isOngoing.not()) return
        if (!isMediaNotification(notif)) return          // 快速过滤

        val extras = notif.extras
        // ① 优先走 MediaSession Token → MediaController 取完整元数据（最可靠）
        val sessionToken: MediaSession.Token? = extras.getParcelable(Notification.EXTRA_MEDIA_SESSION)
        if (sessionToken != null) {
            val controller = MediaControllerManager.get(this, sessionToken)
            val meta = controller.metadata
            val state = controller.playbackState
            val artwork = meta?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                       ?: meta?.getBitmap(MediaMetadata.METADATA_KEY_ART)
            if (meta != null) {
                ArtBus.emit(
                    ArtEvent(
                        title = meta.getString(MediaMetadata.METADATA_KEY_TITLE),
                        artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST),
                        album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM),
                        artwork = artwork?.scaleDown(512),   // 限制内存
                        isPlaying = state?.isActive == true
                    )
                )
                return
            }
        }
        // ② 兜底：直接读 extras 中的 artwork（部分播放器仅提供此路径）
        val art: Bitmap? = extras.getParcelable(Notification.EXTRA_PICTURE)
            ?: (extras.getParcelable(Notification.EXTRA_LARGE_ICON) as? Bitmap)
        if (art != null) ArtBus.emit(ArtEvent(artwork = art.scaleDown(512), isPlaying = true))
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 播放器通知被移除 → 按设置决定是否保留上次封面
        if (!ConfigStore.load(Mode.WALLPAPER).keepArtOnPause) {
            ArtBus.emit(ArtEvent(artwork = null, isPlaying = false))
        }
    }

    private fun isMediaNotification(n: Notification): Boolean {
        // MediaStyle 判定：category == CATEGORY_TRANSPORT 或存在 EXTRA_MEDIA_SESSION
        return n.category == Notification.CATEGORY_TRANSPORT ||
               n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION) ||
               n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION2)
    }
}
```

### Manifest 声明（关键）
```xml
<service
    android:name=".media.MediaNotificationListener"
    android:label="@string/nls_label"
    android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"
    android:exported="false">
    <intent-filter>
        <action android:name="android.service.notification.NotificationListenerService" />
    </intent-filter>
</service>
```

## 3. 兼容性矩阵

| 场景 | 行为 | 处理 |
|------|------|------|
| Apple Music / Spotify / YT Music / SoundCloud / Google Play Music | 标准 MediaStyle 通知 | 自动提取 ✅ |
| QQ音乐 / 网易云 / 酷狗等国产播放器 | 自定义非标准通知样式 | 无法提取 → 帮助页引导切换「系统原生通知样式」 |
| 折叠/静默通知 | `onNotificationPosted` 可能延迟 | 提示用户保持通知栏可见 |
| Android 13+ 通知权限被拒 | 媒体通知不显示 | 引导开启 `POST_NOTIFICATIONS` |
| 息屏深度睡眠 | 监听被系统冻结 | 见保活策略 |

## 4. 后台保活与电源优化策略

**背景事实**：`NotificationListenerService` 由系统绑定（Bound Service），开发者**无法**通过 `START_STICKY` 保活；系统（尤其国产 ROM）会回收权限或冻结进程。策略组合：

1. **权限自检（WorkManager 周期任务）**
   ```kotlin
   class ListenerHealthCheck : CoroutineWorker(...) {
       override suspend fun doWork(): Result {
           val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
               .contains(context.packageName)
           NotificationCenter.status.emit(if (enabled) CONNECTED else REVOKED)
           return Result.retry()  // 周期重跑，指数退避上限 4h
       }
   }
   ```
2. **开机/解锁恢复**：`BOOT_COMPLETED` + `USER_PRESENT` 广播接收器 → 触发 WorkManager 立即自检；
3. **一键重启监听服务**（`Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS` 不可编程触发重绑，标准做法）：
   - App 内提供按钮 → 跳转系统「通知使用权」设置页，文案引导「关闭后再开启」；
   - 同时用 hidden API 尝试 `NotificationManager.requestRebind(ComponentName)`（反射，降级处理）；
   - 回跳后 WorkManager 自检确认状态 → Snackbar 反馈；
4. **电池优化白名单引导**：跳转 `ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`（需权限声明），或引导进入厂商省电管理页（按品牌 Intent 映射：MIUI/EMUI/ColorOS 等）；
5. **前台服务补强（可选）**：Android 14+ 声明 `FOREGROUND_SERVICE_MEDIA_PLAYBACK` 类型前台服务，避免进程整体被杀（与媒体场景相符）；
6. **多进程守护（可选）**：`android:process=":listener"` 独立进程隔离，避免渲染崩溃连带杀死监听（需评估内存开销）。

## 5. 常见故障自查清单（写入帮助页）
- 通知权限是否被回收 → 一键重启 + 自检；
- 播放器通知是否被系统折叠 → 解锁后重试；
- 国产播放器 → 切换系统原生通知样式；
- 省电冻结 → 电池白名单引导；
- 仍失败 → 反馈日志（`dumpsys notification --noredact` 可协助定位，调试视图展示解析状态）。
