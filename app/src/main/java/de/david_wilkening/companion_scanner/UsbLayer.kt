package de.david_wilkening.companion_scanner

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.nio.charset.Charset

class UsbLayer (private val usbManager: UsbManager) {
    private var outStream: FileOutputStream? = null

    suspend fun openDevice(usbAccessory: UsbAccessory) {
        return withContext(Dispatchers.IO) {
            val usbFd = usbManager.openAccessory(usbAccessory)!!
            outStream = FileOutputStream(usbFd.fileDescriptor)
        }
    }

    suspend fun send(out: String) {
        return withContext(Dispatchers.IO) {
            outStream?.write(out.toByteArray())
            outStream?.write("\n".toByteArray())
            outStream?.flush()
        }
    }
}