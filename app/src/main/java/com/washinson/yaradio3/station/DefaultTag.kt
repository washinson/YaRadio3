package com.washinson.yaradio3.station

import org.json.JSONObject

class DefaultTag(idJson: JSONObject, stations: JSONObject, override val type: Type?, override val id: String) : Tag {
    override var children: ArrayList<Tag>? = null
    override val tag: String
    override val name: String
    override val idForForm: String
    override val icon: Icon

    init {
        tag = idJson.getString("tag")
        val cur = stations.getJSONObject("$id:$tag")
        val station = cur.getJSONObject("station")
        name = station.getString("name")
        icon = Icon(station.getJSONObject("icon"))
        idForForm = station.getString("idForFrom")

        if (station.has("children")) {
            children = ArrayList()
            val child = station.getJSONArray("children")
            for (i in 0 until child.length()) {
                children!!.add(DefaultTag(child.getJSONObject(i), stations, type, id))
            }
        }
    }

    override fun toString(): String {
        return "DefaultTag(id='$id', tag='$tag', name='$name')"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DefaultTag

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