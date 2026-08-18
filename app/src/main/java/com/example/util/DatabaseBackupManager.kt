package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.db.StoreDatabase
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class DatabaseBackupInfo(
    val fileName: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val path: String
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            return if (kb > 1024) {
                String.format(Locale.getDefault(), "%.2f MB", kb / 1024.0)
            } else {
                String.format(Locale.getDefault(), "%.1f KB", kb)
            }
        }

    val formattedDate: String
        get() = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(lastModified))
}

object DatabaseBackupManager {

    /**
     * Get details about the current active database file
     */
    fun getCurrentDatabaseInfo(context: Context): DatabaseBackupInfo? {
        val dbFile = context.getDatabasePath(StoreDatabase.DB_NAME)
        return if (dbFile.exists()) {
            DatabaseBackupInfo(
                fileName = dbFile.name,
                sizeBytes = dbFile.length(),
                lastModified = dbFile.lastModified(),
                path = dbFile.absolutePath
            )
        } else null
    }

    /**
     * Export the current SQLite database (.db) file to local storage backup folder
     */
    fun createLocalDatabaseBackup(context: Context): Result<File> {
        return try {
            // Checkpoint Room WAL
            val db = StoreDatabase.getInstance(context)
            try {
                val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val dbFile = context.getDatabasePath(StoreDatabase.DB_NAME)
            if (!dbFile.exists()) {
                return Result.failure(Exception("Active database file not found."))
            }

            val backupDir = File(context.getExternalFilesDir(null), "db_backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val backupFile = File(backupDir, "alkhair_store_db_backup_$timeStamp.db")

            FileInputStream(dbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                }
            }

            Result.success(backupFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Export SQLite database file to a Uri chosen by the user (via SAF/DocumentPicker)
     */
    fun exportDatabaseToUri(context: Context, destinationUri: Uri): Result<Unit> {
        return try {
            // Checkpoint Room WAL
            val db = StoreDatabase.getInstance(context)
            try {
                val cursor = db.openHelper.writableDatabase.query("PRAGMA wal_checkpoint(FULL)")
                cursor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            val dbFile = context.getDatabasePath(StoreDatabase.DB_NAME)
            if (!dbFile.exists()) {
                return Result.failure(Exception("Active database file not found."))
            }

            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(dbFile).use { input ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Could not open destination stream."))

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import SQLite database file from a chosen Uri (via SAF/DocumentPicker)
     */
    fun importDatabaseFromUri(context: Context, sourceUri: Uri): Result<Unit> {
        return try {
            // 1. Close active DB connection
            StoreDatabase.closeInstance()

            val dbFile = context.getDatabasePath(StoreDatabase.DB_NAME)

            // 2. Clear WAL and SHM files to prevent corruption/stale WAL logs
            val walFile = context.getDatabasePath("${StoreDatabase.DB_NAME}-wal")
            val shmFile = context.getDatabasePath("${StoreDatabase.DB_NAME}-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 3. Overwrite main .db file
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(Exception("Could not open source file stream."))

            // 4. Re-open database
            StoreDatabase.getInstance(context)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Import SQLite database file from a local File
     */
    fun importDatabaseFromFile(context: Context, sourceFile: File): Result<Unit> {
        return try {
            if (!sourceFile.exists()) {
                return Result.failure(Exception("Source backup file does not exist."))
            }

            // 1. Close active DB connection
            StoreDatabase.closeInstance()

            val dbFile = context.getDatabasePath(StoreDatabase.DB_NAME)

            // 2. Clear WAL and SHM files
            val walFile = context.getDatabasePath("${StoreDatabase.DB_NAME}-wal")
            val shmFile = context.getDatabasePath("${StoreDatabase.DB_NAME}-shm")
            if (walFile.exists()) walFile.delete()
            if (shmFile.exists()) shmFile.delete()

            // 3. Copy file
            FileInputStream(sourceFile).use { input ->
                FileOutputStream(dbFile).use { output ->
                    input.copyTo(output)
                }
            }

            // 4. Re-open DB
            StoreDatabase.getInstance(context)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * List all available local .db backup files sorted by newest first
     */
    fun getLocalBackups(context: Context): List<DatabaseBackupInfo> {
        val backupDir = File(context.getExternalFilesDir(null), "db_backups")
        if (!backupDir.exists()) return emptyList()

        return backupDir.listFiles { file -> file.extension.lowercase() == "db" }
            ?.map {
                DatabaseBackupInfo(
                    fileName = it.name,
                    sizeBytes = it.length(),
                    lastModified = it.lastModified(),
                    path = it.absolutePath
                )
            }
            ?.sortedByDescending { it.lastModified }
            ?: emptyList()
    }

    /**
     * Share a backup .db file via Android Intent (WhatsApp, Drive, Email, etc.)
     */
    fun shareBackupFile(context: Context, backupFile: File) {
        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            backupFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/octet-stream"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Store Manager - Database Backup (.db)")
            putExtra(Intent.EXTRA_TEXT, "Local SQLite Database backup file for Store Manager POS.")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, "Share Database Backup File (.db)")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}
