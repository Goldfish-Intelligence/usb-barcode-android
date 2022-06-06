package de.david_wilkening.companion_scanner

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
import androidx.compose.ui.graphics.Color
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
class MainActivity : ComponentActivity() {
    private val TAG = MainActivity::class.qualifiedName

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val viewModel: MainViewModel by viewModels()

        intent.getParcelableExtra<UsbAccessory>(UsbManager.EXTRA_ACCESSORY)?.also { it ->
            Log.d(TAG, "Received USB intent $it")
            viewModel.openAccessory(it)
        }

        setContent {
            val cameraPermissionState = rememberPermissionState(
                android.Manifest.permission.CAMERA
            )

            Column {
                when (cameraPermissionState.status) {
                    is PermissionStatus.Denied -> {
                        Text("Hard to read barcodes if you are blind.\nGimme access human.")
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Request permission")
                        }
                    }
                    PermissionStatus.Granted -> {
                        val usbError = viewModel.getUsbErrorText()
                        when (usbError.value) {
                            null -> {
                                Box {
                                    val isCooldown = viewModel.getInCooldown()
                                    if (isCooldown.value) {
                                        Box(modifier = Modifier
                                            .background(Color.Green)
                                            .alpha(0.4f)
                                            .fillMaxSize())
                                    }
                                    BarcodeScanner(onScan = { x -> viewModel.sendBarcode(x) }, isTorchOn = viewModel.getIsTorchOn().value)
                                }
                                Button(onClick = { viewModel.toggleTorch() }) {
                                    Text("Licht an/aus")
                                }
                            }
                            else -> {
                                Text(usbError.value!!)
                                Button(onClick = { viewModel.openByEnumerate() }) {
                                    Text("(Re)try USB connect")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

}