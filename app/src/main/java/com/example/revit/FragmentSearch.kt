package com.example.revit

import RevitThread
import SearchAdapter
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.SearchView
import android.widget.Spinner
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentSearch.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentSearch : Fragment(), AdapterView.OnItemSelectedListener {

    private val resultDataset: ArrayList<RevitThread> = ArrayList()
    private val resultIdset: ArrayList<String> = ArrayList()
    private lateinit var searchAdapter: SearchAdapter
    private lateinit var rvSearchResults: RecyclerView
    private lateinit var spinner: Spinner
    private lateinit var spinnerSort: Spinner
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        // Create filter spinner
        spinner = view.findViewById(R.id.filterSpinner)
        // Sort spinner
        spinnerSort = view.findViewById(R.id.sortSpinner)

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.filter_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinner.adapter = adapter
        }

        spinner.onItemSelectedListener = this

        // ArrayAdapter for sort spinner
        ArrayAdapter.createFromResource(
            requireContext(),
            R.array.sort_choices,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            spinnerSort.adapter = adapter
        }

        spinnerSort.onItemSelectedListener = this

        // Search results adapter
        rvSearchResults = view.findViewById(R.id.searchResults)
        rvSearchResults.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        searchAdapter = SearchAdapter(resultDataset.toMutableList(), resultIdset.toMutableList(), resultDataset, resultIdset)
        rvSearchResults.adapter = searchAdapter

        // Create listener for search view
        val searchView: SearchView = view.findViewById(R.id.searchView)

        searchView.setOnQueryTextListener(object: SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(p0: String?): Boolean {
                if(p0 != null) {
                    db.collection("threads")
                        .orderBy("created_at", Query.Direction.DESCENDING)
                        .get()
                        .addOnSuccessListener { documents ->
                            resultDataset.clear()
                            resultIdset.clear()

                            for (doc in documents) {
                                val title = doc.getString("title")
                                if (title!!.contains(p0, ignoreCase=true)) {
                                    resultDataset.add(doc.toObject<RevitThread>())
                                    resultIdset.add(doc.id)
                                    Log.d(TAG, doc.getString("title")!!)
                                }
                            }
                            searchAdapter = SearchAdapter(resultDataset.toMutableList(), resultIdset.toMutableList(), resultDataset, resultIdset)
                            rvSearchResults.adapter = searchAdapter
                        }
                }
                return true
            }

            override fun onQueryTextChange(p0: String?): Boolean {
                return false
            }
        })

        return view
    }

    override fun onItemSelected(parent: AdapterView<*>?, view: View?, pos: Int, id: Long) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)
        if (parent!!.equals(spinner)) {
            when {
                parent.getItemAtPosition(pos).equals("Last 24 Hours") -> searchAdapter.filter("last-day")
                parent.getItemAtPosition(pos).equals("This Week") -> searchAdapter.filter("last-week")
                parent.getItemAtPosition(pos).equals("This Month") -> searchAdapter.filter("last-month")
                parent.getItemAtPosition(pos).equals("This Year") -> searchAdapter.filter("last-year")
                parent.getItemAtPosition(pos).equals("None") -> searchAdapter.clearFilterSort()
            }
        } else {
            when {
                parent.getItemAtPosition(pos).equals("Most saved") -> searchAdapter.sort("most-saved")
                parent.getItemAtPosition(pos).equals("Most comments") -> searchAdapter.sort("most-comments")
                parent.getItemAtPosition(pos).equals("None") -> searchAdapter.clearFilterSort()
            }
        }
    }

    override fun onNothingSelected(parent: AdapterView<*>?) {
        TODO("Not yet implemented")
    }
}