package com.washinson.yaradio3


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.*
import androidx.media.session.MediaButtonReceiver
import com.washinson.yaradio3.Player.PlayerService
import com.washinson.yaradio3.Session.Session


/**
 * A simple [Fragment] subclass.
 *
 */
class SettingsFragment : Fragment() {
    companion object {
        const val TAG_PREFERENCES = "traffic"
        const val QUALITY = "quality"
        const val defautQualityValue = "aac_192"
    }

    var sharedPreferences: SharedPreferences? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = activity?.getSharedPreferences(TAG_PREFERENCES, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(sharedPreferences == null)
            return

        val button: RadioButton = when(sharedPreferences!!.getString(
            QUALITY,
            defautQualityValue
        )) {
            "mp3_192" -> view.findViewById(R.id.radioButtonMP3_192)
            "aac_192" -> view.findViewById(R.id.radioButtonAAC_192)
            "aac_128" -> view.findViewById(R.id.radioButtonAAC_128)
            "aac_64" ->  view.findViewById(R.id.radioButtonAAC_64)
            else -> view.findViewById(R.id.radioButtonAAC_192)
        }
        button.isChecked = true

        val radioGroup: RadioGroup = view.findViewById(R.id.radio_group)
        radioGroup.setOnCheckedChangeListener {
                _, i ->
            val curQuality = when(i) {
                R.id.radioButtonMP3_192 -> "mp3_192"
                R.id.radioButtonAAC_192 -> "aac_192"
                R.id.radioButtonAAC_128 -> "aac_128"
                R.id.radioButtonAAC_64 -> "aac_64"
                else -> "aac_192"
            }
            sharedPreferences!!.edit().putString(QUALITY, curQuality).apply()
            Session.getInstance(0, context).quality = curQuality
        }

        val apkButton = view.findViewById<Button>(R.id.apk_button)
        apkButton.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/washinson/YaRadio3/releases")))
        }
        val version = context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionName
        apkButton.text = apkButton.text.toString().format(version)

        val setTimerButton: Button = view.findViewById(R.id.set_timer_button)
        setTimerButton.setOnClickListener{
            val alarmMgr = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent= MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)

            val hours = view.findViewById<EditText>(R.id.hours).text
            val minutes = view.findViewById<EditText>(R.id.minutes).text

            val additionalTime = (3600 * hours.toString().toInt() + 60 * minutes.toString().toInt()) * 1000

            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + additionalTime, alarmIntent)

            Toast.makeText(context!!, getString(R.string.updated), Toast.LENGTH_SHORT).show()
        }
    }
}
