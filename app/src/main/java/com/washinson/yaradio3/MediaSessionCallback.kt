package com.washinson.yaradio3

import android.support.v4.media.session.MediaSessionCompat
import android.media.AudioManager
import android.content.Intent
import android.content.IntentFilter
import android.content.BroadcastReceiver
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import com.washinson.yaradio3.Session.Session
import com.washinson.yaradio3.TrackNotification.Companion.refreshNotificationAndForegroundStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MediaSessionCallback(val service: PlayerService) : MediaSessionCompat.Callback() {
    companion object {
        val likeIntentFilter = "com.washinson.yaradio3.like_broadcast"
        val dislikeIntentFilter = "com.washinson.yaradio3.dislike_broadcast"
    }

    lateinit var mFocusRequest: AudioFocusRequest

    override fun onPause() {
        try{
            service.unregisterReceiver(becomingNoisyReceiver)
        } catch (e: IllegalArgumentException) {}
        service.simpleExoPlayer.playWhenReady = false

        service.mediaSession.setPlaybackState(
            service.stateBuilder.setState(
                PlaybackStateCompat.STATE_PAUSED,
                service.simpleExoPlayer.currentPosition, 1F).build())

        val track = Session.getInstance(0, service).track ?: return
        refreshNotificationAndForegroundStatus(PlaybackStateCompat.STATE_PAUSED, service.mediaSession, service, track)
    }

    override fun onSkipToNext() {
        service.nextTrack(false, service.simpleExoPlayer.currentPosition / 1000.0)
    }

    @Suppress("DEPRECATION")
    override fun onPlay() {
        val audioFocusResult: Int
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            audioFocusResult = service.audioManager!!.requestAudioFocus(
                audioFocusChangeListener,
                AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN)
        } else {
            val audioAttributes = AudioAttributes.Builder()
                // Собираемся воспроизводить звуковой контент
                // (а не звук уведомления или звонок будильника)
                .setUsage(AudioAttributes.USAGE_MEDIA)
                // ...и именно музыку (а не трек фильма или речь)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            mFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setOnAudioFocusChangeListener(audioFocusChangeListener)
                // Если получить фокус не удалось, ничего не делаем
                // Если true - нам выдадут фокус как только это будет возможно
                // (например, закончится телефонный разговор)
                .setAcceptsDelayedFocusGain(false)
                // Вместо уменьшения громкости собираемся вставать на паузу
                .setWillPauseWhenDucked(true)
                .setAudioAttributes(audioAttributes)
                .build()
            audioFocusResult = service.audioManager!!
                .requestAudioFocus(mFocusRequest)
        }

        if (audioFocusResult != AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
            return

        // Аудиофокус надо получить строго до вызова setActive!
        service.mediaSession.isActive = true

        service.registerReceiver(becomingNoisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        service.mediaSession.setPlaybackState(
            service.stateBuilder.setState(
                PlaybackStateCompat.STATE_PLAYING,
                service.simpleExoPlayer.currentPosition, 1F).build())

        service.simpleExoPlayer.playWhenReady = true
    }

    @Suppress("DEPRECATION")
    override fun onStop() {
        Log.d("wtf", "onstop")
        try{
            service.unregisterReceiver(becomingNoisyReceiver)
        } catch (e: IllegalArgumentException) {}

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            service.audioManager!!.abandonAudioFocus(audioFocusChangeListener);
        } else {
            service.audioManager!!.abandonAudioFocusRequest(mFocusRequest)
        }

        service.simpleExoPlayer.playWhenReady = false
        service.simpleExoPlayer.stop(true)
        service.simpleExoPlayer.release()

        service.mediaSession.setPlaybackState(
            service.stateBuilder.setState(
                PlaybackStateCompat.STATE_STOPPED,
                PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN, 1F).build())
        Log.d("wtf", "onstop -- end")
        service.stopSelf()
    }

    private val audioFocusChangeListener = AudioManager.OnAudioFocusChangeListener { focusChange ->
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN ->
                // Фокус предоставлен.
                // Например, был входящий звонок и фокус у нас отняли.
                // Звонок закончился, фокус выдали опять
                // и мы продолжили воспроизведение.
                onPlay()
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK ->
                // Фокус отняли, потому что какому-то приложению надо
                // коротко "крякнуть".
                // Например, проиграть звук уведомления или навигатору сказать
                // "Через 50 метров поворот направо".
                // В этой ситуации нам разрешено не останавливать вопроизведение,
                // но надо снизить громкость.
                // Приложение не обязано именно снижать громкость,
                // можно встать на паузу, что мы здесь и делаем.
                onPause()
            else ->
                // Фокус совсем отняли.
                onPause()
        }
    }

    private val becomingNoisyReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (AudioManager.ACTION_AUDIO_BECOMING_NOISY == intent.action) {
                service.mediaSessionCallback.onPause()
            }
        }
    }

    val likeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val position = service.simpleExoPlayer.currentPosition / 1000.0
            service.launch(Dispatchers.IO) {
                val session = Session.getInstance(0, service)
                val track = session.track ?: return@launch
                if (session.track?.liked == false) {
                    session.like(track, position)
                    track.liked = true
                } else {
                    session.unlike(track, position)
                    track.liked = false
                }

                service.mediaSession.setMetadata(service.mediaSession.controller.metadata)
            }
        }
    }


    val dislikeReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val position = service.simpleExoPlayer.currentPosition / 1000.0
            service.launch(Dispatchers.IO) {
                val session = Session.getInstance(0, service)
                val track = session.track ?: return@launch
                session.dislike(track, position)

                onSkipToNext()
            }
        }
    }
}