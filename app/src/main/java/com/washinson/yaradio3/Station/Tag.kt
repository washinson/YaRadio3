package com.washinson.yaradio3.Station

interface Tag {
    val type: Type
    val tag: String
    val name: String
    val idForForm: String
    val icon: Icon

    fun getSettings(): Settings
    fun setSettings(lang: String, moodEnergy: String, diversity: String)
}