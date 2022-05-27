import com.google.firebase.Timestamp

data class Comment(
    val userId: String? = null,
    val threadId: String? = null,
    val comment: String? = null,
    val created_at: Timestamp = Timestamp.now()
)
