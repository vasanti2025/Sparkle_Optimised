package com.loyalstring.rfid.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditProductViewModel @Inject constructor(
    private val retrofitInterface: RetrofitInterface,
    private val dao: BulkItemDao
) : ViewModel() {

    val uploadState = mutableStateOf(UploadState.Idle)
    val errorMessage = mutableStateOf<String?>(null)

    fun uploadImage(
        clientCode: String,
        itemCode: String,
        imageFile: File
    ) {
        viewModelScope.launch {
            uploadState.value = UploadState.Uploading
            errorMessage.value = null
            try {
                val clientCodePart = clientCode.toRequestBody("text/plain".toMediaTypeOrNull())
                val itemCodePart = itemCode.toRequestBody("text/plain".toMediaTypeOrNull())

                val requestFile = imageFile.asRequestBody("image/*".toMediaTypeOrNull())
                val multipartBody = MultipartBody.Part.createFormData(
                    name = "File",
                    filename = imageFile.name,
                    body = requestFile
                )

                val response = retrofitInterface.uploadLabelStockImage(
                    clientCodePart,
                    itemCodePart,
                    listOf(multipartBody)
                )

                if (response.isSuccessful) {
                    val bodyString = response.body()?.string()  // Convert ResponseBody to String
                    if (!bodyString.isNullOrEmpty()) {
                        val jsonobj = JSONObject(bodyString)
                        val imagePath = jsonobj.getString("images")
                        dao.updateImageUrl(itemCode, imagePath)
                        uploadState.value = UploadState.Success
                    } else {
                        uploadState.value = UploadState.Failed
                        errorMessage.value = "Empty response body"
                    }
                } else {
                    uploadState.value = UploadState.Failed
                    errorMessage.value = response.message()
                }

            } catch (e: Exception) {
                uploadState.value = UploadState.Error
                errorMessage.value = e.localizedMessage
            }
        }
    }
}

enum class UploadState {
    Idle, Uploading, Success, Failed, Error
}