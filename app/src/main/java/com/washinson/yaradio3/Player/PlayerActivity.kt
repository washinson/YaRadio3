package com.washinson.yaradio3.Player

import android.content.Intent
import android.content.ServiceConnection
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.washinson.yaradio3.Session.Session
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.os.IBinder
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import com.washinson.yaradio3.R
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class PlayerActivity : AppCompatActivity() {
    var session: Session? = null
    var playerService: PlayerService? = null
    var mediaController: MediaControllerCompat? = null


    val playerInfoFragment = PlayerInfoFragment()
    val playerHistoryFragment = PlayerHistoryFragment()
    val playerNextFragment = PlayerNextFragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        val adapter = PlayerAdapter(supportFragmentManager)

        val viewPager: ViewPager = findViewById(R.id.view_pager_player)
        viewPager.adapter = adapter
        viewPager.currentItem = 1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this , R.color.colorPrimary)
        }

        GlobalScope.launch {
            session = Session.getInstance(0, this@PlayerActivity)
        }

        connectService()
    }

    fun connectService() {
        val intent1 = Intent(this, PlayerService::class.java)
        intent1.putExtra("tag", intent.getStringExtra("tag"))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent1)
            bindService(Intent(this, PlayerService::class.java),
                mConnection, Context.BIND_AUTO_CREATE)
        } else {
            startService(intent1)
            bindService(Intent(this, PlayerService::class.java),
                mConnection, Context.BIND_AUTO_CREATE)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(mConnection)
    }

    override fun onResume() {
        super.onResume()
    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            playerService = (service as PlayerService.PlayerServiceBinder).getService()
            mediaController = playerService!!.mediaSession.controller
            mediaController!!.registerCallback(object : MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    if (state?.state == PlaybackStateCompat.STATE_STOPPED) {
                        finish()
                    }
                    playerInfoFragment.updateOnPlaybackState(state ?: return)
                }

                override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
                    playerInfoFragment.updateOnMedatada(metadata ?: return)
                    playerHistoryFragment.adapter.onMetadataUpdate()
                    playerNextFragment.adapter.onMetadataUpdate()
                }
            })
            playerInfoFragment.onServiceConnected(playerService ?: return)
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            playerService = null
            mediaController = null
        }
    }

    inner class PlayerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getItem(position: Int): Fragment {
            return when (position) {
                0 -> playerHistoryFragment
                1 -> playerInfoFragment
                else -> playerNextFragment
            }
        }

        override fun getCount() = 3
    }
}
