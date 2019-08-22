package com.washinson.yaradio3.player


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.exoplayer2.Player
import com.washinson.yaradio3.common.Mp3Downloader
import com.washinson.yaradio3.session.Session
import android.app.AlertDialog
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Context
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.widget.*
import androidx.core.app.ActivityCompat
import com.washinson.yaradio3.R
import com.washinson.yaradio3.TagsFragment
import kotlinx.android.synthetic.main.fragment_player_info.view.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.text.SimpleDateFormat


/**
 * A simple [Fragment] subclass.
 *
 */
class PlayerInfoFragment : Fragment(), CoroutineScope {
    protected val job = SupervisorJob() // экземпляр Job для данной активности
    override val coroutineContext = Dispatchers.Main.immediate+job

    private var listener: OnFragmentInteractionListener? = null

    lateinit var nextButton: ImageView
    lateinit var pauseButton: ImageView
    lateinit var likeButton: ImageView
    lateinit var dislikeButton: ImageView
    lateinit var settingsButton: ImageView
    lateinit var advancedButton: ImageView
    lateinit var historyButton: ImageView
    lateinit var backButton: ImageView
    lateinit var trackCover: ImageView
    lateinit var trackTitle: TextView
    lateinit var trackArtist: TextView
    lateinit var trackDuration: TextView
    lateinit var trackTime: TextView
    lateinit var spinKitView: SpinKitView
    lateinit var progressBar: SeekBar

    var isInterfaceInited = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player_info, container, false)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initInterface()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    fun initInterface() {
        nextButton = view!!.findViewById(R.id.forward)
        pauseButton = view!!.findViewById(R.id.pause)
        likeButton = view!!.findViewById(R.id.like)
        dislikeButton = view!!.findViewById(R.id.dislike)
        settingsButton = view!!.findViewById(R.id.settings)
        advancedButton = view!!.findViewById(R.id.advanced)
        historyButton = view!!.findViewById(R.id.history)
        backButton = view!!.findViewById(R.id.back)

        trackCover = view!!.findViewById(R.id.album)
        trackTitle = view!!.findViewById(R.id.name)
        trackArtist = view!!.findViewById(R.id.artist)
        trackDuration = view!!.findViewById(R.id.duration)
        trackTime = view!!.findViewById(R.id.time)

        progressBar = view!!.findViewById(R.id.progressBar)

        //spinKitView = view!!.findViewById(R.id.spin_kit)
        val curActivity = (activity ?: return) as PlayerActivity
        nextButton.setOnClickListener {
            curActivity.playerService?.mediaSessionCallback?.onSkipToNext()
        }
        pauseButton.setOnClickListener {
            if (curActivity.playerService?.mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING)
                curActivity.playerService?.mediaSessionCallback?.onPause()
            else
                curActivity.playerService?.mediaSessionCallback?.onPlay()
        }
        likeButton.setOnClickListener {
            curActivity.sendBroadcast(Intent(MediaSessionCallback.likeIntentFilter))
        }
        dislikeButton.setOnClickListener {
            curActivity.sendBroadcast(Intent(MediaSessionCallback.dislikeIntentFilter))
        }

        settingsButton.setOnClickListener {
            startActivityForResult(Intent(context, PlayerTagSettingsActivity::class.java), 0)
        }
        trackCover.setOnClickListener {
            onLoadTrackClicked()
        }

        trackTitle.setOnClickListener { Utils.trackIntoClipboard(context!!,
            Session.getInstance(0, context).track ?: return@setOnClickListener) }
        trackArtist.setOnClickListener { Utils.trackIntoClipboard(context!!,
            Session.getInstance(0, context).track ?: return@setOnClickListener) }

        advancedButton.setOnClickListener {
            listener?.changeView(2)
        }
        historyButton.setOnClickListener {
            listener?.changeView(0)
        }
        backButton.setOnClickListener {
            listener?.finishActivity()
        }

        progressBar.isEnabled = false

        isInterfaceInited = true

        val metadata = curActivity.playerService?.mediaSession?.controller?.metadata
        if (metadata != null) onMetadataUpdate(metadata)

        val state = curActivity.playerService?.mediaSession?.controller?.playbackState
        if (state != null) updateOnPlaybackState(state)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            val curMoodEnergy =
                data.getStringExtra("moodEnergy") ?: return
            val curDiversity =
                data.getStringExtra("diversity") ?: return
            val curLanguage =
                data.getStringExtra("language") ?: return
            launch(Dispatchers.IO) {
                Session.getInstance(0, activity).updateInfo(curLanguage, curMoodEnergy, curDiversity)
                launch(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.updated), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun onLoadTrackClicked() {
        val builder = AlertDialog.Builder(context)

        builder.setMessage(getString(R.string.download_track))
            .setTitle(getString(R.string.download_title))

        builder.setPositiveButton(getString(android.R.string.yes)) { dialogInterface, _ ->
            val requested = ContextCompat.checkSelfPermission(context!!, WRITE_EXTERNAL_STORAGE)
            if(requested == PackageManager.PERMISSION_DENIED){
                val ACCESS_EXTERNAL_STORAGE_STATE = 1
                ActivityCompat.requestPermissions(activity!!,
                    arrayOf(WRITE_EXTERNAL_STORAGE),
                    ACCESS_EXTERNAL_STORAGE_STATE)
            } else {
                try {
                    loadTrack()
                } catch (e: Exception) {
                    val alertBuilder1 = AlertDialog.Builder(activity)
                    alertBuilder1.setMessage(getString(R.string.error))
                        .setTitle(e.message)
                        .create().show()
                    e.printStackTrace()
                }
            }
            dialogInterface.cancel()
        }
        builder.setNegativeButton(getString(android.R.string.no)) { dialogInterface, _ ->
            dialogInterface.cancel()
        }

        builder.create().show()
    }

    fun loadTrack() {
        val mp3Downloader = Mp3Downloader(context!!)
        val track = Session.getInstance(0, context).track ?: return
        mp3Downloader.loadTrack(track)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            when(requestCode) {
                1 -> {
                    if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        loadTrack()
                    } else {
                        throw Exception(getString(R.string.permission_denied))
                    }
                }
            }
        } catch (e: Exception) {
            val alertBuilder1 = AlertDialog.Builder(activity)
            alertBuilder1.setMessage(getString(R.string.error))
                .setTitle(e.message)
                .create().show()
            e.printStackTrace()
        }
    }

    fun onServiceConnected(playerService: PlayerService) {
        playerService.simpleExoPlayer.addListener(object : Player.EventListener {
            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                setProgressIfBuffering()
            }
        })

        val metadata = playerService.mediaSession.controller.metadata
        onMetadataUpdate(metadata)

        val state = playerService.mediaSession.controller.playbackState
        updateOnPlaybackState(state)
    }

    @Suppress("DEPRECATION")
    fun updateOnPlaybackState(state: PlaybackStateCompat) {
        if (!isInterfaceInited)
            return

        val curActivity = (activity ?: return) as PlayerActivity

        //progressBar.setProgress(state.position.toInt(), true)
        progressBar.progress = state.position.toInt()
        if(curActivity.playerService?.mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(curActivity.getDrawable(R.drawable.ic_pause_button))
            else
                pauseButton.setImageDrawable(resources.getDrawable(R.drawable.ic_pause_button))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(curActivity.getDrawable(R.drawable.ic_button_play))
            else
                pauseButton.setImageDrawable(resources.getDrawable(R.drawable.ic_button_play))
        }

        if(curActivity.session?.track?.liked == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(curActivity.getDrawable(R.drawable.ic_like_active))
            else
                likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_like_active))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(curActivity.getDrawable(R.drawable.ic_like_passive))
            else
                likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_like_passive))
        }

        val duration = state.position / 1000
        trackTime.text = String.format("%02d:%02d", duration / 60, duration % 60)
    }

    @SuppressLint("SetTextI18n")
    fun onMetadataUpdate(metadata: MediaMetadataCompat) {
        if (!isInterfaceInited)
            return

        trackTitle.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_TITLE)
        trackArtist.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST)
        trackCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))
        val duration: Long = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION) / 1000

        trackDuration.text = String.format("%02d:%02d", duration / 60, duration % 60)
        trackTime.text = "00:00"

        setProgressIfBuffering()

        progressBar.progress = 0
        progressBar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
    }

    fun setProgressIfBuffering() {
        val activity = (activity ?: return) as PlayerActivity
        val service = activity.playerService ?: return
        val metadata = service.mediaSession.controller.metadata ?: return
        val simpleExoPlayer = service.simpleExoPlayer

        //if (metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) == null
        //    || simpleExoPlayer.playbackState == STATE_BUFFERING)
            //spinKitView.visibility = View.VISIBLE
        //else
            //spinKitView.visibility = View.GONE

    }

    interface OnFragmentInteractionListener {
        fun changeView(id: Int)
        fun finishActivity()
    }
}
