package com.washinson.yaradio3.Player

import android.app.Notification
import androidx.core.content.ContextCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.session.MediaButtonReceiver
import android.content.Intent
import android.app.PendingIntent
import android.content.Context
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import com.washinson.yaradio3.Session.Track
import androidx.core.app.NotificationManagerCompat
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import com.washinson.yaradio3.R

class TrackNotification {
    companion object {
        val NOTIFICATION_ID = 1
        val channelID = "1"
        fun refreshNotificationAndForegroundStatus(playbackState: Int, mediaSession: MediaSessionCompat, playerService: PlayerService, track: Track?) {
            when (playbackState) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    val notification = getNotification(
                        playbackState,
                        mediaSession,
                        playerService,
                        track
                    ) ?: return
                    val mNotificationManager =
                        playerService.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNotificationManager != null) {
                        val mChannel = NotificationChannel(
                            channelID, "YaRadio3",
                            NotificationManager.IMPORTANCE_DEFAULT
                        )
                        mNotificationManager.createNotificationChannel(mChannel)
                    }

                    playerService.startForeground(NOTIFICATION_ID, notification)
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    // На паузе мы перестаем быть foreground, однако оставляем уведомление,
                    // чтобы пользователь мог play нажать
                    val notification = getNotification(
                        playbackState,
                        mediaSession,
                        playerService,
                        track
                    ) ?: return
                    NotificationManagerCompat.from(playerService).notify(NOTIFICATION_ID, notification)
                    playerService.stopForeground(false)
                }
                else -> {
                    // Все, можно прятать уведомление
                    playerService.stopForeground(true)
                }
            }
        }
        private fun getNotification(playbackState: Int, mediaSession: MediaSessionCompat, context: Context, track: Track?): Notification? {
            val builder = NotificationCompat.Builder(context,
                channelID
            )

            val controller = mediaSession.controller
            val mediaMetadata = controller.metadata ?: return null
            val description = mediaMetadata.description

            builder.setContentTitle(description.title)
                .setContentText(description.subtitle)
                .setLargeIcon(description.iconBitmap)
                .setSubText(mediaMetadata.getText(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION))
                .setContentIntent(controller.sessionActivity)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDeleteIntent(
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_STOP
                    )
                )
                .setChannelId(channelID)
                .setSmallIcon(R.drawable.ic_music)

            // ...play/pause
            if (playbackState == PlaybackStateCompat.STATE_PLAYING)
                builder.addAction(
                    NotificationCompat.Action(
                        R.drawable.exo_icon_pause, context.getString(R.string.pause),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                    )
                )
            else
                builder.addAction(
                    NotificationCompat.Action(
                        R.drawable.exo_icon_play, context.getString(R.string.play),
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_PLAY_PAUSE
                        )
                    )
                )

            // ...на следующий трек
            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.exo_icon_next, context.getString(R.string.next),
                    MediaButtonReceiver.buildMediaButtonPendingIntent(
                        context,
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                    )
                )
            )

            builder.addAction(
                NotificationCompat.Action(
                    R.drawable.ic_dislike, context.getString(R.string.dislike_track),
                    PendingIntent.getBroadcast(context, 0, Intent(MediaSessionCallback.dislikeIntentFilter), 0)
                )
            )

            builder.addAction(
                NotificationCompat.Action(
                    if (track?.liked == true) R.drawable.ic_liked else R.drawable.ic_like, context.getString(
                        R.string.like_track
                    ),
                    PendingIntent.getBroadcast(context, 0, Intent(MediaSessionCallback.likeIntentFilter), 0)
                )
            )

            builder.setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    // В компактном варианте показывать Action с данным порядковым номером.
                    .setShowActionsInCompactView(0, 1, 3)
                    // Отображать крестик в углу уведомления для его закрытия.
                    // Это связано с тем, что для API < 21 из-за ошибки во фреймворке
                    // пользователь не мог смахнуть уведомление foreground-сервиса
                    // даже после вызова stopForeground(false).
                    // Так что это костыль.
                    // На API >= 21 крестик не отображается, там просто смахиваем уведомление.
                    .setShowCancelButton(true)
                    // Указываем, что делать при нажатии на крестик или смахивании
                    .setCancelButtonIntent(
                        MediaButtonReceiver.buildMediaButtonPendingIntent(
                            context,
                            PlaybackStateCompat.ACTION_STOP
                        )
                    )
                    // Передаем токен. Это важно для Android Wear. Если токен не передать,
                    // кнопка на Android Wear будет отображаться, но не будет ничего делать
                    .setMediaSession(mediaSession.sessionToken)
            )

            builder.color = ContextCompat.getColor(context, R.color.colorPrimaryDark)

            // Не отображать время создания уведомления. В нашем случае это не имеет смысла
            builder.setShowWhen(false)

            // Это важно. Без этой строчки уведомления не отображаются на Android Wear
            // и криво отображаются на самом телефоне.
            builder.priority = NotificationCompat.PRIORITY_HIGH

            // Не надо каждый раз вываливать уведомление на пользователя
            builder.setOnlyAlertOnce(true)
            return builder.build()
        }
    }
}