package com.example.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.example.data.entity.Product
import com.example.ui.theme.*
import com.example.util.SoundEffectHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.atomic.AtomicBoolean

private const val TAG = "CameraBarcodeScanner"

/**
 * Hardened Crash-Proof Real-time Camera Barcode Scanner View.
 *
 * Root Cause Fixes:
 * 1. Threading: All ML Kit barcode callbacks dispatch to ContextCompat.getMainExecutor(ctx)
 *    preventing Compose Snapshot and background thread mutation crashes.
 * 2. Execution safety: SafeCameraExecutor catches and drops tasks during shutdown, preventing
 *    fatal RejectedExecutionException from Camera2 dispatch threads.
 * 3. Recomposition isolation: Laser reticle animation is isolated in LaserScannerReticle,
 *    preventing 120 FPS recomposition from flooding CameraControl.enableTorch().
 * 4. Safe hardware binding: Checks hasCamera with try-catch and auto-fallback from Back to Front
 *    camera before binding. Removes restrictive 16:9 aspect ratio constraints that fail on LEGACY hardware.
 * 5. Lifecycle teardown: ClearAnalyzer -> UnbindAll -> Executor shutdown in exact guaranteed order.
 * 6. Vibrate permission added to Manifest to avoid SecurityException on Android 12+.
 */
@Composable
fun CameraBarcodeScannerView(
    onBarcodeScanned: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    availableProducts: List<Product>? = null
) {
    val context = LocalContext.current
    val composeLifecycleOwner = LocalLifecycleOwner.current

    // Resolve true Activity LifecycleOwner for stable camera binding
    val hostActivity = remember(context) { findComponentActivity(context) }
    val effectiveLifecycleOwner: LifecycleOwner = hostActivity ?: composeLifecycleOwner

    // Camera Permission State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Camera Controls State
    var isTorchOn by remember { mutableStateOf(false) }
    var useBackCamera by remember { mutableStateOf(true) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var cameraInfo by remember { mutableStateOf<CameraInfo?>(null) }
    var cameraErrorMessage by remember { mutableStateOf<String?>(null) }
    var isCameraReady by remember { mutableStateOf(false) }

    // Barcode Scanning & Debounce State
    val isProcessingBarcode = remember { AtomicBoolean(false) }
    var lastScannedCode by remember { mutableStateOf<String?>(null) }
    var scannedNotFoundCode by remember { mutableStateOf<String?>(null) }
    var manualCodeInput by remember { mutableStateOf("") }
    var showManualDialog by remember { mutableStateOf(false) }

    // Track active resources for clean disposal
    var activeCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var activeImageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var activeExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    val isDisposed = remember { AtomicBoolean(false) }

    // Safe Torch management (runs only when isTorchOn or cameraControl actually changes)
    LaunchedEffect(isTorchOn, cameraControl, cameraInfo) {
        try {
            if (cameraInfo?.hasFlashUnit() == true) {
                cameraControl?.enableTorch(isTorchOn)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Torch toggle ignored: ${e.message}")
        }
    }

    // Lifecycle cleanup on disposal
    DisposableEffect(effectiveLifecycleOwner) {
        onDispose {
            try {
                Log.d(TAG, "Safely releasing camera session and executor on dispose")
                isDisposed.set(true)
                isProcessingBarcode.set(true)
                try {
                    activeImageAnalysis?.clearAnalyzer()
                } catch (e: Exception) {
                    Log.w(TAG, "Error clearing analyzer: ${e.message}")
                }
                try {
                    activeCameraProvider?.unbindAll()
                } catch (e: Exception) {
                    Log.w(TAG, "Error unbinding camera: ${e.message}")
                }
                try {
                    activeExecutor?.shutdown()
                } catch (e: Exception) {
                    Log.w(TAG, "Error shutting down executor: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up camera: ${e.message}", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("camera_barcode_scanner_container")
    ) {
        if (hasCameraPermission) {
            // Camera Preview & MLKit Scanner
            AndroidView(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("camera_preview_view"),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply {
                        implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        scaleType = PreviewView.ScaleType.FILL_CENTER
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }

                    val rawExecutor = Executors.newSingleThreadExecutor()
                    activeExecutor = rawExecutor

                    // Safe executor wrapper: drops frame tasks during shutdown rather than throwing RejectedExecutionException
                    val safeExecutor = Executor { command ->
                        if (!isDisposed.get() && !rawExecutor.isShutdown) {
                            try {
                                rawExecutor.execute(command)
                            } catch (_: RejectedExecutionException) {
                                // Gracefully ignore dropped frames during teardown
                            }
                        }
                    }

                    // ML Kit barcode scanner configuration
                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                            Barcode.FORMAT_CODE_128,
                            Barcode.FORMAT_EAN_13,
                            Barcode.FORMAT_EAN_8,
                            Barcode.FORMAT_UPC_A,
                            Barcode.FORMAT_UPC_E,
                            Barcode.FORMAT_CODE_39,
                            Barcode.FORMAT_ITF,
                            Barcode.FORMAT_QR_CODE
                        )
                        .build()
                    val barcodeScanner = BarcodeScanning.getClient(options)
                    val mainExecutor = ContextCompat.getMainExecutor(ctx)

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx.applicationContext)

                    cameraProviderFuture.addListener({
                        if (isDisposed.get()) return@addListener

                        try {
                            val cameraProvider = cameraProviderFuture.get()
                            activeCameraProvider = cameraProvider

                            // Camera selector with automatic fallback (Back -> Front -> Any available)
                            val resolvedSelector = try {
                                when {
                                    useBackCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> CameraSelector.DEFAULT_BACK_CAMERA
                                    !useBackCamera && cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> CameraSelector.DEFAULT_FRONT_CAMERA
                                    cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA) -> {
                                        useBackCamera = true
                                        CameraSelector.DEFAULT_BACK_CAMERA
                                    }
                                    cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA) -> {
                                        useBackCamera = false
                                        CameraSelector.DEFAULT_FRONT_CAMERA
                                    }
                                    else -> null
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "Camera availability check failed: ${e.message}")
                                null
                            }

                            if (resolvedSelector == null) {
                                cameraErrorMessage = "No camera hardware available on this device."
                                isCameraReady = false
                                return@addListener
                            }

                            // Dynamic Preview use case
                            val preview = Preview.Builder()
                                .build()
                                .also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }

                            // High-speed Image Analysis use case
                            val imageAnalysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also { analysis ->
                                    analysis.setAnalyzer(safeExecutor) { imageProxy ->
                                        if (isDisposed.get()) {
                                            imageProxy.close()
                                            return@setAnalyzer
                                        }

                                        @SuppressLint("UnsafeOptInUsageError")
                                        val mediaImage = imageProxy.image

                                        if (mediaImage != null && !isProcessingBarcode.get()) {
                                            val image = InputImage.fromMediaImage(
                                                mediaImage,
                                                imageProxy.imageInfo.rotationDegrees
                                            )

                                            barcodeScanner.process(image)
                                                .addOnSuccessListener(mainExecutor) { barcodes ->
                                                    if (isDisposed.get()) return@addOnSuccessListener

                                                    for (barcode in barcodes) {
                                                        val rawValue = barcode.rawValue?.trim()
                                                        if (!rawValue.isNullOrBlank() && !isProcessingBarcode.get()) {
                                                            if (isProcessingBarcode.compareAndSet(false, true)) {
                                                                lastScannedCode = rawValue
                                                                Log.d(TAG, "Barcode detected: $rawValue")

                                                                // Check in catalog if available
                                                                val isKnown = if (availableProducts != null) {
                                                                    availableProducts.any {
                                                                        it.barcode.trim().equals(rawValue, ignoreCase = true) ||
                                                                                it.name.trim().equals(rawValue, ignoreCase = true)
                                                                    }
                                                                } else true

                                                                if (isKnown) {
                                                                    SoundEffectHelper.playBeepAndVibrate(ctx, true)
                                                                    onBarcodeScanned(rawValue)
                                                                } else {
                                                                    SoundEffectHelper.playBeepAndVibrate(ctx, false)
                                                                    scannedNotFoundCode = rawValue
                                                                }
                                                                break
                                                            }
                                                        }
                                                    }
                                                }
                                                .addOnFailureListener(mainExecutor) { e ->
                                                    Log.w(TAG, "Barcode processing: ${e.message}")
                                                }
                                                .addOnCompleteListener(mainExecutor) {
                                                    try {
                                                        imageProxy.close()
                                                    } catch (_: Exception) {}
                                                }
                                        } else {
                                            try {
                                                imageProxy.close()
                                            } catch (_: Exception) {}
                                        }
                                    }
                                }

                            activeImageAnalysis = imageAnalysis

                            // Safe unbind before binding
                            cameraProvider.unbindAll()

                            // Verify lifecycle state is safe for binding
                            val lifecycle = effectiveLifecycleOwner.lifecycle
                            if (lifecycle.currentState.isAtLeast(Lifecycle.State.INITIALIZED)) {
                                val camera = cameraProvider.bindToLifecycle(
                                    effectiveLifecycleOwner,
                                    resolvedSelector,
                                    preview,
                                    imageAnalysis
                                )

                                cameraControl = camera.cameraControl
                                cameraInfo = camera.cameraInfo
                                isCameraReady = true
                                cameraErrorMessage = null

                                // Tap to focus support
                                previewView.setOnTouchListener { _, event ->
                                    if (event.action == MotionEvent.ACTION_UP) {
                                        try {
                                            val factory = previewView.meteringPointFactory
                                            val point = factory.createPoint(event.x, event.y)
                                            val action = FocusMeteringAction.Builder(point).build()
                                            cameraControl?.startFocusAndMetering(action)
                                        } catch (e: Exception) {
                                            Log.w(TAG, "Tap to focus error: ${e.message}")
                                        }
                                    }
                                    true
                                }
                            } else {
                                Log.w(TAG, "Lifecycle state not ready: ${lifecycle.currentState}")
                            }

                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to initialize CameraX safely: ${e.message}", e)
                            cameraErrorMessage = "Unable to start camera. Please close other apps using the camera or use manual barcode entry."
                            isCameraReady = false
                        }
                    }, mainExecutor)

                    previewView
                },
                update = {
                    // Intentionally empty to avoid recomposition flooding
                }
            )

            // Scanning Overlay Frame & Laser (isolated composable to prevent parent recompositions)
            LaserScannerReticle(
                isCameraReady = isCameraReady,
                isNotFound = scannedNotFoundCode != null
            )

            // Top Bar Controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.65f))
                        .testTag("btn_close_scanner")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                }

                // Status Hint Badge
                Surface(
                    color = Color.Black.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(if (isCameraReady) Emerald400 else Gold400, CircleShape)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isCameraReady) "Camera Live • Ready" else "Starting Camera...",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Right controls: Switch Camera + Torch
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Switch Camera (Back/Front)
                    IconButton(
                        onClick = {
                            useBackCamera = !useBackCamera
                            isProcessingBarcode.set(false)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .testTag("btn_switch_camera")
                    ) {
                        Icon(Icons.Default.Cameraswitch, contentDescription = "Flip Camera", tint = Color.White)
                    }

                    // Torch Flashlight
                    val hasFlash = cameraInfo?.hasFlashUnit() == true
                    IconButton(
                        onClick = {
                            if (hasFlash) {
                                isTorchOn = !isTorchOn
                            }
                        },
                        enabled = hasFlash,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isTorchOn) Gold500 else Color.Black.copy(alpha = 0.65f))
                            .testTag("btn_toggle_torch")
                    ) {
                        Icon(
                            if (isTorchOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flashlight",
                            tint = if (isTorchOn) Navy900 else if (hasFlash) Color.White else Color.Gray
                        )
                    }
                }
            }

            // In-Scanner "Product Not Found" Non-destructive Alert
            if (scannedNotFoundCode != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 90.dp, start = 20.dp, end = 20.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy900.copy(alpha = 0.95f)),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Rose500),
                        modifier = Modifier.fillMaxWidth().testTag("card_scanned_not_found")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = Rose500, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Product Not Found", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Barcode: '$scannedNotFoundCode'",
                                fontFamily = FontFamily.Monospace,
                                color = Gold300,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "This barcode is not registered in your product catalog.",
                                color = Slate300,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        scannedNotFoundCode = null
                                        isProcessingBarcode.set(false)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    modifier = Modifier.weight(1f).testTag("btn_scan_again")
                                ) {
                                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 4.dp))
                                    Text("Scan Again", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = {
                                        manualCodeInput = scannedNotFoundCode ?: ""
                                        showManualDialog = true
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    modifier = Modifier.weight(1f).testTag("btn_manual_override")
                                ) {
                                    Text("Manual Search", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Bottom Bar Fallback Actions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.BottomCenter
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Align barcode within reticle • Tap screen to focus",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )

                    Button(
                        onClick = { showManualDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier.testTag("btn_manual_code_fallback")
                    ) {
                        Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Barcode / SKU Manually", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Error Overlay if Camera Failed
            if (cameraErrorMessage != null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.85f))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Navy900),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Rose500),
                        modifier = Modifier.fillMaxWidth().testTag("card_camera_error")
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Rose500, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Camera Scanner Notice",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cameraErrorMessage ?: "Unable to access camera hardware.",
                                color = Slate300,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(onClick = onClose, modifier = Modifier.weight(1f)) {
                                    Text("Close", color = Color.White)
                                }
                                Button(
                                    onClick = {
                                        showManualDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Manual Search")
                                }
                            }
                        }
                    }
                }
            }

        } else {
            // Permission Denied / Request State
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    color = Emerald500.copy(alpha = 0.15f),
                    shape = CircleShape,
                    modifier = Modifier.size(88.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = Emerald400,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Camera Permission Required",
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Camera permission is required to scan product barcodes directly from the camera viewfinder.",
                    color = Slate300,
                    fontSize = 13.5.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("btn_grant_camera_permission")
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp).padding(end = 6.dp))
                    Text("Allow Camera", fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = {
                        try {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open settings: ${e.message}")
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier.fillMaxWidth().testTag("btn_open_settings")
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp).padding(end = 6.dp))
                    Text("Open App Settings")
                }

                Spacer(modifier = Modifier.height(10.dp))

                TextButton(
                    onClick = { showManualDialog = true },
                    modifier = Modifier.testTag("btn_manual_entry_permission_denied")
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, tint = Emerald400, modifier = Modifier.size(16.dp).padding(end = 6.dp))
                    Text("Enter Barcode / SKU Manually", color = Emerald400)
                }

                Spacer(modifier = Modifier.height(6.dp))

                TextButton(onClick = onClose) {
                    Text("Cancel & Return to POS", color = Slate400)
                }
            }
        }

        // Manual Barcode / SKU Input Dialog Fallback
        if (showManualDialog) {
            AlertDialog(
                onDismissRequest = { showManualDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.QrCode, contentDescription = null, tint = Emerald600)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Enter Barcode or SKU", fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column {
                        Text(
                            "Type the exact product barcode or SKU code to lookup and add:",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = manualCodeInput,
                            onValueChange = { manualCodeInput = it },
                            placeholder = { Text("e.g. 8901234567890 or SKU-101") },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().testTag("manual_barcode_input_field")
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val code = manualCodeInput.trim()
                            if (code.isNotBlank()) {
                                showManualDialog = false
                                onBarcodeScanned(code)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.testTag("btn_confirm_manual_barcode")
                    ) {
                        Text("Search & Add", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showManualDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

/**
 * Isolated Viewfinder Reticle with Laser animation.
 * Isolates the 60/120 FPS animation to this component alone,
 * completely preventing parent recompositions from affecting camera hardware.
 */
@Composable
private fun LaserScannerReticle(
    isCameraReady: Boolean,
    isNotFound: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_transition")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_offset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(270.dp)
                .border(2.5.dp, if (isNotFound) Rose500 else Emerald400, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .testTag("viewfinder_reticle")
        ) {
            // Animated Scanning Laser Line
            if (isCameraReady && !isNotFound) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (270 * laserOffset).dp)
                        .background(Emerald400)
                )
            }

            // Center Focus Guide Target
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.Center)
                    .border(1.5.dp, Color.White.copy(alpha = 0.6f), CircleShape)
            )
        }
    }
}

/**
 * Traverses ContextWrapper chain to extract the true ComponentActivity LifecycleOwner
 */
private fun findComponentActivity(context: Context): ComponentActivity? {
    var ctx = context
    while (ctx is ContextWrapper) {
        if (ctx is ComponentActivity) return ctx
        ctx = ctx.baseContext
    }
    return null
}
