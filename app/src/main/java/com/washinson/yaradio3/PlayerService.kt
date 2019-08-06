package com.washinson.yaradio3

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.washinson.yaradio3.Session.Session
import kotlin.concurrent.thread
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.MediaSessionCompat
import android.app.PendingIntent
import android.content.Context
import android.media.AudioManager
import androidx.media.session.MediaButtonReceiver
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_READY
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.washinson.yaradio3.Session.Manager
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.upstream.HttpDataSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.BandwidthMeter
import com.washinson.yaradio3.TrackNotification.Companion.refreshNotificationAndForegroundStatus


class PlayerService : Service() {
    var audioManager: AudioManager? = null
    val metadataBuilder = MediaMetadataCompat.Builder()
    val stateBuilder:PlaybackStateCompat.Builder =
        PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS )
    val mediaSessionCallback = MediaSessionCallback(this)
    var mediaSession: MediaSessionCompat = MediaSessionCompat(this, "YaRadio3")

    var videoTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory()
    var trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
    val simpleExoPlayer: SimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

    override fun onCreate() {
        super.onCreate();
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(mediaSessionCallback)

        val activityIntent = Intent(this, PlayerActivity::class.java)
        mediaSession.setSessionActivity(PendingIntent.getActivity(this, 0, activityIntent, 0))

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON,
            null, this, MediaButtonReceiver::class.java)
        mediaSession.setMediaButtonReceiver(
            PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0))

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        startTag()
    }

    fun onTrackLoaded() {
        val session = Session.getInstance(0, this)
        val track = session.track
        if (track == null)
            return
        val metadata = metadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, track.tag.name)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            //.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap)
            .build()
        mediaSession.setMetadata(metadata)
        refreshNotificationAndForegroundStatus(mediaSession.controller.playbackState.state, mediaSession, this, track);
    }

    fun startTrack(downloadPath: String) {
        onTrackLoaded()
        val dataSourceFactory = DefaultHttpDataSourceFactory(Session.getInstance(0, this).manager.browser)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(android.net.Uri.parse(downloadPath))

        simpleExoPlayer.prepare(mediaSource);
        simpleExoPlayer.playWhenReady = true;
    }

    fun startTag() {
        if (simpleExoPlayer.playWhenReady)
            simpleExoPlayer.stop()
        thread {
            val session = Session.getInstance(0, this)
            startTrack(session.startTrack())
        }
    }

    fun nextTrack(finished: Boolean, duration: Double) {
        thread {
            val session = Session.getInstance(0, this)
            session.nextTrack(finished, duration)
            startTrack(session.startTrack())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
        simpleExoPlayer.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    val eventListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            if (playbackState == STATE_ENDED) {
                val time = simpleExoPlayer.currentPosition
                nextTrack(true, time / 1000.0)
            }
        }
    }

    override fun onBind(intent: Intent): IBinder {
        return PlayerServiceBinder()
    }

    inner class PlayerServiceBinder : Binder() {
        fun getService(): PlayerService {
            return this@PlayerService
        }
    }
}
