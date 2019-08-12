package com.washinson.yaradio3.station

interface Type {
    val tags: ArrayList<Tag>
    val name: String
    val id: String
    val showInMenu: Boolean
}