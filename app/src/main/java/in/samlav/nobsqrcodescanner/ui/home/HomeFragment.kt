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
import `in`.samlav.processqrcode.APPEND_ERROR
import com.example.nobsqrcodescanner.R
import `in`.samlav.processqrcode.QRCode
import `in`.samlav.processqrcode.QRCodeOptions
import `in`.samlav.processqrcode.QRCodeType
import android.content.ClipData
import android.content.ClipboardManager
import android.util.Log
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ALL_FORMATS
import com.google.mlkit.vision.common.InputImage
import io.fotoapparat.Fotoapparat
import io.fotoapparat.configuration.CameraConfiguration
import io.fotoapparat.log.logcat
import io.fotoapparat.log.loggers
import io.fotoapparat.parameter.Flash
import io.fotoapparat.parameter.ScaleType
import io.fotoapparat.parameter.Zoom
import io.fotoapparat.selector.back
import io.fotoapparat.selector.front
import io.fotoapparat.selector.off
import io.fotoapparat.selector.torch
import io.fotoapparat.view.CameraRenderer
import io.fotoapparat.view.CameraView
import kotlinx.android.synthetic.main.fragment_home.*
import kotlin.math.roundToInt

class HomeFragment : Fragment()
{
    var fotoapparat: Fotoapparat? = null
    var fotoapparatState: FotoapparatState? = null
    var cameraStatus: CameraState? = null
    var flashState: FlashState? = null
    var cameraView: CameraRenderer? = null
    val permissions = arrayOf(Manifest.permission.CAMERA)
    var alertDialog: AlertDialog? = null
    var hasExecuted = false
    var curZoom: Float = 0f
    private lateinit var barcodeScanner: BarcodeScanner
    private lateinit var cameraZoom: Zoom.VariableZoom
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
                FORMAT_ALL_FORMATS
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
                    focusView = focusView,
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
        adjustViewsVisibility()
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
        hasExecuted = false
        if (hasNoPermissions())
        {
            requestPermission()
        } else
        {
            fotoapparat?.start()
            fotoapparatState = FotoapparatState.ON
            adjustViewsVisibility()
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
        hasExecuted = false
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
            if ((this.alertDialog == null || !this.alertDialog?.isShowing!!) && !hasExecuted)
            {
                for (barcodeObject in barCodesList)
                {
                    val barcodeValue = barcodeObject.rawValue
                    if (barcodeValue != null)
                    {
                        val sharedPreferences = activity?.getSharedPreferences(Constants.SHARED_PREF, Context.MODE_PRIVATE)
                        val processQRCodeOptions = QRCodeOptions()
                        if (sharedPreferences != null)
                        {
                            processQRCodeOptions.setOption(APPEND_ERROR, sharedPreferences.getBoolean(Constants.APPEND_ERROR, true))
                        }
                        else
                        {
                            processQRCodeOptions.setOption(APPEND_ERROR, true)
                        }
                        val processQRCode = QRCode(barcodeValue, processQRCodeOptions)
                        if ((processQRCode.type != QRCodeType.TEXT) && (sharedPreferences != null) && sharedPreferences.getBoolean(Constants.EXECUTE_INTENTS, false))
                        {
                            hasExecuted = true
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
                                builder.setPositiveButton("Copy Text") {_, _ ->
                                    val clipboard = requireActivity().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip: ClipData = ClipData.newPlainText("Copied text from QR code", message)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(requireActivity().applicationContext, "Copied QR code text to clipboard.", Toast.LENGTH_SHORT).show()
                                }
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

    private fun adjustViewsVisibility() {
        fotoapparat?.getCapabilities()
            ?.whenAvailable { capabilities ->
                capabilities
                    ?.let {
                        (it.zoom as? Zoom.VariableZoom)
                            ?.let {
                                cameraZoom = it
                                focusView.scaleListener = this::scaleZoom
                                focusView.ptrListener = this::pointerChanged
                            }
                            ?: run {
                                zoomLvl?.visibility = View.GONE
                                focusView.scaleListener = null
                                focusView.ptrListener = null
                            }

                        fab_flash.visibility = if (it.flashModes.contains(Flash.Torch)) View.VISIBLE else View.GONE
                    }
            }

        fab_switch_camera.visibility = if (fotoapparat?.isAvailable(front()) == true) View.VISIBLE else View.GONE
    }

    //When zooming slowly, the values are approximately 0.9 ~ 1.1
    private fun scaleZoom(scaleFactor: Float) {
        //convert to -0.1 ~ 0.1
        val plusZoom = if (scaleFactor < 1) -1 * (1 - scaleFactor) else scaleFactor - 1
        val newZoom = curZoom + plusZoom
        if (newZoom < 0 || newZoom > 1) return

        curZoom = newZoom
        fotoapparat?.setZoom(curZoom)
        val progress = (cameraZoom.maxZoom * curZoom).roundToInt()
        val value = cameraZoom.zoomRatios[progress]
        val roundedValue = ((value.toFloat()) / 10).roundToInt().toFloat() / 10

        zoomLvl.visibility = View.VISIBLE
        zoomLvl.text = String.format("%.1f×", roundedValue)
    }

    private fun pointerChanged(fingerCount: Int){
        if(fingerCount == 0) {
            zoomLvl?.visibility = View.GONE
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