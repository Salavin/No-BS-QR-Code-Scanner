package `in`.samlav.nobsqrcodescanner.ui.home

import android.Manifest
import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProviders
import `in`.samlav.nobsqrcodescanner.Constants
import `in`.samlav.nobsqrcodescanner.MainActivity
import com.example.nobsqrcodescanner.R
import `in`.samlav.processqrcode.QRCode
import `in`.samlav.processqrcode.QRCodeType
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.barcode.Barcode.FORMAT_QR_CODE
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
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

class HomeFragment : Fragment()
{
    var fotoapparat: Fotoapparat? = null
    val sd = Environment.getExternalStorageDirectory()
    var fotoapparatState: FotoapparatState? = null
    var cameraStatus: CameraState? = null
    var flashState: FlashState? = null
    var cameraView: CameraRenderer? = null
    val permissions = arrayOf(Manifest.permission.CAMERA)
    var alertDialog: AlertDialog? = null
    private lateinit var barcodeScanner: BarcodeScanner

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
        root.findViewById<FloatingActionButton>(R.id.fab_upload).setOnClickListener {
            uploadImage()
        }
        createFotoapparat()

        cameraStatus = CameraState.BACK
        flashState = FlashState.OFF
        fotoapparatState = FotoapparatState.OFF

        return root
    }

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                FORMAT_QR_CODE
            ).build()

        barcodeScanner = BarcodeScanning.getClient(options)
    }

    private fun createFotoapparat()
    {
        if (cameraView != null)
        {
            val cameraConfiguration = CameraConfiguration(
                frameProcessor = { frame ->
                    val inputImage = InputImage.fromByteArray(
                        frame.image,
                        frame.size.width,
                        frame.size.height,
                        frame.rotation,
                        InputImage.IMAGE_FORMAT_NV21
                    )
                    handleScanning(inputImage)
                }
            )
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
                    },
                    cameraConfiguration = cameraConfiguration
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

    private fun uploadImage()
    {
        val gallery = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.INTERNAL_CONTENT_URI)
        startActivityForResult(gallery, Constants.PICK_IMAGE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == Constants.PICK_IMAGE)
        {
            val imageUri = data?.data
            val inputImage = InputImage.fromFilePath(context, imageUri)
            handleScanning(inputImage)
        }
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
        return ContextCompat.checkSelfPermission(
            requireActivity().applicationContext,
            Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission()
    {
        activity?.let { ActivityCompat.requestPermissions(it, permissions, Constants.REQUEST_PERMS) }
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

    private fun handleScanning(inputImage: InputImage)
    {
        val task = barcodeScanner.process(inputImage)
        task.addOnSuccessListener { barCodesList ->
            if (this.alertDialog == null || !this.alertDialog?.isShowing!!)
            {
                for (barcodeObject in barCodesList)
                {
                    val barcodeValue = barcodeObject.rawValue
                    if (barcodeValue != null)
                    {
                        val processQRCode = QRCode(barcodeValue)
                        val sharedPreferences = activity?.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
                        if ((sharedPreferences != null) && sharedPreferences.getBoolean(Constants.EXECUTE_INTENTS, false))
                        {
                            executeIntent(processQRCode)
                            return@addOnSuccessListener
                        }
                        var message: String

                        val title: String = when (processQRCode.type)
                        {
                            QRCodeType.TEXT -> "Decoded text:"
                            QRCodeType.URL -> "Open URL?"
                            QRCodeType.EMAIL -> "Open email application?"
                            QRCodeType.TEL -> "Open phone app?"
                            QRCodeType.CONTACT -> "Add contact?"
                            QRCodeType.SMS -> "Send text message?"
                            QRCodeType.GEO -> "Open maps application?"
                            QRCodeType.WIFI -> "Connect to WiFi network?"
                            QRCodeType.MARKET -> "Open app store?"
                            else -> "Error"
                        }
                        if (processQRCode.type == QRCodeType.TEXT)
                        {
                            message = processQRCode.string
                        } else
                        {
                            message = ""
                            for (key in processQRCode.data.keys)
                            {
                                message += key + ": " + processQRCode.data[key] + '\n'
                            }
                        }
                        this.alertDialog = activity?.let {
                            val builder = AlertDialog.Builder(it)
                            builder.apply {
                                setTitle(title)
                                setMessage(message)
                            }
                            if (processQRCode.type == QRCodeType.TEXT)
                            {
                                builder.setNeutralButton("Dismiss") { _, _ -> }
                            } else
                            {
                                builder.setNegativeButton("No") { _, _ -> }
                                builder.setPositiveButton("Yes") { _, _ ->
                                    executeIntent(processQRCode)
                                }
                            }
                            builder.show()
                        }
                    }
                }
            }
        }
    }

    private fun executeIntent(processQRCode: QRCode)
    {
        if (processQRCode.type == QRCodeType.WIFI)
        {
            startActivityForResult(processQRCode.intent, Constants.WIFI_CODE)
        } else
        {
            startActivity(processQRCode.intent)
        }
    }

    fun Context.toast(message: CharSequence) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
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