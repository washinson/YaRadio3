package com.washinson.yaradio3.common

import android.accounts.NetworkErrorException
import kotlinx.coroutines.delay
import kotlin.Exception

class ThreadWaitForResult {
    companion object {
        suspend fun load(onErrorDelay: Long = 1000, body: () -> Unit) {
            while(true) {
                try {
                    body()
                    break
                } catch (e: Exception) {
                    e.printStackTrace()
                    delay(onErrorDelay)
                }
            }
        }
    }
}