package com.washinson.yaradio3.Station

import org.json.JSONObject

class StationTag(station: JSONObject, override val type: Type? = null) : Tag {
    override var children: ArrayList<Tag>? = null
    override val id: String
    override val tag: String
    override val name: String
    override val idForForm: String
    override val icon: Icon

    init {
        val idJson = station.getJSONObject("id")

        id = idJson.getString("type")
        tag = idJson.getString("tag")

        icon = Icon(station.getJSONObject("icon"))
        idForForm = station.getString("idForFrom")

        name = station.getString("name")
    }

    override fun toString(): String {
        return "StationTag(id='$id', tag='$tag', name='$name')"
    }
}