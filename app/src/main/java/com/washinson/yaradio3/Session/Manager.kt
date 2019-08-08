package com.washinson.yaradio3.Session

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.washinson.yaradio3.Station.Tag
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.time.Duration
import java.util.*
import android.R.attr.track
import android.R.attr.duration
import android.R.attr.name
import android.R.attr.targetName
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import android.R.attr.track
import android.accounts.NetworkErrorException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.lang.Exception

@Suppress("SpellCheckingInspection")
class Manager(context: Context) {
    val TAG = "Manager"

    val trackStarted = "trackStarted"
    val dislike = "dislike"
    val like = "like"
    val unlike = "unlike"
    val trackFinished = "trackFinished"
    val skip = "skip"
    val radioStarted = "radioStarted"
    val undislike = "undislike"

    val cookieJar: PersistentCookieJar = PersistentCookieJar(SetCookieCache(), SharedPrefsCookiePersistor(context))
    var okHttpClient: OkHttpClient = OkHttpClient.Builder().cookieJar(cookieJar).build()
    var browser = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0"

    fun typeAndTag(tag: Tag?): String {
        if(tag == null) return ""
        return "${tag.type.id}/${tag.tag}"
    }

    fun historyFeedback(track: Track, duration: Double, auth: Auth, feedback: String): String? {
        val path = "https://radio.yandex.ru/api/v2.1/handlers/track/none/history/feedback/retry"
        val postBody = JSONObject()
        setDefaulHistoryFeedbackBody(track, postBody, auth, duration, feedback)
        return post(path,
            postBody.toString().toRequestBody("application/json".toMediaTypeOrNull()), null, "application/json", track.tag)
    }

    fun setDefaulHistoryFeedbackBody(track: Track, postBody: JSONObject, auth: Auth, duration: Double, feedback: String) {
        postBody.put("external-domain", "radio.yandex.ru")
        postBody.put("overembed", "no")
        postBody.put("sign", auth.sign)
        postBody.put("timestamp", Date().getTime())

        val jsonTrack = JSONObject()
        jsonTrack.put("album", Integer.valueOf(track.albumId))
        jsonTrack.put("context", "radio")
        jsonTrack.put("contextItem", track.tag.type.id + ":" + track.tag.tag)
        jsonTrack.put("duration", track.durationMs / 1000.0)
        jsonTrack.put("feedback", feedback)
        jsonTrack.put("from", "radio-web-${track.tag.type.id}-${track.tag.tag}-direct")
        jsonTrack.put("playId", Utils.getPlayId(track, okHttpClient))
        jsonTrack.put("played", duration)
        jsonTrack.put("position", duration)
        jsonTrack.put("trackId", track.id)
        jsonTrack.put("yaDisk", false)
        jsonTrack.put("timestamp", Date().time)

        if (feedback.equals(trackStarted))
            jsonTrack.put("sendReason", "start")
        else
            jsonTrack.put("sendReason", "end")

        postBody.put("data", JSONArray().put(jsonTrack))
    }

    fun getTracks(tag: Tag, curTrack: Track? = null, nextTrack: Track? = null): ArrayDeque<Track> {
        var url = "https://radio.yandex.ru/api/v2.1/handlers/radio/${typeAndTag(tag)}/tracks?queue="
        if (curTrack != null) {
            url += "${curTrack.id}:${curTrack.albumId}"
            if (nextTrack != null) {
                url += ",${nextTrack.id}:${nextTrack.albumId}"
            }
        }
        Log.d(TAG, "getTracks: ${url}")
        Log.d(TAG, "Time: ${System.currentTimeMillis() / 1000}")
        val response = get(url, null, tag) ?: throw NetworkErrorException()
        val tracks = JSONObject(response)
        val array = tracks.getJSONArray("tracks")
        val trackList = ArrayDeque<Track>()
        for (i in 0 until array.length()) {
            val trackObject = array.getJSONObject(i)
            if (trackObject.getString("type") == "track") {
                trackList.add(Track(trackObject, tag))
            }
        }
        return trackList
    }

    fun updateInfo(moodEnergy: String, diversity: String, language: String, tag: Tag, auth: Auth): String? {
        Log.d(TAG, "Update station : $moodEnergy $diversity $language")
        Log.d(TAG, "Time: ${System.currentTimeMillis() / 1000}")
        val path = "https://radio.yandex.ru/api/v2.1/handlers/radio/${typeAndTag(tag)}/settings"
        val postData = PostConfig()

        postData.put("language", language)
        postData.put("moodEnergy", moodEnergy)
        postData.put("diversity", diversity)
        postData.put("sign", auth.sign)
        postData.put("external-domain", "radio.yandex.ru")
        postData.put("overembed", "no")

        return post(path, postData.toString().toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull()), null, "application/x-www-form-urlencoded", tag)
    }

    fun sayAboutTrack(track: Track, duration: Double, auth: Auth, feedback: String): String? {
        Log.d(TAG, "$feedback : Track duration: $duration")
        Log.d(TAG, "Time: ${System.currentTimeMillis() / 1000}")
        val path = "https://radio.yandex.ru/api/v2.1/handlers/radio/${typeAndTag(track.tag)}/feedback/$feedback/${track.id}:${track.albumId}"

        val postData = PostConfig()
        setDefaultPostDataTrack(postData, track, auth)
        postData.put("totalPlayed", duration.toString())

        val out = post(path, postData.toString().toRequestBody("application/x-www-form-urlencoded".toMediaTypeOrNull())
            , null, "application/x-www-form-urlencoded", track.tag)

        if(feedback == trackFinished || feedback == trackStarted || feedback == skip){
            historyFeedback(track, duration, auth, feedback)
        }

        return out
    }

    fun getGetRequest(url: String, tag: Tag?): Request {
        val builder = Request.Builder()
        builder.url(url)

        setDefaultHeaders(builder)

        builder.addHeader("Referer", "https://radio.yandex.ru" + typeAndTag(tag))
        builder.addHeader("X-Retpath-Y", "https://radio.yandex.ru" + typeAndTag(tag))
        val httpUrl = "https://radio.yandex.ru".toHttpUrlOrNull()
        if (httpUrl != null)
            builder.addHeader("Cookie", getCookiesString(okHttpClient.cookieJar.loadForRequest(httpUrl)))

        return builder.build()
    }

    fun getPostRequest(url: String, contentType: String, tag: Tag, requestBody: RequestBody): Request {
        val builder = Request.Builder()
        builder.url(url).post(requestBody)

        setDefaultHeaders(builder)

        builder.addHeader("Referer", "https://radio.yandex.ru" + typeAndTag(tag))
        builder.addHeader("X-Retpath-Y", "https://radio.yandex.ru" + typeAndTag(tag))
        builder.addHeader("Origin", "https://radio.yandex.ru")
        builder.addHeader("Content-Type", contentType)
        val httpUrl = "https://radio.yandex.ru".toHttpUrlOrNull()
        if (httpUrl != null)
            builder.addHeader("Cookie", getCookiesString(okHttpClient.cookieJar.loadForRequest(httpUrl)))

        return builder.build()
    }

    fun get(url: String, request1: Request?, tag: Tag?): String? {
        val request = request1 ?: getGetRequest(url, tag)
        return doRequest(request)
    }

    fun post(url: String, requestBody: RequestBody, request1: Request?, contentType: String, tag: Tag): String? {
        val request = request1 ?: getPostRequest(url, contentType, tag, requestBody)
        return doRequest(request)
    }

    fun setDefaultPostDataTrack(postData: PostConfig, track: Track, auth: Auth) {
        postData.put("timestamp", Date().time.toString())
        postData.put("from", "radio-web-${track.tag.type.id}-${track.tag.tag}-direct")
        postData.put("sign", auth.sign)
        postData.put("external-domain", "radio.yandex.ru")
        postData.put("overembed", "no")
        postData.put("batchId", track.batchId)
        postData.put("trackId", track.id.toString())
        postData.put("albumId", track.albumId.toString())
    }

    fun getCookiesString(cookies: List<Cookie>): String {
        val builder = StringBuilder()
        var i = 0
        for (cookie in cookies) {
            builder.append(cookie.name).append("=").append(cookie.value)
            if(i++ != cookies.size - 1) builder.append("; ")
        }
        return builder.toString()
    }

    fun setDefaultHeaders(request: Request.Builder) {
        request.addHeader("Accept", "application/json; q=1.0, text/*; q=0.8, */*; q=0.1")
        request.addHeader("Accept-Encoding", "gzip, deflate, sdch, br")
        request.addHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.6,en;q=0.4")
        //connection.addHeader("Cache-Control", "max-age=0")
        request.cacheControl(CacheControl.parse(
            Headers.Builder().add("Cache-Control", "max-age=0").build()))
        request.addHeader("Connection", "keep-alive")
        request.addHeader("Host", "radio.yandex.ru")
        request.addHeader("User-Agent", browser)
        request.addHeader("X-Requested-With", "XMLHttpRequest")
    }

    fun doRequest(request: Request): String? {
        var res: String?
        val response: Response
        try {
           response = okHttpClient.newCall(request).execute()
        } catch (exception: IOException) {
            throw NetworkErrorException()
        }
        try {
            if (response.body == null) {
                Log.d(TAG, "response body: null")
                return null
            }

            val q = response.body!!.bytes()
            val contentEncoding = response.header("Content-Encoding")
            if (contentEncoding != null && contentEncoding == "gzip")
                res = Utils.decodeGZIP(q)
            else
                res = String(q)
            response.close()
        } catch (e: Exception) {
            response.close()
            e.printStackTrace()
            okHttpClient = OkHttpClient.Builder().cookieJar(cookieJar).build()
            Log.d(TAG,"Connection Problem")
            res = doRequest(request)
        }

        return res
    }
}