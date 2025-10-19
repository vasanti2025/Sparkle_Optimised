package com.loyalstring.rfid.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sparklepos.models.loginclasses.customerBill.AddEmployeeRequest
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeResponse
import com.google.gson.Gson
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.PurityModel
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.CustomOrderUpdateResponse
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<out T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}

@HiltViewModel
class OrderViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: OrderRepository // or whatever your dependency is
) : ViewModel() {
    private val _addEmpResponse = MutableLiveData<Resource<EmployeeResponse>>()
    val addEmpReposnes: LiveData<Resource<EmployeeResponse>> = _addEmpResponse

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading


    /* private val _empListResponse = MutableLiveData<List<EmployeeList>>()
    val empListResponse: LiveData<List<EmployeeList>> = _empListResponse
    val empListFlow = _empListResponse.asStateFlow()*/

    private val _empListFlow = MutableStateFlow<UiState<List<EmployeeList>>>(UiState.Loading)
    val empListFlow: StateFlow<UiState<List<EmployeeList>>> = _empListFlow
    val isEmpListLoading = MutableStateFlow(false)


    private val _itemCodeResponse = MutableStateFlow<List<ItemCodeResponse>>(emptyList())
    val itemCodeResponse: StateFlow<List<ItemCodeResponse>> = _itemCodeResponse
    val isItemCodeLoading = MutableStateFlow(false)


    private val _lastOrderNOResponse = MutableStateFlow(LastOrderNoResponse())
    val lastOrderNoresponse: StateFlow<LastOrderNoResponse> = _lastOrderNOResponse

    private val _orderResponse = MutableStateFlow<CustomOrderResponse?>(null)
    val orderResponse: StateFlow<CustomOrderResponse?> = _orderResponse

    private val _orderUpdateResponse = MutableStateFlow<CustomOrderUpdateResponse?>(null)
    val orderUpdateResponse: StateFlow<CustomOrderUpdateResponse?> = _orderUpdateResponse

    private val _allOrderItems = MutableStateFlow<List<OrderItem>>(emptyList())
    val allOrderItems: StateFlow<List<OrderItem>> = _allOrderItems


    private val _insertOrderOffline = MutableStateFlow<CustomOrderRequest?>(null)
    val insertOrderOffline: StateFlow<CustomOrderRequest?> = _insertOrderOffline


    private val _getAllOrderList = MutableStateFlow<List<CustomOrderResponse>>(emptyList())
    val getAllOrderList: StateFlow<List<CustomOrderResponse>> = _getAllOrderList

    private val _getAllDailyRate = MutableStateFlow<List<DailyRateResponse>>(emptyList())
    val getAllDailyRate: StateFlow<List<DailyRateResponse>> = _getAllDailyRate

//    private val _orderPlaced = mutableStateOf(false)
//    val orderPlaced: State<Boolean> = _orderPlaced

    fun setOrderResponse(response: CustomOrderResponse) {
        _orderResponse.value = response
    }

    suspend fun clearOrderItems() {
        repository.clearOrderItems()
        _allOrderItems.value = emptyList()
    }

    fun clearOrderRequest() {
        _insertOrderOffline.value = null
    }

    fun clearOrderResponse() {
        _orderResponse.value = null
    }


//
//    fun placeOrder(request: ClientCodeRequest, order: CustomOrderRequest) {
//        viewModelScope.launch {
//            val response = repository.submitOrder(request, order)
//            _orderPlaced.value = response.isSuccessful
//        }
//    }
//
//    fun resetOrderPlaced() {
//        _orderPlaced.value = false
//    }

    /*add employee*/
    fun addEmployee(request: AddEmployeeRequest) {
        viewModelScope.launch {
            _addEmpResponse.value = Resource.Loading()
            try {
                val gson = Gson()
                val json = gson.toJson(request)  // Convert to JSON string
                Log.d("AddEmployeeRequestJSON", json)
                val response = repository.AAddAllEmployeeDetails(request)
                if (response.isSuccessful) {
                    val body = response.body()
                    Log.d("orderViewModel", "Response Body: $body")
                    if (body != null) {
                        _addEmpResponse.value = Resource.Success(body)
                    } else {
                        _addEmpResponse.value = Resource.Error("Invalid response data")
                    }
                } else {
                    Log.d("orderViewModel", "Response Error: ${response.errorBody()?.string()}")
                    _addEmpResponse.value = Resource.Error("Server error: ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("orderViewModel", "Exception: ${e.message}")
                _addEmpResponse.value = Resource.Error("Exception: ${e.message}")
            }
        }
    }


    fun setLocalOrderList(data: List<CustomOrderResponse>) {
        _getAllOrderList.value = data
    }

    fun removeOrderById(orderId: Int) {
        _getAllOrderList.value = _getAllOrderList.value.filterNot { it.CustomOrderId == orderId }
    }


    /*emp list function*/

    /*  fun getAllEmpList(clientCode: String) {
          viewModelScope.launch {
             // delay(1000)
              isEmpListLoading.value = true

              try {
                  val response = repository.getAllEmpList(ClientCodeRequest(clientCode)) // API call

                  if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                      val data = response.body()

                      // Save to Room
                      // repository.clearAllEmployees()
                    // repository.saveEmpListToRoom(data!!)

                    *//*  if (!data.isNullOrEmpty()) {
                        withContext(Dispatchers.IO) {
                            val chunkSize = 10 // try with 500–1000
                            data.chunked(chunkSize).forEach { chunk ->
                                repository.saveEmpListToRoom(chunk)
                            }
                        }
                    }*//*
                   // repository.saveEmpListToRoom(data!!)
                    _empListFlow.value = UiState.Success(data!!)

                } else {
                    val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                    _empListFlow.value = UiState.Success(localData)

                    // API failed => try loading from local DB
                  *//*  val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                    val employeeList = localData.map { it.toEmployeeList() }  // Convert to List<EmployeeList>
                    _empListFlow.value = UiState.Success(employeeList!!)*//*
                }

            } catch (e: Exception) {
                val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                _empListFlow.value = UiState.Success(localData)
                // Exception (e.g., no internet) => try loading from local DB
              *//*  val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                val employeeList = localData.map { it.toEmployeeList() }  // Convert to List<EmployeeList>
                _empListFlow.value = UiState.Success(employeeList!!)*//*
            } finally {
                isEmpListLoading.value = false
            }
        }
    }
*/

    fun getAllEmpList(clientCode: String) {
        viewModelScope.launch {
            //  delay(1000)
            isEmpListLoading.value = true

            try {
                val response = repository.getAllEmpList(ClientCodeRequest(clientCode)) // API call

                if (response.isSuccessful && response.body() != null && response.body()!!.isNotEmpty()) {
                    val data = response.body()

                    // Save to Room
                    // repository.clearAllEmployees()
                    repository.saveEmpListToRoom(data!!)

                    _empListFlow.value = UiState.Success(data)

                } else {
                    // API failed => try loading from local DB
                    val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                    _empListFlow.value = UiState.Success(localData)
                }

            } catch (e: Exception) {
                // Exception (e.g., no internet) => try loading from local DB
                val localData = repository.getAllEmpListFromRoom(ClientCodeRequest(clientCode))
                _empListFlow.value = UiState.Success(localData)
            } finally {
                isEmpListLoading.value = false
            }
        }
    }
    fun AddEmployeeRequest.toEmployeeList(): EmployeeList {
        return EmployeeList(
            custId = 0, // auto-generate in Room
            Id = this.Id?.toIntOrNull() ?: 0,
            FirstName = this.FirstName.orEmpty(),
            LastName = this.LastName.orEmpty(),
            PerAddStreet = this.PerAddStreet.orEmpty(),
            CurrAddStreet = this.CurrAddStreet.orEmpty(),
            Mobile = this.Mobile.orEmpty(),
            Email = this.Email.orEmpty(),
            Password = this.Password.orEmpty(),
            CustomerLoginId = this.CustomerLoginId.orEmpty(),
            DateOfBirth = this.DateOfBirth.orEmpty(),
            MiddleName = this.MiddleName.orEmpty(),
            PerAddPincode = this.PerAddPincode.orEmpty(),
            Gender = this.Gender,
            OnlineStatus = this.OnlineStatus,
            CurrAddTown = this.PerAddTown, // using PerAddTown because CurrAddTown not in API
            CurrAddPincode = this.CurrAddPincode.orEmpty(),
            CurrAddState = this.CurrAddState.orEmpty(),
            PerAddTown = this.PerAddTown.orEmpty(),
            PerAddState = this.PerAddState,
            GstNo = this.GstNo.orEmpty(),
            PanNo = this.PanNo.orEmpty(),
            AadharNo = this.AadharNo.orEmpty(),
            BalanceAmount = this.BalanceAmount ?: "0.0",
            AdvanceAmount = this.AdvanceAmount.orEmpty(),
            Discount = this.Discount.orEmpty(),
            CreditPeriod = this.CreditPeriod,
            FineGold = this.FineGold.orEmpty(),
            FineSilver = this.FineSilver.orEmpty(),
            ClientCode = this.ClientCode.orEmpty(),
            VendorId = this.VendorId ?: 0,
            AddToVendor = this.AddToVendor == true,
            CustomerSlabId = this.CustomerSlabId ?: 0,
            CreditPeriodId = this.CreditPeriodId ?: 0,
            RateOfInterestId = this.RateOfInterestId ?: 0,
            CustomerSlab = null,
            RateOfInterest = null,
            CreatedOn = "", // not in API, can be current time or empty
            LastUpdated = "", // not in API
            StatusType = true,
            Remark = this.Remark.orEmpty(),
            Area = this.Area.orEmpty(),
            City = this.City.orEmpty(),
            Country = this.Country.orEmpty()
        )
    }

    /*get all item code list*/
    fun getAllItemCodeList(request: ClientCodeRequest) {
        viewModelScope.launch {
            isItemCodeLoading.value = true
            delay(2000)
            try {
                val response = repository.getAllItemCodeList(request)
                if (response.isSuccessful && response.body() != null) {
                    _itemCodeResponse.value = response.body()!!
                    Log.d("OrderViewModel", "itemcode: ${response.body()}")
                    repository.saveAllItemCodeToRoom(response.body()!!)
                } else {
                    val localData = repository.getAllItemCodeFromRoom(request)
                    _itemCodeResponse.value = localData
                    Log.e("OrderViewModel", "Response error: ${response.code()}")
                }
            } catch (e: Exception) {
                val localData = repository.getAllItemCodeFromRoom(request)
                _itemCodeResponse.value = localData
                Log.e("OrderViewModel", "Exception: ${e.message}")
            }
            finally {
                isItemCodeLoading.value = false
            }
        }
    }

    /*customer order*/
    fun addOrderCustomer(request: CustomOrderRequest) {
        viewModelScope.launch {
            try {
                val response = repository.addOrder(request)
                if (response.isSuccessful && response.body() != null) {
                    _orderResponse.value = response.body()!!
                    Log.d("OrderViewModel", "Custom Order: ${response.body()}")
                } else {
                    _orderResponse.value = response.body()// ✅ Use default object
                    Log.e("OrderViewModel", "Custom Order Response error: ${response.code()}")
                }
            } catch (e: Exception) {
                _orderResponse.value = _orderResponse.value
                Log.e("OrderViewModel", "Custom Order Exception: ${e.message}")
            }
        }
    }

    /*last order no*/
    fun fetchLastOrderNo(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                val response = repository.getLastOrderNo(request)
                if (response.isSuccessful && response.body() != null) {
                    _lastOrderNOResponse.value = response.body()!!
                    //repository.clearLastOrderNo()
                    repository.saveLastOrderNoToRoom(response.body()!!)
                    Log.d("OrderViewModel", "Last Order No: ${response.body()}")
                } else {
                    Log.e("OrderViewModel", "Error: ${response.code()} ${response.message()}")
                    val localData = repository.getLastOrderNoFromRoom(request)
                    _lastOrderNOResponse.value = localData
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception: ${e.message}")
                val localData = repository.getLastOrderNoFromRoom(request)
                _lastOrderNOResponse.value = localData
            }
        }
    }

    fun deleteOrder(request: ClientCodeRequest, id: Int) {


    }

    /*get All order list in list screen*/
    /*last order no*/
    fun fetchAllOrderListFromApi(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val response = repository.getAllOrderList(request)
                if (response.isSuccessful && response.body() != null) {
                    _getAllOrderList.value = response.body()!!
                    setLocalOrderList(_getAllOrderList.value) // ✅ Set the local list here

                    //repository.clearLastOrderNo()
                    for (order in response.body()!!) {
                        order.toRequest()
                        //4   repository.saveCustomerOrder(request)
                    }
                    Log.d("OrderViewModel", "get All order list: ${response.body()}")
                    _isLoading.value = false
                } else {
                    Log.e("OrderViewModel", "Error: ${response.code()} ${response.message()}")
                    val localData = repository.getAllCustomerOrders(request.clientcode.toString())

// Assuming you need to map CustomerOrderRequest to CustomerOrderResponse
                    localData.map { customerOrderRequest ->
                        CustomOrderResponse(
                            CustomOrderId = customerOrderRequest.CustomOrderId,
                            CustomerId = customerOrderRequest.CustomerId.toInt(),
                            ClientCode = customerOrderRequest.ClientCode,
                            OrderId = customerOrderRequest.OrderId,
                            TotalAmount = customerOrderRequest.TotalAmount,
                            PaymentMode = customerOrderRequest.PaymentMode,
                            Offer = customerOrderRequest.Offer,
                            Qty = customerOrderRequest.Qty,
                            GST = customerOrderRequest.GST,
                            OrderStatus = customerOrderRequest.OrderStatus,
                            MRP = customerOrderRequest.MRP,
                            VendorId = customerOrderRequest.VendorId,
                            TDS = customerOrderRequest.TDS,
                            PurchaseStatus = customerOrderRequest.PurchaseStatus,
                            GSTApplied = customerOrderRequest.GSTApplied,
                            Discount = customerOrderRequest.Discount,
                            TotalNetAmount = customerOrderRequest.TotalNetAmount,
                            TotalGSTAmount = customerOrderRequest.TotalGSTAmount,
                            TotalPurchaseAmount = customerOrderRequest.TotalPurchaseAmount,
                            ReceivedAmount = customerOrderRequest.ReceivedAmount,
                            TotalBalanceMetal = customerOrderRequest.TotalBalanceMetal,
                            BalanceAmount = customerOrderRequest.BalanceAmount?:"0.0",
                            TotalFineMetal = customerOrderRequest.TotalFineMetal,
                            CourierCharge = customerOrderRequest.CourierCharge,
                            SaleType = customerOrderRequest.SaleType,
                            OrderDate = customerOrderRequest.OrderDate,
                            OrderCount = customerOrderRequest.OrderCount,
                            AdditionTaxApplied = customerOrderRequest.AdditionTaxApplied,
                            CategoryId = customerOrderRequest.CategoryId,
                            OrderNo = customerOrderRequest.OrderNo,
                            DeliveryAddress = customerOrderRequest.DeliveryAddress,
                            BillType = customerOrderRequest.BillType,
                            UrdPurchaseAmt = customerOrderRequest.UrdPurchaseAmt,
                            BilledBy = customerOrderRequest.BilledBy,
                            SoldBy = customerOrderRequest.SoldBy,
                            CreditSilver = customerOrderRequest.CreditSilver,
                            CreditGold = customerOrderRequest.CreditGold,
                            CreditAmount = customerOrderRequest.CreditAmount,
                            BalanceAmt = customerOrderRequest.BalanceAmt,
                            BalanceSilver = customerOrderRequest.BalanceSilver,
                            BalanceGold = customerOrderRequest.BalanceGold,
                            TotalSaleGold = customerOrderRequest.TotalSaleGold,
                            TotalSaleSilver = customerOrderRequest.TotalSaleSilver,
                            TotalSaleUrdGold = customerOrderRequest.TotalSaleUrdGold,
                            TotalSaleUrdSilver = customerOrderRequest.TotalSaleUrdSilver,
                            FinancialYear = customerOrderRequest.FinancialYear,
                            BaseCurrency = customerOrderRequest.BaseCurrency,
                            TotalStoneWeight = customerOrderRequest.TotalStoneWeight,
                            TotalStoneAmount = customerOrderRequest.TotalStoneAmount,
                            TotalStonePieces = customerOrderRequest.TotalStonePieces,
                            TotalDiamondWeight = customerOrderRequest.TotalDiamondWeight,
                            TotalDiamondPieces = customerOrderRequest.TotalDiamondPieces,
                            TotalDiamondAmount = customerOrderRequest.TotalDiamondAmount,
                            FineSilver = customerOrderRequest.FineSilver,
                            FineGold = customerOrderRequest.FineGold,
                            DebitSilver = customerOrderRequest.DebitSilver,
                            DebitGold = customerOrderRequest.DebitGold,
                            PaidMetal = customerOrderRequest.PaidMetal,
                            PaidAmount = customerOrderRequest.PaidAmount,
                            TotalAdvanceAmt = customerOrderRequest.TotalAdvanceAmt,
                            TaxableAmount = customerOrderRequest.TaxableAmount,
                            TDSAmount = customerOrderRequest.TDSAmount,
                            CreatedOn = customerOrderRequest.CreatedOn.toString(),
                            LastUpdated = customerOrderRequest.LastUpdated.toString(),
                            StatusType = customerOrderRequest.StatusType!!,
                            FineMetal = customerOrderRequest.FineMetal,
                            BalanceMetal = customerOrderRequest.BalanceMetal,
                            AdvanceAmt = customerOrderRequest.AdvanceAmt,
                            PaidAmt = customerOrderRequest.PaidAmt,
                            TaxableAmt = customerOrderRequest.TaxableAmt,
                            GstAmount = customerOrderRequest.GstAmount,
                            GstCheck = customerOrderRequest.GstCheck,
                            Category = customerOrderRequest.Category,
                            TDSCheck = customerOrderRequest.TDSCheck,
                            Remark = customerOrderRequest.Remark,
                            OrderItemId = customerOrderRequest.OrderItemId!!.toInt(),
                            StoneStatus = customerOrderRequest.StoneStatus,
                            DiamondStatus = customerOrderRequest.DiamondStatus,
                            BulkOrderId = customerOrderRequest.BulkOrderId,
                            CustomOrderItem = customerOrderRequest.CustomOrderItem,
                            Payments = customerOrderRequest.Payments,
                            Customer = customerOrderRequest.Customer,
                            syncStatus = customerOrderRequest.syncStatus,
                            ProductName = ""
                        )
                    }

                    // _getAllOrderList.value = mappedData
                    // _getAllOrderList.value = localData
                    _isLoading.value = false
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Exception: ${e.message}")
                repository.getAllCustomerOrders(request.clientcode.toString())

// Assuming you need to map CustomerOrderRequest to CustomerOrderResponse
                /*
                                val mappedData = localData.map { customerOrderRequest ->
                                    CustomOrderResponse(
                                        CustomOrderId = customerOrderRequest.CustomOrderId,
                                        CustomerId = customerOrderRequest.CustomerId.toInt(),
                                        ClientCode = customerOrderRequest.ClientCode,
                                        OrderId = customerOrderRequest.OrderId,
                                        TotalAmount = customerOrderRequest.TotalAmount,
                                        PaymentMode = customerOrderRequest.PaymentMode,
                                        Offer = customerOrderRequest.Offer,
                                        Qty = customerOrderRequest.Qty,
                                        GST = customerOrderRequest.GST,
                                        OrderStatus = customerOrderRequest.OrderStatus,
                                        MRP = customerOrderRequest.MRP,
                                        VendorId = customerOrderRequest.VendorId,
                                        TDS = customerOrderRequest.TDS,
                                        PurchaseStatus = customerOrderRequest.PurchaseStatus,
                                        GSTApplied = customerOrderRequest.GSTApplied,
                                        Discount = customerOrderRequest.Discount,
                                        TotalNetAmount = customerOrderRequest.TotalNetAmount,
                                        TotalGSTAmount = customerOrderRequest.TotalGSTAmount,
                                        TotalPurchaseAmount = customerOrderRequest.TotalPurchaseAmount,
                                        ReceivedAmount = customerOrderRequest.ReceivedAmount,
                                        TotalBalanceMetal = customerOrderRequest.TotalBalanceMetal,
                                        BalanceAmount = customerOrderRequest.BalanceAmount?:"0.0",
                                        TotalFineMetal = customerOrderRequest.TotalFineMetal,
                                        CourierCharge = customerOrderRequest.CourierCharge,
                                        SaleType = customerOrderRequest.SaleType,
                                        OrderDate = customerOrderRequest.OrderDate,
                                        OrderCount = customerOrderRequest.OrderCount,
                                        AdditionTaxApplied = customerOrderRequest.AdditionTaxApplied,
                                        CategoryId = customerOrderRequest.CategoryId,
                                        OrderNo = customerOrderRequest.OrderNo,
                                        DeliveryAddress = customerOrderRequest.DeliveryAddress,
                                        BillType = customerOrderRequest.BillType,
                                        UrdPurchaseAmt = customerOrderRequest.UrdPurchaseAmt,
                                        BilledBy = customerOrderRequest.BilledBy,
                                        SoldBy = customerOrderRequest.SoldBy,
                                        CreditSilver = customerOrderRequest.CreditSilver,
                                        CreditGold = customerOrderRequest.CreditGold,
                                        CreditAmount = customerOrderRequest.CreditAmount,
                                        BalanceAmt = customerOrderRequest.BalanceAmt,
                                        BalanceSilver = customerOrderRequest.BalanceSilver,
                                        BalanceGold = customerOrderRequest.BalanceGold,
                                        TotalSaleGold = customerOrderRequest.TotalSaleGold,
                                        TotalSaleSilver = customerOrderRequest.TotalSaleSilver,
                                        TotalSaleUrdGold = customerOrderRequest.TotalSaleUrdGold,
                                        TotalSaleUrdSilver = customerOrderRequest.TotalSaleUrdSilver,
                                        FinancialYear = customerOrderRequest.FinancialYear,
                                        BaseCurrency = customerOrderRequest.BaseCurrency,
                                        TotalStoneWeight = customerOrderRequest.TotalStoneWeight,
                                        TotalStoneAmount = customerOrderRequest.TotalStoneAmount,
                                        TotalStonePieces = customerOrderRequest.TotalStonePieces,
                                        TotalDiamondWeight = customerOrderRequest.TotalDiamondWeight,
                                        TotalDiamondPieces = customerOrderRequest.TotalDiamondPieces,
                                        TotalDiamondAmount = customerOrderRequest.TotalDiamondAmount,
                                        FineSilver = customerOrderRequest.FineSilver,
                                        FineGold = customerOrderRequest.FineGold,
                                        DebitSilver = customerOrderRequest.DebitSilver,
                                        DebitGold = customerOrderRequest.DebitGold,
                                        PaidMetal = customerOrderRequest.PaidMetal,
                                        PaidAmount = customerOrderRequest.PaidAmount,
                                        TotalAdvanceAmt = customerOrderRequest.TotalAdvanceAmt,
                                        TaxableAmount = customerOrderRequest.TaxableAmount,
                                        TDSAmount = customerOrderRequest.TDSAmount,
                                        CreatedOn = customerOrderRequest.CreatedOn.toString(),
                                        LastUpdated = customerOrderRequest.LastUpdated.toString(),
                                        StatusType = customerOrderRequest.StatusType!!,
                                        FineMetal = customerOrderRequest.FineMetal,
                                        BalanceMetal = customerOrderRequest.BalanceMetal,
                                        AdvanceAmt = customerOrderRequest.AdvanceAmt,
                                        PaidAmt = customerOrderRequest.PaidAmt,
                                        TaxableAmt = customerOrderRequest.TaxableAmt,
                                        GstAmount = customerOrderRequest.GstAmount,
                                        GstCheck = customerOrderRequest.GstCheck,
                                        Category = customerOrderRequest.Category,
                                        TDSCheck = customerOrderRequest.TDSCheck,
                                        Remark = customerOrderRequest.Remark,
                                        OrderItemId = customerOrderRequest.OrderItemId!!.toInt(),
                                        StoneStatus = customerOrderRequest.StoneStatus,
                                        DiamondStatus = customerOrderRequest.DiamondStatus,
                                        BulkOrderId = customerOrderRequest.BulkOrderId,
                                        CustomOrderItem = customerOrderRequest.CustomOrderItem,
                                        Payments = customerOrderRequest.Payments,
                                        Customer = customerOrderRequest.Customer,
                                        syncStatus = customerOrderRequest.syncStatus,
                                        ProductName = ""
                                    )
                                }
                */

                //  _getAllOrderList.value = mappedData
                // _getAllOrderList.value = localData
                _isLoading.value = false
            }
        }
    }

    fun CustomOrderResponse.toRequest(): CustomOrderRequest {
        return CustomOrderRequest(
            CustomOrderId = this.CustomOrderId,
            CustomerId = this.CustomerId.toString(),
            ClientCode = this.ClientCode,
            OrderId = this.OrderId,
            TotalAmount = this.TotalAmount,
            PaymentMode = this.PaymentMode,
            Offer = this.Offer,
            Qty = this.Qty,
            GST = this.GST,
            OrderStatus = this.OrderStatus,
            MRP = this.MRP,
            VendorId = this.VendorId,
            TDS = this.TDS,
            PurchaseStatus = this.PurchaseStatus,
            GSTApplied = this.GSTApplied,
            Discount = this.Discount,
            TotalNetAmount = this.TotalNetAmount,
            TotalGSTAmount = this.TotalGSTAmount,
            TotalPurchaseAmount = this.TotalPurchaseAmount,
            ReceivedAmount = this.ReceivedAmount,
            TotalBalanceMetal = this.TotalBalanceMetal,
            BalanceAmount = this.BalanceAmount?:"0.0",
            TotalFineMetal = this.TotalFineMetal,
            CourierCharge = this.CourierCharge,
            SaleType = this.SaleType,
            OrderDate = this.OrderDate,
            OrderCount = this.OrderCount,
            AdditionTaxApplied = this.AdditionTaxApplied,
            CategoryId = this.CategoryId,
            OrderNo = this.OrderNo,
            DeliveryAddress = this.DeliveryAddress,
            BillType = this.BillType,
            UrdPurchaseAmt = this.UrdPurchaseAmt,
            BilledBy = this.BilledBy,
            SoldBy = this.SoldBy,
            CreditSilver = this.CreditSilver,
            CreditGold = this.CreditGold,
            CreditAmount = this.CreditAmount,
            BalanceAmt = this.BalanceAmt,
            BalanceSilver = this.BalanceSilver,
            BalanceGold = this.BalanceGold,
            TotalSaleGold = this.TotalSaleGold,
            TotalSaleSilver = this.TotalSaleSilver,
            TotalSaleUrdGold = this.TotalSaleUrdGold,
            TotalSaleUrdSilver = this.TotalSaleUrdSilver,
            FinancialYear = this.FinancialYear,
            BaseCurrency = this.BaseCurrency,
            TotalStoneWeight = this.TotalStoneWeight,
            TotalStoneAmount = this.TotalStoneAmount,
            TotalStonePieces = this.TotalStonePieces,
            TotalDiamondWeight = this.TotalDiamondWeight,
            TotalDiamondPieces = this.TotalDiamondPieces,
            TotalDiamondAmount = this.TotalDiamondAmount,
            FineSilver = this.FineSilver,
            FineGold = this.FineGold,
            DebitSilver = this.DebitSilver,
            DebitGold = this.DebitGold,
            PaidMetal = this.PaidMetal,
            PaidAmount = this.PaidAmount,
            TotalAdvanceAmt = this.TotalAdvanceAmt,
            TaxableAmount = this.TaxableAmount,
            TDSAmount = this.TDSAmount,
            CreatedOn = this.CreatedOn,
            StatusType = this.StatusType,
            FineMetal = this.FineMetal,
            BalanceMetal = this.BalanceMetal,
            AdvanceAmt = this.AdvanceAmt,
            PaidAmt = this.PaidAmt,
            TaxableAmt = this.TaxableAmt,
            GstAmount = this.GstAmount,
            GstCheck = this.GstCheck,
            Category = this.Category,
            TDSCheck = this.TDSCheck,
            Remark = this.Remark?: "",
            OrderItemId = this.OrderItemId,
            StoneStatus = this.StoneStatus?: "",
            DiamondStatus = this.DiamondStatus?: "",
            BulkOrderId = this.BulkOrderId?: "",
            CustomOrderItem = this.CustomOrderItem,
            Payments = this.Payments,
            uRDPurchases = emptyList(), // You can map if needed
            Customer = this.Customer,
            syncStatus = this.syncStatus,
            LastUpdated = this.LastUpdated
        )
    }


    /*insert order item locally*/
    fun insertOrderItemToRoom(item: OrderItem) {
        viewModelScope.launch {
            try {

                if (!item.rfidCode.equals("null")) {
                    repository.insertOrderItems(item)
                    Log.d("OrderViewModel", "Order item inserted into Room: $item")
                }

            } catch (e: Exception) {
                Log.e("OrderViewModel", "Room Insert Error: ${e.message}")
            }
        }
    }

    /*getAll order items from the roomdatbase*/

    fun getAllOrderItemsFromRoom() {
        viewModelScope.launch {
            repository.getAllOrderItems().collect { items ->
                // Filter out items with null or empty rfidCode
                val filteredItems = items.filter { !it.rfidCode.isNullOrBlank() }
                _allOrderItems.value = filteredItems
                Log.d(
                    "OrderViewModel",
                    "Fetched ${filteredItems.size} order items with valid RFID code"
                )
            }
        }
    }

    /*delete all order*/
    fun deleteOrders(
        request: ClientCodeRequest,
        id: Int,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val response = repository.deleteOrder(request, id)
                onResult(response.isSuccessful)
            } catch (e: Exception) {
                onResult(false)
            }
        }
    }

    /*insert order item or update locally*/
    fun insertOrderItemToRoomORUpdate(item: OrderItem) {
        viewModelScope.launch {
            try {
                repository.insertORUpdate(item)
                Log.d("OrderViewModel", "Order item updated into Room: $item")
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Room update Error: ${e.message}")
            }
        }
    }


    /*sync data to server*/
    // Save the customer order to Room
    fun saveOrder(customerOrderRequest: CustomOrderRequest) {
        viewModelScope.launch {
            try {
                repository.saveCustomerOrder(customerOrderRequest)
                _insertOrderOffline.value = (customerOrderRequest)
                repository.saveCustomerOrder(customerOrderRequest)
                Log.d("orderViewModel", "orderViewModel" + customerOrderRequest)
            } catch (e: Exception) {
                _insertOrderOffline.value = (customerOrderRequest)
                Log.d("orderViewModel", "orderViewModel" + e.toString())
            }
        }
    }

    // Fetch all customer orders based on the client code
    fun getAllOrders(clientCode: String) {
        viewModelScope.launch {
            try {
                repository.getAllCustomerOrders(clientCode)
                // _orderResponse.value = (orders)
            } catch (e: Exception) {
                // _orderResponse.value =("Failed to fetch orders: ${e.message}")
            }
        }
    }

    // Delete unsynced orders (syncStatus = 0)
    fun deleteUnsyncedOrders() {
        viewModelScope.launch {
            try {
                repository.deleteUnsyncedOrders()
                //_orderResponse.value = ("Unsynced orders deleted successfully.")
            } catch (e: Exception) {
                // _orderResponse.value = UiState.Error("Failed to delete unsynced orders: ${e.message}")
            }
        }
    }

    fun syncDataWhenOnline() {
        viewModelScope.launch {
            val unsyncedOrders = repository.getAllCustomerOrders("LS000241")
            try {
                Log.e("unsyncedOrders", "Successfully done" + unsyncedOrders.get(0).Category)
            } catch (e: Exception) {
                Log.e("Sync", "Failed to sync: ${e.message}")
            }
            for (order in unsyncedOrders) {
                try {
                    val response = repository.addOrder(order)
                    if (response.isSuccessful) {
                        repository.addOrder(order)
                        Log.e("Sync", "Successfully done")
                    }
                } catch (e: Exception) {
                    Log.e("Sync", "Failed to sync: ${e.message}")
                }
            }
        }
    }

    /*update customer Order*/
    /*customer order*/
    fun updateOrderCustomer(request: CustomOrderRequest) {
        viewModelScope.launch {
            try {
                val response = repository.updateOrder(request)
                if (response.isSuccessful && response.body() != null) {
                    _orderUpdateResponse.value = response.body()!!
                    Log.d("OrderViewModel", "Custom Order: ${response.body()}")
                } else {
                    _orderUpdateResponse.value = response.body()// ✅ Use default object
                    Log.e("OrderViewModel", "Custom Order Response error: ${response.code()}")
                }
            } catch (e: Exception) {
              //  _orderUpdateResponse.value = _orderResponse.value.OrderStatus.toString()
                Log.e("OrderViewModel", "Custom Order Exception: ${e.message}")
            }
        }
    }

    fun clearUpdateResponse() {
        _orderUpdateResponse.value = null
    }

    fun clearAddEmpResponse() {
        _addEmpResponse.value = null
    }

    /*update customer Order*/
    /*customer order*/
    fun getDailyRate(request: ClientCodeRequest) {
        viewModelScope.launch {
            try {
                val response = repository.dailyRate(request)
                if (response.isSuccessful && response.body() != null) {
                    _getAllDailyRate.value = response.body()!!
                    Log.d("OrderViewModel", "Custom Order: ${response.body()}")
                } else {
                    _getAllDailyRate.value = response.body()!!// ✅ Use default object
                    Log.e("OrderViewModel", "Custom Order Response error: ${response.code()}")
                }
            } catch (e: Exception) {
                //  _orderUpdateResponse.value = _orderResponse.value.OrderStatus.toString()
                Log.e("OrderViewModel", "Custom Order Exception: ${e.message}")
            }
        }
    }
}






