package com.washinson.yaradio3.session

class PostConfig {
    val cur = StringBuilder();

    fun put(f: String, s: String) {
        if (cur.length != 0)
            cur.append("&")
        cur.append(f).append("=").append(s)
    }

    override fun toString(): String {
        return cur.toString()
    }
}