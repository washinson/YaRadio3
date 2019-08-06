package com.washinson.yaradio3

import android.annotation.SuppressLint
import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import okhttp3.HttpUrl
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import android.provider.Browser
import android.content.Context.MODE_PRIVATE
import android.content.ComponentName
import android.os.IBinder
import android.content.ServiceConnection
import android.content.Intent




class LoginActivity : AppCompatActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val browser = findViewById<WebView>(R.id.login_webview)
        browser.settings.javaScriptEnabled = true
        browser.loadUrl("https://passport.yandex.ru/auth?origin=radio")
        browser.setWebViewClient(MyWebViewClient())
    }

    internal inner class MyWebViewClient : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            if (CookieManager.getInstance().getCookie("https://radio.yandex.ru") != null &&
                CookieManager.getInstance().getCookie("https://radio.yandex.ru").contains("yandex_login")) {
                val intent = Intent()
                intent.putExtra("cookies", CookieManager.getInstance().getCookie("https://radio.yandex.ru"));
                setResult(RESULT_OK, intent)
                view!!.destroy();
                this@LoginActivity.finish()
            }
        }
    }
}
