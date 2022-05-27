package com.example.revit

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class Login : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize Firebase Auth
        auth = Firebase.auth
//        Log.d("auth to string", auth.toString())
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        findViewById<TextView>(R.id.btnSignUp).setOnClickListener {
            startActivity(Intent(this, Register::class.java))
        }

        findViewById<Button>(R.id.btnSignIn).setOnClickListener {
            login()
        }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        verifyLogin()
    }

    private fun verifyLogin() {
        val currentUser = auth.currentUser
        if(currentUser != null){
            if(currentUser.displayName == "admin") {
                startActivity(Intent(this, MainAdmin::class.java))
            } else {
                startActivity(Intent(this, MainActivity::class.java))
            }
        }
    }

    private fun login() {
        val email = findViewById<TextInputLayout>(R.id.txtEmail).editText?.text.toString()
        val password = findViewById<TextInputLayout>(R.id.txtPassword).editText?.text.toString()

        if(email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "All fields must be filled!", Toast.LENGTH_SHORT).show()
        } else {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "signInWithEmail:success")
                        verifyLogin()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "signInWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext,
                            "Incorrect email or password.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }
}