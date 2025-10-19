package com.loyalstring.rfid.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loyalstring.rfid.data.local.entity.Category
import com.loyalstring.rfid.data.local.entity.Design
import com.loyalstring.rfid.data.local.entity.Product
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.addSingleItem.PurityModel
import com.loyalstring.rfid.data.model.addSingleItem.SKUModel
import kotlinx.coroutines.flow.Flow

@Dao
interface DropdownDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategory(category: Category)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: Product)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertDesign(design: Design)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertBranch(branchModel: BranchModel)



    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertSku(skuModel: SKUModel)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPurity(purityModel: PurityModel)

    @Query("SELECT * FROM category")
    fun getAllCategories(): Flow<List<Category>>

    @Query("SELECT * FROM product")
    fun getAllProducts(): Flow<List<Product>>

    @Query("SELECT * FROM design")
    fun getAllDesigns(): Flow<List<Design>>

    @Query("SELECT * FROM branch")
    fun getAllBranchModel(): Flow<List<BranchModel>>

    @Query("SELECT * FROM sku")
    fun getAllSKU(): Flow<List<SKUModel>>

    @Query("SELECT * FROM purity")
    fun getAllPurity(): Flow<List<PurityModel>>
}
