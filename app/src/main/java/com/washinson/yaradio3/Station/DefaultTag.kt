package com.washinson.yaradio3.Station

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
}