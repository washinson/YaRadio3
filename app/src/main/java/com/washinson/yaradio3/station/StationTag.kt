package com.washinson.yaradio3.station

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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StationTag

        if (id != other.id) return false
        if (tag != other.tag) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + tag.hashCode()
        return result
    }


}