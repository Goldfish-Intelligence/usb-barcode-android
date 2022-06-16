package de.david_wilkening.companion_scanner

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.camera2.CaptureRequest
import android.util.Log
import android.util.Range
import android.util.Rational
import android.util.Size
import android.view.WindowManager
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Composable
fun KeepScreenOn() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = context.findActivity()?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
fun BarcodeScanner(onScan: (Barcode) -> Unit, isTorchOn: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewBuilder = Preview.Builder()
    var camera2Extender = Camera2Interop.Extender(previewBuilder).setCaptureRequestOption(
        CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, Range(24,24)
    )
    val preview = previewBuilder.build()
    val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(Size(1200, 1200)).build()
        .also {
            it.setAnalyzer(
                ContextCompat.getMainExecutor(context),
                BarcodeImageAnalyzer(onScan)
            )
        }
    val cameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
        .build()

    val previewView = remember { PreviewView(context) }

    // this re-initializes the whole camera on torch toggle
    // the camera provider could probably be saved outside the LaunchedEffect
    // totally unrelated: I find it interesting to handle the (physical) torch as part of the UI tree
    LaunchedEffect(isTorchOn) {
        val viewPort =  ViewPort.Builder(Rational(1, 1), preview.targetRotation).build()
        val useCaseGroup = UseCaseGroup.Builder()
            .addUseCase(preview)
            .addUseCase(imageAnalysis)
            .setViewPort(viewPort)
            .build()


        val cameraProvider = context.getCameraProvider()
        cameraProvider.unbindAll()
        val cam = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            useCaseGroup
        )
        cam.cameraControl.enableTorch(isTorchOn)
        preview.setSurfaceProvider(previewView.surfaceProvider)
    }


    KeepScreenOn()
    AndroidView({ previewView }) {

    }
}

private class BarcodeImageAnalyzer(val onScan: (Barcode) -> Unit) : ImageAnalysis.Analyzer {

    var currentTimestamp: Long = 0

    @SuppressLint("UnsafeOptInUsageError")
    override fun analyze(imageProxy: ImageProxy) {
        currentTimestamp = System.currentTimeMillis()
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            val result = scanner.process(image).addOnSuccessListener { x ->
                run {
                    for (c in x) {
                        onScan(c)
                    }
                }
            }
            result.addOnCompleteListener {
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1500 - (System.currentTimeMillis() - currentTimestamp))
                    imageProxy.close()
                }
            };
        }
    }
}

suspend fun Context.getCameraProvider(): ProcessCameraProvider = suspendCoroutine { continuation ->
    ProcessCameraProvider.getInstance(this).also { cameraProvider ->
        cameraProvider.addListener({
            continuation.resume(cameraProvider.get())
        }, ContextCompat.getMainExecutor(this))
    }
}

fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}