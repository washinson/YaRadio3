package com.washinson.yaradio3.player

import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import com.washinson.yaradio3.session.Session
import android.R.attr.track
import android.content.ClipData
import android.widget.Toast
import com.washinson.yaradio3.R
import com.washinson.yaradio3.session.Track


class Utils {
    companion object {
        fun trackIntoClipboard(context: Context, track: Track) {
            val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager?

            val clip =
                ClipData.newPlainText(context.getString(R.string.track_info), track.artist + " - " + track.title)
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, context.getString(R.string.copied), Toast.LENGTH_SHORT).show()
            }
        }
    }
}