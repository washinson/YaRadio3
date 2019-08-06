package com.washinson.yaradio3

import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageButton
import com.washinson.yaradio3.Session.Session
import kotlin.concurrent.thread
import android.content.ComponentName
import android.content.Context
import android.media.session.MediaSession
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat


class PlayerActivity : AppCompatActivity() {
    var session: Session? = null
    var playerService: PlayerService? = null
    var mediaController: MediaControllerCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        thread {
            session = Session.getInstance(0, this)
        }

        connectService()
        initInterface()
    }

    fun connectService() {
        bindService(Intent(this, PlayerService::class.java),
            mConnection, Context.BIND_AUTO_CREATE)
    }

    fun initInterface() {
        findViewById<ImageButton>(R.id.track_next).setOnClickListener {

        }
        findViewById<ImageButton>(R.id.track_pause).setOnClickListener {

        }
        findViewById<ImageButton>(R.id.track_like).setOnClickListener {

        }
        findViewById<ImageButton>(R.id.track_dislike).setOnClickListener {

        }
    }

    fun updateInterface() {

    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            playerService = (service as PlayerService.PlayerServiceBinder).getService()
            mediaController = MediaControllerCompat(this@PlayerActivity, playerService!!.mediaSession.sessionToken)
            mediaController!!.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    updateInterface()
                }

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    updateInterface()
                }
            })
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            playerService = null
            mediaController = null
        }
    }

}
