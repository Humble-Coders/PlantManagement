package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PendingBillsScreen(
    saleViewModel: SaleViewModel,
    userRole: UserRole? = null
) {
    val saleState = saleViewModel.saleState
    var showClearPartialDialog by remember { mutableStateOf(false) }
    var saleToClearPartial by remember { mutableStateOf<Sale?>(null) }
    var showClearAllDialog by remember { mutableStateOf(false) }
    var saleToClearAll by remember { mutableStateOf<Sale?>(null) }
    
    // Filter sales with PENDING_BILLED status
    val pendingBills = saleState.sales.filter { it.billingStatus == BillingStatus.PENDING_BILLED }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .padding(24.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Pending Bills",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )
            
            Text(
                text = "${pendingBills.size} pending bills",
                fontSize = 16.sp,
                color = Color(0xFF9CA3AF)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        if (pendingBills.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = "No pending bills",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Pending Bills",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All sales have been fully billed",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        } else {
            // Pending bills list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingBills) { sale ->
                    PendingBillCard(
                        sale = sale,
                        onClearPartialClick = {
                            saleToClearPartial = sale
                            showClearPartialDialog = true
                        },
                        onClearAllClick = {
                            saleToClearAll = sale
                            showClearAllDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Clear Partial Dialog
    if (showClearPartialDialog && saleToClearPartial != null) {
        ClearPartialDialog(
            sale = saleToClearPartial!!,
            onDismiss = {
                showClearPartialDialog = false
                saleToClearPartial = null
            },
            onClearBill = { quantityToClear ->
                saleViewModel.clearBill(saleToClearPartial!!.id, quantityToClear)
                showClearPartialDialog = false
                saleToClearPartial = null
            }
        )
    }
    
    // Clear All Dialog
    if (showClearAllDialog && saleToClearAll != null) {
        ClearAllDialog(
            sale = saleToClearAll!!,
            onDismiss = {
                showClearAllDialog = false
                saleToClearAll = null
            },
            onClearAll = {
                val remainingQuantity = saleToClearAll!!.quantityKg - saleToClearAll!!.clearedInventory
                saleViewModel.clearBill(saleToClearAll!!.id, remainingQuantity)
                showClearAllDialog = false
                saleToClearAll = null
            }
        )
    }
}

@Composable
fun PendingBillCard(
    sale: Sale,
    onClearPartialClick: () -> Unit,
    onClearAllClick: () -> Unit
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
            // Header row with customer name and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = sale.firmName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "Bill #${sale.billNumber}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                Text(
                    text = sale.saleDate,
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity details
                Column {
                    Text(
                        text = "Quantity",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.2f", sale.clearedInventory)} / ${String.format("%.2f", sale.quantityKg)} kg",
                        fontSize = 16.sp,
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Portal amount
                Column {
                    Text(
                        text = "Portal Amount",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹ ${String.format("%.2f", sale.totalPortalAmount)}",
                        fontSize = 16.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Billing status
                Column {
                    Text(
                        text = "Status",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Card(
                        backgroundColor = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PENDING",
                            fontSize = 12.sp,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { onClearPartialClick() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF3B82F6)),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(
                        text = "Clear Partial",
                        color = Color(0xFFF9FAFB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Button(
                    onClick = { onClearAllClick() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(
                        text = "Clear All",
                        color = Color(0xFFF9FAFB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ClearPartialDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onClearBill: (Double) -> Unit
) {
    var quantityToClear by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(400.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Clear Partial Bill",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sale details
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = sale.firmName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "Bill #${sale.billNumber}",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Quantity: ${String.format("%.2f", sale.quantityKg)} kg",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "Cleared: ${String.format("%.2f", sale.clearedInventory)} kg",
                                fontSize = 14.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                        Text(
                            text = "Remaining: ${String.format("%.2f", sale.quantityKg - sale.clearedInventory)} kg",
                            fontSize = 14.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity input
                OutlinedTextField(
                    value = quantityToClear,
                    onValueChange = { 
                        quantityToClear = it
                        errorMessage = ""
                    },
                    label = { Text("Quantity to Clear (kg)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF10B981)
                    ),
                    singleLine = true
                )
                
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF9CA3AF))
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            val quantity = quantityToClear.toDoubleOrNull()
                            val remainingQuantity = sale.quantityKg - sale.clearedInventory
                            
                            when {
                                quantity == null || quantity <= 0 -> {
                                    errorMessage = "Please enter a valid quantity"
                                }
                                quantity > remainingQuantity -> {
                                    errorMessage = "Cannot clear more than remaining quantity (${String.format("%.2f", remainingQuantity)} kg)"
                                }
                                else -> {
                                    onClearBill(quantity)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Clear Partial", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ClearAllDialog(
    sale: Sale,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(400.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Clear All Bill",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sale details
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = sale.firmName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "Bill #${sale.billNumber}",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Quantity: ${String.format("%.2f", sale.quantityKg)} kg",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "Cleared: ${String.format("%.2f", sale.clearedInventory)} kg",
                                fontSize = 14.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                        Text(
                            text = "Remaining: ${String.format("%.2f", sale.quantityKg - sale.clearedInventory)} kg",
                            fontSize = 14.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Warning message
                Card(
                    backgroundColor = Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "This will clear the entire remaining quantity (${String.format("%.2f", sale.quantityKg - sale.clearedInventory)} kg)",
                            color = Color(0xFFFCA5A5),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF9CA3AF))
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = onClearAll,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Clear All", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
