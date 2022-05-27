import android.content.ContentValues.TAG
import android.net.Uri
import android.nfc.Tag
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.revit.*
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

class ThreadAdapter(val dataSet: List<RevitThread>, val idSet: List<String>, val fragment: String)
    : RecyclerView.Adapter<ThreadAdapter.ViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val card: CardView

        init {
            card = view.findViewById(R.id.card_thread)
        }
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_thread, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val view = viewHolder.itemView
        view.setOnClickListener {
            if(fragment.equals("home")) {
                val action = FragmentHomeDirections.actionPageHomeToFragmentThreadDetail(idSet[position])
                view.findNavController().navigate(action)
            } else if(fragment.equals("saved_threads")) {
                val action = FragmentSavedThreadsDirections.actionFragmentSavedThreadsToFragmentThreadDetail(idSet[position])
                view.findNavController().navigate(action)
            }
        }

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val thread: RevitThread = dataSet[position]

        // Get view components
        val lblTitle = viewHolder.itemView.findViewById<TextView>(R.id.lblTitle)
        val lblDesc = viewHolder.itemView.findViewById<TextView>(R.id.lblDescription)
        val lblTags = viewHolder.itemView.findViewById<TextView>(R.id.lblTags)
        val lblComments = viewHolder.itemView.findViewById<TextView>(R.id.lblComments)
        val iconSave = viewHolder.itemView.findViewById<ImageView>(R.id.iconSave)
        val imgPreview = viewHolder.itemView.findViewById<ShapeableImageView>(R.id.imgCover)

        // Store into views
        lblTitle.setText(thread.title)
        lblDesc.setText(thread.description)

        var strTags = "Tags: "
        for (tag in thread.tags!!) {
            strTags += tag.trim() + ", "
        }
        strTags = strTags.dropLast(2)
        lblTags.setText(strTags)

        // Get database references
        val user = Firebase.auth.currentUser?.uid
        val db = Firebase.firestore
        val storage = Firebase.storage.reference

        storage.child(thread.cover_img!!).downloadUrl.addOnSuccessListener {
            Glide.with(viewHolder.itemView)
                .load(it)
                .into(imgPreview)
        }

        // Add listener for value changes
        db.collection("saves")
            .whereEqualTo("userId", user)
            .whereEqualTo("threadId", idSet[position])
            .addSnapshotListener { value, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed.", e)
                    return@addSnapshotListener
                }

                if(value!!.size() > 0) {
                    iconSave.setImageURI(Uri.parse("android.resource://com.example.revit/drawable/ic_baseline_bookmark_24"))
                }

                for (dc in value!!.documentChanges) {
                    when (dc.type) {
                        DocumentChange.Type.ADDED -> iconSave.setImageURI(Uri.parse("android.resource://com.example.revit/drawable/ic_baseline_bookmark_24"))
                        DocumentChange.Type.REMOVED -> iconSave.setImageURI(Uri.parse("android.resource://com.example.revit/drawable/ic_baseline_bookmark_border_24"))
                    }
                }
            }

        iconSave.setOnClickListener {
            val docref = db.collection("saves")
                .whereEqualTo("userId", user)
                .whereEqualTo("threadId", idSet[position])

            docref
                .get()
                .addOnSuccessListener { documents ->
                    if(documents.isEmpty) {
                        val data = hashMapOf(
                            "userId" to user,
                            "threadId" to idSet[position]
                        )
                        db.collection("saves").add(data)
                    } else {
                        for (doc in documents) {
                            db.collection("saves").document(doc.id)
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

        // Total number of comments
        db.collection("comments")
            .whereEqualTo("threadId", idSet[position])
            .get()
            .addOnSuccessListener { documents ->
                lblComments.setText(documents.size().toString() + " comments")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}
