package com.washinson.yaradio3.player

import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.washinson.yaradio3.common.ThreadWaitForResult
import com.washinson.yaradio3.R
import com.washinson.yaradio3.session.Session
import com.washinson.yaradio3.station.Settings
import kotlinx.coroutines.*

class PlayerTagSettingsActivity : AppCompatActivity(), CoroutineScope {
    protected val job = SupervisorJob() // экземпляр Job для данной активности
    override val coroutineContext = Dispatchers.Main.immediate+job

    lateinit var moodGroup: CustomRadioGroup
    lateinit var languageGroup: CustomRadioGroup
    lateinit var diversityGroup: CustomRadioGroup

    var settings: Settings? = null
    var session: Session? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player_tag_settings)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = ContextCompat.getColor(this , R.color.colorHeader)
        }

        moodGroup = CustomRadioGroup(findViewById(R.id.mood_energy))
        languageGroup = CustomRadioGroup(findViewById(R.id.language))
        diversityGroup =  CustomRadioGroup(findViewById(R.id.diversity))

        launch(Dispatchers.IO) {
            ThreadWaitForResult.load{
                session = Session.getInstance(0, this@PlayerTagSettingsActivity)
                settings = session!!.tag?.getSettings()
            }
            launch(Dispatchers.Main) {

                for ((id, i) in settings!!.moodEnergies.possibleValues.withIndex()) {
                    moodGroup.addItem(i.second, id)
                    if (i.first == settings!!.moodEnergy) moodGroup.setChecked(id)
                }
                for ((id, i) in settings!!.languages.possibleValues.withIndex()) {
                    languageGroup.addItem(i.second, id)
                    if (i.first == settings!!.language) languageGroup.setChecked(id)
                }
                for ((id, i) in settings!!.diversities.possibleValues.withIndex()) {
                    diversityGroup.addItem(i.second, id)
                    if (i.first == settings!!.diversity) diversityGroup.setChecked(id)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    fun updateResult() {
        val intent = Intent()
        if (settings != null) {
            val curMoodEnergy =
                settings!!.moodEnergies.possibleValues[moodGroup.checkedRadioButtonId].first
            val curDiversity =
                settings!!.diversities.possibleValues[diversityGroup.checkedRadioButtonId].first
            val curLanguage =
                settings!!.languages.possibleValues[languageGroup.checkedRadioButtonId].first
            if (curMoodEnergy != settings!!.moodEnergy || curDiversity != settings!!.diversity || curLanguage != settings!!.language) {
                intent.putExtra("moodEnergy", curMoodEnergy)
                intent.putExtra("diversity", curDiversity)
                intent.putExtra("language", curLanguage)
            }
        }
        setResult(Activity.RESULT_OK, intent)
    }

    inner class CustomRadioGroup(val layout: LinearLayout) {
        var checkedRadioButtonId = 0

        fun addItem(i: String, id: Int) {
            val newView = layoutInflater.inflate(R.layout.settings_radio_item, layout, false)

            val button = newView.findViewById<Button>(R.id.button)
            button.text = i
            button.id = id
            button.setOnClickListener { setChecked(it.id); updateResult() }

            layout.addView(newView)
            setChecked(checkedRadioButtonId)
        }

        @Suppress("DEPRECATION")
        fun setChecked(id: Int) {
            checkedRadioButtonId = id
            for (i in 0 until layout.childCount) {
                val cur = layout.getChildAt(i) as? Button ?: continue

                if (cur.id != checkedRadioButtonId)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        cur.background = this@PlayerTagSettingsActivity.getDrawable(R.drawable.settings_radio_item_passive)
                    else
                        cur.setBackgroundDrawable(resources.getDrawable(R.drawable.settings_radio_item_passive))
                else
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                        cur.background = this@PlayerTagSettingsActivity.getDrawable(R.drawable.settings_radio_item_active)
                    else
                        cur.setBackgroundDrawable(resources.getDrawable(R.drawable.settings_radio_item_active))
            }
        }
    }
}
