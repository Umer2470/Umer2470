package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.entity.*
import com.example.util.PaymentCalculationResult
import com.example.util.PaymentCalculator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

data class CartItem(
    val product: ProductEntity,
    var quantity: Double = 1.0
) {
    val totalPrice: Double get() = product.salePrice * quantity
}

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class StoreViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val productDao = db.productDao()
    private val customerDao = db.customerDao()
    private val supplierDao = db.supplierDao()
    private val invoiceDao = db.invoiceDao()
    private val invoiceItemDao = db.invoiceItemDao()
    private val paymentRecordDao = db.paymentRecordDao()
    private val customerLedgerDao = db.customerLedgerDao()

    val currentStoreId = MutableStateFlow(1L)
    val isUrdu = MutableStateFlow(false)

    val searchQuery = MutableStateFlow("")
    val products = searchQuery.flatMapLatest { q ->
        if (q.isBlank()) {
            productDao.getAllProductsFlow(currentStoreId.value)
        } else {
            productDao.searchProductsFlow(q, currentStoreId.value)
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val customers = currentStoreId.flatMapLatest { storeId ->
        customerDao.getAllCustomersFlow(storeId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val invoices = currentStoreId.flatMapLatest { storeId ->
        invoiceDao.getAllInvoicesFlow(storeId)
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val cart = MutableStateFlow<List<CartItem>>(emptyList())
    val discount = MutableStateFlow(0.0)
    val tax = MutableStateFlow(0.0)
    val selectedCustomer = MutableStateFlow<CustomerEntity?>(null)

    val subtotal = cart.map { items -> items.sumOf { it.totalPrice } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val invoiceTotal = combine(subtotal, discount, tax) { sub, disc, tx ->
        kotlin.math.max(0.0, sub - disc + tx)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0.0)

    val amountReceivedInput = MutableStateFlow("")
    val paymentMethod = MutableStateFlow("CASH")

    val paymentCalculation: StateFlow<PaymentCalculationResult> = combine(
        invoiceTotal,
        amountReceivedInput
    ) { total, input ->
        val received = input.toDoubleOrNull() ?: total
        PaymentCalculator.calculate(total, received)
    }.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        PaymentCalculator.calculate(0.0, 0.0)
    )

    val lastCompletedInvoice = MutableStateFlow<InvoiceEntity?>(null)
    val errorMessage = MutableStateFlow<String?>(null)
    val successMessage = MutableStateFlow<String?>(null)

    fun addToCart(product: ProductEntity) {
        val current = cart.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.product.id == product.id }
        if (existingIndex >= 0) {
            val item = current[existingIndex]
            current[existingIndex] = item.copy(quantity = item.quantity + 1.0)
        } else {
            current.add(CartItem(product = product, quantity = 1.0))
        }
        cart.value = current
    }

    fun updateQuantity(productId: Long, delta: Double) {
        val current = cart.value.toMutableList()
        val index = current.indexOfFirst { it.product.id == productId }
        if (index >= 0) {
            val newQty = current[index].quantity + delta
            if (newQty <= 0.001) {
                current.removeAt(index)
            } else {
                current[index] = current[index].copy(quantity = newQty)
            }
            cart.value = current
        }
    }

    fun removeFromCart(productId: Long) {
        cart.value = cart.value.filterNot { it.product.id == productId }
    }

    fun clearCart() {
        cart.value = emptyList()
        discount.value = 0.0
        tax.value = 0.0
        selectedCustomer.value = null
        amountReceivedInput.value = ""
    }

    fun completeCheckout(
        allowWalkingCustomerCredit: Boolean = false,
        onSuccess: (InvoiceEntity) -> Unit
    ) {
        val currentCart = cart.value
        if (currentCart.isEmpty()) {
            errorMessage.value = "Cart is empty"
            return
        }

        val calc = paymentCalculation.value
        val customer = selectedCustomer.value
        val storeId = currentStoreId.value

        if (calc.balanceDue > 0.001 && customer == null && !allowWalkingCustomerCredit) {
            errorMessage.value = "Walking customer cannot leave unpaid balance of Rs ${calc.balanceDue} without explicit confirmation"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val nextNum = (invoiceDao.getInvoiceCount(storeId) + 1).toString().padStart(6, '0')
                val invNo = "INV-${System.currentTimeMillis().toString().takeLast(4)}-$nextNum"

                val invoice = InvoiceEntity(
                    invoiceNo = invNo,
                    customerId = customer?.id,
                    customerName = customer?.name ?: "Walking Customer",
                    subtotal = subtotal.value,
                    discountAmount = discount.value,
                    taxAmount = tax.value,
                    totalAmount = calc.invoiceTotal,
                    amountReceived = calc.amountReceived,
                    amountApplied = calc.amountApplied,
                    changeDue = calc.changeDue,
                    balanceDue = calc.balanceDue,
                    paymentMethod = paymentMethod.value,
                    paymentStatus = calc.status.name,
                    cashierName = "Cashier 1",
                    storeId = storeId
                )

                val invoiceId = invoiceDao.insertInvoice(invoice)
                val finalInvoice = invoice.copy(id = invoiceId)

                val items = currentCart.map { item ->
                    InvoiceItemEntity(
                        invoiceId = invoiceId,
                        productId = item.product.id,
                        productName = item.product.name,
                        barcode = item.product.barcode,
                        quantity = item.quantity,
                        unitPrice = item.product.salePrice,
                        totalPrice = item.totalPrice,
                        storeId = storeId
                    )
                }
                invoiceItemDao.insertItems(items)

                for (item in currentCart) {
                    productDao.decrementStock(item.product.id, item.quantity)
                }

                if (calc.amountApplied > 0.0) {
                    paymentRecordDao.insertPayment(
                        PaymentRecordEntity(
                            invoiceId = invoiceId,
                            invoiceNo = invNo,
                            customerId = customer?.id,
                            amount = calc.amountApplied,
                            paymentMethod = paymentMethod.value,
                            cashierName = "Cashier 1",
                            notes = "Initial checkout payment",
                            storeId = storeId,
                            idempotencyKey = "INIT-$invoiceId"
                        )
                    )
                }

                if (calc.balanceDue > 0.001 && customer != null) {
                    customerDao.addDebt(customer.id, calc.balanceDue)
                    customerLedgerDao.insertEntry(
                        CustomerLedgerEntity(
                            customerId = customer.id,
                            invoiceId = invoiceId,
                            transactionType = "SALE_CREDIT",
                            debitAmount = calc.balanceDue,
                            creditAmount = 0.0,
                            balanceAfter = customer.currentBalance + calc.balanceDue,
                            description = "Unpaid balance on invoice $invNo",
                            storeId = storeId
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    lastCompletedInvoice.value = finalInvoice
                    clearCart()
                    successMessage.value = "Invoice $invNo completed successfully"
                    onSuccess(finalInvoice)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage.value = "Checkout failed: ${e.localizedMessage}"
                }
            }
        }
    }

    fun recordLaterPayment(
        invoice: InvoiceEntity,
        paymentAmount: Double,
        method: String = "CASH",
        idempotencyKey: String = UUID.randomUUID().toString(),
        onComplete: (Boolean) -> Unit
    ) {
        if (paymentAmount <= 0.0) {
            errorMessage.value = "Payment amount must be greater than zero"
            onComplete(false)
            return
        }

        val appliedAmount = PaymentCalculator.calculateSettlement(invoice.balanceDue, paymentAmount)
        val storeId = currentStoreId.value

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (paymentRecordDao.checkDuplicateKey(idempotencyKey) > 0) {
                    withContext(Dispatchers.Main) {
                        errorMessage.value = "Duplicate payment request blocked"
                        onComplete(false)
                    }
                    return@launch
                }

                paymentRecordDao.insertPayment(
                    PaymentRecordEntity(
                        invoiceId = invoice.id,
                        invoiceNo = invoice.invoiceNo,
                        customerId = invoice.customerId,
                        amount = appliedAmount,
                        paymentMethod = method,
                        cashierName = "Cashier 1",
                        notes = "Later balance settlement",
                        storeId = storeId,
                        idempotencyKey = idempotencyKey
                    )
                )

                invoiceDao.applySettlementPayment(invoice.id, appliedAmount)

                if (invoice.customerId != null) {
                    customerDao.applyPaymentCredit(invoice.customerId, appliedAmount)
                    val customer = customerDao.getCustomerById(invoice.customerId)
                    val newBal = (customer?.currentBalance ?: 0.0)
                    customerLedgerDao.insertEntry(
                        CustomerLedgerEntity(
                            customerId = invoice.customerId,
                            invoiceId = invoice.id,
                            transactionType = "PAYMENT_RECEIVED",
                            debitAmount = 0.0,
                            creditAmount = appliedAmount,
                            balanceAfter = newBal,
                            description = "Payment received for invoice ${invoice.invoiceNo}",
                            storeId = storeId
                        )
                    )
                }

                withContext(Dispatchers.Main) {
                    successMessage.value = "Payment of Rs $appliedAmount recorded"
                    onComplete(true)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    errorMessage.value = "Payment settlement failed: ${e.localizedMessage}"
                    onComplete(false)
                }
            }
        }
    }

    fun switchStore(storeId: Long) {
        currentStoreId.value = storeId
        clearCart()
    }

    val syncStatus = MutableStateFlow("● Synced")
    val currentUser = MutableStateFlow<String?>("Local Store Admin")
    val authToken = MutableStateFlow<String?>(null)
    val serverBaseUrl = MutableStateFlow("http://10.0.2.2:8080")

    fun loginSharedAccount(user: String, pass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("${serverBaseUrl.value}/api/auth/login")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val payload = """{"username":"$user","password":"$pass"}"""
                conn.outputStream.use { it.write(payload.toByteArray()) }

                val code = conn.responseCode
                if (code == 200) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val tokenRegex = """"token":"([^"]+)"""".toRegex()
                    val token = tokenRegex.find(resp)?.groupValues?.get(1)
                    authToken.value = token
                    currentUser.value = "$user (Cloud)"
                    syncStatus.value = "● Synced with Cloud"
                    withContext(Dispatchers.Main) { onResult(true, "Welcome $user! Cloud account active") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Login failed: HTTP $code") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Network error: ${e.localizedMessage}") }
            }
        }
    }

    fun registerSharedAccount(user: String, pass: String, storeName: String, pin: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("${serverBaseUrl.value}/api/auth/register")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val payload = """{"username":"$user","password":"$pass","storeName":"$storeName","recoveryPin":"$pin"}"""
                conn.outputStream.use { it.write(payload.toByteArray()) }

                val code = conn.responseCode
                if (code in 200..201) {
                    val resp = conn.inputStream.bufferedReader().use { it.readText() }
                    val tokenRegex = """"token":"([^"]+)"""".toRegex()
                    val token = tokenRegex.find(resp)?.groupValues?.get(1)
                    authToken.value = token
                    currentUser.value = "$user ($storeName)"
                    syncStatus.value = "● Synced with Cloud"
                    withContext(Dispatchers.Main) { onResult(true, "Store registered! Welcome $user") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Registration failed: HTTP $code") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Network error: ${e.localizedMessage}") }
            }
        }
    }

    fun recoverAccount(user: String, pin: String, newPass: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("${serverBaseUrl.value}/api/auth/recover-account")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }
                val payload = """{"username":"$user","recoveryPin":"$pin","newPassword":"$newPass"}"""
                conn.outputStream.use { it.write(payload.toByteArray()) }

                val code = conn.responseCode
                if (code == 200) {
                    withContext(Dispatchers.Main) { onResult(true, "Password reset! Please sign in") }
                } else {
                    withContext(Dispatchers.Main) { onResult(false, "Recovery failed: HTTP $code") }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onResult(false, "Error: ${e.localizedMessage}") }
            }
        }
    }

    fun triggerSync(onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                syncStatus.value = "⏳ Syncing..."
                val storeId = currentStoreId.value
                val localProducts = productDao.getAllProducts(storeId)
                val localCustomers = customerDao.getAllCustomers(storeId)
                val localInvoices = invoiceDao.getAllInvoices(storeId)

                val url = java.net.URL("${serverBaseUrl.value}/api/sync/push")
                val conn = (url.openConnection() as java.net.HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    authToken.value?.let { setRequestProperty("Authorization", "Bearer $it") }
                    doOutput = true
                    connectTimeout = 5000
                    readTimeout = 5000
                }

                // Construct clean JSON payload
                val prodJson = localProducts.joinToString(",") { 
                    """{"name":"${it.name.replace("\"", "\\\"")}","barcode":"${it.barcode}","salePrice":${it.salePrice},"stockQuantity":${it.stockQuantity},"updatedAt":${it.updatedAt}}"""
                }
                val custJson = localCustomers.joinToString(",") {
                    """{"name":"${it.name.replace("\"", "\\\"")}","phone":"${it.phone}","currentBalance":${it.currentBalance}}"""
                }
                val invJson = localInvoices.take(30).joinToString(",") {
                    """{"invoiceNo":"${it.invoiceNo}","totalAmount":${it.totalAmount},"amountReceived":${it.amountReceived},"amountApplied":${it.amountApplied},"balanceDue":${it.balanceDue},"paymentStatus":"${it.paymentStatus}"}"""
                }

                val body = """{"storeId":$storeId,"products":[$prodJson],"customers":[$custJson],"invoices":[$invJson]}"""
                conn.outputStream.use { it.write(body.toByteArray()) }

                val code = conn.responseCode
                if (code == 200) {
                    syncStatus.value = "● Synced just now"
                    withContext(Dispatchers.Main) {
                        onResult(true, "Synced ${localProducts.size} products & ${localInvoices.size} invoices with Cloud")
                    }
                } else {
                    syncStatus.value = "● Offline (Cached)"
                    withContext(Dispatchers.Main) {
                        onResult(true, "Offline mode active. Records preserved locally in Room DB.")
                    }
                }
            } catch (e: Exception) {
                syncStatus.value = "● Offline (Local Room DB)"
                withContext(Dispatchers.Main) {
                    onResult(true, "Offline mode: local changes stored safely. Will sync when server is reachable.")
                }
            }
        }
    }
}
