package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.invoice.ScannedInvoiceVerificationDialog
import com.example.ui.theme.*
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors

@Composable
fun BarCodeScannerDialog(
    onBarcodeScanned: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualInput by remember { mutableStateOf("") }
    var scannedVerificationData by remember { mutableStateOf<String?>(null) }
    var isFlashlightOn by remember { mutableStateOf(false) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    var scanSuccessMessage by remember { mutableStateOf<String?>(null) }

    if (scannedVerificationData != null) {
        ScannedInvoiceVerificationDialog(
            scannedData = scannedVerificationData!!,
            onDismiss = {
                scannedVerificationData = null
                onDismiss()
            }
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.88f)
                .testTag("barcode_scanner_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            color = Navy900,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Scanner",
                                    tint = Gold500,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Live Barcode Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Navy900
                            )
                            Text(
                                text = "Powered by ZXing Barcode Engine",
                                style = MaterialTheme.typography.bodySmall,
                                color = Slate500,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (hasCameraPermission && cameraControl != null) {
                            IconButton(
                                onClick = {
                                    val nextState = !isFlashlightOn
                                    isFlashlightOn = nextState
                                    cameraControl?.enableTorch(nextState)
                                },
                                modifier = Modifier.testTag("toggle_torch_button")
                            ) {
                                Icon(
                                    imageVector = if (isFlashlightOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                                    contentDescription = "Flashlight",
                                    tint = if (isFlashlightOn) Amber500 else Navy700
                                )
                            }
                        }

                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.testTag("close_scanner_dialog_button")
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Navy700)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Camera Scanner Viewport / Fallback Permission UI
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission) {
                        LiveCameraPreviewWithZxing(
                            modifier = Modifier.fillMaxSize(),
                            onCameraReady = { control -> cameraControl = control },
                            onBarcodeDetected = { barcode ->
                                triggerVibration(context)
                                scanSuccessMessage = "Scanned: $barcode"
                                if (barcode.startsWith("Store:") || barcode.contains("Invoice #:")) {
                                    scannedVerificationData = barcode
                                } else {
                                    onBarcodeScanned(barcode)
                                    onDismiss()
                                }
                            }
                        )

                        // Scanner Overlay (Reticle, Corners & Laser animation)
                        ScannerOverlayReticle(modifier = Modifier.fillMaxSize())

                        // Scanned notification toast over viewfinder
                        if (scanSuccessMessage != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Emerald600,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 16.dp)
                            ) {
                                Text(
                                    text = scanSuccessMessage!!,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    } else {
                        // Permission Request View
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Camera Permission Required",
                                tint = Slate300,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Camera Permission Required",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enable camera access to scan 1D/2D barcodes & QR codes instantly.",
                                color = Slate400,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Gold500, contentColor = Navy900),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("request_camera_permission_button")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Grant Permission", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Manual Barcode Input Fallback
                Surface(
                    color = Slate100,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Manual Barcode Entry (Optional / Damaged Code)",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Navy800
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = manualInput,
                                onValueChange = { manualInput = it },
                                placeholder = { Text("Enter barcode or QR number...", fontSize = 12.sp) },
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("barcode_manual_input")
                            )
                            Button(
                                onClick = {
                                    if (manualInput.isNotBlank()) {
                                        val code = manualInput.trim()
                                        if (code.startsWith("Store:") || code.contains("Invoice #:")) {
                                            scannedVerificationData = code
                                        } else {
                                            onBarcodeScanned(code)
                                            onDismiss()
                                        }
                                    }
                                },
                                enabled = manualInput.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Navy900),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("submit_barcode_button")
                            ) {
                                Text("Submit", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LiveCameraPreviewWithZxing(
    modifier: Modifier = Modifier,
    onCameraReady: (CameraControl) -> Unit,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    AndroidView(
        modifier = modifier.testTag("zxing_camera_preview"),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
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
                        .also { analysis ->
                            analysis.setAnalyzer(
                                cameraExecutor,
                                ZxingBarcodeImageAnalyzer(onBarcodeDetected)
                            )
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )

                    onCameraReady(camera.cameraControl)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

class ZxingBarcodeImageAnalyzer(
    private val onBarcodeScanned: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val reader = MultiFormatReader().apply {
        val hints = mapOf(
            DecodeHintType.POSSIBLE_FORMATS to listOf(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.UPC_A,
                BarcodeFormat.UPC_E,
                BarcodeFormat.CODE_128,
                BarcodeFormat.CODE_39,
                BarcodeFormat.CODE_93,
                BarcodeFormat.QR_CODE,
                BarcodeFormat.DATA_MATRIX,
                BarcodeFormat.ITF,
                BarcodeFormat.CODABAR
            ),
            DecodeHintType.TRY_HARDER to true,
            DecodeHintType.CHARACTER_SET to "UTF-8"
        )
        setHints(hints)
    }

    @Volatile
    private var isDetected = false

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        if (isDetected) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null && mediaImage.planes.isNotEmpty()) {
            try {
                val plane = mediaImage.planes[0]
                val buffer = plane.buffer
                val rowStride = plane.rowStride
                val pixelStride = plane.pixelStride
                val width = imageProxy.width
                val height = imageProxy.height

                val yData = ByteArray(width * height)
                var destPos = 0

                val rowBuffer = ByteArray(rowStride)
                for (row in 0 until height) {
                    buffer.position(row * rowStride)
                    if (pixelStride == 1) {
                        val toRead = minOf(width, buffer.remaining())
                        buffer.get(yData, destPos, toRead)
                        destPos += toRead
                    } else {
                        val toRead = minOf(rowStride, buffer.remaining())
                        buffer.get(rowBuffer, 0, toRead)
                        for (col in 0 until width) {
                            if (col * pixelStride < toRead && destPos < yData.size) {
                                yData[destPos++] = rowBuffer[col * pixelStride]
                            }
                        }
                    }
                }

                // Handle rotation according to imageProxy.imageInfo.rotationDegrees
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val (finalData, finalWidth, finalHeight) = when (rotationDegrees) {
                    90 -> Triple(rotateY90(yData, width, height), height, width)
                    180 -> Triple(rotateY180(yData, width, height), width, height)
                    270 -> Triple(rotateY270(yData, width, height), height, width)
                    else -> Triple(yData, width, height)
                }

                val source = PlanarYUVLuminanceSource(
                    finalData,
                    finalWidth,
                    finalHeight,
                    0,
                    0,
                    finalWidth,
                    finalHeight,
                    false
                )

                val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

                try {
                    val result = reader.decodeWithState(binaryBitmap)
                    val text = result.text
                    if (!text.isNullOrBlank()) {
                        isDetected = true
                        onBarcodeScanned(text.trim())
                    }
                } catch (_: NotFoundException) {
                    // Try inverted binarizer for high contrast or dark backgrounds
                    try {
                        val invertedBitmap = BinaryBitmap(HybridBinarizer(source.invert()))
                        val result = reader.decodeWithState(invertedBitmap)
                        val text = result.text
                        if (!text.isNullOrBlank()) {
                            isDetected = true
                            onBarcodeScanned(text.trim())
                        }
                    } catch (_: Exception) {}
                }
            } catch (e: Exception) {
                // Ignore transient frame analysis errors
            } finally {
                reader.reset()
            }
        }
        imageProxy.close()
    }

    private fun rotateY90(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        var i = 0
        for (x in 0 until width) {
            for (y in height - 1 downTo 0) {
                rotated[i++] = data[y * width + x]
            }
        }
        return rotated
    }

    private fun rotateY180(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        val size = data.size
        for (i in 0 until size) {
            rotated[i] = data[size - 1 - i]
        }
        return rotated
    }

    private fun rotateY270(data: ByteArray, width: Int, height: Int): ByteArray {
        val rotated = ByteArray(data.size)
        var i = 0
        for (x in width - 1 downTo 0) {
            for (y in 0 until height) {
                rotated[i++] = data[y * width + x]
            }
        }
        return rotated
    }
}

@Composable
fun ScannerOverlayReticle(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "laser_anim")
    val laserProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser_y"
    )

    Canvas(modifier = modifier) {
        val boxWidth = size.width * 0.75f
        val boxHeight = size.height * 0.45f
        val left = (size.width - boxWidth) / 2f
        val top = (size.height - boxHeight) / 2f
        val right = left + boxWidth
        val bottom = top + boxHeight

        // Dark dimming around viewfinder box
        val dimColor = Color.Black.copy(alpha = 0.5f)
        drawRect(dimColor, size = Size(size.width, top))
        drawRect(dimColor, topLeft = Offset(0f, bottom), size = Size(size.width, size.height - bottom))
        drawRect(dimColor, topLeft = Offset(0f, top), size = Size(left, boxHeight))
        drawRect(dimColor, topLeft = Offset(right, top), size = Size(size.width - right, boxHeight))

        // Corner bracket indicators
        val cornerLen = 28.dp.toPx()
        val cornerStroke = 4.dp.toPx()
        val bracketColor = Color(0xFFFFD700) // Gold accent

        // Top-Left Corner
        drawLine(bracketColor, Offset(left, top), Offset(left + cornerLen, top), cornerStroke)
        drawLine(bracketColor, Offset(left, top), Offset(left, top + cornerLen), cornerStroke)

        // Top-Right Corner
        drawLine(bracketColor, Offset(right, top), Offset(right - cornerLen, top), cornerStroke)
        drawLine(bracketColor, Offset(right, top), Offset(right, top + cornerLen), cornerStroke)

        // Bottom-Left Corner
        drawLine(bracketColor, Offset(left, bottom), Offset(left + cornerLen, bottom), cornerStroke)
        drawLine(bracketColor, Offset(left, bottom), Offset(left, bottom - cornerLen), cornerStroke)

        // Bottom-Right Corner
        drawLine(bracketColor, Offset(right, bottom), Offset(right - cornerLen, bottom), cornerStroke)
        drawLine(bracketColor, Offset(right, bottom), Offset(right, bottom - cornerLen), cornerStroke)

        // Animated red/emerald laser beam
        val laserY = top + (boxHeight * laserProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, Color(0xFFFF3333), Color(0xFFFF6666), Color(0xFFFF3333), Color.Transparent),
                startX = left,
                endX = right
            ),
            start = Offset(left + 8.dp.toPx(), laserY),
            end = Offset(right - 8.dp.toPx(), laserY),
            strokeWidth = 3.dp.toPx()
        )
    }
}

private fun triggerVibration(context: Context) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator?.vibrate(
                VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(100)
            }
        }
    } catch (_: Exception) {}
}
