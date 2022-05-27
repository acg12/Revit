package com.example.revit

import RevitThread
import ThreadAdapter
import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.util.rangeTo
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Timestamp
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
 * Use the [FragmentHome.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentHome : Fragment() {

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        // Recent recyclerview
        val recyclerView = view.findViewById<RecyclerView>(R.id.listRecent)
        recyclerView.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        db.collection("threads")
            .get()
            .addOnSuccessListener { result ->
                val dataSet: ArrayList<RevitThread> = ArrayList()
                val idSet: ArrayList<String> = ArrayList()
                for (document in result) {
                    Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                    dataSet.add(document.toObject<RevitThread>())
                    idSet.add(document.id)
                }
                val adapter = ThreadAdapter(dataSet, idSet, "home")
                recyclerView.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }

        // Popular 5 recyclerview
        val popularRv = view.findViewById<RecyclerView>(R.id.listPopular)
        popularRv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)

        db.collection("threads")
            .orderBy("created_at", Query.Direction.DESCENDING)
            .limit(5)
            .get()
            .addOnSuccessListener { result ->
                val dataSet: ArrayList<RevitThread> = ArrayList()
                val idSet: ArrayList<String> = ArrayList()
                for (document in result) {
                    Log.d(ContentValues.TAG, "${document.id} => ${document.data}")
                    dataSet.add(document.toObject<RevitThread>())
                    idSet.add(document.id)
                }
                val adapter = ThreadAdapter(dataSet, idSet, "home")
                popularRv.adapter = adapter
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }

        val appBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        setMenuClickListener(appBar)

        return view
    }

    private fun setMenuClickListener(toolbar: MaterialToolbar) = with(toolbar) {
        setOnMenuItemClickListener { menuItem ->
            if(menuItem.itemId == R.id.menuNotifications) {
                val action = FragmentHomeDirections.actionPageHomeToFragmentNotifications()
                requireView().findNavController().navigate(action)
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
    }
}