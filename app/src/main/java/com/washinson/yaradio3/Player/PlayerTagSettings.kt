package com.washinson.yaradio3.Player

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.RadioButton
import android.widget.RadioGroup
import androidx.core.content.ContextCompat
import com.washinson.yaradio3.R
import com.washinson.yaradio3.Session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class PlayerTagSettings : AppCompatActivity() {

    lateinit var moodGroup: RadioGroup
    lateinit var languageGroup: RadioGroup
    lateinit var diversityGroup: RadioGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_tag_settings)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this , R.color.colorPrimary)
        }

        moodGroup = findViewById(R.id.mood_energy)
        languageGroup = findViewById(R.id.language)
        diversityGroup = findViewById(R.id.diversity)

        GlobalScope.launch {
            val session = Session.getInstance(0, this@PlayerTagSettings)
            val settings = session.tag?.getSettings() ?: return@launch
            launch(Dispatchers.Main) {
                var q = 0
                for (i in settings.moodEnergies.possibleValues) {
                    val radioButton = RadioButton(this@PlayerTagSettings)
                    radioButton.text = i.second
                    radioButton.id = q++
                    radioButton.isChecked = i.first == settings.moodEnergy
                    moodGroup.addView(radioButton)
                }
                q = 0
                for (i in settings.languages.possibleValues) {
                    val radioButton = RadioButton(this@PlayerTagSettings)
                    radioButton.text = i.second
                    radioButton.id = q++
                    radioButton.isChecked = i.first == settings.language
                    languageGroup.addView(radioButton)
                }
                q = 0
                for (i in settings.diversities.possibleValues) {
                    val radioButton = RadioButton(this@PlayerTagSettings)
                    radioButton.text = i.second
                    radioButton.id = q++
                    radioButton.isChecked = i.first == settings.diversity
                    diversityGroup.addView(radioButton)
                }
            }
        }
    }
}
