package com.example.revit

import RevitThread
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.fragment.navArgs
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentEditThread.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentEditThread : Fragment() {
    // TODO: Rename and change types of parameters
    private val args: FragmentThreadDetailArgs by navArgs()
    private val db = Firebase.firestore
    private val user = Firebase.auth.currentUser
    private val storage = Firebase.storage.reference
    private lateinit var imgPreview: ImageView
    private lateinit var imgUri: Uri

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_thread, container, false)

        val title = view.findViewById<TextInputLayout>(R.id.txtTitle).editText
        val description = view.findViewById<TextInputLayout>(R.id.txtDescription).editText
        val tags = view.findViewById<TextInputLayout>(R.id.txtTags).editText
        val btnEdit: Button = view.findViewById(R.id.btn_edit_thread)
        val btnCancel: Button = view.findViewById(R.id.btn_cancel_thread)
        val btnUpload: Button = view.findViewById(R.id.btn_upload_img)
        val appbar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        imgPreview = view.findViewById(R.id.img_preview)

        appbar.setNavigationOnClickListener {
            back(view)
        }

        // Add details
        val threadRef = db.collection("threads").document(args.threadId)
        threadRef.get()
            .addOnSuccessListener { document ->
                val data = document.toObject<RevitThread>()

                title!!.setText(data!!.title)
                description!!.setText(data.description)

                var strTags = ""
                for (tag in data.tags!!) {
                    strTags += tag.trim() + ", "
                }
                strTags = strTags.dropLast(2)
                tags!!.setText(strTags)

                storage.child(data.cover_img!!).downloadUrl.addOnSuccessListener {
                    Glide.with(view)
                        .load(it)
                        .into(imgPreview)
                }
            }

        // Add button listeners
        btnEdit.setOnClickListener {
            saveEdits(view, title, description, tags)
        }

        btnUpload.setOnClickListener {
            uploadImage(view)
        }

        btnCancel.setOnClickListener {
            back(view)
        }

        return view
    }

    private fun uploadImage(view: View) {
        imgPreview = view.findViewById(R.id.img_preview)

        if (ContextCompat.checkSelfPermission(this.requireContext(), android.Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this.requireActivity(), arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                1)
        } else {
            val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(galleryIntent,2)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val galleryIntent = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            imgUri = data.data!!

            val storageRef = Firebase.storage.reference
            val riversRef = storageRef.child("thread-images/${imgUri.lastPathSegment}")
            val uploadTask = riversRef.putFile(imgUri)

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener { e ->
                Log.d("Image upload", e.stackTraceToString())
                Toast.makeText(context, "Image upload failed. Please try again later.", Toast.LENGTH_SHORT).show()
            }.addOnSuccessListener { taskSnapshot ->
                imgPreview.setImageURI(imgUri)
                Log.d("Image upload", taskSnapshot.metadata.toString())
                Toast.makeText(context, "Image upload successful.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun saveEdits(view: View, title: EditText?, description: EditText?, tags: EditText?) {
        val txtTitle = title!!.text!!.toString()
        val txtDesc = description!!.text!!.toString()
        val txtTags = tags!!.text!!.toString()

        if(txtTitle.isEmpty() || txtDesc.isEmpty() || txtTags.isEmpty() || imgUri == null) {
            Toast.makeText(context, "All fields must be filled.", Toast.LENGTH_SHORT).show()
        } else {
            val listTags = txtTags.split(',')

            db.collection("thread").document(args.threadId)
                .update(mapOf(
                    "title" to txtTitle,
                    "description" to txtDesc,
                    "tags" to listTags,
                    "cover_img" to imgUri.lastPathSegment
                ))
                .addOnSuccessListener {
                    back(view)
                }
                .addOnFailureListener { e ->
                    Log.w(ContentValues.TAG, "Error updating thread", e)
                }
        }
    }

    private fun back(view: View) {
        view.findNavController().navigateUp()
    }
}