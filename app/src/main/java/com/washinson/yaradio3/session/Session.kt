package com.washinson.yaradio3.session

import android.accounts.NetworkErrorException
import android.content.Context
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject
import kotlin.concurrent.thread
import com.washinson.yaradio3.SettingsFragment
import com.washinson.yaradio3.station.*
import org.json.JSONException

/**
 * Session provides API to Yandex Radio
 * Call getInstance to get Session. If call is first you must provide a context to generate Session
 *
 * To start you must set tag to play with setTagToPlay
 * If you changed tag to play call startTrack to start track
 *
 * Further call nextTrack and startTrack to work with tracks
 *
 * @param context Needed to cookies support
 */
class Session private constructor(context: Context) {
    lateinit var auth: Auth
    lateinit var manager: Manager
    lateinit var yandexCommunicator: YandexCommunicator

    val track: Track?
        get() = yandexCommunicator.track
    val nextTrack: Track?
        get() = yandexCommunicator.nextTrack
    var tag: Tag?
        get() = yandexCommunicator.tag
        private set(tag) {yandexCommunicator.tag = tag}
    var quality: String = "aac_192"
        set(value) {
            if (value != "aac_192" && value != "aac_128" &&
                    value != "aac_64" && value != "mp3_192") throw Exception("Incorrect quality")
            field = value
        }
    val trackHistory: ArrayList<Track>
        get() = yandexCommunicator.trackHistory

    companion object {
        private val sessions: HashMap<Int, Session> = hashMapOf()

        /**
         * Call it to get Session. If call is first you must provide a context to generate Session
         * The future idea is in supporting multi session. Id provided for it, but now it's useless
         *
         * @param id ID needed session. Currently is useless
         * @param context
         * @return
         */
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

    init {
        updateSession(context)
    }

    /**
     * (Re)init session
     *
     * @throws NetworkErrorException
     *
     * @param context
     */
    fun updateSession(context: Context) {
        quality = context.getSharedPreferences(SettingsFragment.TAG_PREFERENCES, Context.MODE_PRIVATE)
            .getString(SettingsFragment.QUALITY, SettingsFragment.defautQualityValue) ?: "aac_192"

        manager = Manager(context)
        auth = Auth(manager)
        yandexCommunicator = YandexCommunicator(manager,auth)
    }

    fun getTagSettings(id: String, tag: String): String {
        return manager.get("https://radio.yandex.ru/api/v2.1/handlers/radio/$id/$tag/settings",
            null, null) ?: throw NetworkErrorException()
    }

    /**
     * Update tag settings
     *
     * @throws NetworkErrorException
     *
     * @param language
     * @param moodEnergy
     * @param diversity
     */
    fun updateInfo(language: String, moodEnergy: String, diversity: String) {
        yandexCommunicator.nextTrack = null
        yandexCommunicator.queue.clear()
        manager.updateInfo(moodEnergy, diversity, language, track?.tag ?: return, auth)

        yandexCommunicator.updateTracksIfNeed(track, nextTrack)
    }

    fun isTagAvailable(tag: Tag) = manager.isTagAvailable(tag)
    fun isTagAvailable(type: String, tag: String) = manager.isTagAvailable(type, tag)

    /**
     * To login you must provide cookies from logged browser's session or like that
     * To regenerate session internet needed
     *
     * @throws NetworkErrorException
     *
     * @param cookies
     */
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

        auth = Auth(manager)
        yandexCommunicator = YandexCommunicator(manager,auth)
    }

    /**
     * Logout
     * To regenerate session internet needed
     *
     * @throws NetworkErrorException
     *
     */
    fun logout() {
        val cookies = manager.okHttpClient.cookieJar.loadForRequest("https://radio.yandex.ru".toHttpUrlOrNull()!!)
        val cookies2 = ArrayList<Cookie>()
        for (i in cookies) {
            // Like newCookie = i; newCookie.expiresAt(now)
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

    /**
     * Check if user logged in
     *
     * @return true if user is logged in or false otherwise
     */
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

    /**
     * Return logged user login or null if user don't logged in
     *
     * @return user login or null if user don't logged in
     */
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

    /**
     * If tag load from /settings api function you must use this function
     *
     * @throws NetworkErrorException
     *
     * @param type
     * @param tag
     * @param parent Parent type which tag will be connect. Use null if you can't provide Type
     * @return Generated tag or null if error
     */
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

    /**
     * Yandex provides big stations list. To save network traffic and upgrade load speed it can be loaded and used in getTypes later
     *
     * @throws NetworkErrorException
     *
     * @return library.jsx to use in getTypes
     */
    fun getTypesResponseForSave(): String {
        return manager.get("https://radio.yandex.ru/handlers/library.jsx?lang=ru", null, null) ?: throw NetworkErrorException()
    }

    /**
     * Return an array of Types
     *
     * @throws NetworkErrorException
     *
     * @param response1 library.jsx from getTypesResponseForSave or null
     * @return
     */
    fun getTypes(response1: String? = null): ArrayList<Type> {
        val response = response1 ?: getTypesResponseForSave()
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

        return typesResult
    }

    /**
     * Yandex provides recommendations
     * In can be loaded here
     *
     * @return Recommendation's Type
     */
    fun getRecommendedType(): RecommendType {
        val response = manager.get("https://radio.yandex.ru/handlers/recommended.jsx", null, null) ?: throw NetworkErrorException()

        return RecommendType("Рекомендации", response, "recommendations", true)
    }

    /**
     * Set tag to play
     * You must call it before start playing
     *
     * @param tag Tag to play
     */
    fun setTagToPlay(tag: Tag) {
        if (this.tag == tag)
            return
        yandexCommunicator.cleanup()
        this.tag = tag
        yandexCommunicator.next()
        manager.sayAboutTrack(track!!, 0.0, auth, manager.radioStarted)
    }

    /**
     * Prepare to next track
     *
     * @throws NetworkErrorException
     *
     * @param finished If track finished true and false otherwise
     * @param duration Playback position in seconds when function called
     */
    fun nextTrack(finished: Boolean, duration: Double) {
        if(track != null) {
            if (finished) {
                manager.sayAboutTrack(track!!, duration, auth, manager.trackFinished)
            } else {
                manager.sayAboutTrack(track!!, duration, auth, manager.skip)
                yandexCommunicator.queue.clear()
            }
        }
        yandexCommunicator.next()
    }

    /**
     * Say yandex that track started and get track url
     *
     * @throws NetworkErrorException
     *
     * @return track url or empty string if you incorrect used Session
     */
    fun startTrack(): String {
        if(track == null)
            return ""
        return yandexCommunicator.startTrack(track!!, quality)
    }

    /**
     * Get next tracks in queue
     *
     * @return tracks
     */
    fun getNextTracks(): ArrayList<Track> {
        val array = ArrayList<Track>()
        if (yandexCommunicator.nextTrack != null)
            array.add(yandexCommunicator.nextTrack!!)
        array.addAll(yandexCommunicator.queue)

        return array
    }

    /**
     * Like current track
     *
     * @param track
     * @param duration Playback position in seconds when function called
     */
    fun like(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.like)
        yandexCommunicator.queue.clear()
        yandexCommunicator.updateTracksIfNeed(this.track, nextTrack)
        track.liked = true
    }

    /**
     * Unlike current track
     *
     * @param track
     * @param duration Playback position in seconds when function called
     */
    fun unlike(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.unlike)
        yandexCommunicator.queue.clear()
        yandexCommunicator.updateTracksIfNeed(this.track, nextTrack)
        track.liked = false
    }

    /**
     * Undislike current track
     *
     * @param track
     * @param duration Playback position in seconds when function called
     */
    fun undislike(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.undislike)
        yandexCommunicator.queue.clear()
        yandexCommunicator.updateTracksIfNeed(this.track, nextTrack)
        track.disliked = false
    }

    /**
     * Dislike current track
     *
     * @param track
     * @param duration Playback position in seconds when function called
     */
    fun dislike(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.dislike)
        yandexCommunicator.queue.clear()
        yandexCommunicator.updateTracksIfNeed(this.track, nextTrack)
        track.disliked = true
    }


}