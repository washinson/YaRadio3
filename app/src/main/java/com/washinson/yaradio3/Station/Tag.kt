package com.washinson.yaradio3.Station

import org.json.JSONObject

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

    fun getSettings(): Settings {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun setSettings(lang: String, moodEnergy: String, diversity: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}