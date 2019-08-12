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
import android.os.Build
import android.os.SystemClock
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.*
import androidx.media.session.MediaButtonReceiver
import com.washinson.yaradio3.Player.PlayerService
import com.washinson.yaradio3.Session.Session
import java.text.SimpleDateFormat
import java.util.*


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
    lateinit var alarmIntent: PendingIntent
    lateinit var alarmMgr: AlarmManager

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

        alarmIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_STOP)
        alarmMgr = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val setTimerButton: Button = view.findViewById(R.id.set_timer_button)
        setTimerButton.setOnClickListener{
            val hours = view.findViewById<EditText>(R.id.hours).text
            val minutes = view.findViewById<EditText>(R.id.minutes).text

            val additionalTime: Int
            try {
                additionalTime = (3600 * hours.toString().toInt() + 60 * minutes.toString().toInt()) * 1000
                if (additionalTime <= 0) throw NumberFormatException()
            } catch (e: NumberFormatException) {
                Toast.makeText(context!!, getString(R.string.incorrect_input), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            alarmMgr.cancel(alarmIntent)
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + additionalTime, alarmIntent)

            val output = getString(R.string.playback_will_stop_in).format(getDate(System.currentTimeMillis() + additionalTime, "kk:mm"))
            Toast.makeText(context!!, output, Toast.LENGTH_SHORT).show()
        }

        view.findViewById<Button>(R.id.get_timer_info).setOnClickListener {
            if (false && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val time = (alarmMgr.nextAlarmClock?.triggerTime ?: return@setOnClickListener) - System.currentTimeMillis()

                val hours = time / 3600_000
                val minutes = time / 60_000 % 3600_000
                val seconds = time / 1000 % 60_000

                Toast.makeText(context, getString(R.string.timer_info_print).format(hours, minutes, seconds), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.not_supported), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Return date in specified format.
     * @param milliSeconds Date in milliseconds
     * @param dateFormat Date format
     * @return String representing date in specified format
     */
    fun getDate(milliSeconds: Long, dateFormat: String): String {
        // Create a DateFormatter object for displaying date in specified format.
        val formatter = SimpleDateFormat(dateFormat, Locale.UK)

        // Create a calendar object that will convert the date and time value in milliseconds to date.
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = milliSeconds
        return formatter.format(calendar.time)
    }
}
