package com.washinson.yaradio3.player


import android.Manifest
import android.accounts.NetworkErrorException
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.washinson.yaradio3.session.Track
import com.bumptech.glide.Glide
import com.washinson.yaradio3.R
import com.washinson.yaradio3.common.Mp3Downloader
import com.washinson.yaradio3.session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * A simple [Fragment] subclass.
 *
 */
class PlayerHistoryFragment : Fragment() {
    lateinit var listHistoryView: ListView
    lateinit var backButton: ImageView
    var adapter: MyAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MyAdapter(context!!)

        backButton = view.findViewById(R.id.back)
        backButton.setOnClickListener {
            val curActivity = (activity ?: return@setOnClickListener) as PlayerActivity
            curActivity.viewPager.currentItem = 1
        }

        listHistoryView = view.findViewById(R.id.session_history)
        listHistoryView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        adapter?.onMetadataUpdate()
    }

    fun onLoadTrackClicked(track: Track) {
        val builder = AlertDialog.Builder(context)

        builder.setMessage(getString(R.string.download_track))
            .setTitle(getString(R.string.download_title))

        builder.setPositiveButton(getString(android.R.string.yes)) { dialogInterface, _ ->
            val requested = ContextCompat.checkSelfPermission(context!!, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            if(requested == PackageManager.PERMISSION_DENIED){
                val ACCESS_EXTERNAL_STORAGE_STATE = 1
                ActivityCompat.requestPermissions(activity!!,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    ACCESS_EXTERNAL_STORAGE_STATE)
            } else {
                try {
                    loadTrack(track)
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

    fun loadTrack(track: Track) {
        val mp3Downloader = Mp3Downloader(context!!)
        mp3Downloader.loadTrack(track)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        try {
            when(requestCode) {
                1 -> {
                    if (grantResults.isNotEmpty()
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(context, getString(R.string.permission_granted), Toast.LENGTH_SHORT).show()
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

    inner class MyAdapter(val context: Context) : BaseAdapter() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var tracks = ArrayList<Track>()

        init {
            onMetadataUpdate()
        }

        fun onMetadataUpdate() {
            tracks = Session.getInstance(0, context).trackHistory
            notifyDataSetChanged()
        }

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, p1: View?, parent: ViewGroup?): View {
            var view = p1
            if (view == null) {
                view = inflater.inflate(R.layout.player_history_item, parent, false)
            }

            val track = tracks[tracks.size - 1 - position]

            val trackTitle = view!!.findViewById<TextView>(R.id.title)
            trackTitle.text = track.title
            trackTitle.setOnClickListener { Utils.trackIntoClipboard(context, track) }

            val trackArtist = view.findViewById<TextView>(R.id.artist)
            trackArtist.text = track.artist
            trackArtist.setOnClickListener { Utils.trackIntoClipboard(context, track) }


            val trackCover = view.findViewById<ImageView>(R.id.track_cover)
            Glide.with(context).load(track.getCoverSize(600, 600)).into(trackCover)
            trackCover.setOnClickListener {
                onLoadTrackClicked(track)
            }

            initDislikeButton(view.findViewById(R.id.track_dislike), track)
            initLikeButton(view.findViewById(R.id.track_like), track)
            return view
        }

        @Suppress("DEPRECATION")
        fun initLikeButton(likeButton: ImageView, track: Track) {
            if (track.liked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    likeButton.setImageDrawable(context.getDrawable(R.drawable.ic_like_track_active))
                else
                    likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_like_track_active))
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    likeButton.setImageDrawable(context.getDrawable(R.drawable.ic_like_track_passive))
                else
                    likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_like_track_passive))
            }

            likeButton.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.click_anim))
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        if (track.liked)
                            Session.getInstance(0, context).unlike(track, 0.0)
                        else
                            Session.getInstance(0, context).like(track, 0.0)
                        launch(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    } catch (e: NetworkErrorException) {
                        Toast.makeText(context, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        fun initDislikeButton(dislikeButton: ImageView, track: Track) {
            if (track.disliked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    dislikeButton.setImageDrawable(context.getDrawable(R.drawable.ic_dislike_track_active))
                else
                    dislikeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_dislike_track_active))
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    dislikeButton.setImageDrawable(context.getDrawable(R.drawable.ic_dislike_track_passive))
                else
                    dislikeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_dislike_track_passive))
            }

            dislikeButton.setOnClickListener {
                it.startAnimation(AnimationUtils.loadAnimation(context, R.anim.click_anim))
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        if (track.disliked)
                            Session.getInstance(0, context).undislike(track, 0.0)
                        else
                            Session.getInstance(0, context).dislike(track, 0.0)
                        launch(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    } catch (e: NetworkErrorException) {
                        Toast.makeText(context, getString(R.string.no_internet), Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun getItem(p0: Int): Any = tracks[p0]

        override fun getItemId(p0: Int): Long = p0.toLong()

        override fun getCount(): Int = tracks.size

    }
}
