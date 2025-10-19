package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.BoxModel
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.addSingleItem.CategoryModel
import com.loyalstring.rfid.data.model.addSingleItem.CounterModel
import com.loyalstring.rfid.data.model.addSingleItem.DesignModel
import com.loyalstring.rfid.data.model.addSingleItem.InsertProductRequest
import com.loyalstring.rfid.data.model.addSingleItem.PacketModel
import com.loyalstring.rfid.data.model.addSingleItem.ProductModel
import com.loyalstring.rfid.data.model.addSingleItem.PurityModel
import com.loyalstring.rfid.data.model.addSingleItem.SKUModel
import com.loyalstring.rfid.data.model.addSingleItem.VendorModel
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.EditDataRequest
import com.loyalstring.rfid.data.remote.data.ProductDeleteModelReq
import com.loyalstring.rfid.data.remote.data.ProductDeleteResponse
import retrofit2.Response
import javax.inject.Inject

class SingleProductRepository @Inject constructor(
    private val apiService: RetrofitInterface
) {
    suspend fun getAllVendorDetails(request: ClientCodeRequest): Response<List<VendorModel>> {
        return apiService.getAllVendorDetails(request)
    }

    suspend fun getAllSKUDetails(request: ClientCodeRequest): Response<List<SKUModel>> {
        return apiService.getAllSKUDetails(request)
    }

    suspend fun getAllCategoryDetails(request: ClientCodeRequest): Response<List<CategoryModel>> {
        return apiService.getAllCategoryDetails(request)
    }

    suspend fun getAllProductDetails(request: ClientCodeRequest): Response<List<ProductModel>> {
        return apiService.getAllProductDetails(request)
    }

    suspend fun getAllDesignDetails(request: ClientCodeRequest): Response<List<DesignModel>> {
        return apiService.getAllDesignDetails(request)
    }

    suspend fun getAllPurityDetails(request: ClientCodeRequest): Response<List<PurityModel>> {
        return apiService.getAllPurityDetails(request)
    }
    suspend fun getAllCounters(request: ClientCodeRequest): Response<List<CounterModel>> {
        return apiService.getAllCounters(request)
    }

    suspend fun getAllBranches(request: ClientCodeRequest): Response<List<BranchModel>> {
        return apiService.getAllBranches(request)
    }

    suspend fun getAllBoxes(request: ClientCodeRequest): Response<List<BoxModel>> {
        return apiService.getAllBoxes(request)
    }

    suspend fun getAllExhibitions(request: ClientCodeRequest): Response<List<BranchModel>> {
        return apiService.getAllBranches(request)
    }
    suspend fun getAllPackets(request: ClientCodeRequest): Response<List<PacketModel>> {
        return apiService.getAllPackets(request)
    }


    suspend fun deleteProduct(request: List<ProductDeleteModelReq>): Response<List<ProductDeleteResponse>> {
        return apiService.deleteProduct(request)
    }




    suspend fun insertLabelledStock(request: InsertProductRequest): Result<List<PurityModel>> {
        return try {
            val payload = listOf(request) // not a single object
            val response = apiService.insertStock(payload)
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateLabelledStock(request: List<EditDataRequest>): Result<List<PurityModel>> {
        return try {
            val response = apiService.updateStock(request) // 👈 no listOf
            if (response.isSuccessful) {
                Result.success(response.body() ?: emptyList())
            } else {
                Result.failure(Exception("Failed: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}