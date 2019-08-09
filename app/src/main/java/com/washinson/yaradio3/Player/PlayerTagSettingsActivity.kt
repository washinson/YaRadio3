package com.washinson.yaradio3.Player

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.washinson.yaradio3.R
import com.washinson.yaradio3.Session.Session
import com.washinson.yaradio3.Station.Settings
import kotlinx.coroutines.*

class PlayerTagSettingsActivity : AppCompatActivity(), CoroutineScope {
    protected val job = SupervisorJob() // экземпляр Job для данной активности
    override val coroutineContext = Dispatchers.Main.immediate+job

    lateinit var moodGroup: RadioGroup
    lateinit var languageGroup: RadioGroup
    lateinit var diversityGroup: RadioGroup
    lateinit var fab: FloatingActionButton

    var settings: Settings? = null
    var session: Session? = null

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

        fab = findViewById(R.id.floatingActionButton2)

        fab.setOnClickListener {
            launch(Dispatchers.IO) {
                if (settings == null || session == null)
                    return@launch
                val newMood =
                    settings!!.moodEnergies.possibleValues[moodGroup.checkedRadioButtonId].first
                val newLanguage =
                    settings!!.languages.possibleValues[languageGroup.checkedRadioButtonId].first
                val newDiversity =
                    settings!!.diversities.possibleValues[diversityGroup.checkedRadioButtonId].first
                session!!.tag?.setSettings(newLanguage, newMood, newDiversity)

                launch(Dispatchers.Main) {
                    Toast.makeText(this@PlayerTagSettingsActivity, getString(R.string.updated), Toast.LENGTH_SHORT).show()
                }
            }
        }

        launch(Dispatchers.IO) {
            session = Session.getInstance(0, this@PlayerTagSettingsActivity)
            settings = session!!.tag?.getSettings() ?: return@launch
            launch(Dispatchers.Main) {
                var q = 0
                for (i in settings!!.moodEnergies.possibleValues) {
                    val radioButton = RadioButton(this@PlayerTagSettingsActivity)
                    radioButton.text = i.second
                    radioButton.id = q++
                    radioButton.isChecked = i.first == settings!!.moodEnergy
                    moodGroup.addView(radioButton)
                }
                q = 0
                for (i in settings!!.languages.possibleValues) {
                    val radioButton = RadioButton(this@PlayerTagSettingsActivity)
                    radioButton.text = i.second
                    radioButton.id = q++
                    radioButton.isChecked = i.first == settings!!.language
                    languageGroup.addView(radioButton)
                }
                q = 0
                for (i in settings!!.diversities.possibleValues) {
                    val radioButton = RadioButton(this@PlayerTagSettingsActivity)
                    radioButton.text = i.second
                    radioButton.id = q++
                    radioButton.isChecked = i.first == settings!!.diversity
                    diversityGroup.addView(radioButton)
                }
            }
        }
    }
}
