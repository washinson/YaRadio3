package com.washinson.yaradio3.Player


import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.washinson.yaradio3.Session.Track
import com.bumptech.glide.Glide
import com.washinson.yaradio3.R
import com.washinson.yaradio3.Session.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


/**
 * A simple [Fragment] subclass.
 *
 */
class PlayerHistoryFragment : Fragment() {
    lateinit var listHistoryView: ListView
    lateinit var adapter: MyAdapter

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

        listHistoryView = view.findViewById(R.id.session_history)
        listHistoryView.adapter = adapter
    }

    inner class MyAdapter(val context: Context) : BaseAdapter() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        var tracks = ArrayList<Track>()

        init {
            onMetadataUpdate()
        }

        fun onMetadataUpdate() {
            tracks = Session.getInstance(0, context).getTrackHistory()
            notifyDataSetChanged()
        }

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, p1: View?, parent: ViewGroup?): View {
            var view = p1
            if (view == null) {
                view = inflater.inflate(R.layout.player_history_item, parent, false)
            }

            val track = tracks[tracks.size - 1 - position]
            view!!.findViewById<TextView>(R.id.track_label).text = track.artist + " - " + track.title
            Glide.with(context).load(track.getCoverSize(600, 600)).into(view.findViewById(R.id.track_cover))
            initDislikeButton(view.findViewById(R.id.track_dislike), track)
            initLikeButton(view.findViewById(R.id.track_like), track)
            return view
        }

        @Suppress("DEPRECATION")
        fun initLikeButton(likeButton: ImageButton, track: Track) {
            if (track.liked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    likeButton.setImageDrawable(context.getDrawable(R.drawable.ic_liked))
                else
                    likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_liked))

                likeButton.setOnClickListener {
                    GlobalScope.launch(Dispatchers.IO) {
                        Session.getInstance(0, context).unlike(track, 0.0)
                        launch(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    likeButton.setImageDrawable(context.getDrawable(R.drawable.ic_like))
                else
                    likeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_like))

                likeButton.setOnClickListener {
                    GlobalScope.launch(Dispatchers.IO) {
                        Session.getInstance(0, context).like(track, 0.0)
                        launch(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    }
                }
            }
        }

        @Suppress("DEPRECATION")
        fun initDislikeButton(dislikeButton: ImageButton, track: Track) {
            if (track.disliked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    dislikeButton.setImageDrawable(context.getDrawable(R.drawable.ic_disliked))
                else
                    dislikeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_disliked))

                dislikeButton.setOnClickListener {
                    GlobalScope.launch(Dispatchers.IO) {
                        Session.getInstance(0, context).undislike(track, 0.0)
                        launch(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    }
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
                    dislikeButton.setImageDrawable(context.getDrawable(R.drawable.ic_dislike))
                else
                    dislikeButton.setImageDrawable(resources.getDrawable(R.drawable.ic_dislike))

                dislikeButton.setOnClickListener {
                    GlobalScope.launch(Dispatchers.IO) {
                        Session.getInstance(0, context).dislike(track, 0.0)
                        launch(Dispatchers.Main) {
                            notifyDataSetChanged()
                        }
                    }
                }
            }
        }

        override fun getItem(p0: Int): Any = tracks[p0]

        override fun getItemId(p0: Int): Long = p0.toLong()

        override fun getCount(): Int = tracks.size

    }
}
