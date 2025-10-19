package com.loyalstring.rfid.viewmodel

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.loyalstring.rfid.ui.utils.UserPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences
) : ViewModel() {

    var sheetUrl by mutableStateOf(userPreferences.getSheetUrl().orEmpty())
        private set

    fun updateSheetUrl(newUrl: String) {
        sheetUrl = newUrl
        userPreferences.saveSheetUrl(newUrl)
        Log.e("SHEET ID", userPreferences.getSheetUrl().toString())
    }
}
