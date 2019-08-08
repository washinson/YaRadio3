package com.washinson.yaradio3.Station

import android.accounts.NetworkErrorException
import com.washinson.yaradio3.Session.Session
import org.json.JSONObject
import org.json.JSONArray



class Tag(id: JSONObject, stations: JSONObject, val type: Type) {
    var children: ArrayList<Tag>? = null
    val tag: String
    val name: String
    val idForForm: String
    val icon: Icon

    init {
        tag = id.getString("tag")
        val cur = stations.getJSONObject(type.id + ":" + tag)
        val station = cur.getJSONObject("station")
        name = station.getString("name")
        icon = Icon(station.getJSONObject("icon"))
        idForForm = station.getString("idForFrom")

        if (station.has("children")) {
            children = ArrayList()
            val childs = station.getJSONArray("children")
            for (i in 0 until childs.length()) {
                children!!.add(Tag(childs.getJSONObject(i), stations, type))
            }
        }
    }

    fun getSettings(): Settings? {
        val response = Session.getInstance(0, null).manager.get(
            "https://radio.yandex.ru/api/v2.1/handlers/radio/${type.id}/$tag/settings"
            , null, null) ?: throw NetworkErrorException()

        return Settings(JSONObject(response))
    }

    fun setSettings(language: String, moodEnergy: String, diversity: String) {
        Session.getInstance(0, null).updateInfo(moodEnergy, diversity, language)
    }
}