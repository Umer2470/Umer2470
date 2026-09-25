package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.example.R
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object BrandingImageHelper {

    private const val BRANDING_DIR = "branding"
    private const val LOGO_FILE_PREFIX = "custom_logo_"
    private const val BANNER_FILE_PREFIX = "dashboard_banner_"
    private const val SMALL_IMG_FILE_PREFIX = "dashboard_small_"

    /**
     * Copies and persists the chosen image Uri into the app's private storage directory.
     * Returns the absolute path of the persisted image file, or null if operation fails.
     */
    fun saveCustomLogoFromUri(context: Context, imageUri: Uri, oldLogoPath: String? = null): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream: InputStream? = contentResolver.openInputStream(imageUri)
            if (inputStream == null) return null

            val brandingDir = File(context.filesDir, BRANDING_DIR).apply {
                if (!exists()) mkdirs()
            }

            // Remove previous custom logo if it exists in branding dir
            deleteOldCustomLogo(oldLogoPath)

            val destinationFile = File(brandingDir, "${LOGO_FILE_PREFIX}${System.currentTimeMillis()}.png")

            // Read, scale down if extremely large (e.g. > 1024px) for snappy POS performance
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap != null) {
                val maxDim = 1024
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

                FileOutputStream(destinationFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
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
     * Saves a custom dashboard banner image to persistent private storage.
     * Returns absolute path, or null on failure.
     */
    fun saveCustomBannerFromUri(context: Context, imageUri: Uri, oldBannerPath: String? = null): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri) ?: return null

            val brandingDir = File(context.filesDir, BRANDING_DIR).apply {
                if (!exists()) mkdirs()
            }

            deleteOldBanner(oldBannerPath)

            val destinationFile = File(brandingDir, "${BANNER_FILE_PREFIX}${System.currentTimeMillis()}.jpg")

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap != null) {
                val maxDim = 1920
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

                FileOutputStream(destinationFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
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
     * Safely deletes custom banner file from private storage
     */
    fun deleteOldBanner(bannerPath: String?) {
        if (!bannerPath.isNullOrBlank()) {
            try {
                val file = File(bannerPath)
                if (file.exists() && file.name.startsWith(BANNER_FILE_PREFIX)) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Saves a custom small foreground image to persistent private storage.
     * Returns absolute path, or null on failure.
     */
    fun saveCustomSmallImageFromUri(context: Context, imageUri: Uri, oldSmallImagePath: String? = null): String? {
        return try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(imageUri) ?: return null

            val brandingDir = File(context.filesDir, BRANDING_DIR).apply {
                if (!exists()) mkdirs()
            }

            deleteOldSmallImage(oldSmallImagePath)

            val destinationFile = File(brandingDir, "${SMALL_IMG_FILE_PREFIX}${System.currentTimeMillis()}.png")

            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            if (originalBitmap != null) {
                val maxDim = 512
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

                FileOutputStream(destinationFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.PNG, 95, out)
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
     * Safely deletes custom small image file from private storage
     */
    fun deleteOldSmallImage(smallImagePath: String?) {
        if (!smallImagePath.isNullOrBlank()) {
            try {
                val file = File(smallImagePath)
                if (file.exists() && file.name.startsWith(SMALL_IMG_FILE_PREFIX)) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getBannerBitmap(bannerPath: String?): Bitmap? {
        if (!bannerPath.isNullOrBlank()) {
            try {
                val file = File(bannerPath)
                if (file.exists() && file.length() > 0) {
                    return BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun getSmallImageBitmap(smallImagePath: String?): Bitmap? {
        if (!smallImagePath.isNullOrBlank()) {
            try {
                val file = File(smallImagePath)
                if (file.exists() && file.length() > 0) {
                    return BitmapFactory.decodeFile(file.absolutePath)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * Safely deletes custom logo file from private storage
     */
    fun deleteOldCustomLogo(logoPath: String?) {
        if (!logoPath.isNullOrBlank()) {
            try {
                val file = File(logoPath)
                if (file.exists() && file.name.startsWith(LOGO_FILE_PREFIX)) {
                    file.delete()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Loads the logo bitmap from custom file path, or returns null if no valid custom logo is configured.
     * Does NOT fall back to any hardcoded image so invoices respect user configuration cleanly.
     */
    fun getLogoBitmap(context: Context, logoPath: String?): Bitmap? {
        if (!logoPath.isNullOrBlank()) {
            try {
                val file = File(logoPath)
                if (file.exists() && file.length() > 0) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    if (bitmap != null) {
                        return bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    /**
     * Returns true if a valid custom logo is currently configured and present on disk.
     */
    fun hasCustomLogo(logoPath: String?): Boolean {
        if (logoPath.isNullOrBlank()) return false
        val file = File(logoPath)
        return file.exists() && file.length() > 0
    }
}
