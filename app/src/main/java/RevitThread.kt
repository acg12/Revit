import com.google.firebase.Timestamp

data class RevitThread(
    val title: String? = null,
    val description: String? = null,
    val tags: List<String>? = null,
    val cover_img: String? = null,
    val created_at: Timestamp? = Timestamp.now()
)
