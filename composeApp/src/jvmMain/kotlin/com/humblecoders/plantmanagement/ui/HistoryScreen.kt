package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.humblecoders.plantmanagement.ui.components.*
import com.humblecoders.plantmanagement.viewmodels.HistoryViewModel
import java.text.NumberFormat
import java.util.*

@Composable
fun HistoryScreen(
    historyViewModel: HistoryViewModel
) {
    val historyState = historyViewModel.historyState
    var showTransactionDetails by remember { mutableStateOf<HistoryTransaction?>(null) }

    // Clear messages after showing
    LaunchedEffect(historyState.successMessage, historyState.error) {
        if (historyState.successMessage != null || historyState.error != null) {
            kotlinx.coroutines.delay(3000)
            historyViewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transaction History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Refresh button
                    IconButton(
                        onClick = { historyViewModel.refresh() }
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color(0xFF06B6D4)
                        )
                    }

                    // Toggle date range view
                    IconButton(
                        onClick = { historyViewModel.toggleDateRangeView() }
                    ) {
                        Icon(
                            if (historyState.showDateRange) Icons.Default.DateRange else Icons.Default.Today,
                            contentDescription = "Toggle Date Range",
                            tint = Color(0xFF06B6D4)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date selection
            if (historyState.showDateRange) {
                DateRangeSelection(
                    startDate = historyState.startDate,
                    endDate = historyState.endDate,
                    onStartDateChange = { historyViewModel.setDateRange(it, historyState.endDate) },
                    onEndDateChange = { historyViewModel.setDateRange(historyState.startDate, it) }
                )
            } else {
                SingleDateSelection(
                    selectedDate = historyState.selectedDate,
                    onDateChange = { historyViewModel.setSelectedDate(it) },
                    onPreviousDay = { historyViewModel.setSelectedDate(historyViewModel.getYesterdayDate()) },
                    onNextDay = { 
                        // Simple next day logic - in a real app you'd use proper date arithmetic
                        val today = historyViewModel.getTodayDate()
                        if (historyState.selectedDate != today) {
                            // For simplicity, just go to today
                            historyViewModel.setSelectedDate(today)
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Error message
            historyState.error?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFFEE2E2),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = error,
                            color = Color(0xFFDC2626),
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Success message
            historyState.successMessage?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = message,
                            color = Color(0xFF059669),
                            fontSize = 14.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading indicator
            if (historyState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF06B6D4)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Content based on view mode
            if (historyState.showDateRange) {
                DateRangeView(historyState.dateRangeSummaries)
            } else {
                SingleDateView(
                    summary = historyState.dailySummary,
                    filteredTransactions = historyState.filteredTransactions,
                    selectedTransactionType = historyState.selectedTransactionType,
                    onTransactionTypeFilter = { historyViewModel.filterTransactionsByType(it) },
                    onTransactionClick = { showTransactionDetails = it }
                )
            }
        }

        // Transaction details dialog
        showTransactionDetails?.let { transaction ->
            TransactionDetailsDialog(
                transaction = transaction,
                onDismiss = { showTransactionDetails = null }
            )
        }
    }
}

@Composable
private fun SingleDateSelection(
    selectedDate: String,
    onDateChange: (String) -> Unit,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = onPreviousDay) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Previous Day",
                    tint = Color(0xFF06B6D4)
                )
            }

            DatePickerButton(
                selectedDate = selectedDate,
                onDateSelected = onDateChange
            )

            IconButton(onClick = onNextDay) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Next Day",
                    tint = Color(0xFF06B6D4)
                )
            }
        }

        // Quick date buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { onDateChange("2024-01-01") }, // Placeholder - would use actual dates
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1F2937)
                )
            ) {
                Text("Today", fontSize = 12.sp)
            }
            OutlinedButton(
                onClick = { onDateChange("2024-01-01") }, // Placeholder - would use actual dates
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color(0xFF1F2937)
                )
            ) {
                Text("Yesterday", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DateRangeSelection(
    startDate: String,
    endDate: String,
    onStartDateChange: (String) -> Unit,
    onEndDateChange: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = "Start Date",
                fontSize = 12.sp,
                color = Color.Gray
            )
            DatePickerButton(
                selectedDate = startDate,
                onDateSelected = onStartDateChange
            )
        }

        Column {
            Text(
                text = "End Date",
                fontSize = 12.sp,
                color = Color.Gray
            )
            DatePickerButton(
                selectedDate = endDate,
                onDateSelected = onEndDateChange
            )
        }
    }
}

@Composable
private fun SingleDateView(
    summary: com.humblecoders.plantmanagement.data.DailyTransactionSummary?,
    filteredTransactions: List<HistoryTransaction>,
    selectedTransactionType: HistoryTransactionType?,
    onTransactionTypeFilter: (HistoryTransactionType?) -> Unit,
    onTransactionClick: (HistoryTransaction) -> Unit
) {
    summary?.let { dailySummary ->
        // Summary card
        TransactionSummaryCard(
            summary = dailySummary,
            onTransactionClick = onTransactionClick
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filter chips
        TransactionTypeFilterChips(
            selectedType = selectedTransactionType,
            onTypeSelected = onTransactionTypeFilter
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions list
        if (filteredTransactions.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFF9FAFB),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "No transactions found",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Try selecting a different date or filter",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredTransactions) { transaction ->
                    TransactionItem(
                        transaction = transaction,
                        onClick = { onTransactionClick(transaction) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DateRangeView(
    summaries: List<com.humblecoders.plantmanagement.data.DailyTransactionSummary>
) {
    if (summaries.isEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFF9FAFB),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.DateRange,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No transactions found",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
                Text(
                    text = "Try selecting a different date range",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(summaries) { summary ->
                TransactionSummaryCard(summary = summary)
            }
        }
    }
}

@Composable
private fun TransactionDetailsDialog(
    transaction: HistoryTransaction,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = transaction.transactionType.name.replace("_", " "),
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text("Amount: ${NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(transaction.amount)}")
                if (transaction.customerName.isNotEmpty()) {
                    Text("Customer: ${transaction.customerName}")
                }
                Text("Date: ${transaction.date}")
                if (transaction.notes.isNotEmpty()) {
                    Text("Notes: ${transaction.notes}")
                }
                Text("Description: ${transaction.description}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
