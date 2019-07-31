package com.washinson.yaradio3.Session

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.util.zip.GZIPInputStream


class Utils {
    companion object {
        fun getPlayId(track: Track, okHttpClient: OkHttpClient): String {
            var httpUrl = "https://radio.yandex.ru".toHttpUrlOrNull()
            if(httpUrl == null) return "";
            val cookies = okHttpClient.cookieJar.loadForRequest(httpUrl);
            TODO("checkIT");
            //String id = Browser.getCookieParam("device_id").replaceAll("\"", "");
            //return id + ":" + track.getId() + ":" + String.valueOf(Math.random()).substring(2);
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