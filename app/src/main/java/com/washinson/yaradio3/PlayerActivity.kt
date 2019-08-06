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
import android.os.IBinder



class PlayerActivity : AppCompatActivity() {
    var session: Session? = null
    var serviceBinder: PlayerService.PlayerServiceBinder? = null

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
        val intent = Intent(this, PlayerService::class.java)
        startService(intent)
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE)
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

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            serviceBinder = service as PlayerService.PlayerServiceBinder
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            serviceBinder = null
        }
    }

}
