package de.david_wilkening.companion_scanner

import android.hardware.usb.UsbAccessory
import android.hardware.usb.UsbManager
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.lang.Exception
import java.nio.charset.Charset

class UsbLayer (private val usbManager: UsbManager) {
    // if ParcelFileDescriptor is not saved in var with the same lifetime as FileOutputStream, the
    // underlying file descriptor is garbage collected beforehand
    private var parcelFD: ParcelFileDescriptor? = null
    private var outStream: FileOutputStream? = null

    suspend fun openDevice(usbAccessory: UsbAccessory) {
        return withContext(Dispatchers.IO) {
            parcelFD = usbManager.openAccessory(usbAccessory)!!
            outStream = FileOutputStream(parcelFD!!.fileDescriptor)
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