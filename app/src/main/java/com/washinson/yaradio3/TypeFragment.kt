package com.washinson.yaradio3

import android.content.Context
import android.graphics.Color
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

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [TypeFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 */
class TypeFragment(val tags: ArrayList<Tag>) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_type, container, false)
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
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = LinearLayoutManager(context!!, LinearLayoutManager.VERTICAL, false)
            adapter = TypeAdapter(tags)
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

    inner class TypeAdapter(val tags: ArrayList<Tag>) : RecyclerView.Adapter<TypeAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.fragment_type_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = tags.size

        override fun onBindViewHolder(holder: ViewHolder, i: Int) {
            holder.border.setCardBackgroundColor(Color.parseColor(tags[i].icon.backgroundColor))
            Glide.with(context!!).load(tags[i].icon.getIcon(200, 200)).into(holder.imageView)
            holder.textView.text = tags[i].name
            holder.itemView.setOnClickListener {
                listener?.startTag(tags[i])
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val border: CardView = itemView.findViewById(R.id.type_border)
            val imageView: ImageView = itemView.findViewById(R.id.type_view)
            val textView: TextView = itemView.findViewById(R.id.type_text)
        }
    }
}
