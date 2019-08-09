package com.washinson.yaradio3.Session


import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup

import android.content.Intent
import android.net.Uri


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
        return inflater.inflate(com.washinson.yaradio3.R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if(sharedPreferences == null)
            return

        val button: RadioButton = when(sharedPreferences!!.getString(QUALITY, defautQualityValue)) {
            "mp3_192" -> view.findViewById(com.washinson.yaradio3.R.id.radioButtonMP3_192)
            "aac_192" -> view.findViewById(com.washinson.yaradio3.R.id.radioButtonAAC_192)
            "aac_128" -> view.findViewById(com.washinson.yaradio3.R.id.radioButtonAAC_128)
            "aac_64" ->  view.findViewById(com.washinson.yaradio3.R.id.radioButtonAAC_64)
            else -> view.findViewById(com.washinson.yaradio3.R.id.radioButtonAAC_192)
        }
        button.isChecked = true

        val radioGroup: RadioGroup = view.findViewById(com.washinson.yaradio3.R.id.radio_group)
        radioGroup.setOnCheckedChangeListener {
                _, i ->
            val curQuality = when(i) {
                com.washinson.yaradio3.R.id.radioButtonMP3_192 -> "mp3_192"
                com.washinson.yaradio3.R.id.radioButtonAAC_192 -> "aac_192"
                com.washinson.yaradio3.R.id.radioButtonAAC_128 -> "aac_128"
                com.washinson.yaradio3.R.id.radioButtonAAC_64  -> "aac_64"
                else -> "aac_192"
            }
            sharedPreferences!!.edit().putString(QUALITY, curQuality).apply()
        }

        val apkButton = view.findViewById<Button>(com.washinson.yaradio3.R.id.apk_button)
        apkButton.setOnClickListener{
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/washinson/YaRadio3/releases")))
        }
        val version = context!!.packageManager.getPackageInfo(context!!.packageName, 0).versionName
        apkButton.text = apkButton.text.toString().format(version)
    }

}
