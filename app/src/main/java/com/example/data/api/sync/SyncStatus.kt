package com.example.data.api.sync

/**
 * Enumeration representing sync status for future cloud synchronization.
 */
enum class SyncStatus {
    LOCAL_ONLY,
    SYNC_PENDING,
    SYNCED,
    SYNC_FAILED
}

/**
 * Interface that can be implemented by Room entities or DTOs to support future data synchronization.
 */
interface SyncableEntity {
    val localId: Long
    val serverId: String?
    val createdAt: Long
    val updatedAt: Long
    val syncStatus: SyncStatus
    val deletedAt: Long?
}
