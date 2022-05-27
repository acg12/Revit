import android.app.Dialog
import android.content.ContentValues.TAG
import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.revit.FragmentCommentsDirections
import com.example.revit.FragmentLikedCommentsDirections
import com.example.revit.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.text.SimpleDateFormat

open class CommentAdapter(val dataSet: List<Comment>, val idSet: List<String>, val fragment: String): RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    private val db = Firebase.firestore
    private val storage = Firebase.storage.reference
    private val curr = Firebase.auth.currentUser?.uid

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_comment, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        val comment: Comment = dataSet[position]

        viewHolder.itemView.setOnClickListener {
            if(fragment.equals("comments")) {
                val action = FragmentCommentsDirections.actionFragmentCommentsToFragmentThreadDetail(comment.threadId!!)
                viewHolder.itemView.findNavController().navigate(action)
            } else if(fragment.equals("liked_comments")) {
                val action = FragmentLikedCommentsDirections.actionFragmentLikedCommentsToFragmentThreadDetail2(comment.threadId!!)
                viewHolder.itemView.findNavController().navigate(action)
            }
        }

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val txtUser: TextView = viewHolder.itemView.findViewById(R.id.username)
        val txtDate: TextView = viewHolder.itemView.findViewById(R.id.date)
        val txtTime: TextView = viewHolder.itemView.findViewById(R.id.time)
        val txtLikeCount: TextView = viewHolder.itemView.findViewById(R.id.likeCount)
        val txtComment: TextView = viewHolder.itemView.findViewById(R.id.commentBody)
        val imgProfile: ImageView = viewHolder.itemView.findViewById(R.id.profilePicture)
        val btnLike: ImageButton = viewHolder.itemView.findViewById(R.id.btnLike)
        val btnEdit: ImageButton = viewHolder.itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = viewHolder.itemView.findViewById(R.id.btnDelete)

        checkIfUsersComment(btnEdit, btnDelete, comment.userId)

        // Set text to components
        txtComment.text = comment.comment

        val dateFormat = SimpleDateFormat("dd/MM/yy")
        val timeFormat = SimpleDateFormat("HH:mm")
        val date = dateFormat.format(comment.created_at.toDate())
        val time = timeFormat.format(comment.created_at.toDate())

        txtDate.text = date
        txtTime.text = time

        // Set username & profile picture
        db.collection("users").document(comment.userId!!)
            .get()
            .addOnSuccessListener { doc ->
                txtUser.text = doc.getString("username")

                storage.child(doc.getString("prof_img")!!).downloadUrl
                    .addOnSuccessListener {
                        Glide.with(viewHolder.itemView)
                            .load(it)
                            .into(imgProfile)
                    }
                    .addOnFailureListener { e ->
                        Log.d(TAG, "Failed to get profile picture for comment with " + e.stackTraceToString())
                    }
            }
            .addOnFailureListener { e ->
                Log.d(TAG, "Failed get user username for comment with " + e.stackTraceToString())
            }

        // Set likes count
        db.collection("likes")
            .whereEqualTo("commentId", idSet[position])
            .addSnapshotListener { documents, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                txtLikeCount.text = documents!!.size().toString()
            }

        // Add listener for value changes
        db.collection("likes")
            .whereEqualTo("userId", curr)
            .whereEqualTo("commentId", idSet[position])
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if(!value!!.isEmpty) {
                    btnLike.setImageURI(Uri.parse("android.resource://com.example.revit/drawable/heart_full"))
                }

                for (dc in value!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> btnLike.setImageURI(Uri.parse("android.resource://com.example.revit/drawable/heart_full"))
                        DocumentChange.Type.REMOVED -> btnLike.setImageURI(Uri.parse("android.resource://com.example.revit/drawable/heart_outline"))
                    }
                }
            }

        btnLike.setOnClickListener {
            val docref = db.collection("likes")
                .whereEqualTo("userId", curr)
                .whereEqualTo("commentId", idSet[position])

            docref.get()
                .addOnSuccessListener { documents ->
                    if(documents.isEmpty) {
                        val data = hashMapOf(
                            "userId" to curr,
                            "commentId" to idSet[position]
                        )
                        db.collection("likes").add(data)
                        createNotification(idSet[position])
                    } else {
                        for (doc in documents) {
                            db.collection("likes").document(doc.id)
                                .delete()
                                .addOnFailureListener { e ->
                                    Log.w(TAG, "Error delete documents: ", e)
                                }
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Error getting document", e)
                }
        }

        // Listener Edit & Delete
        btnEdit.setOnClickListener {
            showEditDialog(viewHolder.itemView.context, idSet[position], comment.comment)
        }

        btnDelete.setOnClickListener {
            deleteComment(idSet[position])
        }
    }

    private fun createNotification(id: String) {
        db.collection("notifications").add(Notification(id, curr!!))
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding notification", e)
            }
    }

    fun deleteComment(id: String) {
        db.collection("comments").document(id)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Comment successfully deleted!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error deleting comment", e) }

        deleteCommentLikes(id)
    }

    fun deleteCommentLikes(id: String) {
        db.collection("likes")
            .whereEqualTo("commentId", id)
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    doc.reference.delete()
                }
            }
    }

    fun showEditDialog(ctx: Context, id: String, comment: String?) {
        val dialog = Dialog(ctx)

        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(true)
        dialog.setContentView(R.layout.edit_comment_dialog)

        val txtComment: TextInputEditText = dialog.findViewById(R.id.editComment)
        txtComment.setText(comment)

        val btnSubmit: ImageButton = dialog.findViewById(R.id.btnSubmit)
        btnSubmit.setOnClickListener {
            val newComment = txtComment.text.toString()
            db.collection("comments").document(id)
                .update("comment", newComment)
                .addOnSuccessListener {
                    Log.d(TAG, "Comment successfully updated!")
                    dialog.dismiss()
                }
                .addOnFailureListener { e -> Log.w(TAG, "Error updating comment", e) }
        }

        dialog.show()
    }

    fun checkIfUsersComment(btnEdit: ImageButton, btnDelete: ImageButton, commentUserId: String?) {
        if(!commentUserId.equals(curr)) {
            btnEdit.visibility = INVISIBLE
            btnDelete.visibility = INVISIBLE
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
