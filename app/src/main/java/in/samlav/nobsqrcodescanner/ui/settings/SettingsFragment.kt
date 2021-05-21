package `in`.samlav.nobsqrcodescanner.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import `in`.samlav.nobsqrcodescanner.Constants
import android.widget.Switch
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
        val appendErrorSwitch = root.findViewById<SwitchMaterial>(R.id.appendErrorSwitch)

        val sharedPreferences = this.activity?.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
        if (sharedPreferences != null)
        {
            intentsSwitch.isChecked = sharedPreferences.getBoolean(Constants.EXECUTE_INTENTS, false)
            appendErrorSwitch.isChecked = sharedPreferences.getBoolean(Constants.APPEND_ERROR, true)
        }
        val switches = setOf<SwitchMaterial>(intentsSwitch, appendErrorSwitch)
        val constants = setOf(Constants.EXECUTE_INTENTS, Constants.APPEND_ERROR)
        switches.zip(constants).forEach {
            it.first.setOnClickListener { view ->
                val sharedPreferences = this.activity?.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
                if (sharedPreferences != null)
                {
                    with(sharedPreferences.edit()) {
                        putBoolean(it.second, it.first.isChecked)
                        apply()
                    }
                }
            }
        }
        return root
    }
}