// Create new file: composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/components/CashInHistoryDialog.kt

package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CashInHistoryDialog(
    saleViewModel: SaleViewModel,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }

    val revenueHistory = saleViewModel.saleState.cashInRevenueHistory.sortedByDescending { it.createdAt?.seconds ?: 0L }
    val differenceHistory = saleViewModel.saleState.cashInOutDifferenceHistory.sortedByDescending { it.createdAt?.seconds ?: 0L }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF1F2937),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash In History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = selectedTab,
                    backgroundColor = Color(0xFF374151),
                    contentColor = Color(0xFF10B981)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Revenue (${revenueHistory.size})", color = if (selectedTab == 0) Color(0xFF10B981) else Color(0xFF9CA3AF)) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Difference (${differenceHistory.size})", color = if (selectedTab == 1) Color(0xFF10B981) else Color(0xFF9CA3AF)) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content
                when (selectedTab) {
                    0 -> {
                        if (revenueHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No revenue cash in history available", color = Color(0xFF9CA3AF), fontSize = 16.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(revenueHistory) { cashIn ->
                                    CashInRevenueHistoryItem(cashIn = cashIn)
                                }
                            }
                        }
                    }
                    1 -> {
                        if (differenceHistory.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No difference cash in/out history available", color = Color(0xFF9CA3AF), fontSize = 16.sp)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                items(differenceHistory) { transaction ->
                                    CashInOutDifferenceHistoryItem(transaction = transaction)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashInRevenueHistoryItem(cashIn: CashInRevenue) {
    var expanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    val formattedDate = cashIn.createdAt?.let {
        Instant.ofEpochSecond(it.seconds)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    } ?: "Unknown Date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = formattedDate, color = Color(0xFF9CA3AF), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%.2f", cashIn.totalAmount)}",
                        color = Color(0xFF10B981),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF10B981))
                ) {
                    Text(if (expanded) "Hide Details" else "Show Details")
                }
            }

            if (cashIn.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${cashIn.notes}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cashIn.saleAllocations.size} Sale(s) Paid",
                    color = Color(0xFFF9FAFB),
                    fontSize = 14.sp
                )
                Text(
                    text = "Total: ₹${String.format("%.2f", cashIn.saleAllocations.sumOf { it.allocatedAmount })}",
                    color = Color(0xFFFBBF24),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF4B5563), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1F2937)).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Bill #", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("Date", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("Amount", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f), fontSize = 13.sp)
                    Text("Status", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                }

                cashIn.saleAllocations.forEach { allocation ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = allocation.billNumber, color = Color(0xFFF9FAFB), modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(text = allocation.saleDate, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(
                            text = "₹${String.format("%.2f", allocation.allocatedAmount)}",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        val statusColor = when (allocation.newPaymentStatus.name) {
                            "PAID" -> Color(0xFF10B981)
                            "PARTIALLY_PAID" -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        }

                        Card(
                            backgroundColor = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = allocation.newPaymentStatus.name.replace("_", " "),
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CashInOutDifferenceHistoryItem(transaction: CashInOutDifference) {
    var expanded by remember { mutableStateOf(false) }

    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    val formattedDate = transaction.createdAt?.let {
        Instant.ofEpochSecond(it.seconds)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    } ?: "Unknown Date"

    val isIncoming = transaction.transactionType == DifferenceTransactionType.CASH_IN

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = formattedDate, color = Color(0xFF9CA3AF), fontSize = 14.sp)
                        Card(
                            backgroundColor = if (isIncoming) Color(0xFF10B981).copy(alpha = 0.2f) else Color(0xFFEF4444).copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = if (isIncoming) "CASH IN" else "CASH OUT",
                                color = if (isIncoming) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%.2f", transaction.totalAmount)}",
                        color = if (isIncoming) Color(0xFF10B981) else Color(0xFFEF4444),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF10B981))
                ) {
                    Text(if (expanded) "Hide Details" else "Show Details")
                }
            }

            if (transaction.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${transaction.notes}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${transaction.saleAllocations.size} Sale(s) Processed",
                    color = Color(0xFFF9FAFB),
                    fontSize = 14.sp
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF4B5563), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF1F2937)).padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Bill #", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("Date", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                    Text("Diff", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.7f), fontSize = 13.sp)
                    Text("Allocated", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f), fontSize = 13.sp)
                    Text("Status", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 13.sp)
                }

                transaction.saleAllocations.forEach { allocation ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = allocation.billNumber, color = Color(0xFFF9FAFB), modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(text = allocation.saleDate, color = Color(0xFF9CA3AF), modifier = Modifier.weight(1f), fontSize = 13.sp)
                        Text(
                            text = "${if (allocation.differenceAmount >= 0) "+" else ""}₹${String.format("%.2f", allocation.differenceAmount)}",
                            color = if (allocation.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.weight(0.7f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "₹${String.format("%.2f", allocation.allocatedAmount)}",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        val statusColor = when (allocation.newPaymentStatus.name) {
                            "PAID" -> Color(0xFF10B981)
                            "PARTIALLY_PAID" -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        }

                        Card(
                            backgroundColor = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = allocation.newPaymentStatus.name.replace("_", " "),
                                color = statusColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}