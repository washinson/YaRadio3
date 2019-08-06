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









class PlayerService : Service() {

    var session: Session? = null
    val metadataBuilder = MediaMetadataCompat.Builder()
    val stateBuilder:PlaybackStateCompat.Builder =
        PlaybackStateCompat.Builder().setActions(
            PlaybackStateCompat.ACTION_PLAY
            or PlaybackStateCompat.ACTION_STOP
            or PlaybackStateCompat.ACTION_PAUSE
            or PlaybackStateCompat.ACTION_PLAY_PAUSE
            or PlaybackStateCompat.ACTION_SKIP_TO_NEXT
            or PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS )
    val mediaSessionCallback = MediaSessionCallback()
    var mediaSession: MediaSessionCompat? = null

    override fun onCreate() {
        super.onCreate();
        mediaSession = MediaSessionCompat(this, "YaRadio3")
        mediaSession!!.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
        mediaSession!!.setCallback(mediaSessionCallback)

        val activityIntent = Intent(this, PlayerActivity::class.java)
        mediaSession!!.setSessionActivity(PendingIntent.getActivity(this, 0, activityIntent, 0))

        thread {
            session = Session.getInstance(0, this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaSession?.release()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder {
        return PlayerServiceBinder()
    }

    class PlayerServiceBinder : Binder() {

    }
}
