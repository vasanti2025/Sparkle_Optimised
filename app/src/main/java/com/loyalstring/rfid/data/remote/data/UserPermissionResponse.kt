package com.loyalstring.rfid.data.remote.data
import com.google.gson.annotations.SerializedName

data class UserPermissionResponse(
    @SerializedName("UserId") val userId: Int,
    @SerializedName("FirstName") val firstName: String,
    @SerializedName("LastName") val lastName: String,
    @SerializedName("RoleId") val roleId: Int,
    @SerializedName("RoleName") val roleName: String,
    @SerializedName("ClientCode") val clientCode: String,
    @SerializedName("Modules") val modules: List<Module>,
    @SerializedName("BranchSelectionJson") val branchSelectionJson: String
)

data class Module(
    @SerializedName("Id") val id: Int,
    @SerializedName("RoleId") val roleId: Int,
    @SerializedName("PageId") val pageId: Int,
    @SerializedName("PageName") val pageName: String,
    @SerializedName("PageDisplayName") val pageDisplayName: String,
    @SerializedName("PagePermission") val pagePermission: String,
    @SerializedName("PermissionId") val permissionId: String,
    @SerializedName("ClientCode") val clientCode: String,
    @SerializedName("UserId") val userId: Int?,
    @SerializedName("PageControls") val pageControls: List<PageControl>
)

data class PageControl(
    @SerializedName("Id") val id: Int,
    @SerializedName("key") val key: String?,
    @SerializedName("label") val label: String?,
    @SerializedName("placeholder") val placeholder: String?,
    @SerializedName("priority") val priority: Int?,
    @SerializedName("type") val type: String?,
    @SerializedName("visibility") val visibility: String?,
    @SerializedName("place") val place: String?
)
