package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.entity.Product
import com.example.ui.theme.Emerald500
import com.example.ui.theme.Emerald700
import com.example.ui.theme.Gold400
import com.example.ui.theme.Gold500
import com.example.ui.theme.Navy700
import com.example.ui.theme.Navy800
import com.example.ui.theme.Navy900
import com.example.ui.theme.Rose500
import com.example.ui.theme.Rose600
import com.example.ui.theme.Slate200
import com.example.ui.theme.Slate400
import com.example.ui.theme.Slate700
import com.example.util.SoundEffectHelper
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

@Composable
fun CameraBarcodeScannerDialog(
    products: List<Product>,
    currencySymbol: String = "Rs",
    onProductScanned: (Product) -> Unit,
    onBarcodeNotFound: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

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

    var isBackCamera by remember { mutableStateOf(true) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(true) }
    var isContinuousMode by remember { mutableStateOf(true) }
    var scannedItemsCount by remember { mutableIntStateOf(0) }

    var lastScannedBarcode by remember { mutableStateOf<String?>(null) }
    var lastScannedProduct by remember { mutableStateOf<Product?>(null) }
    var scanFeedbackMessage by remember { mutableStateOf<String?>(null) }
    var lastScanTimestamp by remember { mutableLongStateOf(0L) }

    var manualBarcodeInput by remember { mutableStateOf("") }
    var cameraInstance by remember { mutableStateOf<Camera?>(null) }

    // Throttle handling to prevent multiple scans within 1.2 seconds of the exact same code
    fun handleBarcodeDetection(rawCode: String) {
        val code = rawCode.trim()
        if (code.isBlank()) return
        val now = System.currentTimeMillis()
        if (code == lastScannedBarcode && (now - lastScanTimestamp) < 1400) {
            return
        }

        lastScanTimestamp = now
        lastScannedBarcode = code

        val matchedProduct = products.firstOrNull { it.barcode.equals(code, ignoreCase = true) }
        if (matchedProduct != null) {
            lastScannedProduct = matchedProduct
            scannedItemsCount++
            scanFeedbackMessage = "Added: ${matchedProduct.name}"
            if (soundEnabled) {
                SoundEffectHelper.playBeepAndVibrate(context, isSuccess = true)
            }
            onProductScanned(matchedProduct)

            if (!isContinuousMode) {
                scope.launch {
                    delay(500)
                    onDismiss()
                }
            }
        } else {
            lastScannedProduct = null
            scanFeedbackMessage = "Barcode '$code' not found in inventory"
            if (soundEnabled) {
                SoundEffectHelper.playBeepAndVibrate(context, isSuccess = false)
            }
            onBarcodeNotFound(code)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
                .testTag("camera_barcode_scanner_dialog"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Navy900)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp)
            ) {
                // Header Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Gold500,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = Navy900,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Column {
                            Text(
                                text = "Camera Barcode Scanner",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                            Text(
                                text = if (isContinuousMode) "Continuous Mode • $scannedItemsCount item(s) scanned" else "Single Scan Mode",
                                fontSize = 11.sp,
                                color = Gold400
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = Slate700, contentColor = Color.White),
                        modifier = Modifier.size(36.dp).testTag("btn_close_scanner")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Camera Preview Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        CameraPreviewView(
                            isBackCamera = isBackCamera,
                            isTorchEnabled = isTorchEnabled,
                            onCameraReady = { camera ->
                                cameraInstance = camera
                            },
                            onBarcodeDetected = { code ->
                                handleBarcodeDetection(code)
                            }
                        )

                        // Scanner Target Box Overlay
                        ScannerOverlayReticle()

                        // Top Controls Overlay
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Flash Toggle
                            Surface(
                                onClick = {
                                    val next = !isTorchEnabled
                                    isTorchEnabled = next
                                    cameraInstance?.cameraControl?.enableTorch(next)
                                },
                                shape = CircleShape,
                                color = if (isTorchEnabled) Gold500 else Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp).testTag("btn_toggle_torch")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                        contentDescription = "Torch",
                                        tint = if (isTorchEnabled) Navy900 else Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Sound Toggle
                            Surface(
                                onClick = { soundEnabled = !soundEnabled },
                                shape = CircleShape,
                                color = if (soundEnabled) Emerald500 else Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp).testTag("btn_toggle_sound")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                                        contentDescription = "Sound",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            // Camera Switch (Front/Back)
                            Surface(
                                onClick = { isBackCamera = !isBackCamera },
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.6f),
                                modifier = Modifier.size(40.dp).testTag("btn_switch_camera")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        Icons.Default.Cameraswitch,
                                        contentDescription = "Switch Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Bottom scan feedback banner inside camera viewport
                        if (scanFeedbackMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (lastScannedProduct != null) Emerald700.copy(alpha = 0.95f) else Rose600.copy(alpha = 0.95f),
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (lastScannedProduct != null) Icons.Default.Check else Icons.Default.Close,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = scanFeedbackMessage ?: "",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (lastScannedProduct != null) {
                                            Text(
                                                text = "$currencySymbol %.2f • Barcode: ${lastScannedProduct?.barcode}".format(lastScannedProduct?.salePrice ?: 0.0),
                                                fontSize = 10.sp,
                                                color = Slate200
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Permission Request Placeholder
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.Videocam,
                                contentDescription = null,
                                tint = Slate400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Camera Permission Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To scan product barcodes directly with your camera, grant camera access.",
                                color = Slate400,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("btn_grant_camera_permission")
                            ) {
                                Text("Grant Camera Access", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector and Continuous Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Continuous Scan",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Switch(
                            checked = isContinuousMode,
                            onCheckedChange = { isContinuousMode = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Navy900,
                                checkedTrackColor = Gold500,
                                uncheckedThumbColor = Slate400,
                                uncheckedTrackColor = Slate700
                            ),
                            modifier = Modifier.size(36.dp).testTag("switch_continuous_scan")
                        )
                    }

                    if (isContinuousMode && scannedItemsCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Emerald700
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.AddShoppingCart, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    text = "$scannedItemsCount Added to Cart",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Manual Barcode Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = manualBarcodeInput,
                        onValueChange = { manualBarcodeInput = it },
                        placeholder = { Text("Enter / paste barcode manually...", fontSize = 12.sp, color = Slate400) },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Search, contentDescription = null, tint = Gold400, modifier = Modifier.size(18.dp))
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = {
                            if (manualBarcodeInput.isNotBlank()) {
                                handleBarcodeDetection(manualBarcodeInput)
                                manualBarcodeInput = ""
                            }
                        }),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Gold500,
                            unfocusedBorderColor = Slate700,
                            focusedContainerColor = Navy800,
                            unfocusedContainerColor = Navy800
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("input_manual_barcode")
                    )

                    Button(
                        onClick = {
                            if (manualBarcodeInput.isNotBlank()) {
                                handleBarcodeDetection(manualBarcodeInput)
                                manualBarcodeInput = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(52.dp).testTag("btn_submit_manual_barcode")
                    ) {
                        Text("Add", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Quick Simulator Barcode Chips (Useful for emulator testing or fast selection)
                if (products.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Quick Sample Barcodes:",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Slate400
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(products.take(6)) { prod ->
                            Surface(
                                onClick = {
                                    handleBarcodeDetection(prod.barcode)
                                },
                                shape = RoundedCornerShape(6.dp),
                                color = Navy800,
                                border = ButtonDefaults.outlinedButtonBorder,
                                modifier = Modifier.testTag("chip_sample_barcode_${prod.barcode}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = prod.name.take(16),
                                        fontSize = 10.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "[${prod.barcode}]",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Gold400
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraPreviewView(
    isBackCamera: Boolean,
    isTorchEnabled: Boolean,
    onCameraReady: (Camera) -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_ALL_FORMATS
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
            barcodeScanner.close()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            barcodeScanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    for (barcode in barcodes) {
                                        val rawValue = barcode.rawValue
                                        if (!rawValue.isNullOrBlank()) {
                                            ContextCompat.getMainExecutor(ctx).execute {
                                                onBarcodeDetected(rawValue)
                                            }
                                            break
                                        }
                                    }
                                }
                                .addOnCompleteListener {
                                    imageProxy.close()
                                }
                        } else {
                            imageProxy.close()
                        }
                    }

                    val cameraSelector = if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                    camera.cameraControl.enableTorch(isTorchEnabled)
                    onCameraReady(camera)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            // Re-bind when camera lens or settings change
            try {
                val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                val cameraProvider = cameraProviderFuture.get()
                val cameraSelector = if (isBackCamera) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                    val mediaImage = imageProxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                        barcodeScanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                for (barcode in barcodes) {
                                    val rawValue = barcode.rawValue
                                    if (!rawValue.isNullOrBlank()) {
                                        ContextCompat.getMainExecutor(context).execute {
                                            onBarcodeDetected(rawValue)
                                        }
                                        break
                                    }
                                }
                            }
                            .addOnCompleteListener {
                                imageProxy.close()
                            }
                    } else {
                        imageProxy.close()
                    }
                }

                cameraProvider.unbindAll()
                val camera = cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageAnalysis
                )
                camera.cameraControl.enableTorch(isTorchEnabled)
                onCameraReady(camera)
            } catch (_: Exception) {
            }
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ScannerOverlayReticle() {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner_laser")
    val laserOffset by infiniteTransition.animateFloat(
        initialValue = -80f,
        targetValue = 80f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Box(
        modifier = Modifier
            .size(240.dp, 160.dp)
            .border(2.dp, Gold500.copy(alpha = 0.8f), RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        // Red / Gold Glowing Laser line moving up and down
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(2.dp)
                .offset(y = laserOffset.dp)
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Rose500,
                            Gold500,
                            Rose500,
                            Color.Transparent
                        )
                    )
                )
        )

        // Corner Guides
        Text(
            text = "ALIGN BARCODE WITHIN FRAME",
            color = Color.White.copy(alpha = 0.85f),
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
        )
    }
}
