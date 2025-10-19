package com.loyalstring.rfid.ui.screens

import android.app.DatePickerDialog
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.sparklepos.models.loginclasses.customerBill.EmployeeList
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.local.entity.OrderItem
import com.loyalstring.rfid.data.model.addSingleItem.BranchModel
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.data.model.order.ItemCodeResponse
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.OrderViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


// Sample OrderDetails data class
data class OrderDetails(
    val branch: String,
    val exhibition: String,
    val remark: String,
    val purity: String,
    val size: String,
    val length: String,
    val typeOfColors: String,
    val screwType: String,
    val polishType: String,
    val finePercentage: String,
    val wastage: String
)

@Composable
fun OrderDetailsDialog(

    selectedCustomerId: Int?,
    selectedCustomer: EmployeeList?,
    selectedItem: ItemCodeResponse,
    branchList: List<BranchModel>,
    onDismiss: () -> Unit,
    onSave: (OrderDetails) -> Unit,
    viewModel: SingleProductViewModel = hiltViewModel(),

    ) {

    Log.e("TAG", "RFID Code: ${selectedItem?.RFIDCode}")

    val orderViewModel: OrderViewModel = hiltViewModel()
    val singleProductViewModel: SingleProductViewModel = hiltViewModel()
    // Form state
    var branch by remember { mutableStateOf("") }
    var exhibition by remember { mutableStateOf("") }
    var remark by remember { mutableStateOf("") }
    var purity by remember { mutableStateOf("") }
    var size by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var typeOfColors by remember { mutableStateOf("") }
    var screwType by remember { mutableStateOf("") }
    var polishType by remember { mutableStateOf("") }
    var finePercentage by remember { mutableStateOf("") }
    var wastage by remember { mutableStateOf("") }
    var orderDate by remember { mutableStateOf("") }
    var deliverDate by remember { mutableStateOf("") }

    LaunchedEffect(selectedItem) {
        branch = selectedItem.BranchName ?: ""
        exhibition = "" // if you store this anywhere in selectedItem
        remark = "" // same as above
        purity = selectedItem.PurityName ?: ""
        size = selectedItem.Size ?: ""
        length = "" // if applicable
        typeOfColors = selectedItem.Colour ?: ""
        screwType = "" // if stored
        polishType = "" // if stored
        finePercentage = selectedItem.FinePercent ?: ""
        wastage = selectedItem.WastagePercent ?: ""
        orderDate = "" // if you want to show default
        deliverDate = ""
    }


    val purityList by singleProductViewModel.purityResponse1.collectAsState()

    val colorsList = listOf(
        "Yellow Gold",
        "White Gold",
        "Rose Gold",
        "Green Gold",
        "Black Gold",
        "Blue Gold",
        "Purple Gold"
    )
    val screwList = listOf("Type 1", "Type 2", "Type 3")
    val polishList = listOf("High Polish", "Matte Finish", "Satin Finish", "Hammered")

    var expandedBranch by remember { mutableStateOf(false) }
    var expandedExhibition by remember { mutableStateOf(false) }
    var expandedPurity by remember { mutableStateOf(false) }
    var expandedSize by remember { mutableStateOf(false) }
    var expandedLength by remember { mutableStateOf(false) }
    var expandedColors by remember { mutableStateOf(false) }
    var expandedScrew by remember { mutableStateOf(false) }
    var expandedPolish by remember { mutableStateOf(false) }

    // Inside @Composable

    val calendar = Calendar.getInstance()
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val context = LocalContext.current
    val employee = UserPreferences.getInstance(context).getEmployee(Employee::class.java)
    /*  LaunchedEffect(Unit) {
          employee?.clientCode?.let {
              orderViewModel.getAllBranchList(ClientCodeRequest(it))
          }
      }*/


    Dialog(onDismissRequest = { onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Title
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp) // Toolbar-like height
                        .background(Color.DarkGray),
                    contentAlignment = Alignment.Center   // or Alignment.Center for centered text
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 16.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.new_order_icon), // or use any Material icon you prefer
                            contentDescription = "Order Details",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Text(
                            text = "Order Details",
                            fontSize = 18.sp,
                            color = Color.White,
                            fontFamily = poppins
                        )
                    }
                }


                Spacer(modifier = Modifier.height(5.dp))
                Column(
                    modifier = Modifier
                        .weight(1f) // take remaining space
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp)
                ){
                    // Use your DropdownMenuField & input rows here
                    // Example: Branch dropdown
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {


                            if (branchList.isEmpty()) {
                                //Text("Loading branches...", modifier = Modifier.padding(8.dp))
                            } else {
                                DropdownMenuField(
                                    label = "Branch",
                                    options = branchList,
                                    selectedValue = branch,
                                    expanded = expandedBranch,
                                    onValueChange = { branch = it },
                                    onExpandedChange = { expandedBranch = it },
                                    labelColor = Color.Black,
                                    getOptionLabel = { it.BranchName }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp), // only inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Exhibition",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)

                                .background(Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (exhibition.isEmpty()) {
                                Text(
                                    text = "Enter exhibition",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp),
                                    fontFamily = poppins
                                )
                            }

                            BasicTextField(
                                value = exhibition,
                                onValueChange = { exhibition = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp) // minimal inner padding for cursor spacing
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp), // only inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Remark",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)

                                .background(Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (remark.isEmpty()) {
                                Text(
                                    text = "Enter remark",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp),
                                    fontFamily = poppins
                                )
                            }

                            BasicTextField(
                                value = remark,
                                onValueChange = { remark = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp) // minimal inner padding for cursor spacing
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            DropdownMenuField(
                                "Purity",
                                purityList,
                                purity,
                                expandedPurity,
                                { purity = it },
                                { expandedPurity = it },
                                labelColor = Color.Black,
                                getOptionLabel = { it.PurityName.toString() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp), // only inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Size",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)

                                .background(Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (exhibition.isEmpty()) {
                                Text(
                                    text = "Enter size",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp),
                                    fontFamily = poppins
                                )
                            }

                            BasicTextField(
                                value = size,
                                onValueChange = { size = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp) // minimal inner padding for cursor spacing
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            DropdownMenuField(
                                "Colors",
                                colorsList,
                                typeOfColors,
                                expandedColors,
                                { typeOfColors = it },
                                { expandedColors = it },
                                labelColor = Color.Black,
                                getOptionLabel = { it.toString() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            DropdownMenuField(
                                "Screw Type",
                                screwList,
                                screwType,
                                expandedScrew,
                                { screwType = it },
                                { expandedScrew = it },
                                labelColor = Color.Black,
                                getOptionLabel = { it.toString() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp),
                        color = Color(0xFFF2F2F3),
                        shape = RoundedCornerShape(8.dp),
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            DropdownMenuField(
                                "Polish Type",
                                polishList,
                                polishType,
                                expandedPolish,
                                { polishType = it },
                                { expandedPolish = it },
                                labelColor = Color.Black,
                                getOptionLabel = { it.toString() }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp), // only inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Fine %",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)

                                .background(Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (finePercentage.isEmpty()) {
                                Text(
                                    text = "Enter Fine %",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp),
                                    fontFamily = poppins
                                )
                            }

                            BasicTextField(
                                value = finePercentage,
                                onValueChange = { finePercentage = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp) // minimal inner padding for cursor spacing
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp), // only inner padding
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Wastage",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)

                                .background(Color.White, RoundedCornerShape(4.dp)),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (wastage.isEmpty()) {
                                Text(
                                    text = "Enter wastage",
                                    fontSize = 13.sp,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(start = 4.dp),
                                    fontFamily = poppins
                                )
                            }

                            BasicTextField(
                                value = wastage,
                                onValueChange = { wastage = it },
                                singleLine = true,
                                textStyle = TextStyle(fontSize = 13.sp, color = Color.Black),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(2.dp) // minimal inner padding for cursor spacing
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Order Date",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedDate = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }
                                            orderDate = dateFormatter.format(selectedDate.time)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (orderDate.isEmpty()) "Enter Order Date" else orderDate,
                                    fontSize = 13.sp,
                                    color = if (orderDate.isEmpty()) Color.Gray else Color.Black,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = poppins
                                )

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calender), // replace with your calendar icon
                                    contentDescription = "Calendar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Deliver Date",
                            modifier = Modifier
                                .weight(0.4f)
                                .padding(start = 2.dp),
                            fontSize = 12.sp,
                            color = Color.Black,
                            fontFamily = poppins
                        )

                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .padding(start = 6.dp, top = 4.dp, end = 2.dp, bottom = 4.dp)
                                .height(32.dp)
                                .background(Color.White, RoundedCornerShape(4.dp))
                                .clickable {
                                    DatePickerDialog(
                                        context,
                                        { _, year, month, dayOfMonth ->
                                            val selectedDate = Calendar.getInstance().apply {
                                                set(year, month, dayOfMonth)
                                            }
                                            deliverDate = dateFormatter.format(selectedDate.time)
                                        },
                                        calendar.get(Calendar.YEAR),
                                        calendar.get(Calendar.MONTH),
                                        calendar.get(Calendar.DAY_OF_MONTH)
                                    ).show()
                                },
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (deliverDate.isEmpty()) "Enter Deliver Date" else orderDate,
                                    fontSize = 13.sp,
                                    color = if (deliverDate.isEmpty()) Color.Gray else Color.Black,
                                    modifier = Modifier.weight(1f),
                                    fontFamily = poppins
                                )

                                Icon(
                                    painter = painterResource(id = R.drawable.ic_calender), // replace with your calendar icon
                                    contentDescription = "Calendar",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {

                    GradientButtonIcon(
                        text = "Cancel",
                        onClick = {
                            println("Form Reset")
                            onDismiss()
                            // showAddCustomerDialog = false
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp) // reduce height here
                            .padding(start = 8.dp, bottom = 16.dp),
                        icon = painterResource(id = R.drawable.ic_cancel),
                        iconDescription = "Check Icon",
                        fontSize = 12
                    )
                    Spacer(modifier = Modifier.width(8.dp)) // Add space between buttons
                    GradientButtonIcon(
                        text = "OK",

                        onClick = {
                            val orderItem = OrderItem(
                                branchId = "1",
                                branchName = branch,
                                exhibition = exhibition,
                                remark = remark,
                                purity = purity,
                                size = size,
                                length = length,
                                typeOfColor = typeOfColors,
                                screwType = screwType,
                                polishType = polishType,
                                finePer = finePercentage,
                                wastage = wastage,
                                orderDate = orderDate,
                                deliverDate = deliverDate,
                                productName = selectedItem?.ProductName.toString(),
                                itemCode = selectedItem?.ProductName.toString(),
                                rfidCode = selectedItem?.RFIDCode.toString(),
                                itemAmt = selectedItem?.MakingFixedAmt,
                                grWt = selectedItem?.GrossWt,
                                nWt = selectedItem?.NetWt,
                                stoneAmt = selectedItem?.TotalStoneAmount,
                                finePlusWt = selectedItem?.FinePercent,

                                packingWt = selectedItem?.PackingWeight.toString(),
                                totalWt = selectedItem?.TotalWeight.toString(),
                                stoneWt = selectedItem?.TotalStoneWeight.toString(),
                                dimondWt = selectedItem?.DiamondWeight.toString(),
                                sku = selectedItem?.SKU.toString(),
                                qty = selectedItem?.ClipQuantity.toString(),
                                hallmarkAmt = selectedItem?.HallmarkAmount.toString(),
                                mrp = selectedItem?.MRP.toString(),
                                image = selectedItem?.Images.toString(),
                                netAmt = "",
                                diamondAmt = selectedItem?.TotalDiamondAmount.toString(),
                                categoryId = selectedItem?.CategoryId,
                                categoryName = selectedItem?.CategoryName ?: "",
                                productId = selectedItem?.ProductId ?: 0,
                                productCode = selectedItem?.ProductCode ?: "",
                                skuId = selectedItem?.SKUId ?: 0,
                                designid = selectedItem?.DesignId ?: 0,
                                designName = selectedItem?.DesignName ?: "",
                                purityid = selectedItem?.PurityId ?: 0,
                                counterId = selectedItem?.CounterId ?: 0,
                                counterName = "",
                                companyId = 0,
                                epc = selectedItem?.TIDNumber ?: "",
                                tid = selectedItem?.TIDNumber ?: "",
                                todaysRate = selectedItem?.TodaysRate?.toString() ?: "0",
                                makingPercentage = selectedItem?.MakingPercentage?.toString() ?: "0",
                                makingFixedAmt = selectedItem?.MakingFixedAmt?.toString() ?: "0",
                                makingFixedWastage = selectedItem?.MakingFixedWastage?.toString() ?: "0",
                                makingPerGram = selectedItem?.MakingPerGram?.toString() ?: "0"



                            )
                            orderViewModel.insertOrderItemToRoomORUpdate(orderItem)
                            onDismiss()

                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(58.dp) // Adjust height as needed
                            .padding(end = 8.dp, bottom = 16.dp),
                        icon = painterResource(id = R.drawable.check_circle),
                        iconDescription = "Check Icon",
                        fontSize = 12
                    )
                }
            }
        }
    }
}



@Composable
fun <T> DropdownMenuField(
    label: String,
    options: List<T>,
    selectedValue: String,
    expanded: Boolean,
    onValueChange: (String) -> Unit,
    onExpandedChange: (Boolean) -> Unit,
    labelColor: Color = Color.Black,
    getOptionLabel: (T) -> String
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
        ) {
            Text(
                text = label,
                modifier = Modifier
                    .weight(0.4f),
                fontSize = 12.sp,
                color = labelColor,
                fontFamily = poppins
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp, top = 1.dp, end = 2.dp, bottom = 1.dp)
                    .height(45.dp)  // match EditText height
                    .clickable { onExpandedChange(true) }
                    .background(Color.White, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.CenterStart) {

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedValue.isEmpty()) "Select $label" else selectedValue,
                        style = TextStyle(fontSize = 12.sp, color = Color.Black),
                        modifier = Modifier.weight(1f),
                        fontFamily = poppins
                    )

                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Dropdown"
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { onExpandedChange(false) }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(getOptionLabel(option), fontFamily = poppins) },
                        onClick = {
                            onValueChange(getOptionLabel(option))
                            onExpandedChange(false)
                        }
                    )
                }
            }
        }
    }
}


