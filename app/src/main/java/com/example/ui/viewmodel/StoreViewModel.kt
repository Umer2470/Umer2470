package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.StoreDatabase
import com.example.data.entity.CategoryEntity
import com.example.data.entity.Customer
import com.example.data.entity.CustomerLedger
import com.example.data.entity.Expense
import com.example.data.entity.Product
import com.example.data.entity.Purchase
import com.example.data.entity.PurchaseItem
import com.example.data.entity.Sale
import com.example.data.entity.SaleItem
import com.example.data.entity.StoreSettings
import com.example.data.entity.Supplier
import com.example.data.entity.SupplierLedger
import com.example.data.entity.UserAccount
import com.example.data.entity.DailyClosing
import com.example.data.entity.ActivityLog
import com.example.data.entity.StoreProfile
import com.example.data.entity.UserStorePermission
import com.example.data.entity.TenantAccount
import com.example.data.entity.SubscriptionPlan
import com.example.data.entity.RecycleBinItem
import com.example.data.entity.SuperAdminRecovery
import com.example.data.api.security.AppActivationManager
import com.example.data.api.network.NetworkConnectivityMonitor
import com.example.data.api.network.ConnectionState
import com.example.data.api.network.DetailedConnectionStatus
import com.example.data.api.sync.SyncManager
import com.example.data.api.sync.SyncState
import com.example.data.api.sync.SyncLogLevel
import com.example.data.entity.AttendanceRecord
import com.example.util.RecoveryUtils
import com.example.util.SecurityUtils
import com.example.util.AppFeature
import com.example.util.LicenseValidationResult
import com.example.util.LicenseValidator
import com.example.util.LicenseManager
import com.example.util.FeatureLimitResult
import kotlinx.coroutines.flow.map
import java.io.FileOutputStream
import com.example.util.BatchImportParser
import com.example.util.BatchImportResult
import com.example.util.BatchProductImportItem
import com.example.util.DatabaseBackupInfo
import com.example.util.DatabaseBackupManager
import java.io.File
import com.example.data.repository.StoreRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class DaySalesPoint(
    val dayName: String,
    val dateLabel: String,
    val totalRevenue: Double,
    val totalVolume: Int,
    val timestamp: Long
)

data class CategorySalesPoint(
    val categoryName: String,
    val totalAmount: Double,
    val totalUnitsSold: Double,
    val percentage: Float
)

data class InventoryHealthSummary(
    val totalLowStockCount: Int = 0,
    val criticalCount: Int = 0, // Stock == 0
    val lowStockCategoryBreakdown: Map<String, Int> = emptyMap(),
    val thresholdValue: Double = 5.0,
    val lastCheckTime: Long = System.currentTimeMillis()
)

data class CartItem(
    val product: Product,
    var quantity: Double,
    var customPrice: Double = product.salePrice
) {
    val total: Double get() = quantity * customPrice
}

data class PurchaseCartItem(
    val product: Product,
    var quantity: Double,
    var costPrice: Double = product.purchasePrice
) {
    val total: Double get() = quantity * costPrice
}

data class CashierProfile(
    val name: String = "Not Assigned",
    val employeeId: String = "",
    val phone: String = "",
    val designation: String = "Cashier",
    val userId: Long? = null
)

class StoreViewModel(application: Application) : AndroidViewModel(application) {

    private val db = StoreDatabase.getInstance(application)
    private val repository = StoreRepository(db.storeDao(), application)

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    fun refreshDashboardData(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            _isRefreshing.value = true
            kotlinx.coroutines.delay(600)
            _isRefreshing.value = false
            showToast("Dashboard widgets & metrics updated!")
            logActivity("DASHBOARD_REFRESH", "Refreshed sales, inventory, and payment figures")
            onComplete?.invoke()
        }
    }

    // --- MULTI-STORE MANAGEMENT ---
    val allStores: StateFlow<List<StoreProfile>> = repository.allStores
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedStoreId = MutableStateFlow(1L) // 0L = Combined All Stores, 1L = Main Store, 2L = Van Shop
    val selectedStoreId: StateFlow<Long> = _selectedStoreId.asStateFlow()

    // --- USER MANAGEMENT & AUTH STATE ---
    private val _currentUser = MutableStateFlow<UserAccount?>(null)
    val currentUser: StateFlow<UserAccount?> = _currentUser.asStateFlow()

    val userPermissions: StateFlow<List<UserStorePermission>> = repository.allUserStorePermissions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userAllowedStores: StateFlow<List<StoreProfile>> = combine(allStores, _currentUser, userPermissions) { stores, user, permissions ->
        if (user == null || user.role == "SUPER_ADMIN") {
            stores
        } else {
            val userPerms = permissions.filter { it.userId == user.id && it.isGranted }
            val assignedIds = user.getAssignedStoreIdsList()
            stores.filter { store ->
                if (store.isLocked) return@filter false
                val hasPerm = userPerms.any { perm ->
                    perm.storeId == store.id &&
                    (perm.verifiedCodeHash == store.secretCodeHash || perm.verifiedCodeHash == store.qrCodeHash || store.secretCodeHash.isBlank())
                }
                hasPerm || store.id in assignedIds
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeStore: StateFlow<StoreProfile?> = combine(allStores, _selectedStoreId) { stores, id ->
        if (id == 0L) {
            StoreProfile(
                id = 0L,
                storeName = "Combined View (All Stores)",
                ownerName = "Multi-Branch Consolidated",
                phone = "System Consolidated",
                address = "All Store Locations Combined",
                code = "ALL"
            )
        } else {
            stores.find { it.id == id } ?: stores.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _showStoreSelectionDialog = MutableStateFlow(false)
    val showStoreSelectionDialog: StateFlow<Boolean> = _showStoreSelectionDialog.asStateFlow()

    fun openStoreSelectionDialog() { _showStoreSelectionDialog.value = true }
    fun dismissStoreSelectionDialog() { _showStoreSelectionDialog.value = false }

    fun selectStore(storeId: Long) {
        viewModelScope.launch {
            val user = _currentUser.value
            val allowed = userAllowedStores.value
            if (user != null && user.role != "SUPER_ADMIN" && storeId != 0L && allowed.none { it.id == storeId }) {
                showToast("Access Denied: You need to join this store branch with QR/Secret Code.")
                return@launch
            }

            _selectedStoreId.value = storeId

            if (user != null && user.id > 0L) {
                val updatedUser = user.copy(lastSelectedStoreId = storeId)
                _currentUser.value = updatedUser
                repository.saveUser(updatedUser)
            }
            _showStoreSelectionDialog.value = false
            val storeName = if (storeId == 0L) "Combined View" else allStores.value.find { it.id == storeId }?.storeName ?: "Selected Store"
            showToast("Switched to $storeName")
        }
    }

    // --- STORE ACCESS SYSTEM VERIFICATION & MANAGEMENT ---
    fun verifyAndGrantStoreAccess(
        storeId: Long,
        codeOrQrInput: String,
        method: String, // "SECRET_CODE" or "QR_CODE"
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val cleanInput = codeOrQrInput.trim()
            if (cleanInput.isBlank()) {
                onResult(false, "Please enter or scan a valid access code/QR payload.")
                return@launch
            }

            val store = repository.getStoreById(storeId)
            if (store == null) {
                onResult(false, "Selected store branch not found.")
                return@launch
            }

            if (store.isLocked) {
                logActivity("STORE_ACCESS_DENIED", "Attempted access to locked store ${store.storeName} via $method")
                onResult(false, "Store '${store.storeName}' is currently locked by Super Admin.")
                return@launch
            }

            if (!store.isAccessCodeEnabled) {
                logActivity("STORE_ACCESS_DENIED", "Attempted access to store ${store.storeName} with disabled access codes")
                onResult(false, "Store access codes for '${store.storeName}' are disabled by Super Admin.")
                return@launch
            }

            val user = _currentUser.value
            val userId = user?.id ?: 1L

            val extractedSecret = SecurityUtils.extractSecretFromQrPayload(cleanInput)
            val extractedStoreId = SecurityUtils.extractStoreIdFromQrPayload(cleanInput)
            val decryptedSecret = SecurityUtils.decryptSecret(store.secretCode)

            val isQrMatch = method == "QR_CODE" && (
                cleanInput == store.qrCode ||
                SecurityUtils.hashSha256(cleanInput) == store.qrCodeHash ||
                (extractedStoreId == storeId && extractedSecret != null && (extractedSecret == decryptedSecret || SecurityUtils.hashSha256(extractedSecret) == store.secretCodeHash))
            )

            val isSecretMatch = method == "SECRET_CODE" && (
                cleanInput.equals(decryptedSecret, ignoreCase = true) ||
                cleanInput.equals(store.secretCode, ignoreCase = true) ||
                SecurityUtils.hashSha256(cleanInput) == store.secretCodeHash ||
                (extractedSecret != null && extractedSecret.equals(decryptedSecret, ignoreCase = true))
            )

            val isMatch = isQrMatch || isSecretMatch

            if (isMatch) {
                val verifiedHash = store.secretCodeHash.ifBlank { SecurityUtils.hashSha256(decryptedSecret) }
                val newPerm = UserStorePermission(
                    userId = userId,
                    storeId = storeId,
                    verifiedCodeHash = verifiedHash,
                    grantedBy = method,
                    isGranted = true
                )
                repository.saveUserPermission(newPerm)

                if (user != null) {
                    val currentList = user.getAssignedStoreIdsList().toMutableList()
                    if (!currentList.contains(storeId)) {
                        currentList.add(storeId)
                        val newCsv = currentList.joinToString(",")
                        val updatedUser = user.copy(assignedStoreIdsCsv = newCsv)
                        _currentUser.value = updatedUser
                        repository.saveUser(updatedUser)
                    }
                }

                val deviceName = SecurityUtils.getDeviceName()
                logActivity("STORE_ACCESS_SUCCESS", "Granted access to '${store.storeName}' via $method on device $deviceName")

                _selectedStoreId.value = storeId
                showToast("Access Granted to ${store.storeName}!")
                onResult(true, "Store access verified & saved successfully!")
            } else {
                val deviceName = SecurityUtils.getDeviceName()
                logActivity("STORE_ACCESS_FAILED", "Failed access attempt for '${store.storeName}' via $method on device $deviceName")
                onResult(false, "Invalid Secret Code or QR Code! Access denied.")
            }
        }
    }

    fun updateStoreAccessCodes(
        storeId: Long,
        newSecretCode: String,
        isAccessCodeEnabled: Boolean,
        isLocked: Boolean,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                onComplete(false, "Only Super Admin can manage Secret Codes.")
                return@launch
            }

            val currentStore = repository.getStoreById(storeId)
            if (currentStore == null) {
                onComplete(false, "Store not found.")
                return@launch
            }

            val rawSecret = SecurityUtils.decryptSecret(currentStore.secretCode)
            var cleanSecret = newSecretCode.trim()
            if (cleanSecret.isBlank()) {
                cleanSecret = if (rawSecret.isNotBlank()) rawSecret else SecurityUtils.generateSecretCode(currentStore.code)
            }

            val encryptedSecret = SecurityUtils.encryptSecret(cleanSecret)
            val newQrPayload = SecurityUtils.generateQrPayload(storeId, currentStore.code, cleanSecret)
            val secHash = SecurityUtils.hashSha256(cleanSecret)
            val qrHash = SecurityUtils.hashSha256(newQrPayload)

            val updatedStore = currentStore.copy(
                secretCode = encryptedSecret,
                qrCode = newQrPayload,
                secretCodeHash = secHash,
                qrCodeHash = qrHash,
                isAccessCodeEnabled = isAccessCodeEnabled,
                isLocked = isLocked
            )

            repository.saveStore(updatedStore)

            val currentUser = _currentUser.value
            val userName = currentUser?.name ?: "Super Admin"
            val deviceName = SecurityUtils.getDeviceName()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val formattedDate = dateFormat.format(Date())

            val actionDesc = if (cleanSecret != rawSecret) "Security Code Changed to $cleanSecret" else "Security Settings Updated"
            val logDetails = "User: $userName | Date: $formattedDate | Device: $deviceName | Action: $actionDesc for Store ${currentStore.storeName}"
            logActivity("STORE_ACCESS_CONFIG_UPDATE", logDetails)

            onComplete(true, "Store access security credentials updated! Previous codes invalidated.")
        }
    }

    fun revokeUserAccessForStore(userId: Long, storeId: Long, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                onComplete(false, "Only Super Admin can revoke store access!")
                return@launch
            }

            repository.revokeUserStorePermission(userId, storeId)

            val user = repository.getUserById(userId)
            if (user != null) {
                val currentList = user.getAssignedStoreIdsList().filter { it != storeId }
                val newCsv = currentList.joinToString(",")
                repository.saveUser(user.copy(assignedStoreIdsCsv = newCsv))
            }

            val deviceName = SecurityUtils.getDeviceName()
            logActivity("REVOKE_STORE_ACCESS", "Revoked access for user #$userId to store #$storeId on device $deviceName")
            onComplete(true, "User access to store revoked successfully.")
        }
    }

    fun saveStoreProfile(store: StoreProfile) {
        viewModelScope.launch {
            repository.saveStore(store)
            showToast("Store profile saved successfully!")
        }
    }

    fun saveShopLogo(context: Context, uri: Uri) {
        viewModelScope.launch {
            try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return@launch
                val activeId = _selectedStoreId.value
                val fileName = "shop_logo_${activeId}_${System.currentTimeMillis()}.png"
                val file = File(context.filesDir, fileName)
                val outputStream = FileOutputStream(file)
                inputStream.use { input ->
                    outputStream.use { output ->
                        input.copyTo(output)
                    }
                }
                val logoPath = file.absolutePath

                val currentSettings = repository.getSettingsSync()
                repository.saveSettings(currentSettings.copy(logoUri = logoPath))

                if (activeId > 0L) {
                    val currentStore = repository.getStoreById(activeId)
                    if (currentStore != null) {
                        repository.saveStore(currentStore.copy(logoUri = logoPath))
                    }
                }
                showToast("Shop Logo updated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Error updating logo: ${e.localizedMessage}")
            }
        }
    }

    fun removeShopLogo() {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsSync()
            repository.saveSettings(currentSettings.copy(logoUri = ""))
            val activeId = _selectedStoreId.value
            if (activeId > 0L) {
                val currentStore = repository.getStoreById(activeId)
                if (currentStore != null) {
                    repository.saveStore(currentStore.copy(logoUri = ""))
                }
            }
            showToast("Shop Logo removed.")
        }
    }

    // --- USER MANAGEMENT & AUTH STATE ---
    val allUsers: StateFlow<List<UserAccount>> = repository.allUsers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allActivityLogs: StateFlow<List<ActivityLog>> = repository.allActivityLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val superAdminRecovery: StateFlow<SuperAdminRecovery?> = repository.superAdminRecovery
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allDailyClosings: StateFlow<List<DailyClosing>> = combine(repository.allDailyClosings, _selectedStoreId) { closings, storeId ->
        if (storeId == 0L) closings else closings.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _pinInput = MutableStateFlow("")
    val pinInput: StateFlow<String> = _pinInput.asStateFlow()

    private val _pinError = MutableStateFlow<String?>(null)
    val pinError: StateFlow<String?> = _pinError.asStateFlow()

    private val _failedPinAttempts = MutableStateFlow(0)
    val failedPinAttempts: StateFlow<Int> = _failedPinAttempts.asStateFlow()

    private val _lockoutEndTime = MutableStateFlow(0L)
    val lockoutEndTime: StateFlow<Long> = _lockoutEndTime.asStateFlow()

    fun isAppLocked(): Boolean {
        return System.currentTimeMillis() < _lockoutEndTime.value
    }

    fun getRemainingLockoutSeconds(): Long {
        val diff = _lockoutEndTime.value - System.currentTimeMillis()
        return if (diff > 0) (diff / 1000) + 1 else 0L
    }

    private val prefs = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    // Attendance Widget State Persistence (Default: Minimized, Not Hidden)
    private val _isAttendanceMinimized = MutableStateFlow(
        prefs.getBoolean("is_attendance_minimized", true)
    )
    val isAttendanceMinimized: StateFlow<Boolean> = _isAttendanceMinimized.asStateFlow()

    private val _isAttendanceHidden = MutableStateFlow(
        prefs.getBoolean("is_attendance_hidden", false)
    )
    val isAttendanceHidden: StateFlow<Boolean> = _isAttendanceHidden.asStateFlow()

    fun toggleAttendanceMinimized() {
        val newValue = !_isAttendanceMinimized.value
        prefs.edit().putBoolean("is_attendance_minimized", newValue).apply()
        _isAttendanceMinimized.value = newValue
    }

    fun setAttendanceMinimized(minimized: Boolean) {
        prefs.edit().putBoolean("is_attendance_minimized", minimized).apply()
        _isAttendanceMinimized.value = minimized
    }

    fun setAttendanceHidden(hidden: Boolean) {
        prefs.edit().putBoolean("is_attendance_hidden", hidden).apply()
        _isAttendanceHidden.value = hidden
    }

    private val appActivationManager = AppActivationManager.getInstance(application)
    val syncManager = SyncManager.getInstance(application)
    val networkMonitor = NetworkConnectivityMonitor.getInstance(application)

    private val _isAppActivated = MutableStateFlow(appActivationManager.isActivated())
    val isAppActivated: StateFlow<Boolean> = _isAppActivated.asStateFlow()

    val activationStateFlow: StateFlow<String> = appActivationManager.activationStateFlow
    val connectionStatus: StateFlow<DetailedConnectionStatus> = networkMonitor.connectionStatus
    val syncState: StateFlow<SyncState> = syncManager.syncState
    val isSyncing: StateFlow<Boolean> = syncManager.isSyncing
    val pendingSyncCount: StateFlow<Int> = syncManager.pendingSyncCount

    fun refreshActivationState() {
        _isAppActivated.value = appActivationManager.isActivated()
    }

    fun pingServerNow() {
        viewModelScope.launch {
            networkMonitor.pingServer()
        }
    }

    fun triggerCloudSync(onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            val success = syncManager.performManualSync()
            onComplete?.invoke(success)
        }
    }

    fun checkConnectionAndSync() {
        viewModelScope.launch {
            networkMonitor.pingServer()
            syncManager.performManualSync()
        }
    }

    private val _isOnboardingCompleted = MutableStateFlow(
        prefs.getBoolean("is_onboarding_completed", false)
    )
    val isOnboardingCompleted: StateFlow<Boolean> = _isOnboardingCompleted.asStateFlow()

    fun markOnboardingCompleted() {
        prefs.edit().putBoolean("is_onboarding_completed", true).apply()
        _isOnboardingCompleted.value = true
        viewModelScope.launch {
            val currentSettings = repository.getSettingsSync()
            if (!currentSettings.isOnboardingCompleted) {
                repository.saveSettings(currentSettings.copy(isOnboardingCompleted = true))
            }
        }
    }

    fun completeBusinessOnboarding(
        businessName: String,
        ownerName: String,
        phone: String,
        whatsappNumber: String,
        address: String,
        businessType: String,
        currencySymbol: String,
        logoUri: String,
        adminName: String,
        adminUsername: String,
        adminPin: String,
        adminPhone: String
    ) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsSync()
            val cleanPin = adminPin.trim().ifBlank { "1234" }
            val newSettings = currentSettings.copy(
                storeName = businessName,
                ownerName = ownerName,
                phone = phone,
                whatsappNumber = whatsappNumber,
                address = address,
                businessType = businessType,
                currencySymbol = currencySymbol,
                logoUri = logoUri,
                pinCode = cleanPin,
                isOnboardingCompleted = true
            )
            repository.saveSettings(newSettings)

            prefs.edit().putBoolean("is_onboarding_completed", true).apply()
            _isOnboardingCompleted.value = true

            val mainStore = repository.getStoreById(1L)
            if (mainStore != null) {
                repository.saveStore(
                    mainStore.copy(
                        storeName = businessName,
                        ownerName = ownerName,
                        phone = phone,
                        address = address,
                        logoUri = logoUri
                    )
                )
            } else {
                repository.saveStore(
                    com.example.data.entity.StoreProfile(
                        id = 1L,
                        storeName = businessName,
                        ownerName = ownerName,
                        phone = phone,
                        address = address,
                        code = "MAIN",
                        logoUri = logoUri
                    )
                )
            }

            val superUser = UserAccount(
                id = 1L,
                username = adminUsername.ifBlank { "owner" },
                name = adminName.ifBlank { ownerName.ifBlank { "Super Admin Owner" } },
                pinCode = cleanPin,
                role = "SUPER_ADMIN",
                phone = adminPhone.ifBlank { phone }
            )
            repository.saveUser(superUser)

            _currentUser.value = superUser
            _isAuthenticated.value = true

            logActivity("BUSINESS_ONBOARDING_COMPLETED", "Business setup completed for $businessName")
            showToast("Welcome to $businessName! Business setup completed successfully.")
        }
    }

    fun factoryResetBusiness(adminPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                onResult(false, "Only Super Admin can reset the business!")
                return@launch
            }
            if (!verifyAdminPin(adminPin)) {
                onResult(false, "Invalid Super Admin PIN Code!")
                return@launch
            }

            repository.clearAllDataForReset()

            prefs.edit().putBoolean("is_onboarding_completed", false).apply()
            _isOnboardingCompleted.value = false

            _currentUser.value = null
            _isAuthenticated.value = false

            logActivity("FACTORY_RESET", "Business data reset by Super Admin")
            showToast("Factory Reset completed! Business data cleared.")
            onResult(true, "Business reset successfully.")
        }
    }

    // --- STORE SETTINGS ---
    val settings: StateFlow<StoreSettings> = repository.settings
        .combine(MutableStateFlow(Unit)) { s, _ -> s ?: StoreSettings() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StoreSettings())

    // --- CASHIER / OPERATOR PROFILE STATE ---
    val activeCashierProfile: StateFlow<CashierProfile> = combine(_currentUser, settings) { user, setts ->
        when {
            user != null && user.name.isNotBlank() -> CashierProfile(
                name = user.name,
                employeeId = if (user.id > 0) user.id.toString() else "",
                phone = user.phone,
                designation = if (user.role == "EMPLOYEE") "Cashier" else if (user.role == "ADMIN") "Store Admin" else user.role,
                userId = user.id
            )
            setts.defaultCashierName.isNotBlank() -> CashierProfile(
                name = setts.defaultCashierName,
                employeeId = setts.defaultCashierEmployeeId,
                phone = setts.defaultCashierPhone,
                designation = setts.defaultCashierDesignation.ifBlank { "Cashier" },
                userId = null
            )
            else -> CashierProfile(
                name = "Not Assigned",
                employeeId = "",
                phone = "",
                designation = "Cashier",
                userId = null
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CashierProfile(name = "Not Assigned"))

    fun getEffectiveCashierName(): String {
        val user = _currentUser.value
        if (user != null && user.name.isNotBlank()) return user.name
        val settCashier = settings.value.defaultCashierName
        if (settCashier.isNotBlank()) return settCashier
        return "Not Assigned"
    }

    fun getEffectiveCashierUserId(): Long? {
        val user = _currentUser.value
        if (user != null && user.id > 0) return user.id
        return null
    }

    fun updateCashierProfile(
        name: String,
        employeeId: String = "",
        phone: String = "",
        designation: String = "Cashier",
        onComplete: ((Boolean, String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val current = repository.getSettingsSync()
                val updated = current.copy(
                    defaultCashierName = name.trim(),
                    defaultCashierEmployeeId = employeeId.trim(),
                    defaultCashierPhone = phone.trim(),
                    defaultCashierDesignation = designation.trim().ifBlank { "Cashier" }
                )
                repository.saveSettings(updated)

                logActivity(
                    action = "CASHIER_PROFILE_UPDATE",
                    details = "Updated default cashier profile to '${name.trim()}' (${designation.trim()})"
                )

                showToast("Cashier profile saved successfully!")
                onComplete?.invoke(true, "Cashier profile saved successfully!")
            } catch (e: Exception) {
                showToast("Failed to save cashier profile: ${e.localizedMessage}")
                onComplete?.invoke(false, e.localizedMessage ?: "Unknown error")
            }
        }
    }

    fun setCashierFromEmployee(user: UserAccount, onComplete: ((Boolean, String) -> Unit)? = null) {
        updateCashierProfile(
            name = user.name,
            employeeId = if (user.id > 0) user.id.toString() else "",
            phone = user.phone,
            designation = if (user.role == "EMPLOYEE") "Cashier" else if (user.role == "ADMIN") "Store Admin" else user.role,
            onComplete = onComplete
        )
    }

    fun clearCashierProfile(onComplete: ((Boolean, String) -> Unit)? = null) {
        updateCashierProfile(
            name = "",
            employeeId = "",
            phone = "",
            designation = "Cashier",
            onComplete = onComplete
        )
    }

    // --- PRODUCTS / INVENTORY ---
    val allCategories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    val allProducts: StateFlow<List<Product>> = combine(repository.allProducts, _selectedStoreId) { products, storeId ->
        if (storeId == 0L) products else products.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lowStockProducts: StateFlow<List<Product>> = combine(allProducts, settings) { products, s ->
        products.filter { it.stockQuantity <= s.defaultLowStockThreshold }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val inventoryHealth: StateFlow<InventoryHealthSummary> = combine(lowStockProducts, settings) { items, s ->
        val critical = items.count { it.stockQuantity <= 0.0 }
        val breakdown = items.groupBy { it.category }.mapValues { it.value.size }
        InventoryHealthSummary(
            totalLowStockCount = items.size,
            criticalCount = critical,
            lowStockCategoryBreakdown = breakdown,
            thresholdValue = s.defaultLowStockThreshold,
            lastCheckTime = System.currentTimeMillis()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), InventoryHealthSummary())

    // --- POS / CART STATE ---
    private val _cart = MutableStateFlow<List<CartItem>>(emptyList())
    val cart: StateFlow<List<CartItem>> = _cart.asStateFlow()

    private val _selectedCustomer = MutableStateFlow<Customer?>(null)
    val selectedCustomer: StateFlow<Customer?> = _selectedCustomer.asStateFlow()

    private val _discountAmount = MutableStateFlow(0.0)
    val discountAmount: StateFlow<Double> = _discountAmount.asStateFlow()

    private val _paidAmountInput = MutableStateFlow("")
    val paidAmountInput: StateFlow<String> = _paidAmountInput.asStateFlow()

    private val _paymentType = MutableStateFlow("Cash") // Cash, Udhaar / Credit, Card
    val paymentType: StateFlow<String> = _paymentType.asStateFlow()

    private val _lastCompletedSale = MutableStateFlow<Sale?>(null)
    val lastCompletedSale: StateFlow<Sale?> = _lastCompletedSale.asStateFlow()

    private val _lastCompletedSaleItems = MutableStateFlow<List<SaleItem>>(emptyList())
    val lastCompletedSaleItems: StateFlow<List<SaleItem>> = _lastCompletedSaleItems.asStateFlow()

    private val _showInvoiceDialog = MutableStateFlow(false)
    val showInvoiceDialog: StateFlow<Boolean> = _showInvoiceDialog.asStateFlow()

    // --- PURCHASE ENTRY STATE ---
    private val _purchaseCart = MutableStateFlow<List<PurchaseCartItem>>(emptyList())
    val purchaseCart: StateFlow<List<PurchaseCartItem>> = _purchaseCart.asStateFlow()

    private val _selectedSupplier = MutableStateFlow<Supplier?>(null)
    val selectedSupplier: StateFlow<Supplier?> = _selectedSupplier.asStateFlow()

    private val _purchasePaidInput = MutableStateFlow("")
    val purchasePaidInput: StateFlow<String> = _purchasePaidInput.asStateFlow()

    // --- CUSTOMERS & SUPPLIERS ---
    val allCustomers: StateFlow<List<Customer>> = combine(repository.allCustomers, _selectedStoreId) { customers, storeId ->
        if (storeId == 0L) customers else customers.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSuppliers: StateFlow<List<Supplier>> = combine(repository.allSuppliers, _selectedStoreId) { suppliers, storeId ->
        if (storeId == 0L) suppliers else suppliers.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeCustomerForLedger = MutableStateFlow<Customer?>(null)
    val activeCustomerForLedger: StateFlow<Customer?> = _activeCustomerForLedger.asStateFlow()

    private val _activeCustomerLedgers = MutableStateFlow<List<CustomerLedger>>(emptyList())
    val activeCustomerLedgers: StateFlow<List<CustomerLedger>> = _activeCustomerLedgers.asStateFlow()

    private val _activeSupplierForLedger = MutableStateFlow<Supplier?>(null)
    val activeSupplierForLedger: StateFlow<Supplier?> = _activeSupplierForLedger.asStateFlow()

    private val _activeSupplierLedgers = MutableStateFlow<List<SupplierLedger>>(emptyList())
    val activeSupplierLedgers: StateFlow<List<SupplierLedger>> = _activeSupplierLedgers.asStateFlow()

    // --- SALES & PURCHASES HISTORY ---
    val allSales: StateFlow<List<Sale>> = combine(repository.allSales, _selectedStoreId) { sales, storeId ->
        if (storeId == 0L) sales else sales.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSaleItems: StateFlow<List<SaleItem>> = repository.allSaleItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allPurchases: StateFlow<List<Purchase>> = combine(repository.allPurchases, _selectedStoreId) { purchases, storeId ->
        if (storeId == 0L) purchases else purchases.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = combine(repository.allExpenses, _selectedStoreId) { expenses, storeId ->
        if (storeId == 0L) expenses else expenses.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecycleBinItems: StateFlow<List<RecycleBinItem>> = combine(repository.allRecycleBinItems, _selectedStoreId) { items, storeId ->
        if (storeId == 0L) items else items.filter { it.storeId == storeId }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addExpense(title: String, category: String, amount: Double, note: String) {
        viewModelScope.launch {
            val storeId = if (_selectedStoreId.value == 0L) 1L else _selectedStoreId.value
            repository.addExpense(Expense(storeId = storeId, title = title, category = category, amount = amount, note = note))
            showToast("Expense recorded successfully!")
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            showToast("Expense removed.")
        }
    }

    suspend fun getSaleItems(saleId: Long): List<SaleItem> {
        return repository.getSaleItemsSync(saleId)
    }

    // --- ANALYTICS & CHARTS DATA ---
    val salesTrend7Days: StateFlow<List<DaySalesPoint>> = allSales
        .combine(MutableStateFlow(Unit)) { sales, _ ->
            val cal = Calendar.getInstance()
            // Truncate to start of today
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)

            val daysList = mutableListOf<DaySalesPoint>()
            val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
            val dateFormat = SimpleDateFormat("dd MMM", Locale.getDefault())

            // Last 7 days including today (from day -6 to today)
            for (i in 6 downTo 0) {
                val dayCal = cal.clone() as Calendar
                dayCal.add(Calendar.DAY_OF_YEAR, -i)
                val dayStart = dayCal.timeInMillis
                val dayEnd = dayStart + (24 * 60 * 60 * 1000) - 1

                val salesForDay = sales.filter { it.timestamp in dayStart..dayEnd }
                val revenue = salesForDay.sumOf { it.netAmount }
                val volume = salesForDay.size

                val dayName = if (i == 0) "Today" else dayFormat.format(dayCal.time)
                val dateLabel = dateFormat.format(dayCal.time)

                daysList.add(
                    DaySalesPoint(
                        dayName = dayName,
                        dateLabel = dateLabel,
                        totalRevenue = revenue,
                        totalVolume = volume,
                        timestamp = dayStart
                    )
                )
            }
            daysList
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val topCategoriesData: StateFlow<List<CategorySalesPoint>> = combine(allSaleItems, allProducts) { items, products ->
        val productCategoryMap = products.associate { it.id to it.category }
        val categoryTotals = mutableMapOf<String, Double>()
        val categoryUnits = mutableMapOf<String, Double>()

        items.forEach { item ->
            val cat = productCategoryMap[item.productId] ?: "General"
            categoryTotals[cat] = (categoryTotals[cat] ?: 0.0) + item.totalPrice
            categoryUnits[cat] = (categoryUnits[cat] ?: 0.0) + item.quantity
        }

        val totalOverallSales = categoryTotals.values.sum().coerceAtLeast(1.0)
        categoryTotals.entries
            .map { (cat, amount) ->
                CategorySalesPoint(
                    categoryName = cat,
                    totalAmount = amount,
                    totalUnitsSold = categoryUnits[cat] ?: 0.0,
                    percentage = ((amount / totalOverallSales) * 100).toFloat()
                )
            }
            .sortedByDescending { it.totalAmount }
            .take(5)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Toast / Message Notice
    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureMasterAndDefaultTenantsExist()
            repository.seedSampleDataIfEmpty()
            val currentSettings = repository.getSettingsSync()
            val userCount = repository.getUserCountSync()

            if (prefs.getBoolean("is_onboarding_completed", false) || currentSettings.isOnboardingCompleted || userCount > 0) {
                markOnboardingCompleted()
            }

            if (!currentSettings.isPinEnabled) {
                _isAuthenticated.value = true
            }
        }
    }

    // --- MULTI-TENANT SAAS STATE & LICENSING ---
    val allTenants: StateFlow<List<TenantAccount>> = repository.allTenants
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSubscriptionPlans: StateFlow<List<SubscriptionPlan>> = repository.allSubscriptionPlans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _currentTenantId = MutableStateFlow("TENANT_DEFAULT")
    val currentTenantId: StateFlow<String> = _currentTenantId.asStateFlow()

    val currentTenant: StateFlow<TenantAccount?> = combine(allTenants, _currentTenantId) { list, id ->
        list.find { it.id == id } ?: list.find { it.id == "TENANT_DEFAULT" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isMasterOwner: StateFlow<Boolean> = combine(_currentUser, currentTenant) { user, tenant ->
        user?.role == "MASTER_OWNER" || tenant?.isMasterOwnerAccount == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isLicenseValid: StateFlow<Boolean> = combine(currentTenant, isMasterOwner) { tenant, isMaster ->
        if (isMaster) true
        else tenant?.isLicenseActive() ?: true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val licenseValidationState: StateFlow<LicenseValidationResult> = currentTenant.map { tenant ->
        LicenseValidator.validateTenantSync(tenant)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), LicenseValidationResult.NotFound("", "Initializing tenant state..."))

    fun validateLicenseKeyDirect(key: String, onResult: (LicenseValidationResult) -> Unit) {
        viewModelScope.launch {
            val result = LicenseValidator.validateLicenseKey(db.storeDao(), key)
            onResult(result)
        }
    }

    fun isFeatureAllowed(feature: AppFeature): Boolean {
        return LicenseValidator.isFeatureAllowed(currentTenant.value, feature)
    }

    fun checkShopLimit(currentShopCount: Int): FeatureLimitResult {
        return LicenseValidator.checkShopLimit(currentTenant.value, currentShopCount)
    }

    fun checkUserLimit(currentUserCount: Int): FeatureLimitResult {
        return LicenseValidator.checkUserLimit(currentTenant.value, currentUserCount)
    }

    fun isMasterOwnerUser(): Boolean = _currentUser.value?.role == "MASTER_OWNER" || currentTenant.value?.isMasterOwnerAccount == true

    fun switchTenant(tenantId: String) {
        viewModelScope.launch {
            val targetTenant = repository.getTenantById(tenantId)
            if (targetTenant != null) {
                _currentTenantId.value = tenantId
                logActivity("SAAS_SWITCH_TENANT", "Switched active tenant workspace to ${targetTenant.businessName} (${targetTenant.tenantCode})")
                showToast("Switched Tenant to: ${targetTenant.businessName}")
            } else {
                showToast("Tenant account not found.")
            }
        }
    }

    fun createCustomerTenant(
        tenantCode: String,
        businessName: String,
        ownerName: String,
        email: String,
        phone: String,
        planType: String,
        maxShops: Int,
        maxUsers: Int,
        customExpiryDays: Int? = null,
        onComplete: (Boolean, String, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val cleanCode = tenantCode.uppercase().trim().replace(" ", "_")
                val existing = repository.getTenantByCode(cleanCode)
                if (existing != null) {
                    onComplete(false, "Tenant code '${tenantCode}' is already taken! Use a unique code.", "")
                    return@launch
                }

                val newTenant = repository.createCustomerTenant(
                    tenantCode = cleanCode,
                    businessName = businessName,
                    ownerName = ownerName,
                    email = email,
                    phone = phone,
                    planType = planType,
                    maxShops = maxShops,
                    maxUsers = maxUsers,
                    customExpiryDays = customExpiryDays
                )

                logActivity("SAAS_CREATE_TENANT", "Created new customer tenant ${newTenant.businessName} with key ${newTenant.licenseKey}")
                onComplete(true, "Customer account '${businessName}' created successfully!", newTenant.licenseKey)
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Error creating tenant: ${e.localizedMessage}", "")
            }
        }
    }

    fun updateTenantStatus(tenantId: String, newStatus: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updated = tenant.copy(status = newStatus, lastSyncAt = System.currentTimeMillis())
                repository.saveTenant(updated)
                logActivity("SAAS_TENANT_STATUS", "Updated tenant ${tenant.businessName} status to $newStatus")
                showToast("Tenant status changed to $newStatus")
            }
        }
    }

    fun renewTenantLicense(tenantId: String, addDays: Int) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val baseTime = if (tenant.licenseExpiryDate > System.currentTimeMillis()) tenant.licenseExpiryDate else System.currentTimeMillis()
                val addMs = addDays * 24L * 60 * 60 * 1000
                val newExpiry = baseTime + addMs
                val updated = tenant.copy(
                    licenseExpiryDate = newExpiry,
                    status = "ACTIVE",
                    lastSyncAt = System.currentTimeMillis()
                )
                repository.saveTenant(updated)
                logActivity("SAAS_RENEW_LICENSE", "Extended tenant ${tenant.businessName} license by $addDays days")
                showToast("License extended by $addDays days for ${tenant.businessName}")
            }
        }
    }

    fun resetTenantLicense(tenantId: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val newKey = com.example.util.LicenseManager.generateLicenseKey(tenant.tenantCode, tenant.planType)
                val updated = tenant.copy(
                    licenseKey = newKey,
                    status = "ACTIVE",
                    lastSyncAt = System.currentTimeMillis()
                )
                repository.saveTenant(updated)
                logActivity("SAAS_RESET_LICENSE", "Generated fresh license key for ${tenant.businessName}")
                showToast("New License Key generated: $newKey")
            }
        }
    }

    fun deleteCustomerTenant(tenantId: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                repository.deleteTenant(tenantId)
                logActivity("SAAS_DELETE_TENANT", "Deleted customer tenant ${tenant.businessName}")
                showToast("Deleted tenant account ${tenant.businessName}")
                if (_currentTenantId.value == tenantId) {
                    _currentTenantId.value = "TENANT_DEFAULT"
                }
            }
        }
    }

    fun activateLicenseWithKey(licenseKey: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val cleanKey = licenseKey.trim().uppercase(Locale.ROOT)
            if (!com.example.util.LicenseManager.isValidLicenseFormat(cleanKey)) {
                onComplete(false, "Invalid License Key format or checksum failed!")
                return@launch
            }

            val matchingTenant = repository.getTenantByLicenseKey(cleanKey)
            if (matchingTenant != null) {
                val updated = matchingTenant.copy(
                    status = "ACTIVE",
                    licenseExpiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                    lastSyncAt = System.currentTimeMillis()
                )
                repository.saveTenant(updated)
                _currentTenantId.value = updated.id
                logActivity("LICENSE_ACTIVATED", "Activated commercial license key for ${updated.businessName}")
                onComplete(true, "License key activated successfully for ${updated.businessName}!")
            } else {
                val curr = currentTenant.value
                if (curr != null) {
                    val updated = curr.copy(
                        licenseKey = cleanKey,
                        status = "ACTIVE",
                        licenseExpiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000)
                    )
                    repository.saveTenant(updated)
                    onComplete(true, "Commercial License Key activated successfully!")
                } else {
                    onComplete(false, "License Key valid but tenant context not found.")
                }
            }
        }
    }

    fun startFreeTrial(trialDays: Int = 15, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val trialTenant = LicenseManager.createFreeTrialTenant(trialDays = trialDays)
                repository.saveTenant(trialTenant)
                _currentTenantId.value = trialTenant.id
                logActivity("SAAS_START_FREE_TRIAL", "Started $trialDays-day free trial for tenant ${trialTenant.tenantCode}")
                onComplete(true, "Free $trialDays-Day Trial Activated successfully!")
            } catch (e: Exception) {
                e.printStackTrace()
                onComplete(false, "Failed to start trial: ${e.localizedMessage}")
            }
        }
    }

    fun loginAsMasterDeveloper() {
        viewModelScope.launch {
            var masterTenant = repository.getTenantById("TENANT_MASTER_OWNER")
            if (masterTenant == null) {
                val created = LicenseManager.createDefaultMasterAccount()
                repository.saveTenant(created)
                masterTenant = created
            }
            _currentTenantId.value = masterTenant.id
            val devUser = UserAccount(
                id = 9999L,
                username = "developer",
                name = "Developer (App Owner)",
                pinCode = "9999",
                role = "MASTER_OWNER"
            )
            _currentUser.value = devUser
            _isAuthenticated.value = true
            showToast("Logged into Developer SaaS Management Portal")
        }
    }

    fun pushGlobalSystemNotice(notice: String) {
        viewModelScope.launch {
            val tenants = repository.allTenants.firstOrNull() ?: emptyList()
            tenants.forEach { t ->
                if (!t.isMasterOwnerAccount) {
                    repository.saveTenant(t.copy(systemNotice = notice))
                }
            }
            logActivity("SAAS_SYSTEM_BROADCAST", "Pushed system notice: $notice")
            showToast("System announcement pushed to all customer accounts!")
        }
    }

    fun setTenantPlan(tenantId: String, planType: String, durationDays: Int) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val newExpiry = System.currentTimeMillis() + (durationDays * 24L * 60 * 60 * 1000)
                val newKey = LicenseManager.generateLicenseKey(tenant.tenantCode, planType)
                val updated = tenant.copy(
                    planType = planType,
                    licenseExpiryDate = newExpiry,
                    licenseKey = newKey,
                    status = "ACTIVE",
                    paymentStatus = if (planType.startsWith("TRIAL")) "FREE_TRIAL" else "PAID",
                    lastSyncAt = System.currentTimeMillis()
                )
                repository.saveTenant(updated)
                logActivity("SAAS_SET_PLAN", "Set plan $planType ($durationDays days) for ${tenant.businessName}")
                showToast("Updated plan to $planType ($durationDays days) for ${tenant.businessName}")
            }
        }
    }

    fun updatePaymentStatus(tenantId: String, paymentStatus: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updated = tenant.copy(paymentStatus = paymentStatus, lastSyncAt = System.currentTimeMillis())
                repository.saveTenant(updated)
                logActivity("SAAS_PAYMENT_STATUS", "Updated payment status to $paymentStatus for ${tenant.businessName}")
                showToast("Payment status updated to $paymentStatus")
            }
        }
    }

    fun blockTenantCopy(tenantId: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updated = tenant.copy(isBlocked = true, status = "BLOCKED", lastSyncAt = System.currentTimeMillis())
                repository.saveTenant(updated)
                logActivity("SAAS_BLOCK_COPY", "Blocked stolen/illegal copy for ${tenant.businessName}")
                showToast("Blocked unauthorized copy for ${tenant.businessName}")
            }
        }
    }

    fun unblockTenantCopy(tenantId: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updated = tenant.copy(isBlocked = false, status = "ACTIVE", lastSyncAt = System.currentTimeMillis())
                repository.saveTenant(updated)
                logActivity("SAAS_UNBLOCK_COPY", "Unblocked tenant copy for ${tenant.businessName}")
                showToast("Unblocked license copy for ${tenant.businessName}")
            }
        }
    }

    fun forceLogoutTenantDevices(tenantId: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updated = tenant.copy(forceLogoutTimestamp = System.currentTimeMillis(), lastSyncAt = System.currentTimeMillis())
                repository.saveTenant(updated)
                logActivity("SAAS_FORCE_LOGOUT", "Forced logout on all devices for ${tenant.businessName}")
                showToast("Triggered Force Logout for ${tenant.businessName}")
            }
        }
    }

    fun resetTenantActivation(tenantId: String) {
        viewModelScope.launch {
            val tenant = repository.getTenantById(tenantId)
            if (tenant != null) {
                val updated = tenant.copy(boundInstallationId = "", lastSyncAt = System.currentTimeMillis())
                repository.saveTenant(updated)
                logActivity("SAAS_RESET_ACTIVATION", "Reset hardware device binding for ${tenant.businessName}")
                showToast("Device activation binding reset for ${tenant.businessName}")
            }
        }
    }

    fun clearToast() { _toastMessage.value = null }
    fun showToast(msg: String) { _toastMessage.value = msg }

    // --- AUTH PIN LOGIC ---
    fun onPinDigit(digit: String) {
        if (isAppLocked()) {
            val secs = getRemainingLockoutSeconds()
            _pinError.value = "App locked for 5 minutes due to 5 failed attempts! Try again in ${secs / 60}m ${secs % 60}s."
            return
        }
        if (_pinInput.value.length < 6) {
            _pinInput.value += digit
            _pinError.value = null
            if (_pinInput.value.length >= 4) {
                verifyPin()
            }
        }
    }

    fun onPinDelete() {
        if (_pinInput.value.isNotEmpty()) {
            _pinInput.value = _pinInput.value.dropLast(1)
            _pinError.value = null
        }
    }

    // --- USER ROLES & ACCESS CONTROL ---
    fun isSuperAdmin(): Boolean = _currentUser.value?.role == "SUPER_ADMIN"
    fun isAdmin(): Boolean = _currentUser.value?.role == "SUPER_ADMIN" || _currentUser.value?.role == "ADMIN"
    fun isEmployee(): Boolean = _currentUser.value?.role == "EMPLOYEE"

    fun canDelete(): Boolean = isAdmin()
    fun canViewProfitAndFinancials(): Boolean = isAdmin()
    fun canManageBackupAndSettings(): Boolean = isSuperAdmin()
    fun canManageUsers(): Boolean = isSuperAdmin()

    // --- ACTIVITY LOG HELPER ---
    fun logActivity(action: String, details: String = "") {
        viewModelScope.launch {
            val user = _currentUser.value
            val uId = user?.id ?: 0L
            val uName = user?.name ?: "Super Admin Owner"
            val uRole = user?.role ?: "SUPER_ADMIN"
            repository.logActivity(uId, uName, uRole, action, details)
        }
    }

    suspend fun detectUserByPin(pin: String): UserAccount? {
        if (pin.isBlank()) return null
        val matched = repository.getUserByPin(pin)
        if (matched != null) return matched
        val currentSettings = repository.getSettingsSync()
        if (pin == currentSettings.pinCode && pin.isNotBlank()) {
            return UserAccount(
                id = 1L,
                username = "superadmin",
                name = currentSettings.ownerName.ifBlank { "Super Admin Owner" },
                pinCode = currentSettings.pinCode,
                role = "SUPER_ADMIN",
                phone = currentSettings.phone
            )
        }
        return null
    }

    private fun verifyPin() {
        viewModelScope.launch {
            if (isAppLocked()) {
                val secs = getRemainingLockoutSeconds()
                _pinError.value = "App locked! Try again in ${secs / 60}m ${secs % 60}s."
                _pinInput.value = ""
                return@launch
            }

            val pin = _pinInput.value
            val matchedUser = repository.getUserByPin(pin)
            val currentSettings = repository.getSettingsSync()

            if (matchedUser != null) {
                _failedPinAttempts.value = 0
                _lockoutEndTime.value = 0L
                _currentUser.value = matchedUser
                _isAuthenticated.value = true

                val roleDisplay = when (matchedUser.role) {
                    "SUPER_ADMIN" -> "Super Admin"
                    "ADMIN" -> "Store Admin"
                    "EMPLOYEE" -> "Employee"
                    else -> matchedUser.role
                }

                val storesList = repository.allStores.firstOrNull() ?: emptyList()
                val allowedStoresForUser = if (matchedUser.role == "SUPER_ADMIN") {
                    storesList
                } else {
                    val assigned = matchedUser.getAssignedStoreIdsList()
                    storesList.filter { it.id in assigned }
                }

                val storeToSelect = if (matchedUser.lastSelectedStoreId > 0L && allowedStoresForUser.any { it.id == matchedUser.lastSelectedStoreId }) {
                    matchedUser.lastSelectedStoreId
                } else if (matchedUser.lastSelectedStoreId == 0L && matchedUser.role == "SUPER_ADMIN") {
                    0L
                } else {
                    allowedStoresForUser.firstOrNull()?.id ?: 1L
                }

                _selectedStoreId.value = storeToSelect

                if (allowedStoresForUser.size > 1 && matchedUser.role != "SUPER_ADMIN") {
                    _showStoreSelectionDialog.value = true
                }

                _pinError.value = null
                _pinInput.value = ""
                logActivity("USER_LOGIN", "User ${matchedUser.name} ($roleDisplay) logged in")
                showToast("Welcome back ${matchedUser.name}! Logged in as $roleDisplay.")
            } else if (pin == currentSettings.pinCode && pin.isNotBlank()) {
                _failedPinAttempts.value = 0
                _lockoutEndTime.value = 0L
                val superAdmin = UserAccount(
                    id = 1L,
                    username = "superadmin",
                    name = currentSettings.ownerName.ifBlank { "Super Admin Owner" },
                    pinCode = currentSettings.pinCode,
                    role = "SUPER_ADMIN",
                    phone = currentSettings.phone,
                    lastSelectedStoreId = _selectedStoreId.value
                )
                _currentUser.value = superAdmin
                _isAuthenticated.value = true
                _pinError.value = null
                _pinInput.value = ""
                logActivity("USER_LOGIN", "Super Admin logged in with primary PIN")
                showToast("Welcome back ${superAdmin.name}! Logged in as Super Admin.")
            } else {
                if (pin.length >= 4) {
                    val attempts = _failedPinAttempts.value + 1
                    _failedPinAttempts.value = attempts
                    if (attempts >= 5) {
                        _lockoutEndTime.value = System.currentTimeMillis() + (5 * 60 * 1000L)
                        _pinError.value = "App locked for 5 minutes due to 5 wrong PIN attempts!"
                    } else {
                        _pinError.value = "Incorrect PIN! Attempt $attempts of 5 before 5-minute lockout."
                    }
                    _pinInput.value = ""
                }
            }
        }
    }

    fun loginWithFingerprint() {
        viewModelScope.launch {
            if (isAppLocked()) {
                val secs = getRemainingLockoutSeconds()
                _pinError.value = "App locked! Try again in ${secs / 60}m ${secs % 60}s."
                return@launch
            }

            // Verify if there is an active detected user via PIN or primary user
            val users = repository.allUsers.firstOrNull() ?: emptyList()
            val currentPin = _pinInput.value
            val detected = if (currentPin.isNotBlank()) detectUserByPin(currentPin) else null

            if (detected != null && detected.role != "SUPER_ADMIN") {
                showToast("Fingerprint Login is available only for Super Admin.")
                _pinError.value = "Fingerprint Login is available only for Super Admin."
                return@launch
            }

            val superAdminUser = users.find { it.role == "SUPER_ADMIN" }
            if (superAdminUser != null) {
                _currentUser.value = superAdminUser
                _isAuthenticated.value = true
                _failedPinAttempts.value = 0
                _lockoutEndTime.value = 0L
                _pinError.value = null
                _pinInput.value = ""

                logActivity("FINGERPRINT_LOGIN", "Authenticated via Fingerprint as ${superAdminUser.name} (Super Admin)")
                showToast("Fingerprint Verified! Welcome back ${superAdminUser.name} (Super Admin).")
            } else {
                val currentSettings = repository.getSettingsSync()
                val superAdmin = UserAccount(
                    id = 1L,
                    username = "superadmin",
                    name = currentSettings.ownerName.ifBlank { "Super Admin Owner" },
                    pinCode = currentSettings.pinCode,
                    role = "SUPER_ADMIN",
                    phone = currentSettings.phone
                )
                _currentUser.value = superAdmin
                _isAuthenticated.value = true
                _failedPinAttempts.value = 0
                _lockoutEndTime.value = 0L
                _pinError.value = null
                _pinInput.value = ""

                logActivity("FINGERPRINT_LOGIN", "Authenticated via Fingerprint as Super Admin Owner")
                showToast("Fingerprint Verified! Welcome back ${superAdmin.name} (Super Admin).")
            }
        }
    }

    suspend fun verifyAdminPin(pin: String): Boolean {
        if (pin.isBlank()) return false
        val user = repository.getUserByPin(pin)
        val settingsPin = repository.getSettingsSync().pinCode
        if (user != null && (user.role == "SUPER_ADMIN" || user.role == "ADMIN")) {
            return true
        }
        return pin == settingsPin
    }

    fun updatePinCode(newPin: String) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                showToast("Permission Denied: Only Super Admin can change PIN/Password credentials.")
                return@launch
            }
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(pinCode = newPin, isPinEnabled = true))
            logActivity("UPDATE_PIN", "Master PIN Code was updated")
            showToast("PIN Code updated successfully!")
        }
    }

    fun togglePinSecurity(enabled: Boolean) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                showToast("Permission Denied: Only Super Admin can change PIN security settings.")
                return@launch
            }
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(isPinEnabled = enabled))
            showToast(if (enabled) "PIN Lock Enabled" else "PIN Lock Disabled")
        }
    }

    fun toggleBiometricSecurity(enabled: Boolean) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                showToast("Permission Denied: Only Super Admin can change biometric security settings.")
                return@launch
            }
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(isBiometricEnabled = enabled))
            showToast(if (enabled) "Biometric / Fingerprint Unlock Enabled" else "Biometric / Fingerprint Unlock Disabled")
        }
    }

    // --- SUPER ADMIN 3-LEVEL RECOVERY SYSTEM ---
    suspend fun getSuperAdminRecoverySync(): SuperAdminRecovery? {
        return repository.getSuperAdminRecoverySync()
    }

    suspend fun initOrGetSuperAdminRecoverySync(): SuperAdminRecovery {
        var rec = repository.getSuperAdminRecoverySync()
        if (rec == null || !rec.isConfigured || rec.passphraseWordsCsv.isBlank()) {
            val words = RecoveryUtils.generate12WordPassphrase()
            val code = RecoveryUtils.generate20CharEmergencyCode()
            val settings = repository.getSettingsSync()
            rec = SuperAdminRecovery(
                id = 1,
                passphraseWordsCsv = words,
                emergencyCode = code,
                recoveryEmail = settings.phone.ifBlank { "" },
                recoveryMobile = settings.phone,
                isConfigured = true,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveSuperAdminRecovery(rec)
            logActivity("SUPER_ADMIN_RECOVERY", "Initialized 3-Level Super Admin Recovery Credentials")
        }
        return rec
    }

    fun initSuperAdminRecoveryIfMissing() {
        viewModelScope.launch {
            initOrGetSuperAdminRecoverySync()
        }
    }

    fun regenerateRecoveryCredentials(currentPin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!verifyAdminPin(currentPin)) {
                onResult(false, "Invalid current PIN!")
                return@launch
            }

            val existing = repository.getSuperAdminRecoverySync() ?: SuperAdminRecovery()
            val newWords = RecoveryUtils.generate12WordPassphrase()
            val newCode = RecoveryUtils.generate20CharEmergencyCode()

            val updated = existing.copy(
                passphraseWordsCsv = newWords,
                emergencyCode = newCode,
                isConfigured = true,
                failedAttemptsCount = 0,
                lockoutEndTimeMs = 0L,
                updatedAt = System.currentTimeMillis()
            )
            repository.saveSuperAdminRecovery(updated)
            logActivity("SUPER_ADMIN_RECOVERY", "Super Admin regenerated 12-Word Passphrase and Emergency Code")
            onResult(true, "Recovery credentials regenerated successfully!")
        }
    }

    fun updateRecoveryContact(email: String, mobile: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val existing = initOrGetSuperAdminRecoverySync()
            val updated = existing.copy(
                recoveryEmail = email.trim(),
                recoveryMobile = mobile.trim(),
                updatedAt = System.currentTimeMillis()
            )
            repository.saveSuperAdminRecovery(updated)
            logActivity("SUPER_ADMIN_RECOVERY", "Updated Super Admin Recovery Email ($email) & Mobile ($mobile)")
            onResult(true, "Recovery contact information updated!")
        }
    }

    fun verifyPassphrase(inputWords: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val rec = initOrGetSuperAdminRecoverySync()
            if (rec.isLockedOut()) {
                val secs = rec.getRemainingLockoutSeconds()
                onResult(false, "Recovery Locked! 5 wrong attempts reached. Try again in ${secs / 60}m ${secs % 60}s.")
                return@launch
            }

            val enteredWords = RecoveryUtils.normalizePassphrase(inputWords)
            val storedWords = rec.getPassphraseWordsList()

            if (enteredWords.isNotEmpty() && enteredWords == storedWords) {
                val resetRec = rec.copy(failedAttemptsCount = 0, lockoutEndTimeMs = 0L)
                repository.saveSuperAdminRecovery(resetRec)
                logActivity("RECOVERY_VERIFIED", "Successfully verified via Level 1 (12-Word Passphrase)")
                onResult(true, "12-Word Passphrase verified!")
            } else {
                val newCount = rec.failedAttemptsCount + 1
                val isLocked = newCount >= 5
                val lockoutEnd = if (isLocked) System.currentTimeMillis() + (30 * 60 * 1000L) else rec.lockoutEndTimeMs
                val updatedRec = rec.copy(failedAttemptsCount = newCount, lockoutEndTimeMs = lockoutEnd)
                repository.saveSuperAdminRecovery(updatedRec)

                logActivity("RECOVERY_FAILED", "Failed passphrase attempt $newCount of 5")
                if (isLocked) {
                    onResult(false, "Invalid Passphrase! 5 consecutive failures reached. Recovery locked for 30 minutes.")
                } else {
                    onResult(false, "Invalid Passphrase! Attempt $newCount of 5.")
                }
            }
        }
    }

    fun verifyEmergencyCode(inputCode: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val rec = initOrGetSuperAdminRecoverySync()
            if (rec.isLockedOut()) {
                val secs = rec.getRemainingLockoutSeconds()
                onResult(false, "Recovery Locked! 5 wrong attempts reached. Try again in ${secs / 60}m ${secs % 60}s.")
                return@launch
            }

            val enteredCode = RecoveryUtils.normalizeEmergencyCode(inputCode)
            val storedCode = RecoveryUtils.normalizeEmergencyCode(rec.emergencyCode)

            if (enteredCode.isNotBlank() && enteredCode == storedCode) {
                val resetRec = rec.copy(failedAttemptsCount = 0, lockoutEndTimeMs = 0L)
                repository.saveSuperAdminRecovery(resetRec)
                logActivity("RECOVERY_VERIFIED", "Successfully verified via Level 2 (Emergency Code)")
                onResult(true, "Emergency Code verified!")
            } else {
                val newCount = rec.failedAttemptsCount + 1
                val isLocked = newCount >= 5
                val lockoutEnd = if (isLocked) System.currentTimeMillis() + (30 * 60 * 1000L) else rec.lockoutEndTimeMs
                val updatedRec = rec.copy(failedAttemptsCount = newCount, lockoutEndTimeMs = lockoutEnd)
                repository.saveSuperAdminRecovery(updatedRec)

                logActivity("RECOVERY_FAILED", "Failed emergency code attempt $newCount of 5")
                if (isLocked) {
                    onResult(false, "Invalid Emergency Code! 5 consecutive failures reached. Recovery locked for 30 minutes.")
                } else {
                    onResult(false, "Invalid Emergency Code! Attempt $newCount of 5.")
                }
            }
        }
    }

    fun requestRecoveryOtp(contactInput: String, isOnline: Boolean, onResult: (Boolean, String, String?) -> Unit) {
        viewModelScope.launch {
            val rec = initOrGetSuperAdminRecoverySync()
            if (rec.isLockedOut()) {
                val secs = rec.getRemainingLockoutSeconds()
                onResult(false, "Recovery Locked! 5 wrong attempts reached. Try again in ${secs / 60}m ${secs % 60}s.", null)
                return@launch
            }

            val cleanContact = contactInput.trim()
            if (cleanContact.isBlank()) {
                onResult(false, "Please enter registered Email or Mobile number.", null)
                return@launch
            }

            val otp = RecoveryUtils.generate6DigitOtp()
            logActivity("RECOVERY_OTP_SENT", "Requested recovery OTP for contact $cleanContact")
            onResult(true, "OTP code generated and sent to $cleanContact. OTP: $otp", otp)
        }
    }

    fun verifyRecoveryOtp(contactInput: String, otpInput: String, expectedOtp: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val rec = initOrGetSuperAdminRecoverySync()
            if (rec.isLockedOut()) {
                val secs = rec.getRemainingLockoutSeconds()
                onResult(false, "Recovery Locked! 5 wrong attempts reached. Try again in ${secs / 60}m ${secs % 60}s.")
                return@launch
            }

            if (otpInput.isNotBlank() && expectedOtp.isNotBlank() && otpInput.trim() == expectedOtp.trim()) {
                val resetRec = rec.copy(failedAttemptsCount = 0, lockoutEndTimeMs = 0L)
                repository.saveSuperAdminRecovery(resetRec)
                logActivity("RECOVERY_VERIFIED", "Successfully verified via Level 3 (OTP Email/Mobile)")
                onResult(true, "OTP Verified!")
            } else {
                val newCount = rec.failedAttemptsCount + 1
                val isLocked = newCount >= 5
                val lockoutEnd = if (isLocked) System.currentTimeMillis() + (30 * 60 * 1000L) else rec.lockoutEndTimeMs
                val updatedRec = rec.copy(failedAttemptsCount = newCount, lockoutEndTimeMs = lockoutEnd)
                repository.saveSuperAdminRecovery(updatedRec)

                logActivity("RECOVERY_FAILED", "Failed OTP attempt $newCount of 5")
                if (isLocked) {
                    onResult(false, "Invalid OTP! 5 consecutive failures reached. Recovery locked for 30 minutes.")
                } else {
                    onResult(false, "Invalid OTP! Attempt $newCount of 5.")
                }
            }
        }
    }

    fun resetSuperAdminPinWithRecovery(newPin: String, recoveryMethod: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (newPin.length != 4 || !newPin.all { it.isDigit() }) {
                onResult(false, "PIN must be a 4-digit number.")
                return@launch
            }

            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(pinCode = newPin, isPinEnabled = true))

            val users = repository.allUsers.firstOrNull() ?: emptyList()
            val superAdmin = users.find { it.role == "SUPER_ADMIN" }
            if (superAdmin != null) {
                repository.saveUser(superAdmin.copy(pinCode = newPin))
            }

            val rec = initOrGetSuperAdminRecoverySync()
            val resetRec = rec.copy(
                failedAttemptsCount = 0,
                lockoutEndTimeMs = 0L,
                lastRecoveryTimestamp = System.currentTimeMillis()
            )
            repository.saveSuperAdminRecovery(resetRec)

            val superAdminAccount = superAdmin ?: UserAccount(
                id = 1L,
                username = "superadmin",
                name = s.ownerName.ifBlank { "Super Admin Owner" },
                pinCode = newPin,
                role = "SUPER_ADMIN",
                phone = s.phone
            )

            _currentUser.value = superAdminAccount
            _isAuthenticated.value = true
            _failedPinAttempts.value = 0
            _lockoutEndTime.value = 0L
            _pinError.value = null
            _pinInput.value = ""

            logActivity("SUPER_ADMIN_PIN_RESET_RECOVERY", "Super Admin PIN reset via $recoveryMethod")
            onResult(true, "Super Admin PIN reset successfully!")
        }
    }

    fun logout() {
        viewModelScope.launch {
            val user = _currentUser.value
            logActivity("USER_LOGOUT", "User ${user?.name ?: ""} logged out")
            _currentUser.value = null
            _isAuthenticated.value = false
            _pinInput.value = ""
        }
    }

    // --- USER MANAGEMENT (SUPER ADMIN) ---
    fun saveUser(user: UserAccount, adminPin: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                onComplete(false, "Permission Denied: Only Super Admin can manage user accounts and PINs!")
                return@launch
            }
            if (!verifyAdminPin(adminPin)) {
                onComplete(false, "Invalid Super Admin PIN Code!")
                return@launch
            }
            val id = repository.saveUser(user)
            logActivity("SAVE_USER", "Saved user account: ${user.name} (${user.role})")
            onComplete(true, "User account saved successfully!")
        }
    }

    fun deleteUser(user: UserAccount, adminPin: String, onComplete: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!isSuperAdmin()) {
                onComplete(false, "Permission Denied: Only Super Admin can delete user accounts!")
                return@launch
            }
            if (user.role == "SUPER_ADMIN") {
                onComplete(false, "Cannot delete Super Admin account!")
                return@launch
            }
            if (!verifyAdminPin(adminPin)) {
                onComplete(false, "Invalid Super Admin PIN Code!")
                return@launch
            }
            repository.deleteUser(user)
            logActivity("DELETE_USER", "Deleted user account: ${user.name}")
            onComplete(true, "User deleted successfully.")
        }
    }

    // --- DAILY CLOSING SYSTEM ---
    fun submitDailyClosing(
        openingCash: Double,
        closingCash: Double,
        notes: String,
        onComplete: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            val user = _currentUser.value
            val userId = user?.id ?: 0L
            val userName = user?.name ?: "Unknown"
            val userRole = user?.role ?: "EMPLOYEE"

            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            val todayStart = cal.timeInMillis

            val todaySalesList = allSales.value.filter { it.timestamp >= todayStart }
            val totalSales = todaySalesList.sumOf { it.netAmount }
            val cashSales = todaySalesList.filter { it.paymentType.contains("Cash", ignoreCase = true) }.sumOf { it.paidAmount }
            val creditSales = todaySalesList.sumOf { it.dueAmount }

            val todayExpenses = allExpenses.value.filter { it.timestamp >= todayStart }.sumOf { it.amount }
            val todayPurchases = allPurchases.value.filter { it.timestamp >= todayStart }.sumOf { it.paidAmount }

            val expectedCash = openingCash + cashSales - todayExpenses - todayPurchases
            val diff = closingCash - expectedCash

            val closing = DailyClosing(
                userId = userId,
                userName = userName,
                userRole = userRole,
                openingCash = openingCash,
                closingCash = closingCash,
                totalSales = totalSales,
                cashSales = cashSales,
                creditSales = creditSales,
                totalExpenses = todayExpenses,
                totalPurchases = todayPurchases,
                returnedProductsAmount = 0.0,
                expectedCash = expectedCash,
                differenceAmount = diff,
                status = "CLOSED",
                notes = notes,
                openTimestamp = todayStart,
                closeTimestamp = System.currentTimeMillis()
            )

            repository.saveDailyClosing(closing)
            logActivity("DAILY_CLOSING", "Daily closing submitted for $userName (Diff: Rs. ${diff.toInt()})")
            onComplete(true, "Daily Closing recorded successfully!")
        }
    }

    // --- INVENTORY MANAGEMENT & BATCH IMPORT ---
    private val _parsedBatchItems = MutableStateFlow<List<BatchProductImportItem>>(emptyList())
    val parsedBatchItems: StateFlow<List<BatchProductImportItem>> = _parsedBatchItems.asStateFlow()

    private val _isParsingBatch = MutableStateFlow(false)
    val isParsingBatch: StateFlow<Boolean> = _isParsingBatch.asStateFlow()

    private val _isImportingBatch = MutableStateFlow(false)
    val isImportingBatch: StateFlow<Boolean> = _isImportingBatch.asStateFlow()

    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedCategory(cat: String) { _selectedCategory.value = cat }

    fun parseBatchImportText(text: String) {
        viewModelScope.launch {
            _isParsingBatch.value = true
            val parsedRaw = BatchImportParser.parseInputText(text)
            val enriched = repository.checkImportItemsStatus(parsedRaw)
            _parsedBatchItems.value = enriched
            _isParsingBatch.value = false
        }
    }

    fun clearBatchParsedItems() {
        _parsedBatchItems.value = emptyList()
    }

    fun executeBatchImport(
        items: List<BatchProductImportItem>,
        addToExistingStock: Boolean,
        updatePrices: Boolean,
        onComplete: (BatchImportResult) -> Unit
    ) {
        viewModelScope.launch {
            _isImportingBatch.value = true
            val result = repository.batchImportProducts(items, addToExistingStock, updatePrices)
            _isImportingBatch.value = false
            _parsedBatchItems.value = emptyList()
            showToast("Batch Import Complete: ${result.addedCount} new items added, ${result.updatedCount} stock items updated.")
            onComplete(result)
        }
    }

    fun saveProduct(product: Product) {
        viewModelScope.launch {
            repository.insertOrUpdateProduct(product)
            showToast("Product '${product.name}' saved successfully!")
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            val json = JSONObject().apply {
                put("id", product.id)
                put("tenantId", product.tenantId)
                put("storeId", product.storeId)
                put("barcode", product.barcode)
                put("name", product.name)
                put("category", product.category)
                put("purchasePrice", product.purchasePrice)
                put("salePrice", product.salePrice)
                put("stockQuantity", product.stockQuantity)
                put("minStockLevel", product.minStockLevel)
                put("unit", product.unit)
            }.toString()

            repository.moveToRecycleBin(
                RecycleBinItem(
                    tenantId = product.tenantId,
                    storeId = product.storeId,
                    itemType = "PRODUCT",
                    originalId = product.id,
                    title = product.name,
                    subtitle = "Category: ${product.category} | Stock: ${product.stockQuantity} ${product.unit}",
                    jsonData = json,
                    deletedBy = _currentUser.value?.name ?: "User"
                )
            )
            repository.deleteProduct(product)
            logActivity("DELETE_PRODUCT", "Moved product '${product.name}' to Recycle Bin")
            showToast("Product '${product.name}' moved to Recycle Bin.")
        }
    }

    // --- POS / SALES CART LOGIC ---
    fun addToCart(product: Product, qty: Double = 1.0) {
        val currentList = _cart.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val existing = currentList[existingIndex]
            currentList[existingIndex] = existing.copy(quantity = existing.quantity + qty)
        } else {
            currentList.add(CartItem(product = product, quantity = qty))
        }
        _cart.value = currentList
    }

    fun updateCartQty(productId: Long, newQty: Double) {
        if (newQty <= 0) {
            removeFromCart(productId)
            return
        }
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(quantity = newQty)
            _cart.value = currentList
        }
    }

    fun updateCartPrice(productId: Long, newPrice: Double) {
        val currentList = _cart.value.toMutableList()
        val index = currentList.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            currentList[index] = currentList[index].copy(customPrice = newPrice)
            _cart.value = currentList
        }
    }

    fun removeFromCart(productId: Long) {
        _cart.value = _cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        _cart.value = emptyList()
        _selectedCustomer.value = null
        _discountAmount.value = 0.0
        _paidAmountInput.value = ""
        _paymentType.value = "Cash"
    }

    fun setSelectedCustomer(customer: Customer?) { _selectedCustomer.value = customer }
    fun setDiscountAmount(disc: Double) { _discountAmount.value = disc }
    fun setPaidAmountInput(amt: String) { _paidAmountInput.value = amt }
    fun setPaymentType(type: String) { _paymentType.value = type }

    fun processSale() {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) {
            showToast("Cart is empty! Add products first.")
            return
        }

        viewModelScope.launch {
            val subtotal = cartItems.sumOf { it.total }
            val netTotal = (subtotal - _discountAmount.value).coerceAtLeast(0.0)
            val paid = _paidAmountInput.value.toDoubleOrNull() ?: netTotal
            val due = (netTotal - paid).coerceAtLeast(0.0)

            val invNum = "INV-" + (System.currentTimeMillis() / 1000).toString().takeLast(6)
            val customer = _selectedCustomer.value
            val currentCashierName = getEffectiveCashierName()
            val currentCashierId = getEffectiveCashierUserId()

            val activeStoreId = if (_selectedStoreId.value == 0L) 1L else _selectedStoreId.value
            val sale = Sale(
                storeId = activeStoreId,
                invoiceNumber = invNum,
                customerId = customer?.id,
                customerName = customer?.name ?: "Cash Customer",
                totalAmount = subtotal,
                discount = _discountAmount.value,
                netAmount = netTotal,
                paidAmount = paid,
                dueAmount = due,
                paymentType = _paymentType.value,
                cashierName = currentCashierName,
                cashierId = currentCashierId,
                timestamp = System.currentTimeMillis()
            )

            val saleItems = cartItems.map { item ->
                SaleItem(
                    saleId = 0,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unit = item.product.unit,
                    purchasePrice = item.product.purchasePrice,
                    salePrice = item.customPrice,
                    totalPrice = item.total
                )
            }

            val saleId = repository.completeSale(sale, saleItems)
            val completedSale = sale.copy(id = saleId)

            _lastCompletedSale.value = completedSale
            _lastCompletedSaleItems.value = saleItems
            _showInvoiceDialog.value = true

            clearCart()
            showToast("Sale completed successfully! Invoice #$invNum generated.")
        }
    }

    fun processQuickSale(customPaidAmount: Double? = null, customPaymentType: String = "Cash") {
        val cartItems = _cart.value
        if (cartItems.isEmpty()) {
            showToast("Cart is empty! Add products first.")
            return
        }

        viewModelScope.launch {
            val subtotal = cartItems.sumOf { it.total }
            val netTotal = (subtotal - _discountAmount.value).coerceAtLeast(0.0)
            val paid = customPaidAmount ?: netTotal
            val due = (netTotal - paid).coerceAtLeast(0.0)
            val change = if (paid > netTotal) paid - netTotal else 0.0

            val invNum = "INV-" + (System.currentTimeMillis() / 1000).toString().takeLast(6)
            val customer = _selectedCustomer.value
            val currentCashierName = getEffectiveCashierName()
            val currentCashierId = getEffectiveCashierUserId()

            val activeStoreId = if (_selectedStoreId.value == 0L) 1L else _selectedStoreId.value
            val sale = Sale(
                storeId = activeStoreId,
                invoiceNumber = invNum,
                customerId = customer?.id,
                customerName = customer?.name ?: "Cash Customer",
                totalAmount = subtotal,
                discount = _discountAmount.value,
                netAmount = netTotal,
                paidAmount = paid,
                dueAmount = due,
                paymentType = customPaymentType,
                cashierName = currentCashierName,
                cashierId = currentCashierId,
                timestamp = System.currentTimeMillis()
            )

            val saleItems = cartItems.map { item ->
                SaleItem(
                    saleId = 0,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    unit = item.product.unit,
                    purchasePrice = item.product.purchasePrice,
                    salePrice = item.customPrice,
                    totalPrice = item.total
                )
            }

            val saleId = repository.completeSale(sale, saleItems)
            val completedSale = sale.copy(id = saleId)

            _lastCompletedSale.value = completedSale
            _lastCompletedSaleItems.value = saleItems
            _showInvoiceDialog.value = true

            clearCart()
            val changeMsg = if (change > 0) " • Change Due: ${settings.value.currencySymbol}${change.toInt()}" else ""
            showToast("⚡ Quick Sale Complete! Invoice #$invNum$changeMsg")
        }
    }

    fun dismissInvoiceDialog() { _showInvoiceDialog.value = false }

    fun viewInvoice(sale: Sale) {
        viewModelScope.launch {
            val items = repository.getSaleItemsSync(sale.id)
            _lastCompletedSale.value = sale
            _lastCompletedSaleItems.value = items
            _showInvoiceDialog.value = true
        }
    }

    fun deleteInvoice(sale: Sale, onResult: ((Boolean) -> Unit)? = null) {
        if (!isAdmin()) {
            showToast("Only Super Admin can manage this feature.")
            onResult?.invoke(false)
            return
        }

        viewModelScope.launch {
            try {
                val success = repository.deleteSale(sale)
                if (success) {
                    if (_lastCompletedSale.value?.id == sale.id) {
                        _lastCompletedSale.value = null
                        _lastCompletedSaleItems.value = emptyList()
                        _showInvoiceDialog.value = false
                    }
                    logActivity("DELETE_INVOICE", "Deleted Invoice #${sale.invoiceNumber} (${sale.customerName})")
                    showToast("Invoice deleted successfully.")
                    onResult?.invoke(true)
                } else {
                    showToast("Unable to delete invoice. Please try again.")
                    onResult?.invoke(false)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                showToast("Unable to delete invoice. Please try again.")
                onResult?.invoke(false)
            }
        }
    }

    // --- PURCHASE ENTRY LOGIC ---
    fun addToPurchaseCart(product: Product, qty: Double = 1.0, cost: Double = product.purchasePrice) {
        val list = _purchaseCart.value.toMutableList()
        val index = list.indexOfFirst { it.product.id == product.id }
        if (index >= 0) {
            list[index] = list[index].copy(quantity = list[index].quantity + qty)
        } else {
            list.add(PurchaseCartItem(product = product, quantity = qty, costPrice = cost))
        }
        _purchaseCart.value = list
    }

    fun updatePurchaseCartQty(productId: Long, newQty: Double) {
        if (newQty <= 0) {
            _purchaseCart.value = _purchaseCart.value.filterNot { it.product.id == productId }
            return
        }
        val list = _purchaseCart.value.toMutableList()
        val idx = list.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(quantity = newQty)
            _purchaseCart.value = list
        }
    }

    fun updatePurchaseCartCost(productId: Long, newCost: Double) {
        val list = _purchaseCart.value.toMutableList()
        val idx = list.indexOfFirst { it.product.id == productId }
        if (idx >= 0) {
            list[idx] = list[idx].copy(costPrice = newCost)
            _purchaseCart.value = list
        }
    }

    fun removeFromPurchaseCart(productId: Long) {
        _purchaseCart.value = _purchaseCart.value.filterNot { it.product.id == productId }
    }

    fun clearPurchaseCart() {
        _purchaseCart.value = emptyList()
        _selectedSupplier.value = null
        _purchasePaidInput.value = ""
    }

    fun setSelectedSupplier(supplier: Supplier?) { _selectedSupplier.value = supplier }
    fun setPurchasePaidInput(amt: String) { _purchasePaidInput.value = amt }

    fun processPurchase() {
        val items = _purchaseCart.value
        if (items.isEmpty()) {
            showToast("Purchase cart is empty!")
            return
        }

        viewModelScope.launch {
            val total = items.sumOf { it.total }
            val paid = _purchasePaidInput.value.toDoubleOrNull() ?: total
            val due = (total - paid).coerceAtLeast(0.0)
            val purNum = "PUR-" + (System.currentTimeMillis() / 1000).toString().takeLast(6)
            val supplier = _selectedSupplier.value

            val purchase = Purchase(
                purchaseNumber = purNum,
                supplierId = supplier?.id,
                supplierName = supplier?.name ?: "General Vendor",
                totalAmount = total,
                paidAmount = paid,
                dueAmount = due,
                timestamp = System.currentTimeMillis()
            )

            val pItems = items.map { item ->
                PurchaseItem(
                    purchaseId = 0,
                    productId = item.product.id,
                    productName = item.product.name,
                    quantity = item.quantity,
                    costPrice = item.costPrice,
                    totalPrice = item.total
                )
            }

            repository.completePurchase(purchase, pItems)
            clearPurchaseCart()
            showToast("Purchase entry #$purNum recorded & inventory updated!")
        }
    }

    // --- CUSTOMER LEDGER ---
    fun saveCustomer(customer: Customer) {
        viewModelScope.launch {
            repository.saveCustomer(customer)
            showToast("Customer '${customer.name}' saved!")
        }
    }

    fun deleteCustomer(customer: Customer) {
        viewModelScope.launch {
            val json = JSONObject().apply {
                put("id", customer.id)
                put("tenantId", customer.tenantId)
                put("storeId", customer.storeId)
                put("name", customer.name)
                put("phone", customer.phone)
                put("address", customer.address)
                put("balance", customer.balance)
            }.toString()

            repository.moveToRecycleBin(
                RecycleBinItem(
                    tenantId = customer.tenantId,
                    storeId = customer.storeId,
                    itemType = "CUSTOMER",
                    originalId = customer.id,
                    title = customer.name,
                    subtitle = "Phone: ${customer.phone} | Udhaar Balance: Rs.${customer.balance.toInt()}",
                    jsonData = json,
                    deletedBy = _currentUser.value?.name ?: "User"
                )
            )
            repository.deleteCustomer(customer)
            logActivity("DELETE_CUSTOMER", "Moved customer '${customer.name}' to Recycle Bin")
            showToast("Customer '${customer.name}' moved to Recycle Bin.")
        }
    }

    fun loadCustomerLedger(customer: Customer) {
        _activeCustomerForLedger.value = customer
        viewModelScope.launch {
            repository.getCustomerLedgers(customer.id).collect {
                _activeCustomerLedgers.value = it
            }
        }
    }

    fun clearCustomerLedger() {
        _activeCustomerForLedger.value = null
        _activeCustomerLedgers.value = emptyList()
    }

    fun recordCustomerPayment(customerId: Long, amount: Double, notes: String, paymentMethod: String = "Cash") {
        viewModelScope.launch {
            repository.recordCustomerPayment(customerId, amount, notes, paymentMethod)
            val updated = allCustomers.value.find { it.id == customerId }
            if (updated != null) _activeCustomerForLedger.value = updated
            showToast("Payment of Rs. ${amount.toInt()} ($paymentMethod) recorded!")
        }
    }

    fun addCustomerDebitAdjustment(customerId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.addCustomerDebitAdjustment(customerId, amount, notes)
            val updated = allCustomers.value.find { it.id == customerId }
            if (updated != null) _activeCustomerForLedger.value = updated
            showToast("Added Udhaar charge of Rs. ${amount.toInt()}!")
        }
    }

    fun exportCustomerLedgerPdf(context: android.content.Context, customer: Customer, action: String = "open") {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val ledgers = activeCustomerLedgers.value
            val file = com.example.util.PdfGenerator.generateCustomerLedgerPdf(context, customer, ledgers, s)
            if (file != null) {
                if (action == "whatsapp") {
                    val waMsg = buildString {
                        appendLine("📋 *CUSTOMER ACCOUNT STATEMENT*")
                        appendLine("🏪 *${s.storeName}*")
                        appendLine("--------------------------")
                        appendLine("Customer: *${customer.name}*")
                        if (customer.phone.isNotBlank()) appendLine("Phone: ${customer.phone}")
                        appendLine("Current Pending Udhaar: *${s.currencySymbol} ${customer.balance.toInt()}*")
                        appendLine("Total Ledger Transactions: ${ledgers.size}")
                        appendLine("--------------------------")
                        appendLine("Please find your detailed account PDF statement attached.")
                        appendLine("Thank you!")
                    }
                    com.example.util.PdfGenerator.shareToWhatsApp(context, file, waMsg, customer.phone)
                } else if (action == "print") {
                    com.example.util.PdfGenerator.printPdf(context, file)
                } else {
                    com.example.util.PdfGenerator.openOrSharePdf(context, file, action)
                }
            } else {
                showToast("Failed to generate Customer Ledger PDF")
            }
        }
    }

    // --- SUPPLIER LEDGER ---
    fun saveSupplier(supplier: Supplier) {
        viewModelScope.launch {
            repository.saveSupplier(supplier)
            showToast("Supplier '${supplier.name}' saved!")
        }
    }

    fun deleteSupplier(supplier: Supplier) {
        viewModelScope.launch {
            val json = JSONObject().apply {
                put("id", supplier.id)
                put("tenantId", supplier.tenantId)
                put("storeId", supplier.storeId)
                put("name", supplier.name)
                put("company", supplier.company)
                put("phone", supplier.phone)
                put("address", supplier.address)
                put("payableBalance", supplier.payableBalance)
            }.toString()

            repository.moveToRecycleBin(
                RecycleBinItem(
                    tenantId = supplier.tenantId,
                    storeId = supplier.storeId,
                    itemType = "SUPPLIER",
                    originalId = supplier.id,
                    title = supplier.name,
                    subtitle = "Company: ${supplier.company} | Payable Balance: Rs.${supplier.payableBalance.toInt()}",
                    jsonData = json,
                    deletedBy = _currentUser.value?.name ?: "User"
                )
            )
            repository.deleteSupplier(supplier)
            logActivity("DELETE_SUPPLIER", "Moved supplier '${supplier.name}' to Recycle Bin")
            showToast("Supplier '${supplier.name}' moved to Recycle Bin.")
        }
    }

    fun loadSupplierLedger(supplier: Supplier) {
        _activeSupplierForLedger.value = supplier
        viewModelScope.launch {
            repository.getSupplierLedgers(supplier.id).collect {
                _activeSupplierLedgers.value = it
            }
        }
    }

    fun clearSupplierLedger() {
        _activeSupplierForLedger.value = null
        _activeSupplierLedgers.value = emptyList()
    }

    fun recordSupplierPayment(supplierId: Long, amount: Double, notes: String, paymentMethod: String = "Cash") {
        viewModelScope.launch {
            repository.recordSupplierPayment(supplierId, amount, notes, paymentMethod)
            val updated = allSuppliers.value.find { it.id == supplierId }
            if (updated != null) _activeSupplierForLedger.value = updated
            showToast("Payment of Rs. ${amount.toInt()} ($paymentMethod) made to supplier!")
        }
    }

    fun addSupplierCreditAdjustment(supplierId: Long, amount: Double, notes: String) {
        viewModelScope.launch {
            repository.addSupplierCreditAdjustment(supplierId, amount, notes)
            val updated = allSuppliers.value.find { it.id == supplierId }
            if (updated != null) _activeSupplierForLedger.value = updated
            showToast("Added payable adjustment of Rs. ${amount.toInt()}!")
        }
    }

    fun exportSupplierLedgerPdf(context: android.content.Context, supplier: Supplier, action: String = "open") {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val ledgers = activeSupplierLedgers.value
            val file = com.example.util.PdfGenerator.generateSupplierLedgerPdf(context, supplier, ledgers, s)
            if (file != null) {
                if (action == "whatsapp") {
                    val waMsg = buildString {
                        appendLine("📦 *SUPPLIER ACCOUNT STATEMENT*")
                        appendLine("🏪 *${s.storeName}*")
                        appendLine("--------------------------")
                        appendLine("Supplier: *${supplier.name}*")
                        if (supplier.phone.isNotBlank()) appendLine("Phone: ${supplier.phone}")
                        appendLine("Current Payable Balance: *${s.currencySymbol} ${supplier.payableBalance.toInt()}*")
                        appendLine("Total Transactions: ${ledgers.size}")
                        appendLine("--------------------------")
                        appendLine("Please find the attached supplier account statement PDF.")
                        appendLine("Thank you!")
                    }
                    com.example.util.PdfGenerator.shareToWhatsApp(context, file, waMsg, supplier.phone)
                } else if (action == "print") {
                    com.example.util.PdfGenerator.printPdf(context, file)
                } else {
                    com.example.util.PdfGenerator.openOrSharePdf(context, file, action)
                }
            } else {
                showToast("Failed to generate Supplier Ledger PDF")
            }
        }
    }

    fun exportSalesReportPdf(
        context: android.content.Context,
        salesList: List<Sale> = allSales.value,
        title: String = "SALES & FINANCIAL ACCOUNTING REPORT",
        action: String = "open"
    ) {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val allItems = allSaleItems.value
            val expenses = allExpenses.value
            val file = com.example.util.PdfGenerator.generateSalesReportPdf(context, salesList, allItems, expenses, s, title)
            if (file != null) {
                if (action == "whatsapp") {
                    val msg = "📊 *Sales & Accounting PDF Report*\nStore: *${s.storeName}*\nTotal Transactions: ${salesList.size}\nTotal Net Sales: ${s.currencySymbol} ${salesList.sumOf { it.netAmount }.toInt()}\nPlease find attached PDF report."
                    com.example.util.PdfGenerator.shareToWhatsApp(context, file, msg)
                } else if (action == "print") {
                    com.example.util.PdfGenerator.printPdf(context, file)
                } else {
                    com.example.util.PdfGenerator.openOrSharePdf(context, file, action)
                }
            } else {
                showToast("Failed to generate Sales Report PDF")
            }
        }
    }

    fun exportInventoryReportPdf(
        context: android.content.Context,
        action: String = "open"
    ) {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val products = allProducts.value
            val file = com.example.util.PdfGenerator.generateInventoryReportPdf(context, products, s)
            if (file != null) {
                if (action == "whatsapp") {
                    val totalUnits = products.sumOf { it.stockQuantity.toInt() }
                    val totalVal = products.sumOf { it.stockQuantity * it.purchasePrice }.toInt()
                    val msg = "📦 *Inventory Valuation PDF Report*\nStore: *${s.storeName}*\nTotal Catalog Items: ${products.size}\nTotal Physical Units: $totalUnits\nStock Cost Valuation: ${s.currencySymbol} $totalVal\nPlease find attached PDF report."
                    com.example.util.PdfGenerator.shareToWhatsApp(context, file, msg)
                } else if (action == "print") {
                    com.example.util.PdfGenerator.printPdf(context, file)
                } else {
                    com.example.util.PdfGenerator.openOrSharePdf(context, file, action)
                }
            } else {
                showToast("Failed to generate Inventory Report PDF")
            }
        }
    }

    // --- DASHBOARD WIDGET REORDERING ---
    val defaultWidgetOrder = listOf(
        "QUICK_SALE",
        "ANALYTICS_CHARTS",
        "LOW_STOCK",
        "TODAY_SALES_KPI",
        "STORE_OPERATIONS",
        "RECENT_TRANSACTIONS"
    )

    fun getDashboardWidgetOrder(): List<String> {
        val saved = settings.value.dashboardWidgetOrder
        if (saved.isBlank()) return defaultWidgetOrder
        val list = saved.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val missing = defaultWidgetOrder.filter { !list.contains(it) }
        return list + missing
    }

    fun updateDashboardWidgetOrder(newOrder: List<String>) {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(dashboardWidgetOrder = newOrder.joinToString(",")))
            showToast("Dashboard layout updated!")
        }
    }

    fun resetDashboardWidgetOrder() {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            repository.saveSettings(s.copy(dashboardWidgetOrder = defaultWidgetOrder.joinToString(",")))
            showToast("Reset dashboard layout to default!")
        }
    }

    // --- SETTINGS STORE INFO & THRESHOLDS ---
    fun updateThemeMode(themeMode: String) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsSync()
            repository.saveSettings(currentSettings.copy(themeMode = themeMode))
            val label = when (themeMode) {
                "LIGHT" -> "Light Mode"
                "DARK" -> "Dark Mode"
                else -> "System Default Theme"
            }
            showToast("Theme changed to $label")
        }
    }

    fun updateAccentColor(accentColor: String) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsSync()
            repository.saveSettings(currentSettings.copy(accentColor = accentColor))
            showToast("Accent color updated!")
        }
    }

    fun updateStoreInfo(
        name: String,
        owner: String,
        phone: String,
        whatsappNumber: String = "",
        address: String,
        businessType: String = "",
        currency: String,
        logoUri: String = "",
        lowStockThreshold: Double = 5.0
    ) {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val updated = s.copy(
                storeName = name,
                ownerName = owner,
                phone = phone,
                whatsappNumber = whatsappNumber.ifBlank { phone },
                address = address,
                businessType = businessType,
                currencySymbol = currency,
                logoUri = logoUri,
                defaultLowStockThreshold = lowStockThreshold
            )
            repository.saveSettings(updated)

            val activeStoreProfile = repository.getStoreById(s.activeStoreId)
            if (activeStoreProfile != null) {
                repository.saveStore(
                    activeStoreProfile.copy(
                        storeName = name,
                        ownerName = owner,
                        phone = phone,
                        whatsappNumber = whatsappNumber.ifBlank { phone },
                        address = address,
                        businessType = businessType,
                        logoUri = logoUri
                    )
                )
            }
            showToast("Business profile updated successfully!")
        }
    }

    fun updateFeatureToggles(
        isBarcodeEnabled: Boolean,
        isQrCodeEnabled: Boolean,
        isWhatsappInvoiceEnabled: Boolean,
        isThermalPrinterEnabled: Boolean,
        isCreditSaleEnabled: Boolean,
        isPurchaseReturnEnabled: Boolean,
        isSalesReturnEnabled: Boolean,
        isExpenseModuleEnabled: Boolean,
        isCashBookEnabled: Boolean,
        isAttendanceEnabled: Boolean,
        isEmployeeCommissionEnabled: Boolean
    ) {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val updated = s.copy(
                isBarcodeEnabled = isBarcodeEnabled,
                isQrCodeEnabled = isQrCodeEnabled,
                isWhatsappInvoiceEnabled = isWhatsappInvoiceEnabled,
                isThermalPrinterEnabled = isThermalPrinterEnabled,
                isCreditSaleEnabled = isCreditSaleEnabled,
                isPurchaseReturnEnabled = isPurchaseReturnEnabled,
                isSalesReturnEnabled = isSalesReturnEnabled,
                isExpenseModuleEnabled = isExpenseModuleEnabled,
                isCashBookEnabled = isCashBookEnabled,
                isAttendanceEnabled = isAttendanceEnabled,
                isEmployeeCommissionEnabled = isEmployeeCommissionEnabled
            )
            repository.saveSettings(updated)
            logActivity("UPDATE_FEATURE_TOGGLES", "Updated Super Admin feature module toggles")
            showToast("Feature module configuration saved successfully!")
        }
    }

    fun updateInvoiceCustomization(header: String, footer: String, taxPercentage: Double) {
        viewModelScope.launch {
            val s = repository.getSettingsSync()
            val updated = s.copy(
                invoiceHeader = header,
                invoiceFooter = footer,
                taxPercentage = taxPercentage
            )
            repository.saveSettings(updated)
            showToast("Invoice template & tax settings saved!")
        }
    }

    fun updateLowStockThreshold(newThreshold: Double, applyToAllProducts: Boolean = false) {
        viewModelScope.launch {
            val currentSettings = repository.getSettingsSync()
            repository.saveSettings(currentSettings.copy(defaultLowStockThreshold = newThreshold))
            if (applyToAllProducts) {
                repository.updateMinStockThresholds(newThreshold)
                showToast("Low stock threshold updated to ${newThreshold.toInt()} units for all inventory!")
            } else {
                showToast("Default low stock alert threshold set to ${newThreshold.toInt()} units!")
            }
        }
    }

    // --- BACKUP & RESTORE DATA ---
    fun exportDataToJson(context: Context): String {
        return try {
            val root = JSONObject()
            val productsArr = JSONArray()
            for (p in allProducts.value) {
                val obj = JSONObject()
                obj.put("barcode", p.barcode)
                obj.put("name", p.name)
                obj.put("category", p.category)
                obj.put("purchasePrice", p.purchasePrice)
                obj.put("salePrice", p.salePrice)
                obj.put("stockQuantity", p.stockQuantity)
                obj.put("minStockLevel", p.minStockLevel)
                obj.put("unit", p.unit)
                productsArr.put(obj)
            }
            root.put("products", productsArr)

            val customersArr = JSONArray()
            for (c in allCustomers.value) {
                val obj = JSONObject()
                obj.put("name", c.name)
                obj.put("phone", c.phone)
                obj.put("address", c.address)
                obj.put("balance", c.balance)
                customersArr.put(obj)
            }
            root.put("customers", customersArr)

            val suppliersArr = JSONArray()
            for (s in allSuppliers.value) {
                val obj = JSONObject()
                obj.put("name", s.name)
                obj.put("company", s.company)
                obj.put("phone", s.phone)
                obj.put("address", s.address)
                obj.put("payableBalance", s.payableBalance)
                suppliersArr.put(obj)
            }
            root.put("suppliers", suppliersArr)

            val expensesArr = JSONArray()
            for (e in allExpenses.value) {
                val obj = JSONObject()
                obj.put("title", e.title)
                obj.put("category", e.category)
                obj.put("amount", e.amount)
                obj.put("note", e.note)
                obj.put("timestamp", e.timestamp)
                expensesArr.put(obj)
            }
            root.put("expenses", expensesArr)

            val settingsObj = JSONObject()
            val st = settings.value
            settingsObj.put("storeName", st.storeName)
            settingsObj.put("ownerName", st.ownerName)
            settingsObj.put("phone", st.phone)
            settingsObj.put("address", st.address)
            settingsObj.put("currencySymbol", st.currencySymbol)
            settingsObj.put("themeMode", st.themeMode)
            root.put("settings", settingsObj)

            root.toString(2)
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    fun restoreDataFromJson(jsonStr: String) {
        viewModelScope.launch {
            try {
                val root = JSONObject(jsonStr)
                if (root.has("products")) {
                    val pArr = root.getJSONArray("products")
                    for (i in 0 until pArr.length()) {
                        val obj = pArr.getJSONObject(i)
                        val p = Product(
                            barcode = obj.optString("barcode", ""),
                            name = obj.getString("name"),
                            category = obj.optString("category", "General"),
                            purchasePrice = obj.optDouble("purchasePrice", 0.0),
                            salePrice = obj.optDouble("salePrice", 0.0),
                            stockQuantity = obj.optDouble("stockQuantity", 0.0),
                            minStockLevel = obj.optDouble("minStockLevel", 5.0),
                            unit = obj.optString("unit", "Pcs")
                        )
                        repository.insertOrUpdateProduct(p)
                    }
                }
                if (root.has("customers")) {
                    val cArr = root.getJSONArray("customers")
                    for (i in 0 until cArr.length()) {
                        val obj = cArr.getJSONObject(i)
                        val c = Customer(
                            name = obj.getString("name"),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            balance = obj.optDouble("balance", 0.0)
                        )
                        repository.saveCustomer(c)
                    }
                }
                if (root.has("suppliers")) {
                    val sArr = root.getJSONArray("suppliers")
                    for (i in 0 until sArr.length()) {
                        val obj = sArr.getJSONObject(i)
                        val s = Supplier(
                            name = obj.getString("name"),
                            company = obj.optString("company", ""),
                            phone = obj.optString("phone", ""),
                            address = obj.optString("address", ""),
                            payableBalance = obj.optDouble("payableBalance", 0.0)
                        )
                        repository.saveSupplier(s)
                    }
                }
                if (root.has("expenses")) {
                    val eArr = root.getJSONArray("expenses")
                    for (i in 0 until eArr.length()) {
                        val obj = eArr.getJSONObject(i)
                        val exp = Expense(
                            title = obj.getString("title"),
                            category = obj.optString("category", "General"),
                            amount = obj.optDouble("amount", 0.0),
                            note = obj.optString("note", ""),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                        )
                        repository.addExpense(exp)
                    }
                }
                showToast("Store backup restored successfully!")
            } catch (e: Exception) {
                showToast("Error restoring backup file format!")
            }
        }
    }

    // --- SQLITE DATABASE (.db) BACKUP & RESTORE MANAGEMENT ---
    private val _databaseBackups = MutableStateFlow<List<DatabaseBackupInfo>>(emptyList())
    val databaseBackups: StateFlow<List<DatabaseBackupInfo>> = _databaseBackups.asStateFlow()

    fun loadLocalDatabaseBackups(context: Context) {
        viewModelScope.launch {
            _databaseBackups.value = DatabaseBackupManager.getLocalBackups(context)
        }
    }

    fun createLocalDatabaseBackup(context: Context) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.createLocalDatabaseBackup(context)
            result.onSuccess { backupFile ->
                showToast("SQLite Database backup created: ${backupFile.name}")
                loadLocalDatabaseBackups(context)
            }.onFailure { err ->
                showToast("Failed to create database backup: ${err.localizedMessage}")
            }
        }
    }

    fun exportDatabaseToUri(context: Context, destinationUri: Uri) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.exportDatabaseToUri(context, destinationUri)
            result.onSuccess {
                showToast("SQLite Database file (.db) exported successfully!")
            }.onFailure { err ->
                showToast("Export failed: ${err.localizedMessage}")
            }
        }
    }

    fun importDatabaseFromUri(context: Context, sourceUri: Uri) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.importDatabaseFromUri(context, sourceUri)
            result.onSuccess {
                showToast("SQLite Database (.db) imported & restored successfully!")
                loadLocalDatabaseBackups(context)
            }.onFailure { err ->
                showToast("Database import failed: ${err.localizedMessage}")
            }
        }
    }

    fun importDatabaseFromFile(context: Context, sourceFile: File) {
        viewModelScope.launch {
            val result = DatabaseBackupManager.importDatabaseFromFile(context, sourceFile)
            result.onSuccess {
                showToast("Database restored from backup: ${sourceFile.name}")
                loadLocalDatabaseBackups(context)
            }.onFailure { err ->
                showToast("Database import failed: ${err.localizedMessage}")
            }
        }
    }

    fun shareDatabaseBackup(context: Context, backupFile: File) {
        DatabaseBackupManager.shareBackupFile(context, backupFile)
    }

    // --- RECYCLE BIN RESTORE & PERMANENT DELETE ---
    fun restoreRecycleBinItem(item: RecycleBinItem) {
        viewModelScope.launch {
            try {
                if (item.jsonData.isNotBlank()) {
                    val obj = JSONObject(item.jsonData)
                    when (item.itemType) {
                        "PRODUCT" -> {
                            val product = Product(
                                id = obj.optLong("id", 0L),
                                tenantId = obj.optString("tenantId", "TENANT_DEFAULT"),
                                storeId = obj.optLong("storeId", 1L),
                                barcode = obj.optString("barcode", ""),
                                name = obj.optString("name", item.title),
                                category = obj.optString("category", "General"),
                                purchasePrice = obj.optDouble("purchasePrice", 0.0),
                                salePrice = obj.optDouble("salePrice", 0.0),
                                stockQuantity = obj.optDouble("stockQuantity", 0.0),
                                minStockLevel = obj.optDouble("minStockLevel", 5.0),
                                unit = obj.optString("unit", "Pcs")
                            )
                            repository.insertOrUpdateProduct(product)
                        }
                        "CUSTOMER" -> {
                            val customer = Customer(
                                id = obj.optLong("id", 0L),
                                tenantId = obj.optString("tenantId", "TENANT_DEFAULT"),
                                storeId = obj.optLong("storeId", 1L),
                                name = obj.optString("name", item.title),
                                phone = obj.optString("phone", ""),
                                address = obj.optString("address", ""),
                                balance = obj.optDouble("balance", 0.0)
                            )
                            repository.saveCustomer(customer)
                        }
                        "SUPPLIER" -> {
                            val supplier = Supplier(
                                id = obj.optLong("id", 0L),
                                tenantId = obj.optString("tenantId", "TENANT_DEFAULT"),
                                storeId = obj.optLong("storeId", 1L),
                                name = obj.optString("name", item.title),
                                company = obj.optString("company", ""),
                                phone = obj.optString("phone", ""),
                                address = obj.optString("address", ""),
                                payableBalance = obj.optDouble("payableBalance", 0.0)
                            )
                            repository.saveSupplier(supplier)
                        }
                    }
                }
                repository.deleteRecycleBinItem(item.id)
                logActivity("RESTORE_TRASH", "Restored item '${item.title}' from Recycle Bin")
                showToast("Restored '${item.title}' successfully!")
            } catch (e: Exception) {
                showToast("Error restoring item: ${e.localizedMessage}")
            }
        }
    }

    fun permanentlyDeleteRecycleBinItem(item: RecycleBinItem) {
        viewModelScope.launch {
            repository.deleteRecycleBinItem(item.id)
            logActivity("PERMANENT_DELETE", "Permanently deleted '${item.title}' from Recycle Bin")
            showToast("Permanently deleted '${item.title}'.")
        }
    }

    fun clearAllRecycleBin() {
        viewModelScope.launch {
            repository.clearRecycleBin()
            logActivity("CLEAR_RECYCLE_BIN", "Emptied entire Recycle Bin")
            showToast("Recycle Bin emptied completely.")
        }
    }

    // --- ATTENDANCE MANAGEMENT & PAYROLL ---
    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun checkInEmployee(userId: Long, userName: String, userRole: String, notes: String = "") {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val existing = repository.getAttendanceForUserAndDate(userId, dateStr)
            val storeId = selectedStoreId.value.let { if (it == 0L) 1L else it }
            val now = System.currentTimeMillis()

            if (existing != null) {
                val updated = existing.copy(
                    checkInTime = existing.checkInTime ?: now,
                    status = "PRESENT",
                    notes = if (notes.isNotBlank()) notes else existing.notes
                )
                repository.updateAttendanceRecord(updated)
            } else {
                val newRec = AttendanceRecord(
                    storeId = storeId,
                    userId = userId,
                    userName = userName,
                    userRole = userRole,
                    dateStr = dateStr,
                    status = "PRESENT",
                    checkInTime = now,
                    notes = notes
                )
                repository.saveAttendanceRecord(newRec)
            }
            logActivity("ATTENDANCE_CHECKIN", "Checked in employee: $userName")
            showToast("Checked In $userName successfully!")
        }
    }

    fun checkOutEmployee(userId: Long, notes: String = "") {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val existing = repository.getAttendanceForUserAndDate(userId, dateStr)
            val now = System.currentTimeMillis()

            if (existing != null && existing.checkInTime != null) {
                val totalElapsedMs = now - existing.checkInTime
                val breakMs = if (existing.breakStartTime != null && existing.breakEndTime != null && existing.breakEndTime > existing.breakStartTime) {
                    existing.breakEndTime - existing.breakStartTime
                } else 0L

                val workedMs = (totalElapsedMs - breakMs).coerceAtLeast(0L)
                val workedMins = workedMs / (1000 * 60)
                val overtimeMins = if (workedMins > 480) workedMins - 480 else 0L

                // Calculate gross pay based on employee rates
                val targetUser = allUsers.value.find { it.id == userId }
                val hourlyRate = targetUser?.hourlyWageRate ?: 0.0
                val dailyRate = targetUser?.dailyWageRate ?: 0.0
                val overtimeRate = if ((targetUser?.overtimeHourlyRate ?: 0.0) > 0) {
                    targetUser!!.overtimeHourlyRate
                } else if (hourlyRate > 0) {
                    hourlyRate * 1.5
                } else if (dailyRate > 0) {
                    (dailyRate / 8.0) * 1.5
                } else 0.0

                val regularMins = workedMins - overtimeMins
                val basePay = if (hourlyRate > 0) {
                    (regularMins / 60.0) * hourlyRate
                } else if (dailyRate > 0) {
                    dailyRate
                } else 0.0

                val overtimePayCalculated = (overtimeMins / 60.0) * overtimeRate
                val totalPay = basePay + overtimePayCalculated + existing.allowances - existing.deductions

                val updated = existing.copy(
                    checkOutTime = now,
                    totalWorkingMinutes = workedMins,
                    overtimeMinutes = overtimeMins,
                    dailyGrossPay = basePay,
                    overtimePay = overtimePayCalculated,
                    totalDailyPay = totalPay,
                    notes = if (notes.isNotBlank()) notes else existing.notes
                )
                repository.updateAttendanceRecord(updated)
                logActivity("ATTENDANCE_CHECKOUT", "Checked out employee: ${existing.userName}")
                showToast("Checked Out ${existing.userName}! Worked: ${workedMins / 60}h ${workedMins % 60}m | Gross Pay: $totalPay")
            } else {
                showToast("No active check-in found for today.")
            }
        }
    }

    fun startBreakEmployee(userId: Long) {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val existing = repository.getAttendanceForUserAndDate(userId, dateStr)
            val now = System.currentTimeMillis()

            if (existing != null) {
                val updated = existing.copy(breakStartTime = now)
                repository.updateAttendanceRecord(updated)
                showToast("Break started for ${existing.userName}")
            } else {
                showToast("Please check in first before starting break.")
            }
        }
    }

    fun endBreakEmployee(userId: Long) {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val existing = repository.getAttendanceForUserAndDate(userId, dateStr)
            val now = System.currentTimeMillis()

            if (existing != null) {
                val updated = existing.copy(breakEndTime = now)
                repository.updateAttendanceRecord(updated)
                showToast("Break ended for ${existing.userName}")
            } else {
                showToast("No active break found.")
            }
        }
    }

    fun markEmployeeAttendanceStatus(userId: Long, userName: String, userRole: String, status: String, notes: String = "") {
        viewModelScope.launch {
            val dateStr = getTodayDateString()
            val existing = repository.getAttendanceForUserAndDate(userId, dateStr)
            val storeId = selectedStoreId.value.let { if (it == 0L) 1L else it }

            if (existing != null) {
                val updated = existing.copy(status = status, notes = notes)
                repository.updateAttendanceRecord(updated)
            } else {
                val targetUser = allUsers.value.find { it.id == userId }
                val basePay = if (status == "PRESENT") (targetUser?.dailyWageRate ?: 0.0) else 0.0
                val newRec = AttendanceRecord(
                    storeId = storeId,
                    userId = userId,
                    userName = userName,
                    userRole = userRole,
                    dateStr = dateStr,
                    status = status,
                    dailyGrossPay = basePay,
                    totalDailyPay = basePay,
                    notes = notes
                )
                repository.saveAttendanceRecord(newRec)
            }
            logActivity("ATTENDANCE_MARKED", "Marked $userName as $status")
            showToast("Marked $userName as $status")
        }
    }

    fun updateUserWageRates(
        userId: Long,
        dailyWage: Double,
        hourlyWage: Double,
        overtimeWage: Double,
        monthlyBase: Double
    ) {
        viewModelScope.launch {
            val user = allUsers.value.find { it.id == userId }
            if (user != null) {
                val updated = user.copy(
                    dailyWageRate = dailyWage,
                    hourlyWageRate = hourlyWage,
                    overtimeHourlyRate = overtimeWage,
                    monthlyBaseSalary = monthlyBase
                )
                repository.saveUser(updated)
                logActivity("EMPLOYEE_WAGE_UPDATED", "Updated pay rates for ${user.name}")
                showToast("Updated pay rates & salary for ${user.name}")
            }
        }
    }

    fun updateAttendancePayRecord(
        recordId: Long,
        dailyGrossPay: Double,
        overtimePay: Double,
        deductions: Double,
        allowances: Double,
        notes: String
    ) {
        viewModelScope.launch {
            val record = allAttendanceRecords.value.find { it.id == recordId }
            if (record != null) {
                val totalPay = dailyGrossPay + overtimePay + allowances - deductions
                val updated = record.copy(
                    dailyGrossPay = dailyGrossPay,
                    overtimePay = overtimePay,
                    deductions = deductions,
                    allowances = allowances,
                    totalDailyPay = totalPay,
                    notes = if (notes.isNotBlank()) notes else record.notes
                )
                repository.updateAttendanceRecord(updated)
                showToast("Updated payroll record for ${record.userName}")
            }
        }
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.deleteAttendanceRecord(record)
            showToast("Deleted attendance record for ${record.userName}")
        }
    }
}
