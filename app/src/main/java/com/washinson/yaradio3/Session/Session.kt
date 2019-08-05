package com.washinson.yaradio3.Session

import android.content.Context
import com.washinson.yaradio3.Station.Tag

class Session(context: Context) {
    val auth: Auth
    val manager: Manager
    val yandexCommunicator: YandexCommunicator
    val track: Track?
        get() = yandexCommunicator.track
    var tag: Tag?
        get() = yandexCommunicator.tag
        private set(tag) {yandexCommunicator.tag = tag}

    init {
        manager = Manager(context)
        auth = Auth(manager)
        yandexCommunicator = YandexCommunicator(manager,auth)
    }

    fun login() {
        TODO()
    }

    fun getTypes() {
        val response = manager.get("https://radio.yandex.ru/handlers/library.jsx?lang=ru", null, null)
        var a = 10;
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
        return startTrack()
    }

    fun like(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.like)
    }

    fun unlike(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.unlike)
    }

    fun dislike(track: Track, duration: Double) {
        manager.sayAboutTrack(track, duration, auth, manager.dislike)
        yandexCommunicator.queue.clear()
    }
}