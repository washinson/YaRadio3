package com.washinson.yaradio3.Session

import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.json.JSONObject

class Auth(manager: Manager) {
    val sign: String
    val deviceId: String

    init {
        val response = manager.get("https://radio.yandex.ru/api/v2.1/handlers/auth", null, null);
        val authData = JSONObject(response)
        sign = authData.getString("csrf");
        deviceId = authData.getString("device_id");

        val cookie = Cookie.Builder().domain("https://radio.yandex.ru".toHttpUrlOrNull()!!.topPrivateDomain()!!)
            .name("device_id").value("\"$deviceId\"").expiresAt(Long.MAX_VALUE).build();
        val cookies = ArrayList<Cookie>()
        cookies.add(cookie)
        manager.okHttpClient.cookieJar.saveFromResponse("https://radio.yandex.ru".toHttpUrlOrNull()!!, cookies)
    }
}