package com.washinson.yaradio3.common

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.washinson.yaradio3.session.Track
import android.app.DownloadManager.EXTRA_DOWNLOAD_ID
import android.content.IntentFilter
import android.net.Uri
import androidx.core.content.ContextCompat.getSystemService
import org.jaudiotagger.audio.mp3.MP3File
import android.provider.MediaStore
import org.jaudiotagger.audio.AudioFileIO
import java.io.File
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException
import org.jaudiotagger.tag.TagException
import org.jaudiotagger.audio.exceptions.CannotReadException
import java.io.IOException
import org.jaudiotagger.tag.id3.ID3v24Tag
import org.jaudiotagger.tag.FieldDataInvalidException
import android.R.attr.track
import org.jaudiotagger.tag.FieldKey
import android.R.attr.tag
import android.R.attr.tag
import org.jaudiotagger.tag.images.ArtworkFactory
import org.jaudiotagger.tag.images.Artwork
import com.google.android.exoplayer2.util.Util.toByteArray
import android.graphics.Bitmap
import android.R.attr.track
import android.accounts.NetworkErrorException
import android.os.Environment
import android.os.Environment.DIRECTORY_MUSIC
import com.bumptech.glide.Glide
import com.washinson.yaradio3.R
import com.washinson.yaradio3.session.Session
import com.washinson.yaradio3.session.YandexCommunicator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.jaudiotagger.audio.exceptions.CannotWriteException
import org.json.JSONObject
import java.io.ByteArrayOutputStream


class Mp3Downloader(val context: Context){
    companion object {
        var queue: HashMap<Long, Track> = HashMap()
    }

    private val onDownloadComplete = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, intent: Intent?) {
            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val id = intent?.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1) ?: return

            if (id == -1L) return

            val track = queue[id] ?: return
            queue.remove(id)
            val uri = downloadManager.getUriForDownloadedFile(id) ?: return

            // Network thread for picture loading
            GlobalScope.launch(Dispatchers.IO) {
                setTags(track, uri)
            }
        }
    }

    init {
        context.registerReceiver(onDownloadComplete, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
    }

    fun setTags(track: Track, uri: Uri) {
        val mp3: MP3File

        val filePath: String

        if (uri.scheme == "content") {
            val cursor =
                context.contentResolver.query(uri,
                    arrayOf(MediaStore.MediaColumns.DATA), null, null, null) ?: return
            cursor.moveToFirst()
            filePath = cursor.getString(0)
            cursor.close()
        } else {
            filePath = uri.path ?: return
        }

        try {
            mp3 = AudioFileIO.read(File(filePath)) as MP3File
        } catch (e: CannotReadException) {
            return
        } catch (e: TagException) {
            return
        } catch (e: IOException) {
            return
        } catch (e: ReadOnlyFileException) {
            return
        } catch (e: InvalidAudioFrameException) {
            return
        }

        val tag = ID3v24Tag()

        try {
            tag.setField(FieldKey.ARTIST, track.artist)
            tag.setField(FieldKey.TITLE, track.title)
            tag.setField(FieldKey.ALBUM, track.album)
            tag.setField(FieldKey.ALBUM_ARTIST, track.artist)
        } catch (e: FieldDataInvalidException) {
            return
        }

        try {
            val bmp = Glide.with(context).asBitmap().load(track.getCoverSize(600,600)).submit().get()
            val stream = ByteArrayOutputStream()
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, stream)
            val byteArray = stream.toByteArray()
            bmp.recycle()

            val artwork = ArtworkFactory.getNew()
            artwork.pictureType = 3
            artwork.description = ""
            artwork.mimeType = "image/jpeg"
            artwork.binaryData = byteArray

            tag.setField(artwork)
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: FieldDataInvalidException) {
            e.printStackTrace()
        }

        mp3.tag = tag

        try {
            AudioFileIO.write(mp3)
        } catch (ignore: CannotWriteException) {

        }
    }

    fun loadTrack(track: Track) {
        val file = File(
            (Environment.getExternalStoragePublicDirectory(DIRECTORY_MUSIC).absolutePath +
                    "/YaRadio/" + track.tag.name + "/" + track.title + "-" + track.artist + ".mp3").replace(
                " ",
                "_"
            )
        )
        if (file.exists()) {
            throw IOException(context.getString(R.string.already_exist))
        }
        GlobalScope.launch(Dispatchers.IO) {
            val path = "https://api.music.yandex.net/tracks/${track.id}/download-info"

            val manager = Session.getInstance(0, context).manager
            val response = manager.get(path, null, track.tag) ?: return@launch
            val qualities = JSONObject(response)

            val qualityInfo = YandexCommunicator.QualityInfo(qualities)

            track.qualityInfo = qualityInfo

            val src = qualityInfo.byQuality("mp3_192") + "&format=json"

            val builder = okhttp3.Request.Builder().get().url(src)
            builder.addHeader("Host", "storage.mds.yandex.net")
            manager.setDefaultHeaders(builder)

            val result = manager.get(src, builder.build(), track.tag) ?: throw NetworkErrorException()
            val downloadInformation = JSONObject(result)
            val info = YandexCommunicator.DownloadInfo(downloadInformation)
            val downloadPath = info.getSrc()

            val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val request = DownloadManager.Request(Uri.parse(downloadPath))
            request.setTitle(track.title)
            request.setDescription(track.artist)
            request.setDestinationUri(Uri.fromFile(file))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            request.setVisibleInDownloadsUi(true)
            queue[downloadManager.enqueue(request)] = track
        }
    }
}