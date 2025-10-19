package com.loyalstring.rfid.repository

import com.loyalstring.rfid.data.local.dao.DropdownDao
import com.loyalstring.rfid.data.local.entity.Category
import com.loyalstring.rfid.data.local.entity.Design
import com.loyalstring.rfid.data.local.entity.Product
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.addSingleItem.PurityModel
import com.loyalstring.rfid.data.model.addSingleItem.SKUModel
import javax.inject.Inject

class DropdownRepository @Inject constructor(private val dao: DropdownDao) {
    val categories = dao.getAllCategories()
    val products = dao.getAllProducts()
    val designs = dao.getAllDesigns()
    val branch = dao.getAllBranchModel()
    val sku = dao.getAllSKU()
    val purity = dao.getAllPurity()

    suspend fun addCategory(name: String) = dao.insertCategory(Category(name = name))
    suspend fun addProduct(name: String) = dao.insertProduct(Product(name = name))
    suspend fun addDesign(name: String) = dao.insertDesign(Design(name = name))

    suspend fun addBranch(id: String, name: String) {
        val partialBranch = BranchModel(Id = id.toIntOrNull() ?: 0, BranchName = name)
        dao.insertBranch(partialBranch)
    }

    suspend fun addSKU(id: String, name: String) {
        val partialSKU = SKUModel(Id = id.toIntOrNull() ?: 0, StockKeepingUnit = name)
        dao.insertSku(partialSKU)
    }

   suspend fun addPurirty(id: String, name: String) {
        val partialPurity = PurityModel(Id = id.toIntOrNull() ?: 0, PurityName = name)
        dao.insertPurity(partialPurity)
    }
}
