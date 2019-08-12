package com.washinson.yaradio3.Session

import android.text.TextUtils.replace
import com.washinson.yaradio3.Station.Tag
import org.json.JSONObject

@Suppress("SpellCheckingInspection")
class Track(jsonObject: JSONObject, val tag: Tag) {
    val id: Int
    val albumId: Int
    val album: String
    val title: String
    val batchId: String
    val artist: String
    val cover: String
    val durationMs: Long
    var liked: Boolean
    var qualityInfo: YandexCommunicator.QualityInfo? = null
    var disliked: Boolean = false

    init {
        val track = jsonObject.getJSONObject("track")
        id = track.getInt("id")
        albumId = track.getJSONArray("albums").getJSONObject(0).getInt("id")
        album = track.getJSONArray("albums").getJSONObject(0).getString("title")
        batchId = track.getString("batchId")
        title = track.getString("title")

        if(track.has("coverUri"))
            cover = "https://${track.getString("coverUri")}"
        else if (track.has("ogImage")) {
            cover = "https://${track.getString("ogImage")}"
        } else if(track.getJSONArray("albums").getJSONObject(0).has("cover")){
            cover = track.getJSONArray("albums")
                .getJSONObject(0).getJSONObject("cover").getString("uri")
        } else {
            // Cover not found
            cover = "https://music.yandex.ru/blocks/common/default.200x200.png"
        }

        durationMs = track.getLong("durationMs")
        val artists = track.getJSONArray("artists")
        val artistNameBuilder = StringBuilder().append("")
        for (i in 0 until artists.length()) {
            artistNameBuilder.append(artists.getJSONObject(i).getString("name"))
            if (i != artists.length() - 1) {
                artistNameBuilder.append(", ")
            }
        }

        artist = artistNameBuilder.toString()

        liked = jsonObject.getBoolean("liked")
    }

    fun getCoverSize(sizeX: Int, sizeY: Int): String {
        return cover.replace("%%", sizeX.toString() + "x" + sizeY.toString())
    }

    override fun toString(): String {
        return "Track(id=$id, albumId=$albumId, " +
                "album='$album', title='$title', " +
                "batchId='$batchId', artist='$artist', " +
                "cover='$cover', durationMs=$durationMs, " +
                "liked=$liked, disliked=$disliked)"
    }
}