import com.google.firebase.Timestamp

data class Notification(
    val commentId: String? = null,
    val userId: String? = null,
    val date: Timestamp = Timestamp.now()
)
