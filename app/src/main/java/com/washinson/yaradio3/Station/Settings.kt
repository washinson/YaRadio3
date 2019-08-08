package com.washinson.yaradio3.Station

import org.json.JSONObject

class Settings(settings: JSONObject) {
    val language: String
    val moodEnergy: String
    val diversity: String

    val languages: Restriction
    val moodEnergies: Restriction
    val diversities: Restriction

    init {
        val settings2 = settings.getJSONObject("settings2")
        language = settings2.getString("language")
        moodEnergy = settings2.getString("moodEnergy")
        diversity = settings2.getString("diversity")

        val restrictions2 = settings
            .getJSONObject("station")
            .getJSONObject("restrictions2")

        languages = Restriction(restrictions2.getJSONObject("language"))
        moodEnergies = Restriction(restrictions2.getJSONObject("moodEnergy"))
        diversities = Restriction(restrictions2.getJSONObject("diversity"))
    }
}