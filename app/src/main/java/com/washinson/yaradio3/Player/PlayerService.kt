package com.washinson.yaradio3.Player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import com.washinson.yaradio3.Session.Session
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.support.v4.media.session.MediaSessionCompat
import android.app.PendingIntent
import android.content.Context
import android.content.IntentFilter
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Build
import android.support.v4.media.session.MediaControllerCompat
import android.util.Log
import androidx.core.graphics.drawable.toBitmap
import androidx.media.session.MediaButtonReceiver
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_ENDED
import com.google.android.exoplayer2.Player.STATE_IDLE
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelector
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.washinson.yaradio3.Player.TrackNotification.Companion.refreshNotificationAndForegroundStatus
import com.washinson.yaradio3.R
import kotlinx.coroutines.*


class PlayerService : Service(), CoroutineScope {
    protected val job = SupervisorJob() // экземпляр Job для данной активности
    override val coroutineContext = Dispatchers.Main.immediate+job

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
    lateinit var mediaSession: MediaSessionCompat

    lateinit var simpleExoPlayer: SimpleExoPlayer

    var curTag: String? = null

    override fun onCreate() {
        super.onCreate()
        mediaSession = MediaSessionCompat(this, "YaRadio3")
        mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession.setCallback(mediaSessionCallback)

        val activityIntent = Intent(this, PlayerActivity::class.java)
        mediaSession.setSessionActivity(PendingIntent.getActivity(this, 0, activityIntent, 0))

        val mediaButtonIntent = Intent(Intent.ACTION_MEDIA_BUTTON,
            null, this, MediaButtonReceiver::class.java)
        mediaSession.setMediaButtonReceiver(
            PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0))

        mediaSession.controller.registerCallback(object : MediaControllerCompat.Callback() {
            override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                refreshNotificationAndForegroundStatus(
                    mediaSession.controller.playbackState.state, mediaSession,
                    this@PlayerService, Session.getInstance(0, this@PlayerService).track)
            }

            override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                refreshNotificationAndForegroundStatus(
                    mediaSession.controller.playbackState.state, mediaSession,
                    this@PlayerService, Session.getInstance(0, this@PlayerService).track)
            }
        })

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        val videoTrackSelectionFactory: TrackSelection.Factory = AdaptiveTrackSelection.Factory()
        val trackSelector: TrackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        simpleExoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector)
        simpleExoPlayer.addListener(eventListener)

        registerReceiver(mediaSessionCallback.likeReceiver, IntentFilter(MediaSessionCallback.likeIntentFilter))
        registerReceiver(mediaSessionCallback.dislikeReceiver, IntentFilter(MediaSessionCallback.dislikeIntentFilter))

        mediaSessionCallback.onPlay()
        setLoadingContent()
        startTag()

        launch(Dispatchers.IO) {
            while (true) {
                launch(Dispatchers.Main) {
                    if (simpleExoPlayer.playWhenReady) {
                        mediaSession.setPlaybackState(
                            stateBuilder.setState(
                                mediaSession.controller.playbackState.state,
                                simpleExoPlayer.currentPosition, 1F).build())
                    }
                }
                delay(1000)
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        super.onTaskRemoved(rootIntent)
        mediaSessionCallback.onStop()
    }

    fun onTrackLoaded() {
        val session = Session.getInstance(0, this)
        val track = session.track ?: return
        Glide.with(this).load(track.getCoverSize(600, 600)).into(object : CustomTarget<Drawable>() {
            override fun onLoadCleared(placeholder: Drawable?) {

            }

            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable>?) {
                val metadata = metadataBuilder
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
                    .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, track.tag.name)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
                    .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, resource.toBitmap())
                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
                    .build()
                mediaSession.setMetadata(metadata)
            }
        })
        val metadata = metadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track.title)
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, track.tag.name)
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track.artist)
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, track.album)
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, track.durationMs)
            .build()
        mediaSession.setMetadata(metadata)
    }

    fun prepareTrack(downloadPath: String) {
        onTrackLoaded()
        val dataSourceFactory = DefaultHttpDataSourceFactory(Session.getInstance(0, this@PlayerService).manager.browser)
        val mediaSource = ProgressiveMediaSource.Factory(dataSourceFactory)
            .createMediaSource(android.net.Uri.parse(downloadPath))

        simpleExoPlayer.prepare(mediaSource)
    }

    fun startTag() {
        mediaSessionCallback.onPause()
        setLoadingContent()
        launch(Dispatchers.IO) {
            val session = Session.getInstance(0, this@PlayerService)
            val url = session.startTrack()

            launch(Dispatchers.Main) {
                prepareTrack(url)
                mediaSessionCallback.onPlay()
            }
        }
    }

    fun setLoadingContent() {
        mediaSession.setMetadata(metadataBuilder
            .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getString(R.string.loading))
            .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION, getString(R.string.loading))
            .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, getString(R.string.loading))
            .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, getString(R.string.loading))
            .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, 100)
            .putBitmap(MediaMetadataCompat.METADATA_KEY_ART, null)
            .build())
    }

    fun nextTrack(finished: Boolean, duration: Double) {
        mediaSessionCallback.onPause()
        setLoadingContent()
        launch(Dispatchers.IO) {
            val session = Session.getInstance(0, this@PlayerService)
            session.nextTrack(finished, duration)
            val url = session.startTrack()
            launch(Dispatchers.Main) {
                prepareTrack(url)
                mediaSessionCallback.onPlay()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession.release()
        simpleExoPlayer.release()
        unregisterReceiver(mediaSessionCallback.likeReceiver)
        unregisterReceiver(mediaSessionCallback.dislikeReceiver)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        MediaButtonReceiver.handleIntent(mediaSession, intent)

        hackNotificationForStartForeground()

        if (curTag == null) {
            curTag = intent?.getStringExtra("tag")
        } else {
            if (intent?.getStringExtra("tag") != null
                && intent.getStringExtra("tag") != curTag) {
                curTag = intent.getStringExtra("tag")
                startTag()
            }
        }

        return START_STICKY
    }

    fun hackNotificationForStartForeground() {
        val notification = TrackNotification.getNotification(
            mediaSession.controller.playbackState.state,
            mediaSession,
            this,
            Session.getInstance(0, this).track
        ) ?: return
        val mNotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && mNotificationManager != null) {
            val mChannel = NotificationChannel(
                TrackNotification.channelID, "YaRadio3",
                NotificationManager.IMPORTANCE_LOW
            )
            mNotificationManager.createNotificationChannel(mChannel)
        }

        startForeground(TrackNotification.NOTIFICATION_ID, notification)
        refreshNotificationAndForegroundStatus(
            mediaSession.controller.playbackState.state,
            mediaSession,
            this,
            Session.getInstance(0, this).track)
    }

    val eventListener = object : Player.EventListener {
        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            when(playbackState) {
                //STATE_IDLE -> mediaSessionCallback.onSkipToNext()
                STATE_ENDED -> {
                    val time = simpleExoPlayer.currentPosition
                    nextTrack(true, time / 1000.0)
                }
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
