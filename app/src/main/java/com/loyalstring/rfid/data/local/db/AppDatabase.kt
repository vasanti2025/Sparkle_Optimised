package com.loyalstring.rfid.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.data.local.converters.UHFTAGInfoConverter
import com.loyalstring.rfid.data.local.dao.BulkItemDao
import com.loyalstring.rfid.data.local.dao.CustomerEmailDao
import com.loyalstring.rfid.data.local.dao.DropdownDao
import com.loyalstring.rfid.data.local.dao.EpcDao
import com.loyalstring.rfid.data.local.dao.OrderItemDao
import com.loyalstring.rfid.data.local.dao.TransferTypeDao
import com.loyalstring.rfid.data.local.dao.UHFTAGDao
import com.loyalstring.rfid.data.local.entity.BulkItem
import com.loyalstring.rfid.data.local.entity.Category
import com.loyalstring.rfid.data.local.entity.CustomerEmailEntity
import com.loyalstring.rfid.data.local.entity.Design
import com.loyalstring.rfid.data.local.entity.EpcDto
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.local.entity.Product
import com.loyalstring.rfid.data.local.entity.TransferTypeEntity
import com.loyalstring.rfid.data.local.entity.UHFTAGEntity
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.addSingleItem.PurityModel
import com.loyalstring.rfid.data.model.addSingleItem.SKUModel
import com.loyalstring.rfid.data.model.order.CustomOrderRequest
import com.loyalstring.rfid.data.model.order.CustomOrderResponse
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.data.model.order.LastOrderNoResponse

@TypeConverters(UHFTAGInfoConverter::class)
@Database(
    entities = [UHFTAGEntity::class,
        Category::class,
        Product::class,
        Design::class,
        BulkItem::class,
        OrderItem::class,
        EmployeeList::class,
        ItemCodeResponse::class,
        BranchModel::class,
        SKUModel::class,
        PurityModel::class,
        LastOrderNoResponse::class,
        CustomOrderResponse::class,
        CustomOrderRequest::class,
        TransferTypeEntity::class,
        EpcDto::class,
        CustomerEmailEntity::class
    ],
    version = 1
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun productDao(): UHFTAGDao
    abstract fun dropdownDao(): DropdownDao
    abstract fun bulkItemDao(): BulkItemDao
    abstract fun orderItemDao(): OrderItemDao
    abstract fun stockTransferDao(): TransferTypeDao
    abstract fun epcDao(): EpcDao
    abstract fun customerEmailDao(): CustomerEmailDao


    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =

            INSTANCE ?: synchronized(this) {
                //   context.deleteDatabase("app_db")
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_db"
                )
                    //        .fallbackToDestructiveMigration(false)
                    .build().also { INSTANCE = it }
            }
    }
}