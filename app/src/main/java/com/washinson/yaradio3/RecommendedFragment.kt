package com.washinson.yaradio3

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.washinson.yaradio3.station.RecommendType
import com.washinson.yaradio3.station.Tag
import kotlinx.android.synthetic.main.fragment_settings.*
import org.w3c.dom.Text

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [RecommendedFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 */
class RecommendedFragment(val recommendType: RecommendType) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_recommended, container, false)
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
        val recyclerView = view.findViewById<RecyclerView>(R.id.recommended_list)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context!!, LinearLayoutManager.HORIZONTAL, false)
            adapter = RecommendedAdapter(recommendType)
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        fun startTag(tag: Tag)
    }


    inner class RecommendedAdapter(val recommendType: RecommendType) : RecyclerView.Adapter<RecommendedAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.fragment_recommened_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = recommendType.tags.size

        override fun onBindViewHolder(holder: ViewHolder, i: Int) {
            holder.border.setCardBackgroundColor(Color.parseColor(recommendType.tags[i].icon.backgroundColor))
            Glide.with(context!!).load(recommendType.tags[i].icon.getIcon(200, 200)).into(holder.imageView)
            holder.textView.text = recommendType.tags[i].name
            holder.itemView.setOnClickListener {
                listener?.startTag(recommendType.tags[i])
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val border: CardView = itemView.findViewById(R.id.tag_border)
            val imageView: ImageView = itemView.findViewById(R.id.tag_view)
            val textView: TextView = itemView.findViewById(R.id.tag_text)
        }
    }
}
