package com.washinson.yaradio3.Player


import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ListView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.washinson.yaradio3.R
import com.washinson.yaradio3.Session.Session
import com.washinson.yaradio3.Session.Track

/**
 * A simple [Fragment] subclass.
 *
 */
class PlayerNextFragment : Fragment() {
    lateinit var listHistoryView: ListView
    lateinit var adapter: MyAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_player_next, container, false)
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
            tracks = Session.getInstance(0, context).getNextTracks()
            notifyDataSetChanged()
        }

        @SuppressLint("SetTextI18n")
        override fun getView(position: Int, p1: View?, parent: ViewGroup?): View {
            var view = p1
            if (view == null) {
                view = inflater.inflate(R.layout.player_next_item, parent, false)
            }

            val track = tracks[position]
            view!!.findViewById<TextView>(R.id.track_label).text = track.artist + " - " + track.title
            Glide.with(context).load(track.getCoverSize(600, 600)).into(view.findViewById(R.id.track_cover))
            return view
        }

        override fun getItem(p0: Int): Any = tracks[p0]

        override fun getItemId(p0: Int): Long = p0.toLong()

        override fun getCount(): Int = tracks.size

    }
}
