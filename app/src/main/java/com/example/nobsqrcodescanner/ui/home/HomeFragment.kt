package com.example.nobsqrcodescanner.ui.home

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.example.nobsqrcodescanner.MainActivity
import com.example.nobsqrcodescanner.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.selector.off
import io.fotoapparat.selector.torch
import io.fotoapparat.view.CameraRenderer
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.fragment_home.*
import java.io.File

class HomeFragment : Fragment()
{
    var fotoapparat: Fotoapparat? = null
    val sd = Environment.getExternalStorageDirectory()
    var fotoapparatState: FotoapparatState? = null
    var cameraStatus: CameraState? = null
    var flashState: FlashState? = null
    var cameraView: CameraRenderer? = null
    val permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)

    private lateinit var homeViewModel: HomeViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View?
    {
        homeViewModel =
            ViewModelProviders.of(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)
        cameraView = root.findViewById<CameraView>(R.id.camera_view)
        root.findViewById<FloatingActionButton>(R.id.fab_switch_camera).setOnClickListener {
            switchCamera()
        }
        root.findViewById<FloatingActionButton>(R.id.fab_flash).setOnClickListener {
            changeFlashState()
        }
        createFotoapparat()

        cameraStatus = CameraState.BACK
        flashState = FlashState.OFF
        fotoapparatState = FotoapparatState.OFF

        return root
    }

    private fun createFotoapparat()
    {
        if (cameraView != null)
        {
            fotoapparat = activity?.let {
                Fotoapparat(
                    context = it.applicationContext,
                    view = cameraView!!,
                    scaleType = ScaleType.CenterCrop,
                    lensPosition = back(),
                    logger = loggers(
                        logcat()
                    ),
                    cameraErrorCallback = { error ->
                        println("Recorder errors: $error")
                    }
                )
            }
        }
    }

    private fun changeFlashState()
    {
        fotoapparat?.updateConfiguration(
            CameraConfiguration(
                flashMode = if (flashState == FlashState.TORCH) off() else torch()
            )
        )

        if (flashState == FlashState.TORCH) flashState = FlashState.OFF
        else flashState = FlashState.TORCH
    }

    private fun switchCamera()
    {
        fotoapparat?.switchTo(
            lensPosition = if (cameraStatus == CameraState.BACK) front() else back(),
            cameraConfiguration = CameraConfiguration()
        )

        if (cameraStatus == CameraState.BACK) cameraStatus = CameraState.FRONT
        else cameraStatus = CameraState.BACK
    }

    override fun onStart()
    {
        super.onStart()
        if (hasNoPermissions())
        {
            requestPermission()
        } else
        {
            fotoapparat?.start()
            fotoapparatState = FotoapparatState.ON
        }
    }

    private fun hasNoPermissions(): Boolean
    {
        return activity?.let {
            ContextCompat.checkSelfPermission(
                it.applicationContext,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
        } != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
            requireActivity().applicationContext,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun requestPermission()
    {
        activity?.let { ActivityCompat.requestPermissions(it, permissions, 0) }
    }

    override fun onStop()
    {
        super.onStop()
        fotoapparat?.stop()
        FotoapparatState.OFF
    }

    override fun onResume()
    {
        super.onResume()
        if (!hasNoPermissions() && fotoapparatState == FotoapparatState.OFF)
        {
            val intent = Intent(activity?.applicationContext, MainActivity::class.java)
            startActivity(intent)
            activity?.finish()
        }
    }
}

enum class CameraState
{
    FRONT, BACK
}

enum class FlashState
{
    TORCH, OFF
}

enum class FotoapparatState
{
    ON, OFF
}