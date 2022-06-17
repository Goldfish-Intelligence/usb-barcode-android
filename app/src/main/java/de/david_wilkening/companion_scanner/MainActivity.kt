package de.david_wilkening.companion_scanner

import android.content.Intent
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private val TAG = MainActivity::class.qualifiedName

    override fun onResume() {
        super.onResume()

        val viewModel: MainViewModel by viewModels()

        intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.also { it ->
            Log.d(TAG, "Received USB intent $it")
            viewModel.openAccessory(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by viewModels()

        setContent {
            val cameraPermissionState = rememberPermissionState(
                android.Manifest.permission.CAMERA
            )

            Column(Modifier.background(Color.Black).fillMaxSize()) {
                when (cameraPermissionState.status) {
                    is PermissionStatus.Denied -> {
                        Text("Hard to read barcodes if you are blind.\nGimme access human.", color = Color.White)
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Request permission", color = Color.White)
                        }
                    }
                    PermissionStatus.Granted -> {
                        val usbError = viewModel.getUsbErrorText()
                        when (usbError.value) {
                            null -> {
                                SystemBroadcastReceiver(UsbManager.ACTION_USB_ACCESSORY_DETACHED) { intent ->
                                    viewModel.disconnectAccessory()
                                }
                                Box {
                                    val isCooldown = viewModel.getInCooldown()
                                    if (isCooldown.value) {
                                        val brush = Brush.horizontalGradient(listOf(Color.Green, Color.Green), 0f, 0f, TileMode.Clamp)
                                        Box(modifier = Modifier
                                            .background(brush, alpha = 0.4f)
                                            .alpha(0.4f)
                                            .zIndex(1f)
                                            .fillMaxSize())
                                    }
                                    BarcodeScanner(onScan = { x -> viewModel.sendBarcode(x) }, isTorchOn = viewModel.getIsTorchOn().value)
                                }
                                Button(onClick = { viewModel.toggleTorch() }) {
                                    Text("Licht an/aus")
                                }
                            }
                            else -> {
                                Text("Bitte USB (neu) verbinden", color = Color.White)
                                Text(usbError.value!!, color = Color.White)
                                //Button(onClick = { viewModel.openByEnumerate() }) {
                                //    Text("(Re)try USB connect")
                                //}
                            }
                        }
                    }
                }
            }
        }
    }

}