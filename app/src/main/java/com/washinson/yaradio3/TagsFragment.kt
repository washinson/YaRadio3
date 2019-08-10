package com.washinson.yaradio3

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.washinson.yaradio3.Station.Tag
import okhttp3.internal.wait
import android.graphics.Bitmap
import com.bumptech.glide.request.FutureTarget


class TagsFragment(val tags: List<Tag>) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        if (isFragmentHaveChild(tags))
            return  inflater.inflate(R.layout.fragment_tags_expandable, container, false)
        else
            return  inflater.inflate(R.layout.fragment_tags_simple, container, false)
    }

    fun isFragmentHaveChild(tags: List<Tag>): Boolean {
        for (i in tags) {
            if (i.children != null && i.children!!.size > 0) return true
        }
        return false
    }

    fun genExpandableView(view: View) {
        val adapter = ExpandableListAdapter(tags, context!!)

        val expandableListView = view.findViewById(R.id.tags_expandable_list) as ExpandableListView
        expandableListView.setAdapter(adapter)
        expandableListView.setOnChildClickListener {
                _, _, i1, i2, _ ->
            if (i2 == 0) listener?.start(tags[i1])
            else listener?.start(tags[i1].children!![i2 - 1])
            true
        }
    }

    fun genSimpleView(view: View) {
        val tags = ArrayList<String>()
        for (i in this.tags) {
            tags.add(i.name)
        }

        val adapter = SimpleListAdapter(this.tags, context!!)

        val listView = view.findViewById<ListView>(R.id.tags_simple_list)
        listView.adapter = adapter
        listView.setOnItemClickListener { _, _, i, _ ->
            listener?.start(this.tags[i])
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (isFragmentHaveChild(tags))
            genExpandableView(view)
        else
            genSimpleView(view)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException("$context must implement OnFragmentInteractionListener")
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
        fun start(tag: Tag)
    }

    inner class ExpandableListAdapter(val tags: List<Tag>, val context: Context) : BaseExpandableListAdapter() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

        // Gets a View that displays the data for the given child within the given group.
        override fun getChildView(p0: Int, p1: Int, p2: Boolean, p3: View?, p4: ViewGroup?): View {
            var view = p3
            if (view == null) {
                view = inflater.inflate(android.R.layout.simple_list_item_1, p4, false)
            }

            val textView: TextView = view!!.findViewById(android.R.id.text1)
            textView.text = getChild(p0, p1).name
            textView.setBackgroundColor(Color.parseColor(getGroup(p0).icon.backgroundColor))

            return view
        }

        // Gets a View that displays the given group.
        override fun getGroupView(p0: Int, isExpanded: Boolean, p2: View?, p3: ViewGroup?): View {
            var view = p2
            if (view == null) {
                view = inflater.inflate(R.layout.simple_expandable_list_item_1_custom, p3, false)
            }

            view!!.findViewById<TextView>(R.id.tag_name).text = getGroup(p0).name
            view.findViewById<LinearLayout>(R.id.expandable_list_layout).setBackgroundColor(Color.parseColor(getGroup(p0).icon.backgroundColor))
            Glide.with(context).load(getGroup(p0).icon.getIcon(200, 200)).into(view.findViewById(R.id.tag_icon))

            return view
        }


        // Gets the data associated with the given group.
        override fun getGroup(p0: Int) = tags[p0]

        // Whether the child at the specified position is selectable.
        override fun isChildSelectable(p0: Int, p1: Int) = true

        // Indicates whether the child and group IDs are stable across changes to the underlying data.
        override fun hasStableIds() = true

        // Indicates whether the child and group IDs are stable across changes to the underlying data.
        override fun getChildrenCount(p0: Int) = (tags[p0].children?.size ?: 0) + 1

        // Gets the data associated with the given child within the given group.
        override fun getChild(p0: Int, p1: Int) = if (p1 == 0) tags[p0] else tags[p0].children!![p1 - 1]

        // Gets the ID for the group at the given position.
        override fun getGroupId(p0: Int) = p0.toLong()

        // Gets the ID for the given child within the given group.
        override fun getChildId(p0: Int, p1: Int) = p1.toLong()

        // Gets the number of groups.
        override fun getGroupCount() = tags.size

    }

    inner class SimpleListAdapter(val tags: List<Tag>,val context: Context) : BaseAdapter() {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        override fun getView(p0: Int, p1: View?, p2: ViewGroup?): View {
            var view = p1
            if (view == null) {
                view = inflater.inflate(R.layout.simple_list_item_1_custom, p2, false)
            }

            Glide.with(context).load(getItem(p0).icon.getIcon(200,200)).into(view!!.findViewById<ImageView>(R.id.tag_icon))
            view.findViewById<LinearLayout>(R.id.simple_list_layout).setBackgroundColor(Color.parseColor(getItem(p0).icon.backgroundColor))
            view.findViewById<TextView>(R.id.tag_name).text = getItem(p0).name

            return view
        }

        override fun getItem(p0: Int) = tags[p0]

        override fun getItemId(p0: Int) = p0.toLong()

        override fun getCount() = tags.size

    }
}
