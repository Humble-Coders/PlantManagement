package com.humblecoders.plantmanagement.repositories

import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.ListenerRegistration
import com.humblecoders.plantmanagement.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class HistoryRepository(
    private val firestore: Firestore,
    private val userId: String,
    private val appId: String
) {
    
    // Cache for all transactions
    private val _allTransactions = MutableStateFlow<List<HistoryTransaction>>(emptyList())
    val allTransactions: StateFlow<List<HistoryTransaction>> = _allTransactions
    
    // Listeners for real-time updates
    private val listeners = mutableListOf<ListenerRegistration>()
    private var isInitialized = false
    
    private fun getSalesCollection() = firestore.collection("sales")
    private fun getPurchasesCollection() = firestore.collection("purchases")
    private fun getCashTransactionsCollection() = firestore.collection("cash_transactions")
    private fun getCashOutCollection() = firestore.collection("cash_out")
    private fun getCashInRevenueCollection() = firestore.collection("cash_in_revenue")
    private fun getCashInOutDifferenceCollection() = firestore.collection("cash_in_out_difference")
    private fun getProductionCollection() = firestore.collection("production_records")
    private fun getExpensesCollection() = firestore.collection("expenses")
    private fun getCashReportCollection() = firestore.collection("cashReports")

    /**
     * Initialize the cache and set up real-time listeners
     * This should be called once when the repository is created
     */
    suspend fun initializeCache(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            if (isInitialized) {
                return@withContext Result.success(Unit)
            }
            
            // Initial fetch of all data
            refreshAllData()
            
            // Set up real-time listeners
            setupListeners()
            
            isInitialized = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh all data from Firebase
     * This can be called manually via refresh button
     */
    suspend fun refreshAllData(): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val transactions = mutableListOf<HistoryTransaction>()
            
            // Get all sales
            val sales = getAllSales()
            transactions.addAll(sales.map { convertSaleToHistoryTransaction(it) })
            
            // Get all purchases
            val purchases = getAllPurchases()
            transactions.addAll(purchases.map { convertPurchaseToHistoryTransaction(it) })
            
            // Get all cash transactions
            val cashTransactions = getAllCashTransactions()
            transactions.addAll(cashTransactions.map { convertCashTransactionToHistoryTransaction(it) })
            
            // Get all cash out transactions
            val cashOuts = getAllCashOut()
            transactions.addAll(cashOuts.map { convertCashOutToHistoryTransaction(it) })
            
            // Get all cash in revenue
            val cashInRevenue = getAllCashInRevenue()
            transactions.addAll(cashInRevenue.map { convertCashInRevenueToHistoryTransaction(it) })
            
            // Get all cash in/out difference
            val cashInOutDifference = getAllCashInOutDifference()
            transactions.addAll(cashInOutDifference.map { convertCashInOutDifferenceToHistoryTransaction(it) })
            
            // Get all production records
            val productionRecords = getAllProductionRecords()
            transactions.addAll(productionRecords.map { convertProductionRecordToHistoryTransaction(it) })
            
            // Get all expenses
            val expenses = getAllExpenses()
            transactions.addAll(expenses.map { convertExpenseToHistoryTransaction(it) })
            
            // Get all cash reports
            val cashReports = getAllCashReports()
            transactions.addAll(cashReports.map { convertCashReportToHistoryTransaction(it) })
            
            // Update cache
            _allTransactions.value = transactions
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Set up real-time listeners for all collections
     */
    private fun setupListeners() {
        // Clear existing listeners
        listeners.forEach { it.remove() }
        listeners.clear()
        
        // Sales listener
        listeners.add(
            getSalesCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshSalesData(it.documents) }
                }
        )
        
        // Purchases listener
        listeners.add(
            getPurchasesCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshPurchasesData(it.documents) }
                }
        )
        
        // Cash transactions listener
        listeners.add(
            getCashTransactionsCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshCashTransactionsData(it.documents) }
                }
        )
        
        // Cash out listener
        listeners.add(
            getCashOutCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshCashOutData(it.documents) }
                }
        )
        
        // Cash in revenue listener
        listeners.add(
            getCashInRevenueCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshCashInRevenueData(it.documents) }
                }
        )
        
        // Cash in/out difference listener
        listeners.add(
            getCashInOutDifferenceCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshCashInOutDifferenceData(it.documents) }
                }
        )
        
        // Production records listener
        listeners.add(
            getProductionCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshProductionData(it.documents) }
                }
        )
        
        // Expenses listener
        listeners.add(
            getExpensesCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshExpensesData(it.documents) }
                }
        )
        
        // Cash reports listener
        listeners.add(
            getCashReportCollection()
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    snapshot?.let { refreshCashReportsData(it.documents) }
                }
        )
    }

    /**
     * Get all transactions for a specific date
     * Uses cached data and filters by date in the app
     */
    suspend fun getTransactionsForDate(date: String): Result<DayHistory> = withContext(Dispatchers.IO) {
        return@withContext try {
            // Use cached data instead of fetching from Firebase
            val allTransactions = _allTransactions.value
            
            // Filter transactions by date in the app
            val filteredTransactions = filterTransactionsByDate(allTransactions, date)
            
            // Sort transactions by creation time
            val sortedTransactions = filteredTransactions.sortedBy { it.createdAt?.seconds ?: 0L }
            
            // Calculate summary
            val summary = calculateDaySummary(sortedTransactions)
            
            Result.success(DayHistory(
                date = date,
                transactions = sortedTransactions,
                summary = summary
            ))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Refresh methods for real-time updates
     */
    private fun refreshSalesData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val sales = documents.mapNotNull { doc ->
            try {
                val sale = doc.toObject(Sale::class.java)
                sale?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(sales.map { convertSaleToHistoryTransaction(it) })
    }

    private fun refreshPurchasesData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val purchases = documents.mapNotNull { doc ->
            try {
                val purchase = doc.toObject(Purchase::class.java)
                purchase?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(purchases.map { convertPurchaseToHistoryTransaction(it) })
    }

    private fun refreshCashTransactionsData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val cashTransactions = documents.mapNotNull { doc ->
            try {
                val cashTransaction = doc.toObject(CashTransaction::class.java)
                cashTransaction?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(cashTransactions.map { convertCashTransactionToHistoryTransaction(it) })
    }

    private fun refreshCashOutData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val cashOuts = documents.mapNotNull { doc ->
            try {
                val cashOut = doc.toObject(CashOut::class.java)
                cashOut?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(cashOuts.map { convertCashOutToHistoryTransaction(it) })
    }

    private fun refreshCashInRevenueData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val cashInRevenue = documents.mapNotNull { doc ->
            try {
                val cashInRevenue = doc.toObject(CashInRevenue::class.java)
                cashInRevenue?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(cashInRevenue.map { convertCashInRevenueToHistoryTransaction(it) })
    }

    private fun refreshCashInOutDifferenceData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val cashInOutDifference = documents.mapNotNull { doc ->
            try {
                val cashInOutDifference = doc.toObject(CashInOutDifference::class.java)
                cashInOutDifference?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(cashInOutDifference.map { convertCashInOutDifferenceToHistoryTransaction(it) })
    }

    private fun refreshProductionData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val productionRecords = documents.mapNotNull { doc ->
            try {
                val productionRecord = doc.toObject(ProductionRecord::class.java)
                productionRecord?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(productionRecords.map { convertProductionRecordToHistoryTransaction(it) })
    }

    private fun refreshExpensesData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val expenses = documents.mapNotNull { doc ->
            try {
                val expense = doc.toObject(Expense::class.java)
                expense?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(expenses.map { convertExpenseToHistoryTransaction(it) })
    }

    private fun refreshCashReportsData(documents: List<com.google.cloud.firestore.DocumentSnapshot>) {
        val cashReports = documents.mapNotNull { doc ->
            try {
                val cashReport = doc.toObject(CashReport::class.java)
                cashReport?.copy(id = doc.id)
            } catch (e: Exception) {
                null
            }
        }
        updateCacheWithNewData(cashReports.map { convertCashReportToHistoryTransaction(it) })
    }

    private fun updateCacheWithNewData(newTransactions: List<HistoryTransaction>) {
        val currentTransactions = _allTransactions.value.toMutableList()
        
        // Remove old transactions of the same type
        val transactionTypes = newTransactions.map { it.transactionType }.distinct()
        transactionTypes.forEach { type ->
            currentTransactions.removeAll { it.transactionType == type }
        }
        
        // Add new transactions
        currentTransactions.addAll(newTransactions)
        
        // Update cache
        _allTransactions.value = currentTransactions
    }

    /**
     * Clean up listeners when repository is no longer needed
     */
    fun cleanup() {
        listeners.forEach { it.remove() }
        listeners.clear()
        isInitialized = false
    }

    private suspend fun getAllSales(): List<Sale> {
        return try {
            val snapshot = getSalesCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val sale = doc.toObject(Sale::class.java)
                    sale?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllPurchases(): List<Purchase> {
        return try {
            val snapshot = getPurchasesCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val purchase = doc.toObject(Purchase::class.java)
                    purchase?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllCashTransactions(): List<CashTransaction> {
        return try {
            val snapshot = getCashTransactionsCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val cashTransaction = doc.toObject(CashTransaction::class.java)
                    cashTransaction?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllCashOut(): List<CashOut> {
        return try {
            val snapshot = getCashOutCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val cashOut = doc.toObject(CashOut::class.java)
                    cashOut?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllCashInRevenue(): List<CashInRevenue> {
        return try {
            val snapshot = getCashInRevenueCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val cashInRevenue = doc.toObject(CashInRevenue::class.java)
                    cashInRevenue?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllCashInOutDifference(): List<CashInOutDifference> {
        return try {
            val snapshot = getCashInOutDifferenceCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val cashInOutDifference = doc.toObject(CashInOutDifference::class.java)
                    cashInOutDifference?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllProductionRecords(): List<ProductionRecord> {
        return try {
            val snapshot = getProductionCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val productionRecord = doc.toObject(ProductionRecord::class.java)
                    productionRecord?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllExpenses(): List<Expense> {
        return try {
            val snapshot = getExpensesCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val expense = doc.toObject(Expense::class.java)
                    expense?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getAllCashReports(): List<CashReport> {
        return try {
            val snapshot = getCashReportCollection()
                .get()
                .get()
            
            snapshot.documents.mapNotNull { doc ->
                try {
                    val cashReport = doc.toObject(CashReport::class.java)
                    cashReport?.copy(id = doc.id)
                } catch (e: Exception) {
                    null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Filter transactions by date in the app (not on Firebase)
     * Uses the same logic as CustomerDetailScreen
     */
    private fun filterTransactionsByDate(transactions: List<HistoryTransaction>, targetDate: String): List<HistoryTransaction> {
        return try {
            val targetLocalDate = parseDate(targetDate)
            
            transactions.filter { transaction ->
                val transactionDate = when (transaction.transactionType) {
                    HistoryTransactionType.SALE -> {
                        // Parse saleDate (yyyy-mm-dd format)
                        try {
                            parseDate(transaction.date)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                    }
                    HistoryTransactionType.PURCHASE -> {
                        // Parse purchaseDate (yyyy-mm-dd format)
                        try {
                            parseDate(transaction.date)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                    }
                    HistoryTransactionType.PRODUCTION -> {
                        // For production records, parse the date field (already converted to yyyy-mm-dd format)
                        try {
                            parseDate(transaction.date)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                    }
                    HistoryTransactionType.EXPENSE, HistoryTransactionType.CASH_REPORT_IN, HistoryTransactionType.CASH_REPORT_OUT -> {
                        // For expense and cash report records, parse the date field (already converted to yyyy-mm-dd format)
                        try {
                            parseDate(transaction.date)
                        } catch (e: Exception) {
                            LocalDate.now()
                        }
                    }
                    else -> {
                        // For timestamp-based transactions, use createdAt
                        transaction.createdAt?.let { timestamp ->
                            try {
                                LocalDate.ofEpochDay(timestamp.seconds / 86400)
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        } ?: LocalDate.now()
                    }
                }
                transactionDate == targetLocalDate
            }
        } catch (e: Exception) {
            transactions // If date parsing fails, return all transactions
        }
    }

    private fun parseDate(dateString: String): LocalDate {
        return when {
            dateString.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            }
            dateString.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
            }
            else -> {
                try {
                    LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
                } catch (e: Exception) {
                    LocalDate.parse(dateString, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                }
            }
        }
    }

    private fun convertSaleToHistoryTransaction(sale: Sale): HistoryTransaction {
        return HistoryTransaction(
            id = sale.id,
            transactionType = HistoryTransactionType.SALE,
            date = sale.saleDate, // Keep original format
            customerId = sale.customerId,
            customerName = sale.firmName,
            amount = sale.totalRevenueAmount,
            description = "Sale - Bill #${sale.billNumber}",
            status = sale.saleStatus.name,
            createdAt = sale.createdAt,
            originalData = sale
        )
    }

    private fun convertPurchaseToHistoryTransaction(purchase: Purchase): HistoryTransaction {
        return HistoryTransaction(
            id = purchase.id,
            transactionType = HistoryTransactionType.PURCHASE,
            date = purchase.purchaseDate, // Keep original format
            customerId = purchase.customerId,
            customerName = purchase.firmName,
            amount = purchase.grandTotal,
            description = "Purchase - ${purchase.items.size} items",
            status = purchase.paymentStatus.name,
            createdAt = purchase.createdAt,
            originalData = purchase
        )
    }

    private fun convertCashTransactionToHistoryTransaction(cashTransaction: CashTransaction): HistoryTransaction {
        val transactionType = when {
            cashTransaction.note.contains("Cash In from Sale Module") -> HistoryTransactionType.CASH_IN_SALES
            cashTransaction.note.contains("Cash Out from Purchase Module") -> HistoryTransactionType.CASH_OUT_PURCHASES
            cashTransaction.transactionType == CashTransactionType.RECEIVE -> HistoryTransactionType.CASH_IN_CUSTOMER
            cashTransaction.transactionType == CashTransactionType.GIVE -> HistoryTransactionType.CASH_OUT_CUSTOMER
            else -> if (cashTransaction.transactionType == CashTransactionType.RECEIVE) 
                HistoryTransactionType.CASH_IN_CUSTOMER 
            else 
                HistoryTransactionType.CASH_OUT_CUSTOMER
        }

        return HistoryTransaction(
            id = cashTransaction.id,
            transactionType = transactionType,
            date = cashTransaction.createdAt?.let { timestamp ->
                LocalDate.ofEpochDay(timestamp.seconds / 86400).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = cashTransaction.customerId,
            customerName = cashTransaction.customerName,
            amount = cashTransaction.amount,
            description = cashTransaction.note.ifEmpty { "Cash Transaction" },
            status = "Completed",
            createdAt = cashTransaction.createdAt,
            originalData = cashTransaction
        )
    }

    private fun convertCashOutToHistoryTransaction(cashOut: CashOut): HistoryTransaction {
        return HistoryTransaction(
            id = cashOut.id,
            transactionType = HistoryTransactionType.CASH_OUT_PURCHASES, // All cash outs are from purchase module
            date = cashOut.createdAt?.let { timestamp ->
                LocalDate.ofEpochDay(timestamp.seconds / 86400).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = "",
            customerName = "Multiple Customers",
            amount = cashOut.totalAmount,
            description = "Cash Out from Purchase Module - ${cashOut.purchaseAllocations.size} allocations",
            status = "Completed",
            createdAt = cashOut.createdAt,
            originalData = cashOut
        )
    }

    private fun convertCashInRevenueToHistoryTransaction(cashInRevenue: CashInRevenue): HistoryTransaction {
        return HistoryTransaction(
            id = cashInRevenue.id,
            transactionType = HistoryTransactionType.CASH_IN_SALES,
            date = cashInRevenue.createdAt?.let { timestamp ->
                LocalDate.ofEpochDay(timestamp.seconds / 86400).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = "",
            customerName = "Multiple Customers",
            amount = cashInRevenue.totalAmount,
            description = "Cash In from Sales - ${cashInRevenue.saleAllocations.size} allocations",
            status = "Completed",
            createdAt = cashInRevenue.createdAt,
            originalData = cashInRevenue
        )
    }

    private fun convertCashInOutDifferenceToHistoryTransaction(cashInOutDifference: CashInOutDifference): HistoryTransaction {
        val transactionType = if (cashInOutDifference.transactionType == DifferenceTransactionType.CASH_IN) {
            HistoryTransactionType.DIFFERENCE_CASH_IN
        } else {
            HistoryTransactionType.DIFFERENCE_CASH_OUT
        }

        return HistoryTransaction(
            id = cashInOutDifference.id,
            transactionType = transactionType,
            date = cashInOutDifference.createdAt?.let { timestamp ->
                LocalDate.ofEpochDay(timestamp.seconds / 86400).format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = "",
            customerName = "Multiple Customers",
            amount = cashInOutDifference.totalAmount,
            description = "Difference ${if (transactionType == HistoryTransactionType.DIFFERENCE_CASH_IN) "Cash In" else "Cash Out"} - ${cashInOutDifference.saleAllocations.size} allocations",
            status = "Completed",
            createdAt = cashInOutDifference.createdAt,
            originalData = cashInOutDifference
        )
    }

    private fun convertProductionRecordToHistoryTransaction(productionRecord: ProductionRecord): HistoryTransaction {
        return HistoryTransaction(
            id = productionRecord.id,
            transactionType = HistoryTransactionType.PRODUCTION,
            date = productionRecord.createdAt?.let { timestamp ->
                // Convert timestamp to LocalDate using system timezone
                val instant = java.time.Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())
                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = "",
            customerName = productionRecord.supervisorName,
            amount = productionRecord.quantityProduced,
            description = "Production - Batch #${productionRecord.batchNumber}",
            status = "Completed",
            createdAt = productionRecord.createdAt,
            originalData = productionRecord
        )
    }

    private fun convertExpenseToHistoryTransaction(expense: Expense): HistoryTransaction {
        return HistoryTransaction(
            id = expense.id,
            transactionType = HistoryTransactionType.EXPENSE,
            date = expense.date?.let { timestamp ->
                // Convert timestamp to LocalDate using system timezone
                val instant = java.time.Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())
                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = "",
            customerName = "",
            amount = expense.amount,
            description = "Expense - ${expense.notes.ifEmpty { "No description" }}",
            status = "Completed",
            createdAt = expense.createdAt,
            originalData = expense
        )
    }

    private fun convertCashReportToHistoryTransaction(cashReport: CashReport): HistoryTransaction {
        val transactionType = if (cashReport.transactionType == CashReportType.CASH_IN) {
            HistoryTransactionType.CASH_REPORT_IN
        } else {
            HistoryTransactionType.CASH_REPORT_OUT
        }

        return HistoryTransaction(
            id = cashReport.id,
            transactionType = transactionType,
            date = cashReport.date?.let { timestamp ->
                // Convert timestamp to LocalDate using system timezone
                val instant = java.time.Instant.ofEpochSecond(timestamp.seconds, timestamp.nanos.toLong())
                val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()
                localDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            } ?: "",
            customerId = "",
            customerName = "",
            amount = cashReport.amount,
            description = "Cash Report ${if (transactionType == HistoryTransactionType.CASH_REPORT_IN) "In" else "Out"} - ${cashReport.notes.ifEmpty { "No description" }}",
            status = "Completed",
            createdAt = cashReport.createdAt,
            originalData = cashReport
        )
    }

    private fun calculateDaySummary(transactions: List<HistoryTransaction>): DaySummary {
        var totalSales = 0.0
        var totalPurchases = 0.0
        var totalCashIn = 0.0
        var totalCashOut = 0.0
        var totalDifferenceIn = 0.0
        var totalDifferenceOut = 0.0
        var totalProduction = 0.0
        var totalExpenses = 0.0
        var totalCashReportIn = 0.0
        var totalCashReportOut = 0.0

        transactions.forEach { transaction ->
            when (transaction.transactionType) {
                HistoryTransactionType.SALE -> totalSales += transaction.amount
                HistoryTransactionType.PURCHASE -> totalPurchases += transaction.amount
                HistoryTransactionType.CASH_IN_CUSTOMER, HistoryTransactionType.CASH_IN_SALES -> totalCashIn += transaction.amount
                HistoryTransactionType.CASH_OUT_CUSTOMER, HistoryTransactionType.CASH_OUT_PURCHASES, HistoryTransactionType.CASH_OUT_GENERAL -> totalCashOut += transaction.amount
                HistoryTransactionType.DIFFERENCE_CASH_IN -> totalDifferenceIn += transaction.amount
                HistoryTransactionType.DIFFERENCE_CASH_OUT -> totalDifferenceOut += transaction.amount
                HistoryTransactionType.PRODUCTION -> totalProduction += transaction.amount
                HistoryTransactionType.EXPENSE -> totalExpenses += transaction.amount
                HistoryTransactionType.CASH_REPORT_IN -> totalCashReportIn += transaction.amount
                HistoryTransactionType.CASH_REPORT_OUT -> totalCashReportOut += transaction.amount
            }
        }

        val netAmount = totalSales + totalCashIn + totalDifferenceIn + totalCashReportIn - totalPurchases - totalCashOut - totalDifferenceOut - totalExpenses - totalCashReportOut

        return DaySummary(
            totalSales = totalSales,
            totalPurchases = totalPurchases,
            totalCashIn = totalCashIn,
            totalCashOut = totalCashOut,
            totalDifferenceIn = totalDifferenceIn,
            totalDifferenceOut = totalDifferenceOut,
            totalProduction = totalProduction,
            totalExpenses = totalExpenses,
            totalCashReportIn = totalCashReportIn,
            totalCashReportOut = totalCashReportOut,
            netAmount = netAmount,
            transactionCount = transactions.size
        )
    }
}
