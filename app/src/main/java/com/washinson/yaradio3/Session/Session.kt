package com.washinson.yaradio3.Session

import android.accounts.NetworkErrorException
import android.content.Context
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import java.io.File
import kotlin.concurrent.thread
import android.R.string.cancel
import android.content.DialogInterface
import android.R
import android.app.AlertDialog
import com.washinson.yaradio3.MainActivity
import com.washinson.yaradio3.Station.*
import org.json.JSONException
import java.net.CookieManager


class Session private constructor(context: Context) {
    lateinit var auth: Auth
    lateinit var manager: Manager
    lateinit var yandexCommunicator: YandexCommunicator
    val track: Track?
        get() = yandexCommunicator.track
    var tag: Tag?
        get() = yandexCommunicator.tag
        private set(tag) {yandexCommunicator.tag = tag}
    //todo: check correct quality
    var quality = "aac_192"

    init {
        updateSession(context)
    }

    fun updateSession(context: Context) {
        manager = Manager(context)
        auth = Auth(manager)
        yandexCommunicator = YandexCommunicator(manager,auth)
    }

    companion object {
        private val sessions: HashMap<Int, Session> = hashMapOf()
        fun getInstance(id: Int, context: Context?): Session {
            if (sessions.contains(id)) {
                return sessions[id]!!
            } else {
                thread {
                    try {
                        sessions[id] = Session(context ?: return@thread)
                    } catch (e: NetworkErrorException) {}
                }.join()
                return sessions[id] ?: throw NetworkErrorException()
            }
        }
    }

    fun updateInfo(language: String, moodEnergy: String, diversity: String) {
        yandexCommunicator.nextTrack = null
        yandexCommunicator.queue.clear()
        manager.updateInfo(moodEnergy, diversity, language, track?.tag ?: return, auth)
        yandexCommunicator.updateTracksIfNeed()
    }

    fun isTagAvailable(tag: Tag) = manager.isTagAvailable(tag)
    fun isTagAvailable(type: String, tag: String) = manager.isTagAvailable(type, tag)

    fun login(cookies: String?) {
        if (cookies == null)
            return
        val t = cookies.split("; ")
        val cookieArrayList = ArrayList<Cookie>()
        for (pair in t) {
            val res = pair.split("=".toRegex(), 2).toTypedArray()
            val cookie = Cookie.Builder().domain("https://radio.yandex.ru".toHttpUrlOrNull()!!.topPrivateDomain()!!)
                .name(res[0]).value(res[1]).expiresAt(java.lang.Long.MAX_VALUE).build()

            cookieArrayList.add(cookie)
        }
        manager.okHttpClient.cookieJar.saveFromResponse("https://radio.yandex.ru".toHttpUrlOrNull()!!, cookieArrayList)
    }

    fun logout() {
        val cookies = manager.okHttpClient.cookieJar.loadForRequest("https://radio.yandex.ru".toHttpUrlOrNull()!!)
        val cookies2 = ArrayList<Cookie>()
        for (i in cookies) {
            val builder = Cookie.Builder()
                .domain(i.domain)
                .expiresAt(System.currentTimeMillis())
                .name(i.name)
                .value(i.value)
                .path(i.path)
            if(i.hostOnly) builder.hostOnlyDomain(i.domain)
            else builder.domain(i.domain)
            if(i.httpOnly) builder.httpOnly()
            if(i.secure) builder.secure()
            cookies2.add(builder.build())
        }
        manager.okHttpClient.cookieJar.saveFromResponse("https://radio.yandex.ru".toHttpUrlOrNull()!!, cookies2)
        auth = Auth(manager)
        yandexCommunicator = YandexCommunicator(manager,auth)
    }

    fun isUserLoggedIn(): Boolean {
        val t = manager.okHttpClient.cookieJar.loadForRequest("https://radio.yandex.ru".toHttpUrlOrNull()!!)
        var result = false
        for (cookie in t) {
            if (cookie.name == "yandex_login") {
                result = true
            }
        }
        return result
    }

    fun getUserLogin(): String? {
        val t = manager.okHttpClient.cookieJar.loadForRequest("https://radio.yandex.ru".toHttpUrlOrNull()!!)
        var result: String? = null
        for (cookie in t) {
            if (cookie.name == "yandex_login") {
                result = cookie.value
                break
            }
        }
        return result
    }

    private fun tryGenTag(type: String, tag: String, parent: Type?): Tag? {
        if(!isTagAvailable(type, tag))
            return null
        val response =
            manager.get("https://radio.yandex.ru/api/v2.1/handlers/radio/$type/$tag/settings", null, null) ?: return null

        try {
            return StationTag(JSONObject(response).getJSONObject("station"), parent)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return null
    }

    fun getTypes(): ArrayList<Type> {
        val response = manager.get("https://radio.yandex.ru/handlers/library.jsx?lang=ru", null, null) ?: throw NetworkErrorException()
        val mainBody = JSONObject(response)
        val types = mainBody.getJSONObject("types")
        val stations = mainBody.getJSONObject("stations")

        val typesResult = ArrayList<Type>()
        val typesArray =  types.toJSONArray(types.names()) ?: return typesResult

        for(i in 0 until typesArray.length()) {
            val currentType = typesArray.getJSONObject(i)
            val type = DefaultType(currentType, stations)

            // Hack for my friend
            if (type.id == "user" && type.tags.size == 0 && isUserLoggedIn()) {
                var t = tryGenTag(type.id, getUserLogin()!!, type)
                if (t != null) type.tags.add(t)
                t = tryGenTag(type.id, "onyourwave", type)
                if (t != null) type.tags.add(t)
            }

            typesResult.add(type)
        }

        typesResult.add(getRecommendationsType())

        return typesResult
    }

    private fun getRecommendationsType(): RecommendType {
        val response = manager.get("https://radio.yandex.ru/handlers/recommended.jsx", null, null) ?: throw NetworkErrorException()

        return RecommendType("Рекомендации", response, "recommendations", true)
    }

    fun setTagToPlay(tag: Tag) {
        yandexCommunicator.cleanup()
        this.tag = tag
        yandexCommunicator.next()
        manager.sayAboutTrack(track!!, 0.0, auth, manager.radioStarted)
    }

    fun nextTrack(finished: Boolean, duration: Double) {
        if(track != null) {
            if (finished) {
                manager.sayAboutTrack(track!!, duration, auth, manager.trackFinished)
            } else {
                manager.sayAboutTrack(track!!, duration, auth, manager.skip)
            }
        }
        yandexCommunicator.next()
    }

    fun startTrack(): String {
        if(track == null)
            return ""
        return yandexCommunicator.startTrack(quality)
    }

    fun getNextTracks(): ArrayList<Track> {
        val array = ArrayList<Track>()
        if (yandexCommunicator.nextTrack != null)
            array.add(yandexCommunicator.nextTrack!!)
        array.addAll(yandexCommunicator.queue)

        return array
    }

    fun getTrackHistory(): ArrayList<Track> {
        return yandexCommunicator.trackHistory
    }

    fun like(track: Track, duration: Double) {
        track.liked = true
        manager.sayAboutTrack(track, duration, auth, manager.like)
    }

    fun unlike(track: Track, duration: Double) {
        track.liked = false
        manager.sayAboutTrack(track, duration, auth, manager.unlike)
    }

    fun undislike(track: Track, duration: Double) {
        track.disliked = false
        manager.sayAboutTrack(track, duration, auth, manager.undislike)
        yandexCommunicator.queue.clear()
        yandexCommunicator.updateTracksIfNeed()
    }

    fun dislike(track: Track, duration: Double) {
        track.disliked = true
        manager.sayAboutTrack(track, duration, auth, manager.dislike)
        yandexCommunicator.queue.clear()
        yandexCommunicator.updateTracksIfNeed()
    }
}