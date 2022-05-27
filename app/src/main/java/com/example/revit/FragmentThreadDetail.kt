package com.example.revit

import Comment
import CommentAdapter
import RevitThread
import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.android.synthetic.main.fragment_thread_detail.*
import org.w3c.dom.Text
import java.text.SimpleDateFormat

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentThreadDetail.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentThreadDetail : Fragment() {

    private val args: FragmentThreadDetailArgs by navArgs()
    private val db = Firebase.firestore
    private val user = Firebase.auth.currentUser
    private val storage = Firebase.storage.reference

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_thread_detail, container, false)

        val title: TextView = view.findViewById(R.id.title)
        val description: TextView = view.findViewById(R.id.description)
        val date: TextView = view.findViewById(R.id.date)
        val comments: TextView = view.findViewById(R.id.comments)
        val tags: TextView = view.findViewById(R.id.tags)
        val rvComments: RecyclerView = view.findViewById(R.id.rvComments)
        val btnAddComment: Button = view.findViewById(R.id.btnAddComment)
        val txtComment: TextInputEditText = view.findViewById(R.id.inputComment)
        val imgCover: ImageView = view.findViewById(R.id.imgCover)
        val btnEdit: ImageButton = view.findViewById(R.id.btnEdit)
        val appbar: MaterialToolbar = view.findViewById(R.id.topAppBar)

        appbar.setNavigationOnClickListener {
            view.findNavController().navigateUp()
        }

        checkIfAdmin(view, btnEdit)

        // Add details
        val threadRef = db.collection("threads").document(args.threadId)
        threadRef.get()
            .addOnSuccessListener { document ->
                val data = document.toObject<RevitThread>()

                var appBarTitle = data!!.title
                if(appBarTitle!!.length > 20) {
                    appBarTitle = appBarTitle.substring(0, 15)
                    appBarTitle = appBarTitle.trimEnd()
                    appBarTitle += "..."
                }

                appbar.title = appBarTitle
                title.text = data.title
                description.text = data.description

                val dateFormat = SimpleDateFormat("dd/MM/yy")
                date.text = dateFormat.format(data.created_at!!.toDate())

                var strTags = "Tags: "
                for (tag in data.tags!!) {
                    strTags += tag.trim() + ", "
                }
                strTags = strTags.dropLast(2)
                tags.text = strTags

                storage.child(data.cover_img!!).downloadUrl.addOnSuccessListener {
                    Glide.with(view)
                        .load(it)
                        .into(imgCover)
                }
            }

        // Add comments
        val cmtRef = db.collection("comments")
            .whereEqualTo("threadId", args.threadId)
            .orderBy("created_at", Query.Direction.DESCENDING)

        val dataSet: ArrayList<Comment> = ArrayList()
        val idSet: ArrayList<String> = ArrayList()

        val adapter = CommentAdapter(dataSet, idSet, "liked_comments")
        rvComments.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        rvComments.adapter = adapter

        // Listener for comment content and number of comments
        cmtRef.addSnapshotListener { documents, e ->
            if(e != null) {
                Log.w(TAG, "Listen failed.", e)
                return@addSnapshotListener
            }

            comments.text = documents?.size().toString() + " comments"
            dataSet.clear()
            idSet.clear()
            for (document in documents!!) {
                Log.d(TAG, "${document.id} => ${document.data}")
                dataSet.add(document.toObject<Comment>())
                idSet.add(document.id)
            }

            adapter.notifyDataSetChanged()
        }

        // Listener for new comment
        btnAddComment.setOnClickListener {
            val comment = txtComment.text?.trim().toString()
            if(comment.isEmpty()) {
                Toast.makeText(context, "Please type in a comment!", Toast.LENGTH_SHORT).show()
            } else {
                val data = Comment(user!!.uid, args.threadId, comment)
                db.collection("comments").add(data)
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Failed to add comment with ", e)
                    }
                txtComment.setText("")
            }
        }

        return view
    }

    private fun checkIfAdmin(view: View, btnEdit: ImageButton) {
        if(!user!!.displayName.equals("admin")) {
            btnEdit.visibility = INVISIBLE
        } else {
            btnEdit.setOnClickListener {
                val action = FragmentThreadDetailDirections.actionFragmentThreadDetailToFragmentEditThread(args.threadId)
                view.findNavController().navigate(action)
            }
        }
    }
}