package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.HistoryTransaction
import com.humblecoders.plantmanagement.data.HistoryTransactionType
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.viewmodels.HistoryViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel
) {
    val historyState = historyViewModel.historyState
    
    // Load history for selected date when screen is first displayed
    LaunchedEffect(Unit) {
        historyViewModel.loadHistoryForDate(historyState.selectedDate)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Text(
                text = "Transaction History",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Date Picker Section
            DatePickerSection(
                selectedDate = historyState.selectedDate,
                onDateSelected = { date ->
                    historyViewModel.selectDate(date)
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Error/Success Messages
            historyState.error?.let { error ->
                ErrorCard(error = error, onDismiss = { historyViewModel.clearError() })
                Spacer(modifier = Modifier.height(16.dp))
            }

            historyState.successMessage?.let { message ->
                SuccessCard(message = message, onDismiss = { historyViewModel.clearSuccessMessage() })
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading State
            if (historyState.isLoading) {
                LoadingCard()
            } else {
                // Day Summary Cards
                historyState.dayHistory?.let { dayHistory ->
                    DaySummaryCards(daySummary = dayHistory.summary)
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    // Transaction Lists
                    TransactionListsSection(
                        dayHistory = dayHistory,
                        historyViewModel = historyViewModel
                    )
                } ?: run {
                    // No data state
                    NoDataCard()
                }
            }
        }
    }
}

@Composable
fun DatePickerSection(
    selectedDate: String,
    onDateSelected: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Select Date",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            DatePicker(
                selectedDate = LocalDate.parse(selectedDate),
                onDateSelected = { date ->
                    onDateSelected(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                }
            )
        }
    }
}

@Composable
fun DaySummaryCards(daySummary: com.humblecoders.plantmanagement.data.DaySummary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row - Sales and Purchases
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Sales",
                amount = daySummary.totalSales,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            
            SummaryCard(
                title = "Purchases",
                amount = daySummary.totalPurchases,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Second row - Cash In and Cash Out
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Cash In",
                amount = daySummary.totalCashIn,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            
            SummaryCard(
                title = "Cash Out",
                amount = daySummary.totalCashOut,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Third row - Difference amounts
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Difference In",
                amount = daySummary.totalDifferenceIn,
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            
            SummaryCard(
                title = "Difference Out",
                amount = daySummary.totalDifferenceOut,
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Fourth row - Net Amount and Transaction Count
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryCard(
                title = "Net Amount",
                amount = daySummary.netAmount,
                color = if (daySummary.netAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
            
            SummaryCard(
                title = "Transactions",
                amount = daySummary.transactionCount.toDouble(),
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f),
                showCurrency = false
            )
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier,
    showCurrency: Boolean = true
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = if (showCurrency) "₹ ${String.format("%.2f", amount)}" else "${amount.toInt()}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionListsSection(
    dayHistory: com.humblecoders.plantmanagement.data.DayHistory,
    historyViewModel: HistoryViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
                    Text(
                        text = "Transaction Details",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
            
            // Sales Records
            val sales = historyViewModel.getSalesTransactions()
            if (sales.isNotEmpty()) {
                TransactionTypeSection(
                    title = "Sales Records",
                    transactions = sales,
                    icon = Icons.Default.ShoppingCart,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Purchase Records
            val purchases = historyViewModel.getPurchaseTransactions()
            if (purchases.isNotEmpty()) {
                TransactionTypeSection(
                    title = "Purchase Records",
                    transactions = purchases,
                    icon = Icons.Default.ShoppingBag,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Cash In from Sales
            val cashInSales = historyViewModel.getSalesModuleTransactions()
            if (cashInSales.isNotEmpty()) {
                TransactionTypeSection(
                    title = "Cash In from Sales",
                    transactions = cashInSales,
                    icon = Icons.Default.TrendingUp,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Cash Out from Purchases
            val cashOutPurchases = historyViewModel.getPurchaseModuleTransactions()
            if (cashOutPurchases.isNotEmpty()) {
                TransactionTypeSection(
                    title = "Cash Out from Purchases",
                    transactions = cashOutPurchases,
                    icon = Icons.Default.TrendingDown,
                    color = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Customer Transactions
            val customerTransactions = historyViewModel.getCustomerTransactions()
            if (customerTransactions.isNotEmpty()) {
                TransactionTypeSection(
                    title = "Customer Transactions",
                    transactions = customerTransactions,
                    icon = Icons.Default.People,
                    color = Color(0xFF3B82F6)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Difference History
            val differences = historyViewModel.getDifferenceTransactions()
            if (differences.isNotEmpty()) {
                TransactionTypeSection(
                    title = "Difference History",
                    transactions = differences,
                    icon = Icons.Default.Balance,
                    color = Color(0xFF8B5CF6)
                )
            }
            
            // Show message if no transactions
            if (dayHistory.transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No transactions found for this date",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionTypeSection(
    title: String,
    transactions: List<HistoryTransaction>,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF9FAFB)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "(${transactions.size})",
                fontSize = 14.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            transactions.forEach { transaction ->
                HistoryTransactionCard(transaction = transaction)
            }
        }
    }
}

@Composable
fun HistoryTransactionCard(transaction: HistoryTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row with type and amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.transactionType.name.replace("_", " "),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTransactionTypeColor(transaction.transactionType)
                    )
                    Text(
                        text = transaction.description,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹ ${String.format("%.2f", transaction.amount)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = getTransactionAmountColor(transaction.transactionType)
                    )
                    Text(
                        text = transaction.status,
                        fontSize = 11.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
            
            // Customer and date info
            if (transaction.customerName.isNotEmpty()) {
                Text(
                    text = "Customer: ${transaction.customerName}",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            Text(
                text = "Date: ${transaction.date}",
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Loading transactions...",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun NoDataCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "No Data",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No transactions found",
                    color = Color(0xFF9CA3AF),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Select a different date to view transactions",
                    color = Color(0xFF6B7280),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ErrorCard(
    error: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFFEF4444).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Error,
                contentDescription = "Error",
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = error,
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Dismiss",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun SuccessCard(
    message: String,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF10B981).copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.CheckCircle,
                contentDescription = "Success",
                tint = Color(0xFF10B981),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = message,
                color = Color(0xFF10B981),
                fontSize = 12.sp,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Clear,
                    contentDescription = "Dismiss",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun getTransactionTypeColor(type: HistoryTransactionType): Color {
    return when (type) {
        HistoryTransactionType.SALE -> Color(0xFF10B981)
        HistoryTransactionType.PURCHASE -> Color(0xFFEF4444)
        HistoryTransactionType.CASH_IN_CUSTOMER, HistoryTransactionType.CASH_IN_SALES -> Color(0xFF10B981)
        HistoryTransactionType.CASH_OUT_CUSTOMER, HistoryTransactionType.CASH_OUT_PURCHASES, HistoryTransactionType.CASH_OUT_GENERAL -> Color(0xFFEF4444)
        HistoryTransactionType.DIFFERENCE_CASH_IN -> Color(0xFF10B981)
        HistoryTransactionType.DIFFERENCE_CASH_OUT -> Color(0xFFEF4444)
    }
}

private fun getTransactionAmountColor(type: HistoryTransactionType): Color {
    return when (type) {
        HistoryTransactionType.SALE, HistoryTransactionType.CASH_IN_CUSTOMER, HistoryTransactionType.CASH_IN_SALES, HistoryTransactionType.DIFFERENCE_CASH_IN -> Color(0xFF10B981)
        HistoryTransactionType.PURCHASE, HistoryTransactionType.CASH_OUT_CUSTOMER, HistoryTransactionType.CASH_OUT_PURCHASES, HistoryTransactionType.CASH_OUT_GENERAL, HistoryTransactionType.DIFFERENCE_CASH_OUT -> Color(0xFFEF4444)
    }
}
