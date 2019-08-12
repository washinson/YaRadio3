package com.washinson.yaradio3.session

import android.content.Context
import com.franmontiel.persistentcookiejar.PersistentCookieJar
import com.franmontiel.persistentcookiejar.cache.SetCookieCache
import com.franmontiel.persistentcookiejar.persistence.SharedPrefsCookiePersistor
import com.washinson.yaradio3.station.Tag
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.*
import android.util.Log
import okhttp3.*
import org.json.JSONArray
import android.accounts.NetworkErrorException
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.lang.Exception

/**
 * Manager is intermediary app with network
 *
 * @param context Needed to cookie support
 */
class Manager(context: Context) {
    private val TAG = "Manager"

    val trackStarted = "trackStarted"
    val dislike = "dislike"
    val like = "like"
    val unlike = "unlike"
    val trackFinished = "trackFinished"
    val skip = "skip"
    val radioStarted = "radioStarted"
    val undislike = "undislike"

    private val sharedPreferences = context.getSharedPreferences("CookiePersistence", Context.MODE_PRIVATE)!!
    private val sharedPrefsCookiePersistor = SharedPrefsCookiePersistor(sharedPreferences)
    private val cookieJar: PersistentCookieJar = PersistentCookieJar(SetCookieCache(), sharedPrefsCookiePersistor)
    var okHttpClient: OkHttpClient = OkHttpClient.Builder().cookieJar(cookieJar).build()
    var browser = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:68.0) Gecko/20100101 Firefox/68.0"

    private fun typeAndTag(tag: Tag?): String {
        if(tag == null) return ""
        return "${tag.id}/${tag.tag}"
    }

    /**
     * Additional function for sayAboutTrack
     *  Needed for to memory a tracks on a yandex servers
     *
     * @throws NetworkErrorException
     *
     * @param track Current track
     * @param duration Track playback position when feedback called
     * @param auth
     * @param feedback Feedback string like trackStarted
     * @return result or null if network error
     */
    private fun historyFeedback(track: Track, duration: Double, auth: Auth, feedback: String): String? {
        Log.d(TAG, "History $feedback: duration $duration")
        Log.d(TAG, "History $feedback: track=$track")

        val path = "https://radio.yandex.ru/api/v2.1/handlers/track/none/history/feedback/retry"
        val postBody = JSONObject()
        setDefaulHistoryFeedbackBody(track, postBody, auth, duration, feedback)
        return post(path,
            postBody.toString().toRequestBody("application/json".toMediaTypeOrNull()), null, "application/json", track.tag)
    }

    private fun setDefaulHistoryFeedbackBody(track: Track, postBody: JSONObject, auth: Auth, duration: Double, feedback: String) {
        postBody.put("external-domain", "radio.yandex.ru")
        postBody.put("overembed", "no")
        postBody.put("sign", auth.sign)
        postBody.put("timestamp", Date().time)

        val jsonTrack = JSONObject()
        jsonTrack.put("album", Integer.valueOf(track.albumId))
        jsonTrack.put("context", "radio")
        jsonTrack.put("contextItem", track.tag.id + ":" + track.tag.tag)
        jsonTrack.put("duration", track.durationMs / 1000.0)
        jsonTrack.put("feedback", feedback)
        jsonTrack.put("from", "radio-web-${track.tag.id}-${track.tag.tag}-direct")
        jsonTrack.put("playId", Utils.getPlayId(track, okHttpClient))
        jsonTrack.put("played", duration)
        jsonTrack.put("position", duration)
        jsonTrack.put("trackId", track.id)
        jsonTrack.put("yaDisk", false)
        jsonTrack.put("timestamp", Date().time)

        if (feedback == trackStarted)
            jsonTrack.put("sendReason", "start")
        else
            jsonTrack.put("sendReason", "end")

        postBody.put("data", JSONArray().put(jsonTrack))
    }

    /**
     * Returns queue of next tracks
     *
     * @throws NetworkErrorException
     *
     * @param tag Current tag
     * @param curTrack Current track. Needed to inform the server
     * @param nextTrack Next track. Needed to inform the server
     * @return ArrayDeque of tracks
     */
    fun getTracks(tag: Tag, curTrack: Track? = null, nextTrack: Track? = null): ArrayDeque<Track> {
        Log.d(TAG, "Get tracks tag=$tag curTrack=$curTrack nextTrack=$nextTrack")

        var url = "https://radio.yandex.ru/api/v2.1/handlers/radio/${typeAndTag(tag)}/tracks?queue="
        if (curTrack != null) {
            url += "${curTrack.id}:${curTrack.albumId}"
            if (nextTrack != null) {
                url += ",${nextTrack.id}:${nextTrack.albumId}"
            }
        }
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

    /**
     * Checks if tag is available
     *
     * @throws NetworkErrorException
     *
     * @param tag Tag for check
     * @return true if avalible and false othervise
     */
    fun isTagAvailable(tag: Tag): Boolean {
        Log.d(TAG, "Is tag avalible $tag")

        val response =
            get("https://radio.yandex.ru/api/v2.1/handlers/radio/${tag.id}/${tag.tag}/available", null, tag) ?: throw NetworkErrorException()
        return JSONObject(response).getBoolean("available")
    }

    /**
     * Checks if tag is available
     *
     * @throws NetworkErrorException
     *
     * @param type tag's id
     * @param tag
     * @return
     */
    fun isTagAvailable(type: String, tag: String): Boolean {
        Log.d(TAG, "Is tag avalible $type:$tag")

        val response =
            get("https://radio.yandex.ru/api/v2.1/handlers/radio/$type/$tag/available", null, null) ?: throw NetworkErrorException()
        return JSONObject(response).getBoolean("available")
    }

    /**
     * Update tag settings
     *
     * @throws NetworkErrorException
     *
     * @param moodEnergy
     * @param diversity
     * @param language
     * @param tag Current tag
     * @param auth
     * @return
     */
    fun updateInfo(moodEnergy: String, diversity: String, language: String, tag: Tag, auth: Auth): String? {
        Log.d(TAG, "Update station: $moodEnergy $diversity $language")
        Log.d(TAG, "Update station: $tag")

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

    /**
     * Send [feedback] to server
     *
     * @throws NetworkErrorException
     *
     * @param track Current track
     * @param duration Track playback position when feedback called
     * @param auth
     * @param feedback Feedback string like trackStarted
     * @return
     */
    fun sayAboutTrack(track: Track, duration: Double, auth: Auth, feedback: String): String? {
        Log.d(TAG, "$feedback: with duration $duration")
        Log.d(TAG, "$feedback: $track")

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

    private fun setDefaultPostDataTrack(postData: PostConfig, track: Track, auth: Auth) {
        postData.put("timestamp", Date().time.toString())
        postData.put("from", "radio-web-${track.tag.id}-${track.tag.tag}-direct")
        postData.put("sign", auth.sign)
        postData.put("external-domain", "radio.yandex.ru")
        postData.put("overembed", "no")
        postData.put("batchId", track.batchId)
        postData.put("trackId", track.id.toString())
        postData.put("albumId", track.albumId.toString())
    }

    /**
     * Build default get request
     *
     * @param url
     * @param tag
     * @return
     */
    private fun getGetRequest(url: String, tag: Tag?): Request {
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

    /**
     * Build default post request
     *
     * @param url
     * @param contentType
     * @param tag
     * @param requestBody
     * @return
     */
    private fun getPostRequest(url: String, contentType: String, tag: Tag, requestBody: RequestBody): Request {
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

    /**
     * Do get request to [url]
     *
     * @throws NetworkErrorException
     *
     * @param url
     * @param request1 Get request. If null get request will be default
     * @param tag
     * @return
     */
    fun get(url: String, request1: Request?, tag: Tag?): String? {
        val request = request1 ?: getGetRequest(url, tag)
        return doRequest(request)
    }

    /**
     * Do post request
     *
     * @throws NetworkErrorException
     *
     * @param url
     * @param requestBody
     * @param request1 Post request. If null post request will be default
     * @param contentType
     * @param tag
     * @return
     */
    fun post(url: String, requestBody: RequestBody, request1: Request?, contentType: String, tag: Tag): String? {
        val request = request1 ?: getPostRequest(url, contentType, tag, requestBody)
        return doRequest(request)
    }

    private fun getCookiesString(cookies: List<Cookie>): String {
        val builder = StringBuilder()
        for ((i, cookie) in cookies.withIndex()) {
            builder.append(cookie.name).append("=").append(cookie.value)
            if(i != cookies.size - 1) builder.append("; ")
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

    /**
     * Connect to server with request
     *
     * @throws NetworkErrorException
     *
     * @param request
     * @return String result or null if connection error
     */
    private fun doRequest(request: Request): String? {
        var res: String? = null
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
            res = if (contentEncoding != null && contentEncoding == "gzip")
                Utils.decodeGZIP(q)
            else
                String(q)
            response.close()
        } catch (e: Exception) {
            response.close()
            e.printStackTrace()
        }

        return res
    }
}