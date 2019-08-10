package com.washinson.yaradio3.Station

import org.json.JSONObject

interface Type {
    val tags: ArrayList<Tag>
    val name: String
    val id: String
    val showInMenu: Boolean
}