package com.washinson.yaradio3.Station

import org.json.JSONObject

class DefaultType(typeStation: JSONObject, stations: JSONObject) : Type {
    override val tags: ArrayList<Tag>
    override val name: String
    override val id: String
    override val showInMenu: Boolean


    init {
        if(typeStation.has("showInMenu"))
            showInMenu = typeStation.getBoolean("showInMenu")
        else showInMenu = false

        id = typeStation.getString("id")

        if(typeStation.has("name"))
            name = typeStation.getString("name")
        else name = "Личные станции" // TODO: make string value

        tags = ArrayList<Tag>()
        if (typeStation.has("children")) {
            val tagsJson = typeStation.getJSONArray("children")
            for (i in 0 until tagsJson.length()) {
                tags.add(DefaultTag(tagsJson.getJSONObject(i), stations, this, id))
            }
        }
    }
}