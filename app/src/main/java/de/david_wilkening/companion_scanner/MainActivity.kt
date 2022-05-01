package de.david_wilkening.companion_scanner

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.Text
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

            when (cameraPermissionState.status) {
                is PermissionStatus.Denied -> {
                    Column {
                        Text("Hard to read barcodes if you are blind.\nGimme access human.")
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Request permission")
                        }
                    }
                }
                PermissionStatus.Granted -> {
                    val usbError = viewModel.getUsbErrorText()
                    when (usbError.value) {
                        null -> BarcodeScanner(onScan = { x -> viewModel.sendBarcode(x) })
                        else -> Column {
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