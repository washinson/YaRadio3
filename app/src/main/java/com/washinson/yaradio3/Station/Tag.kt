package com.washinson.yaradio3.Station

import android.accounts.NetworkErrorException
import com.washinson.yaradio3.Session.Session
import org.json.JSONObject


interface Tag {
    var children: ArrayList<Tag>?
    val tag: String
    val name: String
    val idForForm: String
    val icon: Icon
    val id: String
    val type: Type?

    fun getSettings(): Settings? {
        val response = Session.getInstance(0, null).getTagSettings(id, tag)
        return Settings(JSONObject(response))
    }

    fun setSettings(language: String, moodEnergy: String, diversity: String) {
        Session.getInstance(0, null).updateInfo(language, moodEnergy, diversity)
    }
}