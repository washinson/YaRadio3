package com.washinson.yaradio3.Station

import com.bumptech.glide.Glide.init
import org.json.JSONObject

class RecommendType(override val name: String,
                    response: String,
                    override val id: String,
                    override val showInMenu: Boolean) : Type {
    override val tags: ArrayList<Tag> = ArrayList()

    init {
        val stationsJson = JSONObject(response).getJSONArray("stations")

        for (i in 0 until stationsJson.length()) {
            val cur = stationsJson.getJSONObject(i)
            tags.add(StationTag(cur.getJSONObject("station")))
        }
    }
}