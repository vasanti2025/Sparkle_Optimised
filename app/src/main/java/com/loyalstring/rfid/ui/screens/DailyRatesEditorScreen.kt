package com.loyalstring.rfid.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.ClientCodeRequest
import com.loyalstring.rfid.data.model.addSingleItem.PurityModel
import com.loyalstring.rfid.data.model.setting.UpdateDailyRatesReq
import com.loyalstring.rfid.data.remote.data.DailyRateResponse
import com.loyalstring.rfid.data.remote.resource.Resource
import com.loyalstring.rfid.navigation.GradientTopBar
import com.loyalstring.rfid.ui.utils.GradientButtonIcon
import com.loyalstring.rfid.ui.utils.ToastUtils
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.ui.utils.poppins
import com.loyalstring.rfid.viewmodel.SettingsViewModel
import com.loyalstring.rfid.viewmodel.SingleProductViewModel
import com.loyalstring.rfid.viewmodel.UiState1
import kotlinx.coroutines.launch
import java.util.*

@Composable
fun DailyRatesEditorScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel(),
    purityVM: SingleProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val employee = remember {
        UserPreferences.getInstance(context)
            .getEmployee(com.loyalstring.rfid.data.model.login.Employee::class.java)
    }

    var purityList by remember { mutableStateOf<List<PurityModel>>(emptyList()) }
    val dailyRates = viewModel.getAllDailyRate.collectAsState()
    val updateState = viewModel.updateDailyRatesState.collectAsState()

    // 🔹 Fetch data
    LaunchedEffect(Unit) {
        employee?.clientCode?.let { cc ->
            purityVM.getAllPurity(ClientCodeRequest(cc))
            viewModel.getDailyRate(ClientCodeRequest(cc))
        }
    }

    val purityResponse = purityVM.purityResponse.observeAsState().value
    LaunchedEffect(purityResponse) {
        if (purityResponse is Resource.Success && purityResponse.data != null) {
            purityList = purityResponse.data
        }
    }

    LaunchedEffect(updateState.value) {
        when (val s = updateState.value) {
            is UiState1.Success -> {
                ToastUtils.showToast(context, "Rates updated successfully")
                navController.popBackStack()
                viewModel.resetUpdateState()
            }
            is UiState1.Failure -> ToastUtils.showToast(context, s.message)
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            GradientTopBar(
                title = stringResource(R.string.title_edit_daily_rates),
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                showCounter = false,
                selectedCount = 0,
                onCountSelected = {}
            )
        },
        containerColor = Color.White // 🔹 White screen background
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(padding)
        ) {
            if (purityList.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                DailyRatesContent(
                    purityList = purityList,
                    rateList = dailyRates.value,
                    onSave = { edited ->
                        coroutineScope.launch {
                            val req = edited.map {
                                UpdateDailyRatesReq(
                                    categoryId = it.CategoryId ?: 0,
                                    categoryName = it.CategoryName.orEmpty(),
                                    clientCode = employee?.clientCode.orEmpty(),
                                    employeeCode = employee?.employeeCode.orEmpty(),
                                    finePercentage = it.FinePercentage.orEmpty(),
                                    purityId = it.PurityId ?: 0,
                                    purityName = it.PurityName.orEmpty(),
                                    rate = it.Rate.orEmpty()
                                )
                            }
                            viewModel.updateDailyRates(req)
                        }
                    },
                    onCancel = { navController.popBackStack() }
                )
            }
        }
    }
}

@Composable
private fun DailyRatesContent(
    purityList: List<PurityModel>,
    rateList: List<DailyRateResponse>,
    onSave: (List<DailyRateResponse>) -> Unit,
    onCancel: () -> Unit
) {
    // ✅ Always build from purityList; rates can be empty
    val combinedRates = remember(rateList, purityList) {
        purityList.map { purity ->
            val match = rateList.find { it.PurityId == purity.Id }
            DailyRateResponse(
                CategoryId = purity.CategoryId,
                CategoryName = purity.CategoryName,
                ClientCode = match?.ClientCode.orEmpty(),
                EmployeeCode = match?.EmployeeCode.orEmpty(),
                FinePercentage = purity.FinePercentage?.toString() ?: "0",
                PurityId = purity.Id,
                PurityName = purity.PurityName,
                Rate = match?.Rate ?: "0.00" // 🔹 Default rate if no match
            )
        }.toMutableStateList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(12.dp)
    ) {

        // 🔹 Header Row
        Row(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.label_category),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = poppins,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp)
            )
            Text(
                text =stringResource(R.string.label_purity),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = poppins,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            )
            Text(
                text = stringResource(R.string.label_todays_rate),
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                fontFamily = poppins,
                modifier = Modifier
                    .width(160.dp)
                    .padding(start = 16.dp)
            )
        }

        Divider(thickness = 1.dp, color = Color.Gray.copy(alpha = 0.4f))

        // 🔹 Even if rateList empty, still show purity list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            items(combinedRates) { row ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Text(
                        text = row.CategoryName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        fontFamily = poppins,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp)
                    )
                    Text(
                        text = row.PurityName,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        fontFamily = poppins,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 8.dp)
                    )

                    CompactEditableField(
                        value = row.Rate,
                        onValueChange = { new ->
                            val idx = combinedRates.indexOf(row)
                            if (idx == -1) return@CompactEditableField
                            combinedRates[idx] = row.copy(Rate = new)

                            val baseFine = row.FinePercentage.toDoubleOrNull()
                            val newRateVal = new.toDoubleOrNull()
                            if (baseFine == null || newRateVal == null) return@CompactEditableField

                            val basePureRate = newRateVal / (baseFine / 100.0)
                            combinedRates.indices.forEach { i ->
                                val other = combinedRates[i]
                                if (other.CategoryName.equals(row.CategoryName, true) && other.PurityId != row.PurityId) {
                                    val finePct = other.FinePercentage.toDoubleOrNull() ?: return@forEach
                                    val recalculated = basePureRate * (finePct / 100.0)
                                    combinedRates[i] = other.copy(
                                        Rate = String.format(Locale.US, "%.2f", recalculated)
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .width(150.dp)
                            .padding(start = 12.dp)
                    )
                }
                Divider(color = Color.Gray.copy(alpha = 0.2f))
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            GradientButtonIcon(
                text = stringResource(R.string.button_cancel),
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 4.dp),
                icon = painterResource(id = R.drawable.ic_cancel),
                iconDescription = "Cancel",
                fontSize = 12
            )

            Spacer(Modifier.width(8.dp))

            GradientButtonIcon(
                text = stringResource(R.string.button_update),
                onClick = { onSave(combinedRates.toList()) },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 4.dp),
                icon = painterResource(id = R.drawable.check_circle),
                iconDescription = "Update",
                fontSize = 12
            )
        }
    }
}


@Composable
fun CompactEditableField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var isFocused by remember { mutableStateOf(false) }

    BasicTextField(
        value = value,
        onValueChange = { input ->
            // 🔹 Allow only digits and one optional decimal point
            val filtered = input.filter { it.isDigit() || it == '.' }
            if (filtered.count { it == '.' } <= 1) {
                onValueChange(filtered)
            }
        },
        singleLine = true,
        textStyle = LocalTextStyle.current.copy(
            fontSize = 12.sp,
            color = Color.Black,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Decimal // ✅ shows numeric keyboard with "."
        ),
        modifier = modifier
            .height(34.dp)
            .background(Color.White, RoundedCornerShape(4.dp))
            .border(
                width = 1.dp,
                color = if (isFocused) Color.Gray else Color.LightGray,
                shape = RoundedCornerShape(4.dp)
            )
            .onFocusChanged { isFocused = it.isFocused },
        cursorBrush = SolidColor(Color.Black),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
        }
    )
}
