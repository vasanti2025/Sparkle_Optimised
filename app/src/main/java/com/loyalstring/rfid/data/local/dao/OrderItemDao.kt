package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderItemDao {
    @Query("DELETE FROM orderItem")
    suspend fun clearAllItems()

    @Query("SELECT * FROM OrderItem WHERE RFIDCode = :rfidCode LIMIT 1")
    suspend fun getItemByRfid(rfidCode: String): OrderItem?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOrderItem(orderItem: OrderItem)

    @Update
    suspend fun update(orderItem: OrderItem)

    // Helper method for upsert logic
   /* suspend fun insertOrUpdate(orderItem: OrderItem) {
        val existing = getItemByRfid(orderItem.rfidCode)
        if (existing == null) {
            insertOrderItem(orderItem)
        } else {
            update(orderItem.copy(id = existing.id)) // preserve the existing ID
        }
    }*/
    @Query("""
    UPDATE OrderItem SET 
        branchId = :branchId,
        branchName = :branchName,
        exhibition = :exhibition,
        remark = :remark,
        purity = :purity,
        size = :size,
        length = :length,
        typeOfColor = :typeOfColor,
        screwType = :screwType,
        polishType = :polishType,
        finePer = :finePer,
        wastage = :wastage,
        orderDate = :orderDate,
        deliverDate = :deliverDate,
        productName = :productName,
        itemCode = :itemCode,
        itemAmt = :itemAmt,
        grWt = :grWt,
        nWt = :nWt,
        stoneAmt = :stoneAmt,
        finePlusWt = :finePlusWt,
        packingWt = :packingWt,
        totalWt = :totalWt,
        stoneWt = :stoneWt,
        dimondWt = :dimondWt,
        sku = :sku,
        qty = :qty,
        hallmarkAmt = :hallmarkAmt,
        mrp = :mrp,
        image = :image,
        netAmt = :netAmt,
        diamondAmt = :diamondAmt,
        categoryId = :categoryId,
        categoryName = :categoryName,
        productId = :productId,
        productCode = :productCode,
        skuId = :skuId,
        designid = :designid,
        designName = :designName,
        purityid = :purityid,
        counterId = :counterId,
        counterName = :counterName,
        companyId = :companyId,
        epc = :epc,
        tid = :tid,
        todaysRate = :todaysRate,
        makingPercentage = :makingPercentage,
        makingFixedAmt = :makingFixedAmt,
        makingFixedWastage = :makingFixedWastage,
        makingPerGram = :makingPerGram
    WHERE rfidCode = :rfidCode
""")
    suspend fun updateByRfidCode(
        rfidCode: String,
        branchId: String,
        branchName: String,
        exhibition: String,
        remark: String,
        purity: String,
        size: String,
        length: String,
        typeOfColor: String,
        screwType: String,
        polishType: String,
        finePer: String,
        wastage: String,
        orderDate: String,
        deliverDate: String,
        productName: String,
        itemCode: String,
        itemAmt: String,
        grWt: String,
        nWt: String,
        stoneAmt: String,
        finePlusWt: String?,
        packingWt: String,
        totalWt: String,
        stoneWt: String,
        dimondWt: String,
        sku: String,
        qty: String,
        hallmarkAmt: String,
        mrp: String,
        image: String,
        netAmt: String,
        diamondAmt: String,
        categoryId: Int?,
        categoryName: String,
        productId: Int,
        productCode: String,
        skuId: Int,
        designid: Int,
        designName: String,
        purityid: Int,
        counterId: Int,
        counterName: String,
        companyId: Int,
        epc: String,
        tid: String,
        todaysRate: String,
        makingPercentage: String,
        makingFixedAmt: String,
        makingFixedWastage: String,
        makingPerGram: String
    )

    suspend fun insertOrUpdate(orderItem: OrderItem) {
        val existing = getItemByRfid(orderItem.rfidCode)
        if (existing == null) {
            insertOrderItem(orderItem)
        } else {
            updateByRfidCode(
                rfidCode = orderItem.rfidCode,
                branchId = orderItem.branchId,
                branchName = orderItem.branchName,
                exhibition = orderItem.exhibition,
                remark = orderItem.remark,
                purity = orderItem.purity,
                size = orderItem.size,
                length = orderItem.length,
                typeOfColor = orderItem.typeOfColor,
                screwType = orderItem.screwType,
                polishType = orderItem.polishType,
                finePer = orderItem.finePer,
                wastage = orderItem.wastage,
                orderDate = orderItem.orderDate,
                deliverDate = orderItem.deliverDate,
                productName = orderItem.productName,
                itemCode = orderItem.itemCode,
                itemAmt = orderItem.itemAmt.toString(),
                grWt = orderItem.grWt.toString(),
                nWt = orderItem.nWt.toString(),
                stoneAmt = orderItem.stoneAmt.toString(),
                finePlusWt = orderItem.finePlusWt,
                packingWt = orderItem.packingWt,
                totalWt = orderItem.totalWt,
                stoneWt = orderItem.stoneWt,
                dimondWt = orderItem.dimondWt,
                sku = orderItem.sku,
                qty = orderItem.qty,
                hallmarkAmt = orderItem.hallmarkAmt,
                mrp = orderItem.mrp,
                image = orderItem.image,
                netAmt = orderItem.netAmt,
                diamondAmt = orderItem.diamondAmt,
                categoryId = orderItem.categoryId,
                categoryName = orderItem.categoryName,
                productId = orderItem.productId,
                productCode = orderItem.productCode,
                skuId = orderItem.skuId,
                designid = orderItem.designid,
                designName = orderItem.designName,
                purityid = orderItem.purityid,
                counterId = orderItem.counterId,
                counterName = orderItem.counterName,
                companyId = orderItem.companyId,
                epc = orderItem.epc,
                tid = orderItem.tid,
                todaysRate = orderItem.todaysRate,
                makingPercentage = orderItem.makingPercentage,
                makingFixedAmt = orderItem.makingFixedAmt,
                makingFixedWastage = orderItem.makingFixedWastage,
                makingPerGram = orderItem.makingPerGram

            )
        }
    }

    @Query("SELECT * FROM orderItem")
    fun getAllOrderItem(): Flow<List<OrderItem>>

    @Query("SELECT * FROM customer WHERE clientCode = :clientCode")
    suspend  fun getAllEmployees(clientCode: String): List<EmployeeList>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend  fun insertAll(employees: List<EmployeeList>)

    @Query("DELETE FROM customer")
    suspend fun clearAllEmployees()

    @Query("SELECT * FROM itemcoderesponse WHERE clientCode = :clientCode")
    suspend  fun getAllItemCode(clientCode: String): List<ItemCodeResponse>


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend  fun insertAllItemCode(employees: List<ItemCodeResponse>)

    @Query("DELETE FROM itemcoderesponse")
    suspend fun clearAllItemCode()

    /************************* Last order N**********************/
    @Query("SELECT * FROM lastorderno")
    suspend  fun getLastOrderNo(): LastOrderNoResponse


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend  fun insertLastOrderNo(orderNo: LastOrderNoResponse)

    @Query("DELETE FROM lastorderno")
    suspend fun clearLastOrderNo()

    /************ customer Order response **************/
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCustomerOrder(customerOrderRequest: CustomOrderRequest)

    @Query("SELECT * FROM customerorderequest WHERE ClientCode = :clientCode")
    suspend fun getAllCustomnerOrderReponse(clientCode: String): List<CustomOrderRequest>

    @Query("DELETE FROM customerorderequest WHERE syncStatus = 0")
    suspend fun deleteUnsyncedOrder()

    @Query("DELETE FROM OrderItem")
    suspend fun clearAll()



}