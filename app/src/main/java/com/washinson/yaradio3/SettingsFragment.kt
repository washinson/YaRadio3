package com.washinson.yaradio3


import android.app.AlarmManager
import android.app.PendingIntent
import android.content.*
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup

import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.*
import androidx.media.session.MediaButtonReceiver
import com.washinson.yaradio3.player.PlayerService
import com.washinson.yaradio3.session.Session
import com.washinson.yaradio3.station.Tag
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

    private var playerService: PlayerService? = null
    private var listener: OnFragmentInteractionListener? = null

    var sharedPreferences: SharedPreferences? = null
    lateinit var alarmIntent: PendingIntent
    lateinit var alarmMgr: AlarmManager

    lateinit var hoursText: TextView
    lateinit var minutesText: TextView
    lateinit var setTimerButton: Button
    lateinit var timerInfoButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedPreferences = activity?.getSharedPreferences(TAG_PREFERENCES, Context.MODE_PRIVATE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
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

        connectService()

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

        alarmIntent = MediaButtonReceiver.buildMediaButtonPendingIntent(context, PlaybackStateCompat.ACTION_PAUSE)
        alarmMgr = context!!.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        hoursText = view.findViewById<EditText>(R.id.hours)
        minutesText = view.findViewById<EditText>(R.id.minutes)

        setTimerButton = view.findViewById(R.id.set_timer_button)
        setTimerButton.setOnClickListener{
            playerService ?: return@setOnClickListener
            if (playerService!!.curTag == null) {
                Toast.makeText(context!!, getString(R.string.please_start_radio), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (playerService!!.timerDate == null) {
                // Timer doesn't created
                val hours = hoursText.text
                val minutes = minutesText.text

                val additionalTime: Int
                try {
                    additionalTime = (3600 * hours.toString().toInt() + 60 * minutes.toString().toInt()) * 1000
                    if (additionalTime <= 0) throw NumberFormatException()
                } catch (e: NumberFormatException) {
                    Toast.makeText(context!!, getString(R.string.incorrect_input), Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                alarmMgr.cancel(alarmIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmMgr.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + additionalTime, alarmIntent)
                } else {
                    alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,SystemClock.elapsedRealtime() + additionalTime, alarmIntent)
                }

                val date = System.currentTimeMillis() + additionalTime
                playerService!!.timerDate = date
                val output = getString(R.string.playback_will_stop_in).format(getDate(date, "HH:mm"))
                setTimerButton.text = getString(android.R.string.cancel)
                Toast.makeText(context!!, output, Toast.LENGTH_SHORT).show()
            } else {
                // Timer created
                alarmMgr.cancel(alarmIntent)
                playerService!!.timerDate = null
                setTimerButton.text = getString(R.string.set_timer)
            }
        }

        timerInfoButton = view.findViewById<Button>(R.id.get_timer_info)
        timerInfoButton.setOnClickListener {
            if (playerService?.timerDate != null) {
                val time = playerService!!.timerDate!! - System.currentTimeMillis()

                val hours = time / 3600_000
                val minutes = time / 60_000 % 60
                val seconds = time / 1000 % 60

                val left = StringBuilder().append(getString(R.string.left))
                if (hours > 0) left.append(" $hours " + getString(R.string.hours).toLowerCase())
                if (minutes > 0) left.append(" $minutes " + getString(R.string.minutes).toLowerCase())
                if (seconds > 0) left.append(" $seconds " + getString(R.string.seconds).toLowerCase())

                Toast.makeText(context, left, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, getString(R.string.no_information), Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<ImageView>(R.id.back).setOnClickListener { listener?.backStackFragment() }
    }

    interface OnFragmentInteractionListener {
        fun backStackFragment()
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

    fun connectService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context!!.bindService(Intent(context!!, PlayerService::class.java),
                mConnection, Context.BIND_AUTO_CREATE)
        } else {
            context!!.bindService(Intent(context!!, PlayerService::class.java),
                mConnection, Context.BIND_AUTO_CREATE)
        }
    }

    private val mConnection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            playerService = (service as PlayerService.PlayerServiceBinder).getService()

            if (playerService!!.timerDate != null) {
                setTimerButton.text = getString(android.R.string.cancel)
            } else {
                setTimerButton.text = getString(R.string.set_timer)
            }

            if (playerService!!.timerDate != null) {
                val time = playerService!!.timerDate!! - System.currentTimeMillis()

                val hours = time / 3600_000
                val minutes = time / 60_000 % 60

                hoursText.text = hours.toString()
                minutesText.text = minutes.toString()
            }
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            playerService = null
        }
    }
}
