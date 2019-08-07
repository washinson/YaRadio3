package com.washinson.yaradio3

import android.annotation.SuppressLint
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
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.github.ybq.android.spinkit.SpinKitView
import androidx.viewpager.widget.ViewPager
import java.util.*


class PlayerActivity : AppCompatActivity() {
    var session: Session? = null
    var playerService: PlayerService? = null
    var mediaController: MediaControllerCompat? = null

    lateinit var nextButton: ImageButton
    lateinit var pauseButton: ImageButton
    lateinit var likeButton: ImageButton
    lateinit var dislikeButton: ImageButton
    lateinit var settingsButton: ImageButton
    lateinit var trackCover: ImageView
    lateinit var trackLabel: TextView
    lateinit var spinKitView: SpinKitView

    @RequiresApi(Build.VERSION_CODES.KITKAT)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val adapter = PlayerAdapter(supportFragmentManager)

        val viewPager: ViewPager = findViewById(R.id.view_pager_player)
        viewPager.adapter = adapter
        viewPager.currentItem = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window1 = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this ,R.color.colorPrimary);
        };

        thread {
            session = Session.getInstance(0, this)
        }

        connectService()
        initInterface()
    }

    fun connectService() {
        /*
        startService(Intent(this, PlayerService::class.java))
        bindService(Intent(this, PlayerService::class.java),
            mConnection, Context.BIND_AUTO_CREATE)
            */
    }

    fun initInterface() {
        /*Log.d("test", "Update Interface")
        nextButton = findViewById(R.id.track_next)
        pauseButton = findViewById(R.id.track_pause)
        likeButton = findViewById(R.id.track_like)
        dislikeButton = findViewById(R.id.track_dislike)
        settingsButton = findViewById(R.id.track_settings)

        trackCover = findViewById(R.id.track_cover)
        trackLabel = findViewById(R.id.track_label)

        //spinKitView = findViewById(R.id.spin_kit)

        nextButton.setOnClickListener {
            playerService?.mediaSessionCallback?.onSkipToNext()
        }
        pauseButton.setOnClickListener {
            if (playerService?.mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING)
                playerService?.mediaSessionCallback?.onPause()
            else
                playerService?.mediaSessionCallback?.onPlay()
        }
        likeButton.setOnClickListener {

        }
        dislikeButton.setOnClickListener {

        }*/
    }

    override fun onDestroy() {
        super.onDestroy()
        //unbindService(mConnection)
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    fun updateInterface() {
        val metadata = playerService?.mediaSession?.controller?.metadata ?: return
        trackLabel.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " - " + metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        trackCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))

        if(session?.track?.liked == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(getDrawable(R.drawable.ic_liked))
            else
                likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_liked))
        }

        if(playerService?.mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(getDrawable(R.drawable.exo_controls_pause))
            else
                pauseButton.setImageDrawable(resources.getDrawable(R.drawable.exo_controls_pause))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(getDrawable(R.drawable.exo_controls_play))
            else
                pauseButton.setImageDrawable(resources.getDrawable(R.drawable.exo_controls_play))
        }
    }

    override fun onResume() {
        super.onResume()
        updateInterface()
    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            playerService = (service as PlayerService.PlayerServiceBinder).getService()
            mediaController = playerService!!.mediaSession.controller
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

    inner class PlayerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            when(position) {
                else -> { return PlayerInfoFragment()}
            }
        }

        override fun getCount() = 3
    }
}
