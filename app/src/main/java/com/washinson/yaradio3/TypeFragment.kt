package com.washinson.yaradio3

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import com.washinson.yaradio3.Station.Type
import android.widget.SimpleExpandableListAdapter
import com.washinson.yaradio3.Station.Tag

class TypeFragment(val type: Type) : Fragment() {
    private var listener: OnFragmentInteractionListener? = null


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return  inflater.inflate(R.layout.fragment_tags, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val listView = view.findViewById<ExpandableListView>(R.id.tags_expandable_list)

        var map: MutableMap<String, String>
        val groupDataList = ArrayList<Map<String, String>>()
        val сhildDataList = ArrayList<ArrayList<Map<String, String>>>()

        for (tag in type.tags) {
            map = HashMap()
            map["groupName"] = tag.name
            groupDataList.add(map)

            val сhildDataItemList = ArrayList<Map<String, String>>()

            map = HashMap()
            map["monthName"] = tag.name
            сhildDataItemList.add(map)
            if(tag.children != null) {
                for (i in 0 until tag.children!!.size) {
                    map = HashMap()
                    map["monthName"] = tag.children!![i].name
                    сhildDataItemList.add(map)
                }
            }

            сhildDataList.add(сhildDataItemList)
        }

        val groupFrom = arrayOf("groupName")
        val groupTo = intArrayOf(android.R.id.text1)

        val childFrom = arrayOf("monthName")
        val childTo = intArrayOf(android.R.id.text1)

        val adapter = SimpleExpandableListAdapter(
            context, groupDataList,
            android.R.layout.simple_expandable_list_item_1, groupFrom,
            groupTo, сhildDataList, android.R.layout.simple_list_item_1,
            childFrom, childTo
        )

        val expandableListView = view.findViewById(R.id.tags_expandable_list) as ExpandableListView
        expandableListView.setAdapter(adapter)
        expandableListView.setOnChildClickListener {
                expandableListView, view, i1, i2, l ->
            if (i2 == 0) listener?.start(type.tags[i1])
            else listener?.start(type.tags[i1].children!![i2 - 1])
            true
        }
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
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

    companion object {
        @JvmStatic
        fun newInstance(type: Type) = TypeFragment(type)
    }
}
