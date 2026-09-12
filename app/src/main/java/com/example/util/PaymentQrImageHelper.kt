package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object PaymentQrImageHelper {

    private const val PAYMENT_QR_DIR = "payment_qr"
    private const val QR_FILE_PREFIX = "payment_qr_"

    /**
     * Imports a user-selected image from content Uri into application-private storage.
     * Retains maximum sharpness for QR readability (lossless PNG, high DPI).
     * Replaces previous application-owned QR image safely if provided.
     * Never modifies or deletes user's external gallery file.
     */
    fun savePaymentQrFromUri(
        context: Context,
        imageUri: Uri,
        oldImagePath: String? = null
    ): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream = contentResolver.openInputStream(imageUri) ?: return null

            // Validate image by reading bounds first
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                return null
            }

            val qrDir = File(context.filesDir, PAYMENT_QR_DIR).apply {
                if (!exists()) mkdirs()
            }

            // Remove old application-owned QR image if replacing
            deleteApplicationQrImage(context, oldImagePath)

            val destinationFile = File(
                qrDir,
                "${QR_FILE_PREFIX}${System.currentTimeMillis()}.png"
            )

            // Re-open stream to decode actual bitmap
            val decodeStream: InputStream = contentResolver.openInputStream(imageUri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(decodeStream)
            decodeStream.close()

            if (originalBitmap != null) {
                // To keep QR scan crisp, preserve original size up to 1600px without blurry interpolation
                val maxDim = 1600
                val width = originalBitmap.width
                val height = originalBitmap.height
                val scaledBitmap = if (width > maxDim || height > maxDim) {
                    val ratio = width.toFloat() / height.toFloat()
                    val newWidth = if (width > height) maxDim else (maxDim * ratio).toInt()
                    val newHeight = if (height > width) maxDim else (maxDim / ratio).toInt()
                    Bitmap.createScaledBitmap(originalBitmap, newWidth, newHeight, true)
                } else {
                    originalBitmap
                }

                // Lossless PNG compression ensures QR code edges remain ultra-sharp
                FileOutputStream(destinationFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }

                if (scaledBitmap != originalBitmap) {
                    originalBitmap.recycle()
                }

                destinationFile.absolutePath
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Safely deletes an application-owned QR file from private storage.
     * Strictly verifies that the file resides in the app's own payment_qr folder
     * and has the expected prefix so user gallery files are never touched.
     */
    fun deleteApplicationQrImage(context: Context, imagePath: String?) {
        if (!imagePath.isNullOrBlank()) {
            try {
                val file = File(imagePath)
                val qrDir = File(context.filesDir, PAYMENT_QR_DIR)
                if (file.exists() &&
                    file.parentFile?.canonicalPath == qrDir.canonicalPath &&
                    file.name.startsWith(QR_FILE_PREFIX)
                ) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Decodes the payment QR bitmap from the local file path.
     * Returns null if file is missing, empty, or cannot be decoded.
     */
    fun getQrBitmap(imagePath: String?): Bitmap? {
        if (imagePath.isNullOrBlank()) return null
        return try {
            val file = File(imagePath)
            if (file.exists() && file.length() > 0) {
                BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Checks whether the QR image path points to an existing, non-empty file.
     */
    fun isImageValid(imagePath: String?): Boolean {
        if (imagePath.isNullOrBlank()) return false
        return try {
            val file = File(imagePath)
            file.exists() && file.length() > 0
        } catch (e: Exception) {
            false
        }
    }
}
