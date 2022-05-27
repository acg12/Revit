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
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentSavedThreads.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentSavedThreads : Fragment() {

    private val user = Firebase.auth.currentUser
    private val db = Firebase.firestore

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_saved_threads, container, false)

        val appbar: MaterialToolbar = view.findViewById(R.id.topAppBar)

        val rv: RecyclerView = view.findViewById(R.id.rvSavedThreads)
        val dataSet: ArrayList<RevitThread> = ArrayList()
        val idSet: ArrayList<String> = ArrayList()
        val adapter = ThreadAdapter(dataSet, idSet, "saved_threads")
        rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rv.adapter = adapter

        db.collection("saves")
            .whereEqualTo("userId", user!!.uid)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    Log.d(ContentValues.TAG, "${document.id} => ${document.data}")

                    db.collection("threads").document(document.getString("threadId")!!)
                        .get()
                        .addOnSuccessListener { thread ->
                            if(thread != null) {
                                dataSet.add(thread.toObject<RevitThread>()!!)
                                idSet.add(thread.id)
                                adapter.notifyDataSetChanged()
                            }
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(ContentValues.TAG, "Error getting documents: ", exception)
            }

        appbar.setNavigationOnClickListener {
            back(view)
        }

        return view
    }

    private fun back(view: View) {
        view.findNavController().navigateUp()
    }
}