package com.zhuomo.flowlume.media

import android.app.Notification
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Build
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.zhuomo.flowlume.config.ConfigStore
import com.zhuomo.flowlume.config.Mode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 核心模块：监听系统媒体通知，提取专辑封面。
 * 兼容 Apple Music / Spotify / YouTube Music / SoundCloud 等标准 MediaStyle 通知；
 * 国产播放器使用自定义样式时无法提取，由帮助文档引导切换系统原生通知样式。
 */
class MediaNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var sessionManager: MediaSessionManager

    override fun onListenerConnected() {
        super.onListenerConnected()
        sessionManager = getSystemService(MediaSessionManager::class.java)
        NotificationCenter.emit(ListenerStatus.CONNECTED)
        Log.i(TAG, "Media notification listener connected")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification, rankingMap: RankingMap?) {
        if (!sbn.isOngoing) return
        val notif = sbn.notification ?: return
        if (!isMediaNotification(notif)) return
        scope.launch { parseAndEmit(sbn, notif) }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // 按设置决定是否保留上次封面
        val keep = ConfigStore.current(Mode.WALLPAPER).keepArtOnPause
        if (!keep) {
            ArtBus.emit(ArtEvent(artwork = null, isPlaying = false))
        }
    }

    override fun onListenerDisconnected() {
        NotificationCenter.emit(ListenerStatus.REVOKED)
        super.onListenerDisconnected()
    }

    @Suppress("DEPRECATION")
    private suspend fun parseAndEmit(sbn: StatusBarNotification, notif: Notification) {
        val extras = notif.extras
        // ① 优先：MediaSession Token → MediaController 完整元数据（含大图封面）
        val sessionToken: android.media.session.MediaSession.Token? =
            extras.parcelableCompat(Notification.EXTRA_MEDIA_SESSION)
        if (sessionToken != null) {
            val controller = MediaController(this, sessionToken)
            val meta = controller.metadata ?: return
            val state = controller.playbackState
            val artwork = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ArtBus.emit(
                ArtEvent(
                    title = meta.getString(MediaMetadata.METADATA_KEY_TITLE),
                    artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST),
                    album = meta.getString(MediaMetadata.METADATA_KEY_ALBUM),
                    artwork = artwork?.scaleDown(512),
                    isPlaying = state?.isActive == true
                )
            )
            return
        }
        // ② 兜底：直接读 extras 中的 artwork
        val art: Bitmap? = extras.parcelableCompat(Notification.EXTRA_PICTURE)
            ?: extras.parcelableCompat<Bitmap>(Notification.EXTRA_LARGE_ICON)
        if (art != null) {
            ArtBus.emit(ArtEvent(artwork = art.scaleDown(512), isPlaying = true))
        }
    }

    private fun isMediaNotification(n: Notification): Boolean =
        n.category == Notification.CATEGORY_TRANSPORT ||
            n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

    private fun Bitmap.scaleDown(maxEdge: Int): Bitmap {
        val w = width; val h = height
        if (w <= maxEdge && h <= maxEdge) return this
        val scale = maxEdge.toFloat() / maxOf(w, h)
        return Bitmap.createScaledBitmap(this, (w * scale).toInt(), (h * scale).toInt(), true)
    }

    companion object {
        private const val TAG = "FlowLumeNLS"
    }
}

/** getParcelable 跨版本兼容：API 33+ 用类型安全重载，旧版本用弃用 API */
@Suppress("DEPRECATION")
private inline fun <reified T : android.os.Parcelable> Bundle.parcelableCompat(key: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        getParcelable(key)
    }
