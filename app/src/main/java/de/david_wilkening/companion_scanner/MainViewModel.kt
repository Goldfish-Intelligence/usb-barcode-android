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

class MainViewModel(application: Application) : AndroidViewModel(
    application
) {
    private val TAG = MainActivity::class.qualifiedName

    private var usbManager = getApplication<Application>().getSystemService(Context.USB_SERVICE) as UsbManager
    private val usbLayer = UsbLayer(usbManager)

    private val usbErrorText = mutableStateOf<String?>("Not connected to USB")
    private val inCooldown = mutableStateOf(false)
    private val isTorchOn = mutableStateOf(false)

    fun getUsbErrorText(): State<String?> {
        return usbErrorText
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

    fun openAccessory(usbAccessory: UsbAccessory) {
        Log.d(TAG, "Opening USB accessory ... $usbAccessory")
        viewModelScope.launch {
            try {
                usbLayer.openDevice(usbAccessory)
                usbErrorText.value = null
            }
            catch (e: NullPointerException) {
                Log.e(TAG, "Failed Opening USB accessory ... $e")
                usbErrorText.value = "Unknown error opening USB communication"
            }
        }
    }

    fun openByEnumerate() {
        val accessoryList = usbManager.accessoryList
        when (val accessory = accessoryList?.first()) {
            null -> usbErrorText.value = "No USB Device found"
            else -> openAccessory(accessory)
        }
    }

    fun sendBarcode(barcode: Barcode) {
        if (inCooldown.value) {
            return
        }

        object : CountDownTimer(1500, 1500) {
            override fun onTick(p0: Long) {
                return
            }

            override fun onFinish() {
                inCooldown.value = false
            }
        }.start()

        val payload = JSONObject()
        barcode.rawValue?.also { it ->
            payload.put("rawUTF8", it)
        }
        barcode.rawBytes?.also { it ->
            val encoded = Base64.encodeToString(it, Base64.DEFAULT)
            payload.put("rawBase64", encoded)
        }

        viewModelScope.launch() {
            try {
                usbLayer.send(payload.toString())
            } catch (e: IOException) {
                // TODO: detect connection loss as event
                usbErrorText.value = "Error in USB communication"
            }
        }
    }
}