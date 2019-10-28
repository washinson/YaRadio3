package com.washinson.yaradio3

import android.accounts.NetworkErrorException
import android.app.Activity
import android.content.*
import android.media.AudioManager
import android.media.session.MediaSession
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.*
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import android.view.inputmethod.BaseInputConnection
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.media.session.MediaButtonReceiver
import com.washinson.yaradio3.common.DisableBatterySaverDialog
import com.washinson.yaradio3.common.ThreadWaitForResult
import com.washinson.yaradio3.player.MediaSessionCallback
import com.washinson.yaradio3.player.PlayerActivity
import com.washinson.yaradio3.player.PlayerService
import com.washinson.yaradio3.session.Session
import com.washinson.yaradio3.station.Tag
import com.washinson.yaradio3.station.Type
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener, CoroutineScope,
        RecommendedFragment.OnFragmentInteractionListener, TagsFragment.OnFragmentInteractionListener, SettingsFragment.OnFragmentInteractionListener {
    protected val job = SupervisorJob() // экземпляр Job для данной активности
    override val coroutineContext = Dispatchers.Main.immediate+job

    val settingsFragmentTag: String = "settingsFragmentTag"
    val tagsFragmentTag: String = "tagsFragmentTag"
    val tagsExtendedFragmentTag: String = "tagsExtendedFragmentTag"

    var session: Session? = null
    var types: ArrayList<Type>? = null
    lateinit var navView: NavigationView
    lateinit var drawerLayout: DrawerLayout
    lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //val toolbar: Toolbar = findViewById(R.id.toolbar)
        //setSupportActionBar(toolbar)

        loginButton = findViewById(R.id.user_login_button)
        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        //val toggle = ActionBarDrawerToggle(
        //    this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        //)
        //drawerLayout.addDrawerListener(toggle)
        //toggle.syncState()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            updateStatusBarColor(R.color.colorPlayerHeaderAlpha)
        }

        navView.setNavigationItemSelectedListener(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val sharedPreferences = getSharedPreferences("DisableBatteryDialog", Context.MODE_PRIVATE)
            if (!sharedPreferences.getBoolean("showed", false)) {
                DisableBatterySaverDialog.create(this).show()
                sharedPreferences.edit().putBoolean("showed", true).apply()
            }
        }

        loadSession()
    }

    override fun openSettings() {
        updateStatusBarColor(R.color.colorHeaderAlpha)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left, R.anim.anim_slide_in_left, R.anim.anim_slide_out_right)
            .addToBackStack(settingsFragmentTag).replace(R.id.tags_frame, SettingsFragment(), settingsFragmentTag).commitAllowingStateLoss()
    }

    override fun openNavBarMenu() {
        drawerLayout.openDrawer(Gravity.LEFT)
    }

    override fun backStackFragment() {
        supportFragmentManager.popBackStackImmediate()
    }

    override fun updateStatusBarColor(colorId: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            window.statusBarColor = ContextCompat.getColor(this, colorId)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onParentTagSelected(tags: ArrayList<Tag>) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left, R.anim.anim_slide_in_left, R.anim.anim_slide_out_right)
            .addToBackStack(tagsFragmentTag).replace(R.id.tags_frame, TagsFragment(tags), tagsFragmentTag).commitAllowingStateLoss()
    }

    override fun startTag(tag: Tag) {
        val intent = Intent(this@MainActivity, PlayerActivity::class.java)
        intent.putExtra("tag", "${tag.id}:${tag.tag}")
        startActivity(intent)
    }

    fun login() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            val cookies = data?.getStringExtra("cookies")

            launch(Dispatchers.IO) {
                stopPlayback()
                session?.login(cookies)
                launch(Dispatchers.Main) {
                    loadSession()
                }
            }
        }
    }

    fun stopPlayback() {
        sendBroadcast(Intent(MediaSessionCallback.stopIntentFilter))
    }

    fun loadSession() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.menu.clear()

        // Erase stack: full restart
        while(supportFragmentManager.popBackStackImmediate()){}

        supportFragmentManager.beginTransaction().replace(R.id.tags_frame, LoadingFragment()).commitAllowingStateLoss()
        launch(Dispatchers.IO) {
            var result = false
            while(!result) {
                try {
                    session = Session.getInstance(0, this@MainActivity)
                    loadTypes()
                    updateNavButtons()
                    result = true
                } catch (error: NetworkErrorException) {
                    error.printStackTrace()
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        getString(R.string.no_internet),
                        Snackbar.LENGTH_LONG
                    ).show()
                    delay(5000)
                }
            }
        }
    }

    fun updateNavButtons() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navHeaderView = navView.getHeaderView(0)
        launch (Dispatchers.Main) {
            if (session!!.isUserLoggedIn()) {
                loginButton.setOnClickListener {
                    logout()
                }
                navHeaderView.findViewById<TextView>(R.id.user_login_text).text = session?.getUserLogin()
                loginButton.text = getString(R.string.logout_text)
            } else {
                loginButton.setOnClickListener {
                    login()
                }
                navHeaderView.findViewById<TextView>(R.id.user_login_text).text = getString(R.string.user_guest)
                loginButton.text = getString(R.string.login_text)
            }
        }
    }

    fun logout() {
        launch(Dispatchers.IO) {
            stopPlayback()
            session?.logout()
            launch(Dispatchers.Main) {
                loadSession()
            }
        }
    }

    fun loadTypes() {
        types = session!!.types

        val recommendedFragment = RecommendedFragment(session!!.getRecommendedType())
        supportFragmentManager.beginTransaction().replace(R.id.tags_frame, recommendedFragment).commitAllowingStateLoss()
        //loadType(types!!.last())

        val navView: NavigationView = findViewById(R.id.nav_view)
        launch (Dispatchers.Main) {
            for(i in 0 until types!!.size)
                navView.menu.add(Menu.NONE, i, Menu.NONE, types!![i].name)
        }
    }
    override fun onBackPressed() {
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> {
                if (supportFragmentManager.findFragmentByTag(settingsFragmentTag) == null && types != null)
                    supportFragmentManager.beginTransaction().replace(R.id.tags_frame,
                        SettingsFragment(), settingsFragmentTag).addToBackStack(settingsFragmentTag).commitAllowingStateLoss()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun loadType(type: Type) {
        if (type.id == "user" && session != null && !session!!.isUserLoggedIn()) {
            Toast.makeText(this, getString(R.string.please_login), Toast.LENGTH_SHORT).show()
            login()
        } else {
            val tagsFragment = TagsFragment(type.tags)

            // Remove all excess fragments
            while (supportFragmentManager.findFragmentByTag(settingsFragmentTag) != null ||
                    supportFragmentManager.findFragmentByTag(tagsFragmentTag) != null ||
                    supportFragmentManager.findFragmentByTag(tagsExtendedFragmentTag) != null)
                if(!supportFragmentManager.popBackStackImmediate()) break

            val tag = if(tagsFragment.isFragmentHaveChild()) tagsExtendedFragmentTag else tagsFragmentTag
            supportFragmentManager.beginTransaction()
                .setCustomAnimations(R.anim.anim_slide_in_right, R.anim.anim_slide_out_left, R.anim.anim_slide_in_left, R.anim.anim_slide_out_right)
                .addToBackStack(tag)
                .replace(R.id.tags_frame, tagsFragment, tag).commitAllowingStateLoss()
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        if (types != null)
            loadType(types!![item.itemId])

        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}
