package com.washinson.yaradio3.Session

import android.util.Log
import com.washinson.yaradio3.Station.Tag
import java.util.*
import android.R.attr.track
import org.json.JSONObject
import java.lang.Exception
import kotlin.collections.HashMap
import android.R.attr.src
import android.R.attr.track
import android.R.attr.src
import android.R.attr.path
import android.R.attr.host
import android.accounts.NetworkErrorException
import java.math.BigInteger
import java.security.MessageDigest
import com.washinson.yaradio3.Session.YandexCommunicator.DownloadInfo
import kotlin.collections.ArrayList

class YandexCommunicator(val manager: Manager, val auth: Auth) {
    val TAG = "YandexCommunicator"

    var track: Track? = null
    var nextTrack: Track? = null
    var tag: Tag? = null
    val queue = ArrayDeque<Track>()
    val trackHistory = ArrayList<Track>()

    fun next() {
        if(track != null)
            trackHistory.add(track!!)
        track = null
        while (track == null) {
            updateTracksIfNeed()
            track = nextTrack
            nextTrack = queue.first
            queue.removeFirst()
        }
    }

    fun updateTracksIfNeed() {
        while(queue.size == 0) {
            queue.addAll(manager.getTracks(tag!!, track, nextTrack))
        }
    }

    fun cleanup() {
        track = null
        nextTrack = null
        tag = null
        queue.clear()
    }

    fun startTrack(): String {
        manager.sayAboutTrack(track!!,0.0, auth, manager.trackStarted)

        Log.i(TAG,"----")
        Log.i(TAG,"Current track: ${track.toString()}")
        Log.i(TAG,"----")

        val path = "https://api.music.yandex.net/tracks/${track!!.id}/download-info"

        val jsonObject = manager.get(path, null, track!!.tag) ?: throw NetworkErrorException()
        val qualityInfo = QualityInfo(JSONObject(jsonObject))

        track!!.qualityInfo = qualityInfo

        //val quality = sharedPreferences.getString("quality", SettingFragment.defVal);
        //todo
        val quality = "aac_64"
        val src = qualityInfo.byQuality(quality) + "&format=json"

        val builder = okhttp3.Request.Builder().get().url(src)
        builder.addHeader("Host", "storage.mds.yandex.net")
        manager.setDefaultHeaders(builder)

        val result = manager.get(src, builder.build(), track!!.tag) ?: throw NetworkErrorException()
        val downloadInformation = JSONObject(result)
        val info = DownloadInfo(downloadInformation)
        val downloadPath = info.getSrc()

        Log.d(TAG, "track: $downloadPath")
        return downloadPath
    }

    class QualityInfo(src: JSONObject) {
        val qualities = HashMap<String, JSONObject>()

        init {
            val jsonArray = src.getJSONArray("result")
            for (i in 0 until jsonArray.length()) {
                val temp = jsonArray.getJSONObject(i)

                qualities[temp.getString("codec") + "_" + temp.getString("bitrateInKbps")] = temp
            }
        }

        fun byQuality(quality: String): String {
            if (qualities[quality] == null)
                throw Exception("Unusual quality")
            return qualities[quality]!!.getString("downloadInfoUrl")
        }
    }

    class DownloadInfo(jsonObject: JSONObject) {
        val s: String
        val ts: String
        val path: String
        val host: String
        val SALT = "XGRlBW9FXlekgbPrRHuSiA"

        init {
            s = jsonObject.getString("s")
            ts = jsonObject.getString("ts")
            path = jsonObject.getString("path")
            host = jsonObject.getString("host")
        }

        fun getSrc(): String {
            val toHash = SALT + path.substring(1) + s
            val toHashBytes = toHash.toByteArray()
            val md = MessageDigest.getInstance("MD5")
            val hashBytes = md.digest(toHashBytes)
            val bigInt = BigInteger(1, hashBytes)
            var md5Hex = bigInt.toString(16)
            while (md5Hex.length < 32) {
                md5Hex = "0$md5Hex"
            }
            return "https://$host/get-mp3/$md5Hex/$ts$path"
        }
    }
}