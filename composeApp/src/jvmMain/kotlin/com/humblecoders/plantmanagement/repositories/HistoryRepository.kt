package com.humblecoders.plantmanagement.repositories

import com.google.cloud.Timestamp
import com.humblecoders.plantmanagement.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class HistoryRepository(
    private val saleRepository: SaleRepository,
    private val purchaseRepository: PurchaseRepository,
    private val cashTransactionRepository: CashTransactionRepository,
    private val expenseRepository: com.humblecoders.plantmanagement.repositories.ExpenseRepository? = null
) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    /**
     * Get all transactions for a specific date
     * This method aggregates data from all repositories and converts them to HistoryTransaction format
     */
    suspend fun getTransactionsByDate(date: String): Result<DailyTransactionSummary> = withContext(Dispatchers.IO) {
        return@withContext try {
            val transactions = mutableListOf<HistoryTransaction>()
            var totalSales = 0.0
            var totalPurchases = 0.0
            var totalCashIn = 0.0
            var totalCashOut = 0.0
            var totalExpenses = 0.0

            // Get sales for the date
            val salesResult = saleRepository.refreshSales()
            if (salesResult.isSuccess) {
                val sales = salesResult.getOrNull()?.filter { it.saleDate == date } ?: emptyList()
                sales.forEach { sale ->
                    // Add sale transaction
                    transactions.add(
                        HistoryTransaction(
                            id = sale.id,
                            transactionType = HistoryTransactionType.SALE,
                            customerId = sale.customerId,
                            customerName = sale.firmName,
                            amount = sale.totalRevenueAmount,
                            description = "Sale - ${sale.billNumber} (${sale.quantityKg}kg)",
                            date = sale.saleDate,
                            createdAt = sale.createdAt,
                            status = sale.status,
                            notes = sale.notes,
                            additionalDetails = mapOf(
                                "quantityKg" to sale.quantityKg,
                                "billNumber" to sale.billNumber,
                                "portalAmount" to sale.totalPortalAmount,
                                "differenceAmount" to sale.differenceAmount,
                                "saleStatus" to sale.saleStatus.name,
                                "differenceStatus" to sale.differenceStatus.name
                            )
                        )
                    )
                    totalSales += sale.totalRevenueAmount

                    // Add cash in from sales if portal amount was paid
                    if (sale.portalAmountPaid > 0) {
                        transactions.add(
                            HistoryTransaction(
                                id = "${sale.id}_portal_cash_in",
                                transactionType = HistoryTransactionType.CASH_IN_SALES,
                                customerId = sale.customerId,
                                customerName = sale.firmName,
                                amount = sale.portalAmountPaid,
                                description = "Cash In - Portal Payment (${sale.billNumber})",
                                date = sale.saleDate,
                                createdAt = sale.createdAt,
                                status = TransactionStatus.APPROVED,
                                notes = "Portal payment for sale ${sale.billNumber}",
                                additionalDetails = mapOf(
                                    "billNumber" to sale.billNumber,
                                    "originalAmount" to sale.totalPortalAmount
                                )
                            )
                        )
                        totalCashIn += sale.portalAmountPaid
                    }

                    // Add difference amount transaction if applicable
                    if (sale.differenceAmountPaid > 0) {
                        val diffType = if (sale.differenceAmountPaid > 0) HistoryTransactionType.CASH_IN_SALES else HistoryTransactionType.CASH_OUT_PURCHASE
                        transactions.add(
                            HistoryTransaction(
                                id = "${sale.id}_difference",
                                transactionType = HistoryTransactionType.DIFFERENCE_HISTORY,
                                customerId = sale.customerId,
                                customerName = sale.firmName,
                                amount = kotlin.math.abs(sale.differenceAmountPaid),
                                description = "Difference Amount - ${sale.billNumber}",
                                date = sale.saleDate,
                                createdAt = sale.createdAt,
                                status = TransactionStatus.APPROVED,
                                notes = "Difference amount for sale ${sale.billNumber}",
                                additionalDetails = mapOf(
                                    "billNumber" to sale.billNumber,
                                    "differenceAmount" to sale.differenceAmount,
                                    "isPositive" to (sale.differenceAmountPaid > 0)
                                )
                            )
                        )
                        if (sale.differenceAmountPaid > 0) {
                            totalCashIn += sale.differenceAmountPaid
                        } else {
                            totalCashOut += kotlin.math.abs(sale.differenceAmountPaid)
                        }
                    }
                }
            }

            // Get purchases for the date
            val purchasesResult = purchaseRepository.refreshPurchases()
            if (purchasesResult.isSuccess) {
                val purchases = purchasesResult.getOrNull()?.filter { it.purchaseDate == date } ?: emptyList()
                purchases.forEach { purchase ->
                    transactions.add(
                        HistoryTransaction(
                            id = purchase.id,
                            transactionType = HistoryTransactionType.PURCHASE,
                            customerId = purchase.customerId,
                            customerName = purchase.firmName,
                            amount = purchase.grandTotal,
                            description = "Purchase - ${purchase.items.joinToString(", ") { "${it.itemName} (${it.quantity}${it.unit})" }}",
                            date = purchase.purchaseDate,
                            createdAt = purchase.createdAt,
                            status = purchase.status,
                            notes = purchase.notes,
                            additionalDetails = mapOf(
                                "items" to purchase.items.map { item ->
                                    mapOf(
                                        "itemName" to item.itemName,
                                        "quantity" to item.quantity,
                                        "unit" to item.unit,
                                        "pricePerUnit" to item.pricePerUnit,
                                        "totalPrice" to item.totalPrice
                                    )
                                },
                                "paymentStatus" to purchase.paymentStatus.name,
                                "amountPaid" to purchase.amountPaid
                            )
                        )
                    )
                    totalPurchases += purchase.grandTotal

                    // Add cash out for purchase if amount was paid
                    if (purchase.amountPaid > 0) {
                        transactions.add(
                            HistoryTransaction(
                                id = "${purchase.id}_cash_out",
                                transactionType = HistoryTransactionType.CASH_OUT_PURCHASE,
                                customerId = purchase.customerId,
                                customerName = purchase.firmName,
                                amount = purchase.amountPaid,
                                description = "Cash Out - Purchase Payment",
                                date = purchase.purchaseDate,
                                createdAt = purchase.createdAt,
                                status = TransactionStatus.APPROVED,
                                notes = "Payment for purchase",
                                additionalDetails = mapOf(
                                    "originalAmount" to purchase.grandTotal,
                                    "paymentStatus" to purchase.paymentStatus.name
                                )
                            )
                        )
                        totalCashOut += purchase.amountPaid
                    }
                }
            }

            // Get cash transactions for the date
            val cashTransactionsResult = cashTransactionRepository.refreshCashTransactions()
            if (cashTransactionsResult.isSuccess) {
                val cashTransactions = cashTransactionsResult.getOrNull()?.filter { transaction ->
                    transaction.createdAt?.let { timestamp ->
                        val transactionDate = dateFormat.format(Date(timestamp.seconds * 1000))
                        transactionDate == date
                    } ?: false
                } ?: emptyList()

                cashTransactions.forEach { cashTransaction ->
                    val transactionType = when (cashTransaction.transactionType) {
                        CashTransactionType.RECEIVE -> HistoryTransactionType.CASH_IN_CUSTOMER
                        CashTransactionType.GIVE -> HistoryTransactionType.CASH_OUT_CUSTOMER
                    }

                    transactions.add(
                        HistoryTransaction(
                            id = cashTransaction.id,
                            transactionType = transactionType,
                            customerId = cashTransaction.customerId,
                            customerName = cashTransaction.customerName,
                            amount = cashTransaction.amount,
                            description = "Cash ${if (cashTransaction.transactionType == CashTransactionType.RECEIVE) "In" else "Out"} - ${cashTransaction.customerName}",
                            date = date,
                            createdAt = cashTransaction.createdAt,
                            status = TransactionStatus.APPROVED,
                            notes = cashTransaction.note,
                            additionalDetails = mapOf(
                                "previousBalance" to cashTransaction.previousBalance,
                                "newBalance" to cashTransaction.newBalance,
                                "transactionType" to cashTransaction.transactionType.name
                            )
                        )
                    )

                    if (cashTransaction.transactionType == CashTransactionType.RECEIVE) {
                        totalCashIn += cashTransaction.amount
                    } else {
                        totalCashOut += cashTransaction.amount
                    }
                }
            }

            // Get expenses for the date (if expense repository is available)
            expenseRepository?.let { repo ->
                val expensesResult = repo.refreshExpenses()
                if (expensesResult.isSuccess) {
                    val expenses = expensesResult.getOrNull()?.filter { expense ->
                        expense.date?.let { timestamp ->
                            val expenseDate = dateFormat.format(Date(timestamp.seconds * 1000))
                            expenseDate == date
                        } ?: false
                    } ?: emptyList()

                    expenses.forEach { expense ->
                        transactions.add(
                            HistoryTransaction(
                                id = expense.id,
                                transactionType = HistoryTransactionType.EXPENSE,
                                customerId = "",
                                customerName = "",
                                amount = expense.amount,
                                description = "Expense - ${expense.categoryId}",
                                date = date,
                                createdAt = expense.createdAt,
                                status = TransactionStatus.APPROVED,
                                notes = expense.notes,
                                additionalDetails = mapOf(
                                    "categoryId" to expense.categoryId,
                                    "documentUrls" to expense.documentUrls
                                )
                            )
                        )
                        totalExpenses += expense.amount
                    }
                }
            }

            // Sort transactions by creation time
            val sortedTransactions = transactions.sortedByDescending { it.createdAt?.seconds ?: 0L }

            val summary = DailyTransactionSummary(
                date = date,
                totalSales = totalSales,
                totalPurchases = totalPurchases,
                totalCashIn = totalCashIn,
                totalCashOut = totalCashOut,
                totalExpenses = totalExpenses,
                netCashFlow = totalCashIn - totalCashOut - totalExpenses,
                transactionCount = sortedTransactions.size,
                transactions = sortedTransactions
            )

            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get transactions for a date range
     */
    suspend fun getTransactionsByDateRange(startDate: String, endDate: String): Result<List<DailyTransactionSummary>> = withContext(Dispatchers.IO) {
        return@withContext try {
            val summaries = mutableListOf<DailyTransactionSummary>()
            val start = dateFormat.parse(startDate)
            val end = dateFormat.parse(endDate)
            
            val calendar = Calendar.getInstance()
            calendar.time = start
            
            while (calendar.time <= end) {
                val currentDate = dateFormat.format(calendar.time)
                val result = getTransactionsByDate(currentDate)
                if (result.isSuccess) {
                    summaries.add(result.getOrNull()!!)
                }
                calendar.add(Calendar.DAY_OF_MONTH, 1)
            }
            
            Result.success(summaries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
