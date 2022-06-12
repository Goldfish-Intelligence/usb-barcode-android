package de.david_wilkening.companion_scanner

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.platform.LocalContext
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

            Column {
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

@Composable
fun SystemBroadcastReceiver(
    systemAction: String,
    onSystemEvent: (intent: Intent?) -> Unit
) {
    // Grab the current context in this part of the UI tree
    val context = LocalContext.current

    // Safely use the latest onSystemEvent lambda passed to the function
    val currentOnSystemEvent by rememberUpdatedState(onSystemEvent)

    // If either context or systemAction changes, unregister and register again
    DisposableEffect(context, systemAction) {
        val intentFilter = IntentFilter(systemAction)
        val broadcast = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                currentOnSystemEvent(intent)
            }
        }

        context.registerReceiver(broadcast, intentFilter)

        // When the effect leaves the Composition, remove the callback
        onDispose {
            context.unregisterReceiver(broadcast)
        }
    }
}
