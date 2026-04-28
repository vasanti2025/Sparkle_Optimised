package com.loyalstring.rfid.data.remote.data

import com.google.gson.annotations.SerializedName

data class Data(
    @SerializedName("ClientCode")
    val clientCode: String? = null,

    @SerializedName("FirstName")
    val firstName: String? = null,

    @SerializedName("LastName")
    val lastName: String? = null,

    @SerializedName("Email")
    val email: String? = null,

    @SerializedName("EmployeeId")
    val employeeId: Int? = null,

    @SerializedName("Role")
    val role: String? = null,

    @SerializedName("CustomRole")
    val customRole: String? = null,

    @SerializedName("RoleId")
    val roleId: Int? = null,

    @SerializedName("ReportingTo")
    val reportingTo: String? = null,

    @SerializedName("IsUpdate")
    val isUpdate: Int? = null,

    @SerializedName("DefaultCompany")
    val defaultCompany: String? = null,

    @SerializedName("DefaultCompanyId")
    val defaultCompanyId: Int? = null,

    @SerializedName("DefaultBranch")
    val defaultBranch: String? = null,

    @SerializedName("DefaultBranchId")
    val defaultBranchId: Int? = null,

    @SerializedName("DefaultCounter")
    val defaultCounter: String? = null,

    @SerializedName("DefaultCounterId")
    val defaultCounterId: Int? = null,

    @SerializedName("Username")
    val username: String? = null,

    @SerializedName("Password")
    val password: String? = null,

    @SerializedName("ConfirmPassword")
    val confirmPassword: String? = null,

    @SerializedName("DayIn")
    val dayIn: String? = null,

    @SerializedName("DayOut")
    val dayOut: String? = null,

    @SerializedName("LastLogin")
    val lastLogin: String? = null,

    @SerializedName("CompanySelectionJson")
    val companySelectionJson: String? = null,

    @SerializedName("BranchSelectionJson")
    val branchSelectionJson: String? = null,

    @SerializedName("CounterJson")
    val counterJson: String? = null,

    @SerializedName("GSTSelectionJson")
    val gstSelectionJson: String? = null,

    @SerializedName("SubscribeState")
    val subscribeState: Boolean? = null,

    @SerializedName("LoginAccess")
    val loginAccess: Boolean? = null,

    @SerializedName("SpecialUserAccess")
    val specialUserAccess: Boolean? = null,

    @SerializedName("UserType")
    val userType: String? = null,

    @SerializedName("Town")
    val town: String? = null,

    @SerializedName("StreetAddress")
    val streetAddress: String? = null,

    @SerializedName("City")
    val city: String? = null,

    @SerializedName("State")
    val state: String? = null,

    @SerializedName("Country")
    val country: String? = null,

    @SerializedName("AadharNo")
    val aadharNo: String? = null,

    @SerializedName("PanNo")
    val panNo: String? = null,

    @SerializedName("DateOfBirth")
    val dateOfBirth: String? = null,

    @SerializedName("Gender")
    val gender: String? = null,

    @SerializedName("Designation")
    val designation: String? = null,

    @SerializedName("WorkLocation")
    val workLocation: String? = null,

    @SerializedName("Department")
    val department: String? = null,

    @SerializedName("BankName")
    val bankName: String? = null,

    @SerializedName("AccountName")
    val accountName: String? = null,

    @SerializedName("BankAccountNo")
    val bankAccountNo: String? = null,

    @SerializedName("BranchName")
    val branchName: String? = null,

    @SerializedName("IfscCode")
    val ifscCode: String? = null,

    @SerializedName("JoiningDate")
    val joiningDate: String? = null,

    @SerializedName("Salary")
    val salary: String? = null,

    @SerializedName("SeatingLocation")
    val seatingLocation: String? = null,

    @SerializedName("FinancialYear")
    val financialYear: String? = null,

    @SerializedName("LabelFormat")
    val labelFormat: String? = null,

    @SerializedName("InvoiceFormat")
    val invoiceFormat: String? = null,

    @SerializedName("SuperAdmin")
    val superAdmin: Int? = null,

    @SerializedName("Counter")
    val counter: String? = null,

    @SerializedName("RDPurchaseFormat")
    val rdPurchaseFormat: String? = null,

    @SerializedName("CompanyNo")
    val companyNo: String? = null,

    @SerializedName("BranchNo")
    val branchNo: String? = null,

    @SerializedName("CompCode")
    val compCode: String? = null,

    @SerializedName("BranchCode")
    val branchCode: String? = null,

    @SerializedName("EmployeeCode")
    val employeeCode: String? = null,

    @SerializedName("MobileNumber")
    val mobileNumber: String? = null,

    @SerializedName("Password2")
    val password2: String? = null,

    @SerializedName("IsGstOnItem")
    val isGstOnItem: Boolean? = null,

    @SerializedName("ManualAdjustmentMode")
    val manualAdjustmentMode: String? = null,

    @SerializedName("PurLotAuditReportAccess")
    val purLotAuditReportAccess: Boolean? = null,

    @SerializedName("DirectPurToStock")
    val directPurToStock: Boolean? = null,

    @SerializedName("AllowAssignLot")
    val allowAssignLot: Boolean? = null,

    @SerializedName("CategoryRatesDashborad")
    val categoryRatesDashborad: Boolean? = null,

    @SerializedName("CustomOrdersDashborad")
    val customOrdersDashborad: Boolean? = null,

    @SerializedName("StatisticsDashborad")
    val statisticsDashborad: Boolean? = null,

    @SerializedName("SampleOutDashborad")
    val sampleOutDashborad: Boolean? = null,

    @SerializedName("PendingRepairsDashborad")
    val pendingRepairsDashborad: Boolean? = null,

    @SerializedName("CustomerBalanceDashborad")
    val customerBalanceDashborad: Boolean? = null,

    @SerializedName("AllowInternalStockTransfer")
    val allowInternalStockTransfer: Boolean? = null,

    @SerializedName("AllowExternalStockTransfer")
    val allowExternalStockTransfer: Boolean? = null,

    @SerializedName("UserPermissionRoleId")
    val userPermissionRoleId: Int? = null,

    @SerializedName("UserPermissionRole")
    val userPermissionRole: String? = null,

    @SerializedName("IsReconciliation")
    val isReconciliation: Boolean? = null,

    @SerializedName("DeviceId")
    val deviceId: String? = null,

    @SerializedName("Id")
    val id: Int? = null,

    @SerializedName("CreatedOn")
    val createdOn: String? = null,

    @SerializedName("LastUpdated")
    val lastUpdated: String? = null,

    @SerializedName("StatusType")
    val statusType: Boolean? = null
)
