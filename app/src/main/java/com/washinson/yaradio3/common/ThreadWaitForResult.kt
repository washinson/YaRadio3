package com.washinson.yaradio3.common

import kotlinx.coroutines.delay
import kotlin.Exception

class ThreadWaitForResult {
    companion object {
        suspend fun load(body: () -> Unit, onErrorDelay: Long = 1000) {
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