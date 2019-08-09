package com.washinson.yaradio3

import android.accounts.NetworkErrorException
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Environment
import com.google.android.material.snackbar.Snackbar
import androidx.core.view.GravityCompat
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.MenuItem
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.widget.TextView
import android.widget.Toast
import com.washinson.yaradio3.Player.PlayerActivity
import com.washinson.yaradio3.Session.Session
import com.washinson.yaradio3.Session.SettingsFragment
import com.washinson.yaradio3.Station.Tag
import com.washinson.yaradio3.Station.Type
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener,
    TypeFragment.OnFragmentInteractionListener, CoroutineScope {
    protected val job = SupervisorJob() // экземпляр Job для данной активности
    override val coroutineContext = Dispatchers.Main.immediate+job

    override fun start(tag: Tag) {
        launch(Dispatchers.IO) {
            session!!.setTagToPlay(tag)
            launch (Dispatchers.Main) {
                startActivity(Intent(this@MainActivity, PlayerActivity::class.java))
            }
        }
    }

    var session: Session? = null
    var types: ArrayList<Type>? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        //val fab: FloatingActionButton = findViewById(R.id.fab)
        //fab.setOnClickListener { view ->
        //    Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
        //        .setAction("Action", null).show()
        //}
        val drawerLayout: DrawerLayout = findViewById(R.id.drawer_layout)
        val navView: NavigationView = findViewById(R.id.nav_view)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        navView.setNavigationItemSelectedListener(this)

        loadSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    fun login() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK) {
            val cookies = data?.getStringExtra("cookies")
            session?.login(cookies)
            loadSession()
        }
    }

    fun loadSession() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        navView.menu.clear()
        supportFragmentManager.beginTransaction().replace(R.id.tags_frame, LoadingFragment()).commit()
        launch(Dispatchers.IO) {
            try {
                session = Session.getInstance(0, this@MainActivity)
                loadTypes()
                updateNavButtons()
            } catch (error: NetworkErrorException) {
                error.printStackTrace()
                Snackbar.make(findViewById(android.R.id.content), getString(R.string.no_internet), Snackbar.LENGTH_LONG).show()
            }
        }
    }

    fun updateNavButtons() {
        val navView: NavigationView = findViewById(R.id.nav_view)
        val navHeaderView = navView.getHeaderView(0)
        launch (Dispatchers.Main) {
            if (session!!.isUserLoggedIn()) {
                navHeaderView.findViewById<TextView>(R.id.user_login_button).setOnClickListener {
                    logout()
                }
                navHeaderView.findViewById<TextView>(R.id.user_login_text).text = session?.getUserLogin()
                navHeaderView.findViewById<TextView>(R.id.user_login_button).text = getString(R.string.logout_text)


                /*launch(Dispatchers.IO) {
                    val filename = "myfile"
                    val fileContents = session?.manager?.get("https://radio.yandex.ru/handlers/library.jsx?lang=ru", null, null) ?: return@launch

                    val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath + "/filename.txt")
                    file.createNewFile()
                    val fileOutputStream = FileOutputStream(file)

                    fileOutputStream.write(fileContents.toByteArray())
                }*/

            } else {
                navHeaderView.setOnClickListener {
                    login()
                }
                navHeaderView.findViewById<TextView>(R.id.user_login_text).text = getString(R.string.user_guest)
                navHeaderView.findViewById<TextView>(R.id.user_login_button).text = getString(R.string.login_text)
            }
        }
    }

    fun logout() {
        Toast.makeText(this, "Ha-ha, you closed here!!! (${getString(R.string.not_supported)})", Toast.LENGTH_SHORT).show()
        //TODO: logout
        //session?.logout()
    }

    fun loadTypes() {
        try {
            types = session!!.getTypes()
        } catch(e: Exception) {
            // todo: rem it
            launch(Dispatchers.Main) {
                val sw = StringWriter()
                val pw = PrintWriter(sw)
                e.printStackTrace(pw)
                e.printStackTrace()
                val sStackTrace = sw.toString() // stack trace as a string

                val builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("Важное сообщение!")
                    .setMessage(sStackTrace)
                    .setIcon(android.R.drawable.ic_delete)
                    .setCancelable(false)
                    .setNegativeButton("ОК",
                        DialogInterface.OnClickListener { dialog, id -> dialog.cancel() })
                val alert = builder.create()
                alert.show()
            }
        }
        val navView: NavigationView = findViewById(R.id.nav_view)
        launch (Dispatchers.Main) {
            for(i in 0 until types!!.size)
                navView.menu.add(Menu.NONE, i, Menu.NONE, types!![i].name)
        }
        loadType(types!![1])
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
                //supportFragmentManager.beginTransaction().replace(R.id.tags_frame, SettingsFragment()).commit()
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
            supportFragmentManager.beginTransaction().replace(R.id.tags_frame, TypeFragment(type)).commit()
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
