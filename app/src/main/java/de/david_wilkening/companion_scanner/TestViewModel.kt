package de.david_wilkening.companion_scanner

import android.app.Application
import android.content.Context
import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.CountDownTimer
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.IOException
import java.time.Duration
import java.time.Instant

class TestViewModel(application: Application) : AndroidViewModel(
    application
) {
    private val inCooldown = mutableStateOf(false)
    private val isTorchOn = mutableStateOf(false)
    private val debugText = mutableStateOf<String>("Debug info here")
    private val temperatureString = mutableStateOf<String>("Battery temperature:")
    private var lastScan = Instant.now()

    fun getDebugText(): State<String> {
        return debugText
    }

    fun getTemperatureString(): State<String> {
        return temperatureString
    }

    fun getInCooldown(): State<Boolean> {
        return inCooldown
    }

    fun getIsTorchOn(): State<Boolean> {
        return isTorchOn
    }

    fun toggleTorch() {
        isTorchOn.value = !isTorchOn.value
    }

    fun sendBarcode(barcode: Barcode) {
        if (inCooldown.value) {
            return
        }
        inCooldown.value = true

        object : CountDownTimer(1200, 1200) {
            override fun onTick(p0: Long) {
                return
            }

            override fun onFinish() {
                inCooldown.value = false
            }
        }.start()

        val now = Instant.now()
        val elapsed = Duration.between(lastScan, now)
        lastScan = now

        debugText.value = "Last scan millis: ${elapsed.toMillis()-1200} (minus cooldown: 1200)\nScan result:${barcode.displayValue}"
    }

    fun setBatteryTemp(temp: Int) {
        temperatureString.value = "Battery temperature: ${temp / 10f}"
    }
}