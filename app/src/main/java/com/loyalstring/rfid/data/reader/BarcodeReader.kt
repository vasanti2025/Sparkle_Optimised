package com.loyalstring.rfid.data.reader

import android.content.Context
import android.util.Log
import com.rscja.barcode.BarcodeDecoder
import com.rscja.barcode.BarcodeFactory
import com.rscja.barcode.BarcodeUtility
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
@Singleton
class BarcodeReader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val _barcodeDecoder: BarcodeDecoder = BarcodeFactory.getInstance().barcodeDecoder

    val barcodeDecoder: BarcodeDecoder
        get() = _barcodeDecoder

    private var isOpened = false

    fun openIfNeeded() {
        if (!isOpened) {
            try {
                _barcodeDecoder.open(context)
                isOpened = true
            } catch (e: SecurityException) {
                e.printStackTrace()
            }
        }
    }

    fun setOnBarcodeScanned(callback: (String) -> Unit) {
        barcodeDecoder.setDecodeCallback { entity ->
            if (entity.resultCode == BarcodeDecoder.DECODE_SUCCESS) {
                val data = entity.barcodeData
                Log.d("BarcodeReader", "Scan success: $data")
                BarcodeUtility.getInstance().enablePlaySuccessSound(context, true)
                callback(data)
            } else {
                Log.e("BarcodeReader", "Scan failed with code: ${entity.resultCode}")
            }
        }
    }

    fun close() {
        if (isOpened) {
            _barcodeDecoder.close()
            isOpened = false
        }
    }
}

