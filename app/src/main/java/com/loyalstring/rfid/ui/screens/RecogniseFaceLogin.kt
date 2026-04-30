package com.loyalstring.rfid.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import com.loyalstring.rfid.R
import com.loyalstring.rfid.data.model.face.FaceData
import com.loyalstring.rfid.data.model.login.Employee
import com.loyalstring.rfid.navigation.Screens
import com.loyalstring.rfid.ui.utils.FaceRecognizerHelper
import com.loyalstring.rfid.ui.utils.UserPreferences
import com.loyalstring.rfid.viewmodel.FaceLoginViewModel
import com.loyalstring.rfid.viewmodel.UserPermissionViewModel
import com.loyalstring.rfid.worker.LocaleHelper
import java.util.concurrent.Executors

@Composable
fun RecogniseFaceLogin(
    navController: NavController,
    viewModel: FaceLoginViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val userPrefs = remember { UserPreferences(context) }
    val faceRecognizer = remember { FaceRecognizerHelper(context) }
    val permissionViewModel: UserPermissionViewModel = hiltViewModel()

    val userPreferences = UserPreferences.getInstance(context)
    val savedLang = userPreferences.getAppLanguage().ifBlank { "en" }
    val currentLocales = AppCompatDelegate.getApplicationLocales()
    val currentLang = currentLocales[0]?.language ?: savedLang
    val localizedContext = LocaleHelper.applyLocale(context, currentLang)

    val matchedFace by viewModel.matchedFace.observeAsState()
    val message by viewModel.message.observeAsState(localizedContext.getString(R.string.scanning_face))

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isProcessing by remember { mutableStateOf(false) }
    var isFaceMatched by remember { mutableStateOf(false) }

    var showExpiryPopup by remember { mutableStateOf(false) }
    var expiryPopupMessage by remember { mutableStateOf("") }
    //var pendingFace by remember { mutableStateOf<com.yourpackage.FaceResponseType?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }

        if (!faceRecognizer.isModelLoaded()) {
            Toast.makeText(
                context,
                context.getString(R.string.mobile_face_net_tflite_file_missing_in_assets),
                Toast.LENGTH_LONG
            ).show()
        }
    }

   /* LaunchedEffect(matchedFace) {
        matchedFace?.let { face ->
            if (!isFaceMatched) {
                isFaceMatched = true
                isProcessing = false

                userPrefs.saveUserName(face.Username ?: "")
                userPrefs.setLoggedIn(true)
                userPrefs.saveBranchId(face.BranchId ?: 0)
               // userPrefs.saveEmployee(face.EmployeeJson?: 0)
              //  userPrefs.sav(face.clientCode ?: "")

                val employeeObj: Employee? = try {
                    Gson().fromJson(face.EmployeeJson, Employee::class.java)
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }

                employeeObj?.let {
                    userPrefs.saveEmployee(it)
                }

                    // optional: last login credentials/meta
                    userPrefs.saveLoginCredentials(
                        face.Username ?: "",
                        "",
                        true,
                        "",
                        face.EmployeeId ?: 0,
                        face.BranchId ?: 0,
                        ""
                    )

                face.ClientCode?.let { clientCode ->
                    face.EmployeeId?.let { empId ->
                        permissionViewModel.loadPermissions(clientCode, empId)
                    }
                }

                Toast.makeText(context, localizedContext.getString(R.string.face_matched_successfully), Toast.LENGTH_SHORT).show()

                navController.navigate(Screens.HomeScreen.route) {
                    popUpTo(Screens.LoginScreen.route) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
    }*/

    LaunchedEffect(matchedFace) {
        matchedFace?.let { face ->
            if (!isFaceMatched && !showExpiryPopup) {
                isProcessing = false

                val employeeObj: Employee? = try {
                    Gson().fromJson(face.EmployeeJson, Employee::class.java)
                } catch (e: Exception) {
                    null
                }

                val expiryDateStr = employeeObj?.clients?.planExpiryDate
                val daysRemaining = getDaysRemaining(expiryDateStr)

                when {
                    daysRemaining == null -> {
                        isProcessing = true
                        Toast.makeText(
                            context,
                            "Unable to verify expiry date. Face login blocked.",
                            Toast.LENGTH_LONG
                        ).show()
                        navController.popBackStack()
                        return@let
                    }

                    daysRemaining < 0 -> {
                        isProcessing = true
                        Toast.makeText(
                            context,
                            localizedContext.getString(R.string.your_subscription_has_expired_please_contact_support),
                            Toast.LENGTH_LONG
                        ).show()
                        return@let
                    }

                    daysRemaining in 0..15 -> {
                        expiryPopupMessage =
                            localizedContext.getString(R.string.subscription_expiry_warning, daysRemaining)
                        showExpiryPopup = true
                    }

                    else -> {
                        isFaceMatched = true

                        userPrefs.saveUserName(face.Username ?: "")
                        userPrefs.setLoggedIn(true)
                        userPrefs.saveBranchId(face.BranchId ?: 0)

                        employeeObj?.let {
                            userPrefs.saveEmployee(it)
                        }

                        userPrefs.saveLoginCredentials(
                            face.Username ?: "",
                            "",
                            true,
                            employeeObj?.clients?.rfidType.toString(),
                            face.EmployeeId ?: 0,
                            face.BranchId ?: 0,
                            employeeObj?.clients?.organisationName.toString()
                        )

                        face.ClientCode?.let { clientCode ->
                            face.EmployeeId?.let { empId ->
                                permissionViewModel.loadPermissions(clientCode, empId)
                            }
                        }

                        navController.navigate(Screens.HomeScreen.route) {
                            popUpTo(Screens.LoginScreen.route) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
            }
        }
    }





    Column(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()

                        val detectorOptions = FaceDetectorOptions.Builder()
                            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .build()

                        val faceDetector = FaceDetection.getClient(detectorOptions)

                        imageAnalysis.setAnalyzer(
                            Executors.newSingleThreadExecutor()
                        ) { imageProxy ->

                            if (isFaceMatched) {
                                imageProxy.close()
                                return@setAnalyzer
                            }

                            processFaceFrame(
                                imageProxy = imageProxy,
                                detector = faceDetector,
                                faceRecognizer = faceRecognizer,
                                onFaceDetected = { embedding ->
                                    if (!isProcessing && !isFaceMatched) {
                                        isProcessing = true
                                        try {
                                            viewModel.getAllFaceData(embedding)
                                        } catch (e: Exception) {
                                            isProcessing = false
                                            e.printStackTrace()
                                        }
                                    }
                                },
                                onNoFace = {
                                    if (!isFaceMatched) {
                                        isProcessing = false
                                    }
                                },
                                onComplete = {
                                    if (!isFaceMatched) {
                                        isProcessing = false
                                    }
                                }
                            )
                        }

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_FRONT_CAMERA,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                            Toast.makeText(
                                ctx,
                                localizedContext.getString(R.string.unable_to_start_camera),
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }, ContextCompat.getMainExecutor(ctx))

                    previewView
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(localizedContext.getString(R.string.camera_permission_required))
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = message)
        }
    }
}


