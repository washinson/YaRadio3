package com.washinson.yaradio3

import android.content.Context
import android.graphics.Color
import android.media.Image
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
import com.washinson.yaradio3.station.Tag

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [TagsFragment.OnFragmentInteractionListener] interface
 * to handle interaction events.
 */
class TagsFragment(val tags: ArrayList<Tag>) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_tags, container, false)
    }

    fun isFragmentHaveChild(): Boolean {
        var cnt = 0
        for (i in tags) {
            if (i.children != null && i.children!!.size > 0) ++cnt
        }
        // First child can have child.
        // Example: expanded parent
        return cnt > 1
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
        }
        if (isFragmentHaveChild()) {
            recyclerView.adapter = ExtendedTagsAdapter(tags)
        } else {
            recyclerView.adapter = TagsAdapter(tags)
        }

        view.findViewById<ImageView>(R.id.back).setOnClickListener { listener?.backStackFragment() }
        view.findViewById<ImageView>(R.id.settings).setOnClickListener { listener?.openSettings() }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onResume() {
        super.onResume()
        listener?.updateStatusBarColor(R.color.colorHeaderAlpha)
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
        fun onParentTagSelected(tags: ArrayList<Tag>)
        fun updateStatusBarColor(colorId: Int)
        fun backStackFragment()
        fun openSettings()
    }

    inner class TagsAdapter(val tags: ArrayList<Tag>) : RecyclerView.Adapter<TagsAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.fragment_tags_item, parent, false)
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
            val border: CardView = itemView.findViewById(R.id.tags_border)
            val imageView: ImageView = itemView.findViewById(R.id.tags_view)
            val textView: TextView = itemView.findViewById(R.id.tags_text)
        }
    }

    inner class ExtendedTagsAdapter(val tags: ArrayList<Tag>) : RecyclerView.Adapter<ExtendedTagsAdapter.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.fragment_extended_tags_item, parent, false)
            return ViewHolder(view)
        }

        override fun getItemCount() = tags.size

        override fun onBindViewHolder(holder: ViewHolder, i: Int) {
            holder.border.setCardBackgroundColor(Color.parseColor(tags[i].icon.backgroundColor))
            Glide.with(context!!).load(tags[i].icon.getIcon(200, 200)).into(holder.imageView)
            holder.textView.text = tags[i].name
            holder.itemView.setOnClickListener {
                val tags = ArrayList<Tag>()
                tags.add(this.tags[i])
                tags.addAll(this.tags[i].children!!)
                listener?.onParentTagSelected(tags)
            }
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val border: CardView = itemView.findViewById(R.id.tags_border)
            val imageView: ImageView = itemView.findViewById(R.id.tags_view)
            val textView: TextView = itemView.findViewById(R.id.tags_text)
        }
    }
}
