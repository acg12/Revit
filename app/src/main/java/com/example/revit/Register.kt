package com.example.revit

import User
import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Register : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        auth = Firebase.auth
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        findViewById<TextView>(R.id.btnSignIn).setOnClickListener {
            startActivity(Intent(this, Login::class.java))
        }

        findViewById<Button>(R.id.btnSignUp).setOnClickListener {
            register()
        }
    }

    private fun register() {
        var email = findViewById<TextInputLayout>(R.id.txtEmail).editText?.text.toString()
        var password = findViewById<TextInputLayout>(R.id.txtPassword).editText?.text.toString()
        var confPassword = findViewById<TextInputLayout>(R.id.txtConfPassword).editText?.text.toString()
        var username = findViewById<TextInputLayout>(R.id.txtUsername).editText?.text.toString()

        if(email.isEmpty() || password.isEmpty() || username.isEmpty() || confPassword.isEmpty()) {
            Toast.makeText(baseContext, "All fields must be filled!", Toast.LENGTH_SHORT).show()
        } else if(!password.equals(confPassword)) {
            Toast.makeText(baseContext, "Passwords don't match!", Toast.LENGTH_SHORT).show()
        } else if(password.length < 6) {
            Toast.makeText(baseContext, "Password must be 6 characters or more!", Toast.LENGTH_SHORT).show()
        }
        else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        val profileUpdates = userProfileChangeRequest {
                            displayName = username
                            photoUri = Uri.parse("profile-images/profile-default.png")
                        }
                        user!!.updateProfile(profileUpdates)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "User profile updated.")

                                    // new user
                                    val newUser = User(username)
                                    // create new user in database
                                    db.collection("users").document(user!!.uid).set(newUser)
                                    // transfer to login page
                                    startActivity(Intent(this, Login::class.java))
                                }
                            }
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(baseContext, "An error occured. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}