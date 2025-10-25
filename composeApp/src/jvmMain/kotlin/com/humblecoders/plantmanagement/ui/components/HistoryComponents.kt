package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.DailyTransactionSummary
import com.humblecoders.plantmanagement.data.HistoryTransaction
import com.humblecoders.plantmanagement.data.HistoryTransactionType
import java.text.NumberFormat
import java.util.*

/**
 * Reusable component for displaying transaction summary cards
 */
@Composable
fun TransactionSummaryCard(
    summary: DailyTransactionSummary,
    modifier: Modifier = Modifier,
    onTransactionClick: (HistoryTransaction) -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = 4.dp,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Date header
            Text(
                text = summary.date,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Summary row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Transactions",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "${summary.transactionCount}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Net Cash Flow",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(summary.netCashFlow),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (summary.netCashFlow >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Transaction type breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TransactionTypeItem(
                    type = "Sales",
                    amount = summary.totalSales,
                    color = Color(0xFF3B82F6)
                )
                TransactionTypeItem(
                    type = "Purchases",
                    amount = summary.totalPurchases,
                    color = Color(0xFF8B5CF6)
                )
                TransactionTypeItem(
                    type = "Cash In",
                    amount = summary.totalCashIn,
                    color = Color(0xFF10B981)
                )
                TransactionTypeItem(
                    type = "Cash Out",
                    amount = summary.totalCashOut,
                    color = Color(0xFFEF4444)
                )
            }
        }
    }
}

@Composable
private fun TransactionTypeItem(
    type: String,
    amount: Double,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = type,
            fontSize = 10.sp,
            color = Color.Gray
        )
        Text(
            text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(amount),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = color
        )
    }
}

/**
 * Reusable component for displaying individual transaction items
 */
@Composable
fun TransactionItem(
    transaction: HistoryTransaction,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 2.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Transaction type icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(getTransactionTypeColor(transaction.transactionType).copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getTransactionTypeIcon(transaction.transactionType),
                    contentDescription = null,
                    tint = getTransactionTypeColor(transaction.transactionType),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Transaction details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = transaction.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (transaction.customerName.isNotEmpty()) {
                    Text(
                        text = transaction.customerName,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                if (transaction.notes.isNotEmpty()) {
                    Text(
                        text = transaction.notes,
                        fontSize = 11.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            // Amount
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = NumberFormat.getCurrencyInstance(Locale("en", "IN")).format(transaction.amount),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = getTransactionTypeColor(transaction.transactionType)
                )
                
                Text(
                    text = transaction.transactionType.name.replace("_", " "),
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

/**
 * Reusable component for transaction type filter chips
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionTypeFilterChips(
    selectedType: HistoryTransactionType?,
    onTypeSelected: (HistoryTransactionType?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All transactions chip
        FilterChip(
            onClick = { onTypeSelected(null) },
            selected = selectedType == null,
            leadingIcon = {
                Icon(
                    Icons.Default.List,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
        ) {
            Text("All")
        }
        
        // Individual type chips
        HistoryTransactionType.values().forEach { type ->
            FilterChip(
                onClick = { onTypeSelected(type) },
                selected = selectedType == type,
                leadingIcon = {
                    Icon(
                        getTransactionTypeIcon(type),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            ) {
                Text(type.name.replace("_", " "))
            }
        }
    }
}

/**
 * Get color for transaction type
 */
private fun getTransactionTypeColor(type: HistoryTransactionType): Color {
    return when (type) {
        HistoryTransactionType.SALE -> Color(0xFF3B82F6)
        HistoryTransactionType.PURCHASE -> Color(0xFF8B5CF6)
        HistoryTransactionType.CASH_IN_CUSTOMER, HistoryTransactionType.CASH_IN_SALES -> Color(0xFF10B981)
        HistoryTransactionType.CASH_OUT_CUSTOMER, HistoryTransactionType.CASH_OUT_PURCHASE -> Color(0xFFEF4444)
        HistoryTransactionType.DIFFERENCE_HISTORY -> Color(0xFFF59E0B)
        HistoryTransactionType.EXPENSE -> Color(0xFFEF4444)
    }
}

/**
 * Get icon for transaction type
 */
private fun getTransactionTypeIcon(type: HistoryTransactionType) = when (type) {
    HistoryTransactionType.SALE -> Icons.Default.ShoppingCart
    HistoryTransactionType.PURCHASE -> Icons.Default.ShoppingBag
    HistoryTransactionType.CASH_IN_CUSTOMER, HistoryTransactionType.CASH_IN_SALES -> Icons.Default.TrendingUp
    HistoryTransactionType.CASH_OUT_CUSTOMER, HistoryTransactionType.CASH_OUT_PURCHASE -> Icons.Default.TrendingDown
    HistoryTransactionType.DIFFERENCE_HISTORY -> Icons.Default.Calculate
    HistoryTransactionType.EXPENSE -> Icons.Default.Receipt
}

/**
 * Reusable component for date picker
 */
@Composable
fun DatePickerButton(
    selectedDate: String,
    onDateSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    OutlinedButton(
        onClick = { showDatePicker = true },
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = Color(0xFF1F2937)
        )
    ) {
        Icon(
            Icons.Default.DateRange,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(selectedDate)
    }
    
    if (showDatePicker) {
        // Note: In a real implementation, you would use a proper date picker
        // For now, we'll use a simple dialog with date input
        AlertDialog(
            onDismissRequest = { showDatePicker = false },
            title = { Text("Select Date") },
            text = {
                Text("Date picker implementation would go here")
            },
            confirmButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("OK")
                }
            }
        )
    }
}
