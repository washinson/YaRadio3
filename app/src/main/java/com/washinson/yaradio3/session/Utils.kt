package com.washinson.yaradio3.session

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream


class Utils {
    companion object {
        fun getPlayId(track: Track, okHttpClient: OkHttpClient): String {
            val cookies = okHttpClient.cookieJar.loadForRequest("https://radio.yandex.ru".toHttpUrlOrNull()!!);
            var deviceId = ""
            for (cookie in cookies) {
                if (cookie.name == "device_id") {
                    deviceId = cookie.value
                }
            }
            deviceId = deviceId.replace("\"", "")
            return deviceId + ":" + track.id + ":" + Math.random().toString().substring(2);
        }

        fun decodeGZIP(bytes: ByteArray): String {
            val gis = GZIPInputStream(ByteArrayInputStream(bytes))
            val reader = InputStreamReader(gis)
            val in1 = BufferedReader(reader)
            val result = StringBuilder()
            var readed: String? = in1.readLine()
            while (readed != null) {
                result.append(readed)
                readed = in1.readLine()
            }
            return result.toString()
        }

    }
}