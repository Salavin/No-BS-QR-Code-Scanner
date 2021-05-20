package com.example.nobsqrcodescanner.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.nobsqrcodescanner.Constants
import com.example.nobsqrcodescanner.R
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsFragment : Fragment()
{

    private lateinit var settingsViewModel: SettingsViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        settingsViewModel =
            ViewModelProviders.of(this).get(SettingsViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        val intentsSwitch = root.findViewById<SwitchMaterial>(R.id.automaticIntentsSwitch)
        val sharedPreferences = this.activity?.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
        if (sharedPreferences != null)
        {
            intentsSwitch.isChecked = sharedPreferences.getBoolean(Constants.EXECUTE_INTENTS, false)
        }
        intentsSwitch.setOnClickListener {
            if (sharedPreferences != null)
            {
                with(sharedPreferences.edit()) {
                    putBoolean(Constants.EXECUTE_INTENTS, intentsSwitch.isChecked)
                    apply()
                }
            }
        }
        return root
    }
}