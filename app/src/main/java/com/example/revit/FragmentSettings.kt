package com.example.revit

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.card.MaterialCardView

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FragmentSettings.newInstance] factory method to
 * create an instance of this fragment.
 */
class FragmentSettings : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        val appbar: MaterialToolbar = view.findViewById(R.id.topAppBar)
        appbar.setNavigationOnClickListener {
            back(view)
        }

        val cardChangePassword: MaterialCardView = view.findViewById(R.id.cardChangePassword)
        cardChangePassword.setOnClickListener {
            val action = FragmentSettingsDirections.actionFragmentSettingsToFragmentChangePassword()
            view.findNavController().navigate(action)
        }

        return view
    }

    private fun back(view: View) {
        view.findNavController().navigateUp()
    }
}