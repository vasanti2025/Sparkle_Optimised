package com.loyalstring.rfid.data.remote.api

import ScannedDataToService
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeResponse
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.local.entity.FaceInfo
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
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.AddDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoRequest
import com.loyalstring.rfid.data.model.deliveryChallan.ChallanNoResponse
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchRequest
import com.loyalstring.rfid.data.model.deliveryChallan.CustomerTunchResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanRequest
import com.loyalstring.rfid.data.model.deliveryChallan.DeleteDeliveryChallanResponse
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanRequestList
import com.loyalstring.rfid.data.model.deliveryChallan.DeliveryChallanResponseList
import com.loyalstring.rfid.data.model.deliveryChallan.UpdateDeliveryChallanRequest
import com.loyalstring.rfid.data.model.face.AllFaceResponse
import com.loyalstring.rfid.data.model.face.FaceRequest
import com.loyalstring.rfid.data.model.face.FaceResponse
import com.loyalstring.rfid.data.model.login.LoginRequest
import com.loyalstring.rfid.data.model.login.LoginResponse
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.CustomOrderUpdateResponse
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse
import com.loyalstring.rfid.data.model.order.OrderSearchRequest
import com.loyalstring.rfid.data.model.box.BoxRfidDetailsResponse
import com.loyalstring.rfid.data.model.box.BoxRfidSearchRequest
import com.loyalstring.rfid.data.model.quotation.AddQuotationRequest
import com.loyalstring.rfid.data.model.quotation.LastQuotationNoResponse
import com.loyalstring.rfid.data.model.quotation.QuotationListRequest
import com.loyalstring.rfid.data.model.quotation.QuotationListResponse
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationRequest
import com.loyalstring.rfid.data.model.quotation.UpdateQuotationResponse
import com.loyalstring.rfid.data.model.report.BatchDetailsResponse
import com.loyalstring.rfid.data.model.report.ScanBatchRequest
import com.loyalstring.rfid.data.model.report.SessionItem
import com.loyalstring.rfid.data.model.report.SessionListResponse
import com.loyalstring.rfid.data.model.report.StockVerificationReqReport
import com.loyalstring.rfid.data.model.report.StockVerificationResponseReport
import com.loyalstring.rfid.data.model.sampleIn.SampleInResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutAddResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutLastNoReq
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListRequest
import com.loyalstring.rfid.data.model.sampleOut.SampleOutListResponse
import com.loyalstring.rfid.data.model.sampleOut.SampleOutUpdateRequest
import com.loyalstring.rfid.data.model.setting.LocationGetRequest
import com.loyalstring.rfid.data.model.setting.LocationItem
import com.loyalstring.rfid.data.model.setting.LocationSyncRequest
import com.loyalstring.rfid.data.model.setting.LocationSyncResponse
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesReq
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesResponse
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransfer
import com.loyalstring.rfid.data.model.stockTransfer.CancelStockTransferResponse
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectRequest
import com.loyalstring.rfid.data.model.stockTransfer.STApproveRejectResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockInOutRequest
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferInOutResponse
import com.loyalstring.rfid.data.remote.data.DeleteOrderRequest
import com.loyalstring.rfid.data.remote.data.DeleteOrderResponse
import com.loyalstring.rfid.data.remote.data.ProductDeleteModelReq
import com.loyalstring.rfid.data.remote.data.ProductDeleteResponse
import com.loyalstring.rfid.data.model.stockTransfer.StockTransferResponse
import com.loyalstring.rfid.data.model.stockVerification.ScanSessionResponse
import com.loyalstring.rfid.data.model.stockVerification.StockVerificationRequestData
import com.loyalstring.rfid.data.remote.data.ClearStockDataModelReq
import com.loyalstring.rfid.data.remote.data.CompanyDetails
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.data.DeviceIdData
import com.loyalstring.rfid.data.remote.data.DeviceIdResponseData
import com.loyalstring.rfid.data.remote.data.EditDataRequest
import com.loyalstring.rfid.data.remote.data.RfidScanToDesktopResponse
import com.loyalstring.rfid.data.remote.data.StockTransferRequest
import com.loyalstring.rfid.data.remote.data.UserPermissionRequest
import com.loyalstring.rfid.data.remote.data.UserPermissionResponse
import com.loyalstring.rfid.data.remote.response.AlllabelResponse
import com.loyalstring.rfid.data.remote.response.BranchLabelledStocksResponse
import com.loyalstring.rfid.data.remote.response.ClearStockDataModelResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Streaming

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

   /* //Get all stock
    @POST("api/ProductMaster/GetAllStockAndroid")
    suspend fun getAllLabeledStock(@Body request: RequestBody): Response<List<AlllabelResponse.LabelItem>>

    @POST("api/ProductMaster/GetAllStockAndroid")
    @Streaming
    suspend fun getAllLabeledStockNew(
        @Body request: RequestBody
    ): Response<ResponseBody>*/


    @POST("api/ProductMaster/branch-labelled-stocks/search")
    suspend fun getAllLabeledStock(
        @Body request: RequestBody
    ): Response<BranchLabelledStocksResponse>

    @POST("api/ProductMaster/branch-labelled-stocks/search")
    @Streaming
    suspend fun getAllLabeledStockNew(
        @Body request: RequestBody
    ): Response<ResponseBody>



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
    @POST("api/ProductMaster/GetAllStockAndroid") // Replace with your actual API endpoint
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

    @POST("api/Order/GetAllOrders")
    suspend fun searchOrdersByRfid(@Body request: OrderSearchRequest): Response<List<CustomOrderResponse>>

    @POST("api/BoxRfid/GetDetailsByRfidCode")
    suspend fun getBoxDetailsByRfidCode(
        @Body request: BoxRfidSearchRequest
    ): Response<BoxRfidDetailsResponse>

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
    ): Response<ProductDeleteResponse>



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


    /*daily rate*/

    @POST("/api/ProductMaster/GetAllDailyRate")
    suspend fun getDailyRate(@Body request: ClientCodeRequest): Response<List<DailyRateResponse>>

    /*update Daily rates*/
    @POST("/api/ProductMaster/UpdateDailyRates")
    suspend fun updateDailyRate(@Body request: List<UpdateDailyRatesReq>): Response<List<UpdateDailyRatesResponse>>

    /*update Daily rates*/
    @POST("/api/ClientOnboarding/AddClientLocation")
    suspend fun addLocation(@Body request: LocationSyncRequest): Response<LocationSyncResponse>

    @POST("/api/ClientOnboarding/GetClientLocations")
    suspend fun getLocation(@Body request: LocationGetRequest): Response<List<LocationItem>>

    @POST("/api/ProductMaster/GetAllStockTransfers")
    suspend fun getAllStockTransfer(@Body request: StockInOutRequest): Response<List<StockTransferInOutResponse>>

    @POST("/api/ProductMaster/ApproveStockTransfer")
    suspend fun approveStockTransfer(@Body request: STApproveRejectRequest): Response<STApproveRejectResponse>

    @POST("/api/ProductMaster/CancelStockTransferMasterDetails")
    suspend fun cancelStockTransferDetails(@Body request: CancelStockTransfer): Response<CancelStockTransferResponse>

    @POST("/api/RoleManagement/GetAllUserPermissions-Optimized")
    suspend fun getAllUserPermissions(@Body request: UserPermissionRequest): Response<List<UserPermissionResponse>>


    @POST("/api/RoleManagement/GetAllUserPermissions-Optimized")
    suspend fun getAllUserPermissionsAll(@Body request: ClientCodeRequest): Response<List<UserPermissionResponse>>

    @POST("/api/Invoice/GetAllDeliveryChallan")
    suspend fun getAllChallanList(@Body request: DeliveryChallanRequestList): Response<List<DeliveryChallanResponseList>>

    @POST("/api/Invoice/GetLastChallanNo")
    suspend fun getLastChallanNo(@Body request: ChallanNoRequest): Response<ChallanNoResponse>

    @POST("/api/Invoice/AddDeliveryChallan")
    suspend fun addDeliveryChallan(@Body request: AddDeliveryChallanRequest): Response<AddDeliveryChallanResponse>

    @POST("/api/Invoice/UpdateDeliveryChallan")
    suspend fun updateDeliveryChallan(@Body request: UpdateDeliveryChallanRequest): Response<AddDeliveryChallanResponse>

    @POST("/api/Invoice/DeleteDeliveryChallan")
    suspend fun deleteDeliveryChallan(@Body request: DeleteDeliveryChallanRequest): Response<DeleteDeliveryChallanResponse>

    @POST("/api/Invoice/GetAllCustomerTounch")
    suspend fun getAllCustomerTounch(@Body request: CustomerTunchRequest): Response<List<CustomerTunchResponse>>

    @POST("api/Transaction/GetAllCustomerIssue")
    suspend fun getAllSampleOut(@Body request: SampleOutListRequest): Response<List<SampleOutListResponse>>

    @POST("api/Transaction/AddCustomerIssue")
    suspend fun addSampleOut(@Body request: SampleOutAddRequest): Response<SampleOutAddResponse>

    @POST("api/Transaction/GetCustLastSampleOutNo")
    suspend fun lastSampleOutNo(@Body request: SampleOutLastNoReq): String

    @POST("api/Transaction/UpdateCustomerIssue")
    suspend fun updateSampleOut(@Body request: SampleOutUpdateRequest): Response<SampleOutAddResponse>

    @POST("api/Transaction/GetAllIssueItemDetails")
    suspend fun getAllSampleIn(@Body request: SampleOutListRequest): Response<List<SampleInResponse>>

    @POST("api/ProductMaster/AddStockVerificationBySession")
    suspend  fun stockVarificationNew(@Body stockVerificationRequestData: StockVerificationRequestData): Response<ScanSessionResponse>

    @POST("api/ProductMaster/AddStockVerificationBySession")
    suspend fun uploadStock(@Body body: RequestBody): Response<ScanSessionResponse>

    @Multipart
    @POST("api/ProductMaster/AddStockVerificationWithBatchFile")
    suspend fun uploadStockVerificationFile(
        @Part("ClientCode") clientCode: RequestBody,
        @Part jsonFile: MultipartBody.Part
    ): ScanSessionResponse


    @POST("/api/RFIDDevice/DeleteRFIDByClientAndDevice")
    suspend fun clearStockData(@Body req: ClearStockDataModelReq): Response<ClearStockDataModelResponse>

    @POST("api/Order/GetAllQuotation")
    suspend fun getAllQuotation(@Body req: QuotationListRequest): Response<List<QuotationListResponse>>

    @POST("api/Order/AddQuotation")
    suspend fun saveQuotation(@Body req: AddQuotationRequest): Response<QuotationListResponse>

    @POST("api/Order/LastQuotationNo")
    suspend fun getLastQuotationNo(@Body req: ClientCodeRequest): Response<LastQuotationNoResponse>

    @POST("api/Order/UpadateQuotation")
    suspend fun updateQuotation(@Body req: UpdateQuotationRequest): Response<UpdateQuotationResponse>
/*
    @POST("api/Order/UpadateQuotation")
    suspend fun getDeliveryChallanById(clientCode: String, challanId: Int) :Response<UpdateQuotationResponse>*/


    @POST("api/ProductMaster/GetConsolidationStockVerificationReport")
    suspend fun stockVerificationReport(@Body req: StockVerificationReqReport): Response<StockVerificationResponseReport>


    @POST("api/ProductMaster/GetAllStockVerificationBySession")
    suspend fun stockVerificationBatchwise(@Body req: ClientCodeRequest): Response<SessionListResponse>

    @POST("api/ProductMaster/GetAllStockVerificationBySession")
    suspend fun stockVerificationBatchwiseItem(@Body req: ScanBatchRequest): Response<BatchDetailsResponse>

    @POST("api/ClientOnboarding/GetAllCompanyDetails")
    suspend fun getCompanyDetails(@Body req: ClientCodeRequest): Response<List<CompanyDetails>>


    @POST("api/FaceLogin/AddFace")
    suspend fun saveFace(@Body request: FaceInfo): Response<FaceResponse>

    @POST("api/FaceLogin/GetAllFaceLogin")
    suspend fun getAllFaceData(@Body request: FaceRequest): Response<AllFaceResponse>

    @POST("api/RFIDDevice/GetAllRFIDDetails")
    suspend fun getAllScantoDesktop(@Body request: ClientCodeRequest): Response<RfidScanToDesktopResponse>

    @POST("api/ClientOnboarding/UpdateMultipleUser")
    suspend fun addDeviceId(@Body request: List<DeviceIdData>): Response<DeviceIdResponseData>

   /* @POST("api/ProductMaster/GetAllRFIDDetails")
    suspend fun getDeviceId(@Body request: ClientCodeRequest): Response<DeviceIdData>
*/


}