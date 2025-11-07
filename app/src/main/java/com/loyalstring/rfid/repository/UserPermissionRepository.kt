package com.loyalstring.rfid.repository

import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.data.local.dao.UserPermissionDao
import com.loyalstring.rfid.data.local.entity.ModuleEntity
import com.loyalstring.rfid.data.local.entity.PageControlEntity
import com.loyalstring.rfid.data.local.entity.UserPermissionEntity
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.remote.api.RetrofitInterface
import com.loyalstring.rfid.data.remote.data.UserPermissionRequest
import com.loyalstring.rfid.data.remote.resource.Resource
import retrofit2.Response
import javax.inject.Inject

class UserPermissionRepository @Inject constructor(
    private val api: RetrofitInterface,
    private val dao: UserPermissionDao
) {

    suspend fun fetchAndSavePermissions(clientCode: String, userId: Int): Resource<Unit> {
        return try {
            val response = api.getAllUserPermissions(UserPermissionRequest(clientCode, userId))

            if (!response.isSuccessful) {
                return Resource.Error("API Error: ${response.code()} ${response.message()}")
            }

            val body = response.body()
            if (body.isNullOrEmpty()) {
                return Resource.Error("Empty API response")
            }

            val userData = body.first()

            val userEntity = UserPermissionEntity(
                userId = userData.userId,
                firstName = userData.firstName,
                lastName = userData.lastName,
                roleId = userData.roleId,
                roleName = userData.roleName,
                clientCode = userData.clientCode,
                branchSelectionJson = userData.branchSelectionJson
            )

            val moduleEntities = userData.modules.map { module ->
                ModuleEntity(
                    id = module.id,
                    userOwnerId = userData.userId,
                    pageId = module.pageId,
                    pageName = module.pageName,
                    pageDisplayName = module.pageDisplayName,
                    pagePermission = module.pagePermission
                )
            }

            val controlEntities = userData.modules.flatMap { mod ->
                mod.pageControls.map { ctrl ->
                    PageControlEntity(
                        id = ctrl.id,
                        moduleId = mod.id,
                        key = ctrl.key,
                        label = ctrl.label,
                        type = ctrl.type,
                        visibility = ctrl.visibility,
                        place = ctrl.place
                    )
                }
            }

            dao.clearAll()
            dao.insertUserPermission(userEntity)
            dao.insertModules(moduleEntities)
            dao.insertPageControls(controlEntities)

            Resource.Success(Unit)

        } catch (e: Exception) {
            e.printStackTrace()
            Resource.Error(e.message ?: "Unexpected error")
        }
    }
    suspend fun getUserPermission(): UserPermissionEntity? {
        return dao.getUserPermission()
    }

    suspend fun getAllEmpList(clientCodeRequest: ClientCodeRequest): Response<List<EmployeeList>> {
        return api.getAllEmpList(clientCodeRequest)
    }
}
