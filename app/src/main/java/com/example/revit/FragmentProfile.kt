package com.example.revit

import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.text.method.KeyListener
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import android.view.View.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentProfile.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentProfile : Fragment() {
    // TODO: Rename and change types of parameters
    private val user = Firebase.auth.currentUser
    private val db = Firebase.firestore
    private val storage = Firebase.storage.reference
    private var isEditing = false
    private lateinit var profilePic: ImageView
    private var profileUri: Uri? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        val txtEmail: EditText = view.findViewById<TextInputLayout>(R.id.txtEmail).editText!!
        val txtUsername: EditText = view.findViewById<TextInputLayout>(R.id.txtUsername).editText!!

        val countSaved: TextView = view.findViewById(R.id.lblSavedCount)
        val countComments: TextView = view.findViewById(R.id.lblCommentsCount)
        val countLikedComm: TextView = view.findViewById(R.id.lblLikedCommCount)

        profilePic = view.findViewById(R.id.profilePicture)

        val appBar: MaterialToolbar = view.findViewById(R.id.topAppBar)

        setMenuClickListener(appBar)
        initViews(view, txtEmail, txtUsername)
        initCounts(countSaved, countComments, countLikedComm)

        val cardSaved: MaterialCardView = view.findViewById(R.id.cardSavedThreads)
        cardSaved.setOnClickListener {
            val action = FragmentProfileDirections.actionPageProfileToFragmentSavedThreads()
            view.findNavController().navigate(action)
        }

        val cardComments: MaterialCardView = view.findViewById(R.id.cardComments)
        cardComments.setOnClickListener {
            val action = FragmentProfileDirections.actionPageProfileToFragmentComments()
            view.findNavController().navigate(action)
        }

        val cardLikedComments: MaterialCardView = view.findViewById(R.id.cardLikedComments)
        cardLikedComments.setOnClickListener {
            val action = FragmentProfileDirections.actionPageProfileToFragmentLikedComments()
            view.findNavController().navigate(action)
        }

        val uploadBtn: TextView = view.findViewById(R.id.btnEditProfPic)
        uploadBtn.setOnClickListener {
            uploadImage()
        }

        val editBtn: Button = view.findViewById(R.id.btnEditProfile)
        editBtn.setOnClickListener {
            if(isEditing) {
                saveProfile(txtEmail, txtUsername, editBtn)
                isEditing = false
            } else {
                editProfile(txtEmail, txtUsername, editBtn)
                isEditing = true
            }
            Log.d("IsEditing", isEditing.toString())
        }

        val btnLogout: Button = view.findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            Firebase.auth.signOut()
            val action = FragmentProfileDirections.actionPageProfileToLogin()
            view.findNavController().navigate(action)
        }

        return view
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.profile_app_bar, menu)
    }

    private fun setMenuClickListener(toolbar: MaterialToolbar) = with(toolbar) {
        setOnMenuItemClickListener { menuItem ->
            if(menuItem.itemId == R.id.menuSettings) {
                val action = FragmentProfileDirections.actionPageProfileToFragmentSettings()
                requireView().findNavController().navigate(action)
                return@setOnMenuItemClickListener true
            }
            return@setOnMenuItemClickListener false
        }
    }

    private fun initCounts(countSaved: TextView, countComments: TextView, countLikedComm: TextView) {
        db.collection("saves")
            .whereEqualTo("userId", user!!.uid)
            .get()
            .addOnSuccessListener { documents ->
                countSaved.text = documents.size().toString()
            }

        db.collection("comments")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                countComments.text = documents.size().toString()
            }

        db.collection("likes")
            .whereEqualTo("userId", user.uid)
            .get()
            .addOnSuccessListener { documents ->
                countLikedComm.text = documents.size().toString()
            }
    }

    private fun initViews(view: View, txtEmail: EditText, txtUsername: EditText) {
        txtEmail.setText(user!!.email)
        txtUsername.setText(user.displayName)
        txtEmail.keyListener = null
        txtUsername.keyListener = null

        storage.child(user.photoUrl.toString()).downloadUrl.addOnSuccessListener {
            Glide.with(view)
                .load(it)
                .into(profilePic)
        }
    }

    private fun saveProfile(
        txtEmail: EditText,
        txtUsername: EditText,
        editBtn: Button
    ) {
        editBtn.text = "Edit Profile"
        txtEmail.keyListener = null
        txtUsername.keyListener = null

        db.collection("users").document(user!!.uid)
            .update("username", txtUsername.text.toString())

        val profileUpdates = userProfileChangeRequest {
            displayName = txtUsername.text.toString()
        }

        user.updateProfile(profileUpdates)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User username updated.")
                }
            }

        user.updateEmail(txtEmail.text.toString())
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "User email address updated.")
                }
            }
    }

    private fun editProfile(
        txtEmail: EditText,
        txtUsername: EditText,
        editBtn: Button
    ) {
        editBtn.text = "Save"
        txtEmail.inputType = InputType.TYPE_TEXT_VARIATION_NORMAL
        txtEmail.isCursorVisible = true
        txtUsername.inputType = InputType.TYPE_TEXT_VARIATION_NORMAL
        txtUsername.isCursorVisible = true
    }

    private fun uploadImage() {
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
                val galleryIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(galleryIntent,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null) {
            profileUri = data.data!!

            val storageRef = Firebase.storage.reference
            val riversRef = storageRef.child("profile-images/${profileUri!!.lastPathSegment}")
            val uploadTask = riversRef.putFile(profileUri!!)

            // Register observers to listen for when the download is done or if it fails
            uploadTask.addOnFailureListener { e ->
                Log.d("Image upload", e.stackTraceToString())
                Toast.makeText(context, "Image upload failed. Please try again later.", Toast.LENGTH_SHORT).show()
            }.addOnSuccessListener { taskSnapshot ->
                profilePic.setImageURI(profileUri)
                Log.d("Image upload", taskSnapshot.metadata.toString())
                Toast.makeText(context, "Image upload successful.", Toast.LENGTH_SHORT).show()
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

}