package com.washinson.yaradio3


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
        return inflater.inflate(R.layout.fragment_player_info, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initInterface()
    }

    fun initInterface() {
        nextButton = view!!.findViewById(R.id.track_next)
        pauseButton = view!!.findViewById(R.id.track_pause)
        likeButton = view!!.findViewById(R.id.track_like)
        dislikeButton = view!!.findViewById(R.id.track_dislike)
        settingsButton = view!!.findViewById(R.id.track_settings)

        trackCover = view!!.findViewById(R.id.track_cover)
        trackLabel = view!!.findViewById(R.id.track_label)

        progressBar = view!!.findViewById(R.id.track_progress_bar)

        spinKitView = view!!.findViewById(R.id.spin_kit)
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
        isInterfaceInited = true

        val state = curActivity.playerService?.mediaSession?.controller?.playbackState
        if (state != null) updateOnPlaybackState(state)

        val metadata = curActivity.playerService?.mediaSession?.controller?.metadata
        if (metadata != null) updateOnMedatada(metadata)
    }

    @Suppress("DEPRECATION")
    fun updateOnPlaybackState(state: PlaybackStateCompat) {
        if (!isInterfaceInited)
            return

        val curActivity = (activity ?: return) as PlayerActivity

        progressBar.progress = state.position.toInt()
        if(curActivity.playerService?.mediaSession?.controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(curActivity.getDrawable(R.drawable.exo_controls_pause))
            else
                pauseButton.setImageDrawable(resources.getDrawable(R.drawable.exo_controls_pause))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                pauseButton.setImageDrawable(curActivity.getDrawable(R.drawable.exo_controls_play))
            else
                pauseButton.setImageDrawable(resources.getDrawable(R.drawable.exo_controls_play))
        }
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetTextI18n")
    fun updateOnMedatada(metadata: MediaMetadataCompat) {
        if (!isInterfaceInited)
            return
        val curActivity = (activity ?: return) as PlayerActivity
        trackLabel.text = metadata.getString(MediaMetadataCompat.METADATA_KEY_ARTIST) + " - " + metadata.getString(
            MediaMetadataCompat.METADATA_KEY_TITLE)
        trackCover.setImageBitmap(metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART))

        if(curActivity.session?.track?.liked == true) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(curActivity.getDrawable(R.drawable.ic_liked))
            else
                likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_liked))
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                likeButton.setImageDrawable(curActivity.getDrawable(R.drawable.ic_like))
            else
                likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_like))
        }

        if (metadata.getBitmap(MediaMetadataCompat.METADATA_KEY_ART) == null)
            spinKitView.visibility = View.VISIBLE
        else
            spinKitView.visibility = View.GONE

        progressBar.max = metadata.getLong(MediaMetadataCompat.METADATA_KEY_DURATION).toInt()
    }
}
