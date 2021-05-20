package com.example.nobsqrcodescanner.ui.about

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.nobsqrcodescanner.Constants
import com.example.nobsqrcodescanner.R

class AboutFragment : Fragment()
{

    private lateinit var aboutViewModel: AboutViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        aboutViewModel =
            ViewModelProviders.of(this).get(AboutViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_about, container, false)
        val imageView = root.findViewById<ImageView>(R.id.appImage)
        val websiteButton = root.findViewById<Button>(R.id.websiteButton)
        val repositoryButton = root.findViewById<Button>(R.id.repositoryButton)
        val googlePayButton = root.findViewById<Button>(R.id.googlePayButton)

        imageView.setOnClickListener {
            Toast.makeText(context, "\uD83D\uDC23", Toast.LENGTH_SHORT).show()
        }

        websiteButton.setOnClickListener {
            goToUrl(Constants.PERSONAL_WEBSITE)
        }

        repositoryButton.setOnClickListener {
            goToUrl(Constants.REPOSITORY)
        }

        googlePayButton.setOnClickListener {
            goToUrl(Constants.GOOGLE_PAY)
        }
        return root
    }

    private fun goToUrl (url: String)
    {
        val uriUrl = Uri.parse(url)
        val launchBrowser = Intent(Intent.ACTION_VIEW, uriUrl)
        startActivity(launchBrowser)
    }
}