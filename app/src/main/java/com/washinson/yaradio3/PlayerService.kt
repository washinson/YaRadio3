package com.washinson.yaradio3

import android.app.Service
import android.content.Intent
import android.os.IBinder

class PlayerService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("Return the communication channel to the service.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }
}
