package com.washinson.yaradio3.station

import org.json.JSONObject

class Icon(icon: JSONObject) {
    val backgroundColor: String
    val imageUrl: String

    init {
        backgroundColor = icon.getString("backgroundColor")
        imageUrl = icon.getString("imageUrl")
    }

    fun getIcon(xSize: Int, ySize: Int): String {
        return "https://" + imageUrl.replace("%%", xSize.toString() + "x" + ySize.toString())
    }
}