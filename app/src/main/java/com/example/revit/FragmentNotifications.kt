package com.example.revit

import Notification
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.ktx.auth
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
 * Use the [FragmentNotifications.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentNotifications : Fragment() {

    private val db = Firebase.firestore
    private val user = Firebase.auth.currentUser

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        val appBar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        appBar.setNavigationOnClickListener {
            view.findNavController().navigateUp()
        }

        val rv: RecyclerView = view.findViewById(R.id.rvNotifications)
        val dataSet: ArrayList<Notification> = ArrayList()
        val adapter = NotificationAdapter(dataSet)
        rv.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rv.adapter = adapter

        db.collection("notifications")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { notifications, error ->
                if(error != null) {
                    Log.w(TAG, "Notification listen failed", error)
                    return@addSnapshotListener
                }

                if (notifications != null) {
                    for (notif in notifications) {
                        db.collection("comments").document(notif.getString("commentId")!!)
                            .get()
                            .addOnSuccessListener { comment ->
                                if (comment.getString("userId")!!.equals(user!!.uid)) {
                                    dataSet.add(notif.toObject<Notification>())
                                    adapter.notifyDataSetChanged()
                                }
                            }
                    }
                }
            }

        return view
    }
}