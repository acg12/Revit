package com.example.revit

import Notification
import android.content.ContentValues
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.util.*

class NotificationAdapter(val dataSet: List<Notification>)
    : RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    private val db = Firebase.firestore
    private val user = Firebase.auth.currentUser
    private val storage = Firebase.storage.reference

    class ViewHolder(view: View): RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): NotificationAdapter.ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_notification, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val view = viewHolder.itemView

        view.setOnClickListener {
            db.collection("comments").document(dataSet[position].commentId!!)
                .get()
                .addOnSuccessListener { document ->
                    val action = FragmentNotificationsDirections.actionFragmentNotificationsToFragmentThreadDetail(document.getString("threadId")!!)
                    view.findNavController().navigate(action)
                }
                .addOnFailureListener {
                    Log.d("Notification", "Failed to find thread")
                }
        }

        val notification: Notification = dataSet[position]
        val profilePic: ShapeableImageView = view.findViewById(R.id.profilePicture)
        val txtNotif: TextView = view.findViewById(R.id.txtNotification)
        val txtComment: TextView = view.findViewById(R.id.previewComment)
        val txtDate: TextView = view.findViewById(R.id.txtDate)

        setProfileImage(view, notification.userId!!, profilePic)
        setNotificationText(notification, txtNotif)
        setCommentPreview(notification.commentId!!, txtComment)
        setDate(view.context, notification.date, txtDate)
    }

    private fun setNotificationText(notification: Notification, txtNotif: TextView) {
        val temp = user!!.displayName + " liked your comment."
        txtNotif.text = temp
    }

    private fun setProfileImage(view: View, userId: String, profilePic: ImageView) {
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { doc ->
                storage.child(doc.getString("prof_img")!!).downloadUrl
                    .addOnSuccessListener {
                        Glide.with(view)
                            .load(it)
                            .into(profilePic)
                    }
                    .addOnFailureListener { e ->
                        Log.d(ContentValues.TAG, "Failed to get profile picture for notification with " + e.stackTraceToString())
                    }
            }
    }

    private fun setCommentPreview(commentId: String, txtComment: TextView) {
        db.collection("comments").document(commentId)
            .get()
            .addOnSuccessListener { document ->
                val comment = document.getString("comment")!!
                if (comment.length > 128) {
                    val temp = comment.substring(0, 128) + "..."
                    txtComment.text = temp
                } else {
                    txtComment.text = comment
                }
            }
            .addOnFailureListener {
                txtComment.text = R.string.no_comment.toString()
            }
    }

    private fun setDate(ctx: Context, timestamp: Timestamp, txtDate: TextView) {
        val date = timestamp.toDate()
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        cal.time = date
        today.time = Timestamp.now().toDate()

        val calMil = cal.timeInMillis
        val mil = today.timeInMillis
        val periodSec = (mil - calMil) / 1000

        val elapsedYears = periodSec / 60 / 60 / 24 / 7 / 52
        val elapsedWeeks = periodSec / 60 / 60 / 24 / 7
        val elapsedDays = periodSec / 60 / 60 / 24
        val elapsedHrs = periodSec / 60 / 60
        val elapsedMin = periodSec / 60
        val elapsedSec = periodSec.toInt()

        when {
            elapsedYears > 0 -> txtDate.text = ctx.getString(R.string.years, elapsedYears)
            elapsedWeeks > 0 -> txtDate.text = ctx.getString(R.string.weeks, elapsedWeeks)
            elapsedDays > 0 -> txtDate.text = ctx.getString(R.string.days, elapsedDays)
            elapsedHrs > 0 -> txtDate.text = ctx.getString(R.string.hours, elapsedHrs)
            elapsedMin > 0 -> txtDate.text = ctx.getString(R.string.mins, elapsedMin)
            elapsedSec > 0 -> txtDate.text = ctx.getString(R.string.sec, elapsedSec)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}