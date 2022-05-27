package com.example.revit

import RevitThread
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentNewThread.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentNewThread : Fragment() {
    private val db = Firebase.firestore
    private lateinit var imgPreview: ImageView
    private var imgUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_new_thread, container, false)
        val createBtn: Button = view.findViewById(R.id.btn_create_thread)
        createBtn.setOnClickListener {
            createThread(view)
        }

        val cancelBtn: Button = view.findViewById(R.id.btn_cancel_thread)
        cancelBtn.setOnClickListener {
            resetFields(view)
        }

        val uploadBtn: Button = view.findViewById(R.id.btn_upload_img)
        uploadBtn.setOnClickListener {
            uploadImage(view)
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
            val riversRef = storageRef.child("thread-images/${imgUri!!.lastPathSegment}")
            val uploadTask = riversRef.putFile(imgUri!!)

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

    private fun createThread(view: View) {
        val title = view.findViewById<TextInputLayout>(R.id.txtTitle).editText?.text.toString()
        val desc = view.findViewById<TextInputLayout>(R.id.txtDescription).editText?.text.toString()
        val tags = view.findViewById<TextInputLayout>(R.id.txtTags).editText?.text.toString()

        if(title.isEmpty() || desc.isEmpty() || tags.isEmpty() || imgUri == null) {
            Toast.makeText(context, "All fields must be filled.", Toast.LENGTH_SHORT).show()
        } else {
            val listTags = tags.split(',')
            val newThread = RevitThread(title, desc, listTags, "thread-images/" + imgUri!!.lastPathSegment)
            db.collection("threads").add(newThread)
                .addOnSuccessListener { documentReference ->
                    Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                }.addOnFailureListener { e ->
                    Log.w(TAG, "Error adding document", e)
                }
            resetFields(view)
        }
    }

    private fun resetFields(view: View) {
        val txtTitle = view.findViewById<TextInputLayout>(R.id.txtTitle).editText
        val txtDesc = view.findViewById<TextInputLayout>(R.id.txtDescription).editText
        val txtTags = view.findViewById<TextInputLayout>(R.id.txtTags).editText

        txtTitle?.setText("")
        txtDesc?.setText("")
        txtTags?.setText("")
    }
}