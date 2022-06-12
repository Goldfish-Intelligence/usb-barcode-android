package de.david_wilkening.companion_scanner

import android.content.Intent
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.zIndex
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.rememberPermissionState

@OptIn(ExperimentalPermissionsApi::class)
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val viewModel: TestViewModel by viewModels()
        setContent {
            val cameraPermissionState = rememberPermissionState(
                android.Manifest.permission.CAMERA
            )

            Column(Modifier.background(Color.White)) {
                Text("This only the testing mode. Please connect USB for production use.", modifier = Modifier.background(
                    Color.Yellow))
                when (cameraPermissionState.status) {
                    is PermissionStatus.Denied -> {
                        Text("Hard to read barcodes if you are blind.\nGimme access human.")
                        Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                            Text("Request permission")
                        }
                    }
                    PermissionStatus.Granted -> {
                        Box {
                            val isCooldown = viewModel.getInCooldown()
                            if (isCooldown.value) {
                                val brush = Brush.horizontalGradient(listOf(Color.Green, Color.Green), 0f, 0f, TileMode.Clamp)
                                Box(modifier = Modifier
                                    .background(brush, alpha=0.4f)
                                    .alpha(0.4f)
                                    .zIndex(1f)
                                    .fillMaxWidth()
                                    .height(Dp(150f)))
                            }
                            BarcodeScanner(onScan = { x -> viewModel.sendBarcode(x) }, isTorchOn = viewModel.getIsTorchOn().value)
                        }
                        Button(onClick = { viewModel.toggleTorch() }) {
                            Text("Licht an/aus")
                        }
                        SystemBroadcastReceiver(Intent.ACTION_BATTERY_CHANGED) { batteryStatus ->
                            viewModel.setBatteryTemp(batteryStatus?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0)!!)
                        }
                        Text(viewModel.getTemperatureString().value)
                        Text(viewModel.getDebugText().value)
                    }
                }
            }
        }
    }
}