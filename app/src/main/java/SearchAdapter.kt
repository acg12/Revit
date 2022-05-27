import android.animation.RectEvaluator
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.revit.FragmentSearchDirections
import com.example.revit.R
import com.google.apphosting.datastore.testing.DatastoreTestTrace
import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import java.util.EnumSet.range
import kotlin.collections.HashMap

class SearchAdapter(private var orgData: MutableList<RevitThread>, private var orgId: MutableList<String>, private var dataSet: MutableList<RevitThread>, private var idSet: MutableList<String>):
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.card_search_result, viewGroup, false)

        return ViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.setOnClickListener {
            val action = FragmentSearchDirections.actionPageSearchToFragmentThreadDetail(idSet[position])
            viewHolder.itemView.findNavController().navigate(action)
        }

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val thread = dataSet[position]
        val threadId = idSet[position]

        val lblTitle: TextView = viewHolder.itemView.findViewById(R.id.lblTitle)
        val lblDesc: TextView = viewHolder.itemView.findViewById(R.id.lblDescription)
        val lblCommentCount: TextView = viewHolder.itemView.findViewById(R.id.lblComments)
        val lblTags: TextView = viewHolder.itemView.findViewById(R.id.lblTags)
        val lblDate: TextView = viewHolder.itemView.findViewById(R.id.lblDate)

        // Assign to labels
        lblTitle.text = thread.title
        lblDesc.text = thread.description

        var strTags = "Tags: "
        for (tag in thread.tags!!) {
            strTags += tag.trim() + ", "
        }
        strTags = strTags.dropLast(2)
        lblTags.text = strTags

        val dateFormat = SimpleDateFormat("dd/MM/yy")
        lblDate.text = dateFormat.format(thread.created_at!!.toDate())

        val db = Firebase.firestore
        db.collection("comments")
            .whereEqualTo("threadId", threadId)
            .get()
            .addOnSuccessListener { documents ->
                lblCommentCount.text = documents.size().toString() + " comments"
            }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

    fun clearFilterSort() {
        dataSet = orgData.toMutableList()
        idSet = orgId.toMutableList()

        this.notifyDataSetChanged()
    }

    fun filter(filter: String) {
        val sdf = SimpleDateFormat("dd/MM/yy")
        val todayDate = Timestamp.now().toDate()
        val todayCal = Calendar.getInstance()
        todayCal.time = todayDate
        var modifiedDate = todayCal.time

        when {
            filter.equals("last-day") -> {
                todayCal.add(Calendar.DATE, -1)
                modifiedDate = todayCal.time
            }
            filter.equals("last-week") -> {
                todayCal.add(Calendar.DATE, -7)
                modifiedDate = todayCal.time
            }
            filter.equals("last-month") -> {
                todayCal.add(Calendar.MONTH, -1)
                modifiedDate = todayCal.time
            }
            filter.equals("last-year") -> {
                todayCal.add(Calendar.YEAR, -1)
                modifiedDate = todayCal.time
            }
        }

        Log.d("Modified date", sdf.format(modifiedDate))

        // Empty datasets and assign new data from original datasets
        dataSet.clear()
        idSet.clear()

        var i = 0
        for (data in orgData) {
            if(modifiedDate <= data.created_at!!.toDate()) {
                dataSet.add(data)
                idSet.add(orgId[i])
            }
            i += 1
        }

        this.notifyDataSetChanged()
    }

    fun sort(args: String) {
        val db = Firebase.firestore
        val tempId = idSet.toMutableList()

        dataSet.clear()
        idSet.clear()

        when {
            args.equals("most-saved") -> {
                val savedCount = HashMap<String, Int>()

                for (id in tempId) {
                    savedCount[id] = 0
                }
                db.collection("saves")
                    .get()
                    .addOnSuccessListener { documents ->
                        for (doc in documents) {
                            val id = doc.getString("threadId")!!
                            savedCount[id] = savedCount[id]!! + 1
                        }
                        val sortedCount = savedCount.toList().sortedByDescending { (_, value) -> value }.toMap()

                        db.collection("threads").get()
                            .addOnSuccessListener { threads ->
                                for (item in sortedCount) {
                                    for (thread in threads) {
                                        if (thread.id == item.key) {
                                            dataSet.add(thread.toObject<RevitThread>())
                                            break
                                        }
                                    }
                                }

                                idSet = sortedCount.keys.toMutableList()
                                this.notifyDataSetChanged()
                            }
                    }
            }
            args.equals("most-comments") -> {
                val commentCount = HashMap<String, Int>()

                for (id in tempId) {
                    commentCount[id] = 0
                }
                db.collection("comments")
                    .get()
                    .addOnSuccessListener { documents ->
                        for (doc in documents) {
                            val id = doc.getString("threadId")!!
                            commentCount[id] = commentCount[id]!! + 1
                        }
                        val sortedCount = commentCount.toList().sortedByDescending { (_, value) -> value }.toMap()

                        db.collection("threads").get()
                            .addOnSuccessListener { threads ->
                                for (item in sortedCount) {
                                    for (thread in threads) {
                                        if (thread.id == item.key) {
                                            dataSet.add(thread.toObject<RevitThread>())
                                            break
                                        }
                                    }
                                }

                                idSet = sortedCount.keys.toMutableList()
                                this.notifyDataSetChanged()
                            }
                    }
            }
        }
    }
}
