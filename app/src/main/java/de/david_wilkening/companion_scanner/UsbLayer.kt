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
            val outBytes = out.toByteArray()
            val lenHeader = ByteArray(2)
            lenHeader[0] = outBytes.size.and(0xff00).shr(8).toByte()
            lenHeader[1] = outBytes.size.and(0x00ff).toByte()

            outStream?.write(lenHeader)
            outStream?.write(outBytes)
            outStream?.flush()
        }
    }
}