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
                manager.sayAboutTrack(track!!, duration / 1000.0, auth, manager.trackFinished)
            } else {
                manager.sayAboutTrack(track!!, duration / 1000.0, auth, manager.skip)
            }
        }
        yandexCommunicator.next()
    }

    fun startTrack(): String {
        if(track == null)
            return ""
        return startTrack()
    }

    fun like() {

    }

    fun unlike() {

    }

    fun dislike() {

    }
}