package com.washinson.yaradio3

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.washinson.yaradio3.Session.Session
import kotlin.concurrent.thread

class PlayerActivity : AppCompatActivity() {
    var session: Session? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        thread {
            session = Session.getInstance(0, this)
        }
    }
}
