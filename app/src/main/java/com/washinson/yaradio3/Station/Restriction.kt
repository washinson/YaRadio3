package com.washinson.yaradio3.Station

import com.bumptech.glide.Glide.init
import org.json.JSONObject

class Restriction(restriction2: JSONObject) {
    val possibleValues: ArrayList<Pair<String, String>> = ArrayList<Pair<String, String>>()

    init {
        val possValues = restriction2.getJSONArray("possibleValues")

        for (i in 0 until possValues.length()) {
            val type = possValues.getJSONObject(i)
            possibleValues.add(Pair(type.getString("value"), type.getString("name")))
        }
    }
}