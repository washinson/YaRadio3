package com.washinson.yaradio3.player


import android.Manifest
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
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.github.ybq.android.spinkit.SpinKitView
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Player.STATE_BUFFERING
import com.washinson.yaradio3.common.Mp3Downloader
import com.washinson.yaradio3.session.Session
import android.app.AlertDialog
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat


/**
 * A simple [Fragment] subclass.
 *
 */
class PlayerInfoFragment : Fragment() {
    lateinit var nextButton: ImageButton
    lateinit var pauseButton: ImageButton
    lateinit var likeButton: ImageButton
    lateinit var dislikeButton: ImageButton
    lateinit var settingsButton: ImageButton
    lateinit var trackCover: ImageView
    lateinit var trackLabel: TextView
    lateinit var spinKitView: SpinKitView
    lateinit var progressBar: ProgressBar

    var isInterfaceInited = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(com.washinson.yaradio3.R.layout.fragment_player_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initInterface()
    }

    fun initInterface() {
        nextButton = view!!.findViewById(com.washinson.yaradio3.R.id.track_next)
        pauseButton = view!!.findViewById(com.washinson.yaradio3.R.id.track_pause)
        likeButton = view!!.findViewById(com.washinson.yaradio3.R.id.track_like)
        dislikeButton = view!!.findViewById(com.washinson.yaradio3.R.id.track_dislike)
        settingsButton = view!!.findViewById(com.washinson.yaradio3.R.id.track_settings)

        trackCover = view!!.findViewById(com.washinson.yaradio3.R.id.track_cover)
        trackLabel = view!!.findViewById(com.washinson.yaradio3.R.id.track_label)

        progressBar = view!!.findViewById(com.washinson.yaradio3.R.id.track_progress_bar)

        spinKitView = view!!.findViewById(com.washinson.yaradio3.R.id.spin_kit)
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
            startActivity(Intent(context, PlayerTagSettingsActivity::class.java))
        }
        trackCover.setOnClickListener {
            onLoadTrackClicked()
        }

        isInterfaceInited = true

        val metadata = curActivity.playerService?.mediaSession?.controller?.metadata
        if (metadata != null) updateOnMedatada(metadata)

        val state = curActivity.playerService?.mediaSession?.controller?.playbackState
        if (state != null) updateOnPlaybackState(state)
    }

    fun onLoadTrackClicked() {
        val builder = AlertDialog.Builder(context)

        builder.setMessage(getString(com.washinson.yaradio3.R.string.download_track))
            .setTitle(getString(com.washinson.yaradio3.R.string.download_title))

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
                    alertBuilder1.setMessage(getString(com.washinson.yaradio3.R.string.error))
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
                        throw Exception(getString(com.washinson.yaradio3.R.string.permission_denied))
                    }
                }
            }
        } catch (e: Exception) {
            val alertBuilder1 = AlertDialog.Builder(activity)
            alertBuilder1.setMessage(getString(com.washinson.yaradio3.R.string.error))
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
        updateOnMedatada(metadata)

        val state = playerService.mediaSession.controller.playbackState
        updateOnPlaybackState(state)
    }

    @Suppress("DEPRECATION")
    fun updateOnPlaybackState(state: PlaybackStateCompat) {
        if (!isInterfaceInited)
            return

        val curActivity = (activity ?: return) as PlayerActivity

        progressBar.progress = state.position.toInt()
        if(curActivity.playerService?.mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(curActivity.getDrawable(com.washinson.yaradio3.R.drawable.exo_controls_pause))
            else
                pauseButton.setImageDrawable(resources.getDrawable(com.washinson.yaradio3.R.drawable.exo_controls_pause))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(curActivity.getDrawable(com.washinson.yaradio3.R.drawable.exo_controls_play))
            else
                pauseButton.setImageDrawable(resources.getDrawable(com.washinson.yaradio3.R.drawable.exo_controls_play))
        }

        if(curActivity.session?.track?.liked == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(curActivity.getDrawable(com.washinson.yaradio3.R.drawable.ic_liked))
            else
                likeButton.setImageDrawable(resources.getDrawable(com.washinson.yaradio3.R.drawable.ic_liked))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(curActivity.getDrawable(com.washinson.yaradio3.R.drawable.ic_like))
            else
                likeButton.setImageDrawable(resources.getDrawable(com.washinson.yaradio3.R.drawable.ic_like))
        }
    }

    @SuppressLint("SetTextI18n")
    fun updateOnMedatada(metadata: MediaMetadataCompat) {
        if (!isInterfaceInited)
            return
        trackLabel.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " - " + metadata.getString(
            MediaMetadataCompat.METADATA_KEY_TITLE)
        trackCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))

        setProgressIfBuffering()

        progressBar.progress = 0
        progressBar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
    }

    fun setProgressIfBuffering() {
        val activity = (activity ?: return) as PlayerActivity
        val service = activity.playerService ?: return
        val metadata = service.mediaSession.controller.metadata ?: return
        val simpleExoPlayer = service.simpleExoPlayer

        if (metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) == null
            || simpleExoPlayer.playbackState == STATE_BUFFERING)
            spinKitView.visibility = View.VISIBLE
        else
            spinKitView.visibility = View.GONE

    }
}
