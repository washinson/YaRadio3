package com.washinson.yaradio3.Station

import org.json.JSONObject

class Type(typeStation: JSONObject, stations: JSONObject) {
    val tags: ArrayList<Tag>
    val name: String
    val id: String
    val showInMenu: Boolean

    init {
        if(typeStation.has("showInMenu"))
            showInMenu = typeStation.getBoolean("showInMenu")
        else showInMenu = false

        id = typeStation.getString("id")

        if(typeStation.has("name"))
            name = typeStation.getString("name")
        else name = "TODO" //todo

        tags = ArrayList<Tag>()
        if (typeStation.has("children")) {
            val tagsJson = typeStation.getJSONArray("children")
            for (i in 0 until tagsJson.length()) {
                tags.add(Tag(tagsJson.getJSONObject(i), stations, this))
            }
        }
    }
}