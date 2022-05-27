package com.example.revit

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.navigation.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentChangePassword.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentChangePassword : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_change_password, container, false)

        val appbar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        appbar.setNavigationOnClickListener {
            back(view)
        }

        val btnChangePw: Button = view.findViewById(R.id.btnChangePw)
        btnChangePw.setOnClickListener {
            val oldPassword = view.findViewById<TextInputLayout>(R.id.txtOldPassword).editText?.text.toString()
            val newPassword = view.findViewById<TextInputLayout>(R.id.txtNewPassword).editText?.text.toString()
            val confPassword = view.findViewById<TextInputLayout>(R.id.txtConfPassword).editText?.text.toString()

            updatePassword(view, oldPassword, newPassword, confPassword)
            clearFields(view)
        }

        return view
    }

    private fun clearFields(view: View?) {
        val old: EditText? = view?.findViewById<TextInputLayout>(R.id.txtOldPassword)?.editText
        val new: EditText? = view?.findViewById<TextInputLayout>(R.id.txtNewPassword)?.editText
        val conf: EditText? = view?.findViewById<TextInputLayout>(R.id.txtConfPassword)?.editText

        old?.setText("")
        new?.setText("")
        conf?.setText("")
    }

    private fun updatePassword(view: View, old: String, new: String, conf: String) {
        val user = Firebase.auth.currentUser

        if(old.isEmpty() || new.isEmpty() || conf.isEmpty()) {
            Toast.makeText(view.context, "All fields must be filled.", Toast.LENGTH_SHORT).show()
        } else if(new != conf) {
            Toast.makeText(view.context, "New and confirmation passwords don't match.", Toast.LENGTH_SHORT).show()
        } else {
            val credentials: AuthCredential = EmailAuthProvider.getCredential(user!!.email.toString(), old)

            user.reauthenticate(credentials)
                .addOnSuccessListener {
                    user.updatePassword(new)
                        .addOnSuccessListener {
                            Log.d(TAG, "User password updated")
                        }
                }
                .addOnFailureListener {
                    Toast.makeText(view.context, "Incorrect password.", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Authentication failed")
                }
        }
    }

    private fun back(view: View) {
        view.findNavController().navigateUp()
    }
}