package com.loyalstring.rfid.data.remote.api

import ScannedDataToService
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeResponse
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.local.entity.TransferTypeEntity
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
import com.loyalstring.rfid.data.model.login.LoginRequest
import com.loyalstring.rfid.data.model.login.LoginResponse
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.CustomOrderUpdateResponse
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse
import com.loyalstring.rfid.data.remote.data.DeleteOrderRequest
import com.loyalstring.rfid.data.remote.data.DeleteOrderResponse
import com.loyalstring.rfid.data.remote.data.ProductDeleteModelReq
import com.loyalstring.rfid.data.remote.data.ProductDeleteResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferResponse
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.data.EditDataRequest
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface RetrofitInterface {
    /*Login*/
    @POST("api/ClientOnboarding/ClientOnboardingLogin")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    /*get all vendor*/
    @POST("api/ProductMaster/GetAllPartyDetails")
    suspend fun getAllVendorDetails(@Body request: ClientCodeRequest): Response<List<VendorModel>>

    /*get all Counters*/
    @POST("api/ClientOnboarding/GetAllCounters")
    suspend fun getAllCounters(@Body request: ClientCodeRequest): Response<List<CounterModel>>

    /*get all branches*/
    @POST("api/ClientOnboarding/GetAllBranchMaster")
    suspend fun getAllBranches(@Body request: ClientCodeRequest): Response<List<BranchModel>>

    /*get all boxes*/
    @POST("api/ProductMaster/GetAllBoxMaster")
    suspend fun getAllBoxes(@Body request: ClientCodeRequest): Response<List<BoxModel>>


    /*Get all SKU*/
    @POST("api/ProductMaster/GetAllSKU")
    suspend fun getAllSKUDetails(@Body request: ClientCodeRequest): Response<List<SKUModel>>

    /*Get all Category*/
    @POST("api/ProductMaster/GetAllCategory")
    suspend fun getAllCategoryDetails(@Body request: ClientCodeRequest): Response<List<CategoryModel>>

    /*Get all Products*/
    @POST("api/ProductMaster/GetAllProductMaster")
    suspend fun getAllProductDetails(@Body request: ClientCodeRequest): Response<List<ProductModel>>

    /*Get all design*/
    @POST("api/ProductMaster/GetAllDesign")
    suspend fun getAllDesignDetails(@Body request: ClientCodeRequest): Response<List<DesignModel>>

    /*Get all purity*/
    @POST("api/ProductMaster/GetAllPurity")
    suspend fun getAllPurityDetails(@Body request: ClientCodeRequest): Response<List<PurityModel>>

    //Get all stock
    @POST("api/ProductMaster/GetAllLabeledStock")
    suspend fun getAllLabeledStock(@Body request: RequestBody): Response<List<AlllabelResponse.LabelItem>>

    //Get all packets
    @POST("api/ProductMaster/GetAllPacketMaster")
    suspend fun getAllPackets(@Body request: ClientCodeRequest): Response<List<PacketModel>>

    /* insert single stock*/
    @POST("api/ProductMaster/InsertLabelledStock")
    suspend fun insertStock(
        @Body payload: List<InsertProductRequest>
    ): Response<List<PurityModel>>

    //AddScannedDataToWeb
    @POST("api/RFIDDevice/AddRFID")
    suspend fun addAllScannedData(@Body scannedDataToService: List<ScannedDataToService>): Response<List<ScannedDataToService>>

    @Multipart
    @POST("api/ProductMaster/UploadImagesByClientCode ")
    suspend fun uploadLabelStockImage(
        @Part("ClientCode") clientCode: RequestBody,
//        @Part("DesignId") skuId: RequestBody,
        @Part("ItemCode") itemCode: RequestBody,
        @Part files: List<MultipartBody.Part>
    ): Response<ResponseBody>



    //add employee api
    @POST("api/ClientOnboarding/AddCustomer")
    suspend fun addEmployee(
        @Body addEmployeeRequest: AddEmployeeRequest
    ): Response<EmployeeResponse>

    /*Get Emp List*/
    @POST("api/ClientOnboarding/GetAllCustomer") // Replace with your actual API endpoint
    suspend fun getAllEmpList(@Body clientCodeRequest: ClientCodeRequest): Response<List<EmployeeList>>

    //Label list
    @POST("api/ProductMaster/GetAllLabeledStock") // Replace with your actual API endpoint
    suspend fun getAllItemCodeList(@Body clientCodeRequest: ClientCodeRequest): Response<List<ItemCodeResponse>>

    @POST("api/ClientOnboarding/GetAllBranchMaster")
    suspend fun getAllBranchList(@Body clientCodeRequest: ClientCodeRequest): Response<List<BranchModel>>

    @POST("/api/Order/AddCustomOrder")
    suspend fun addOrder(@Body customerOrderRequest: CustomOrderRequest): Response<CustomOrderResponse>

    @POST("/api/ProductMaster/GetStockTransferTypes")
    suspend fun getStockTransferTypes(@Body clientCodeRequest: ClientCodeRequest): Response<List<TransferTypeEntity>>


    @POST("/api/ProductMaster/GetAllRFID")
    suspend fun getAllRFID(@Body request: RequestBody): Response<List<EpcDto>>

    //get last order no
    @POST("api/Order/LastOrderNo")
    suspend fun getLastOrderNo(@Body clientCodeRequest: ClientCodeRequest): Response<LastOrderNoResponse>

    /*get all order list*/

    @POST("api/Order/GetAllOrders")
    suspend fun getAllOrderList(@Body clientCodeRequest: ClientCodeRequest): Response<List<CustomOrderResponse>>

    @POST("api/Order/DeleteCustomOrder")
    suspend fun deleteCustomerOrder(

        @Body request: DeleteOrderRequest
    ): Response<DeleteOrderResponse>



    @POST("/api/ProductMaster/AddStockTransfer")
    suspend fun postStockTransfer(
        @Body request: StockTransferRequest
    ): Response<StockTransferResponse>


    /*delete product api*/
    @POST("/api/ProductMaster/DeleteLabeledStock")
    suspend fun deleteProduct(
        @Body request: List<ProductDeleteModelReq>
    ): Response<List<ProductDeleteResponse>>



    /* update single stock*/
    @POST("api/ProductMaster/UpdateLabeledStock")
    suspend fun updateStock(
        @Body payload: List<EditDataRequest>
    ): Response<List<PurityModel>>

    /*update customer order*/
    @POST("api/Order/UpdateCustomOrder")
    suspend fun updateCustomerOrder(@Body customerOrderRequest: CustomOrderRequest): Response<CustomOrderUpdateResponse>

    /*daily rate*/

    @POST("/api/ProductMaster/GetAllDailyRate")
    suspend fun getDailyDailyRate(@Body request: ClientCodeRequest): Response<List<DailyRateResponse>>


}