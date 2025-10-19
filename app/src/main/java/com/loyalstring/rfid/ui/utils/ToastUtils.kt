package com.loyalstring.rfid.ui.utils

import android.content.Context
import android.widget.Toast

object ToastUtils {
    fun showToast(context: Context, message: String, duration: Int = Toast.LENGTH_SHORT) {
        if (message.isNotBlank()) {
            Toast.makeText(context.applicationContext, message, duration).show()
        }
    }
}