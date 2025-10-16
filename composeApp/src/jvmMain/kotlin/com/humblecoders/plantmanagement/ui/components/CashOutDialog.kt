package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.PurchaseAllocation
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import kotlinx.coroutines.launch

@Composable
fun CashOutDialog(
    purchaseViewModel: PurchaseViewModel,
    entityViewModel: EntityViewModel,
    onDismiss: () -> Unit
) {
    var selectedEntity by remember { mutableStateOf<Entity?>(null) }
    var showEntityDropdown by remember { mutableStateOf(false) }
    var cashOutAmount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var allocations by remember { mutableStateOf<List<PurchaseAllocation>>(emptyList()) }
    var editableAllocations by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isCalculating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val entities = entityViewModel.entityState.entities

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF1F2937),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash Out",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Error message
                if (errorMessage != null) {
                    Card(
                        backgroundColor = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Customer/Entity Selection
                Text(
                    text = "Select Customer/Entity",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { showEntityDropdown = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF374151)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedEntity?.firmName ?: "Select Customer",
                                modifier = Modifier.weight(1f),
                                color = Color(0xFFF9FAFB)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown, 
                                contentDescription = null,
                                tint = Color(0xFFF9FAFB)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showEntityDropdown,
                        onDismissRequest = { showEntityDropdown = false },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .background(Color.White)
                    ) {
                        entities.forEach { entity ->
                            DropdownMenuItem(
                                onClick = {
                                    selectedEntity = entity
                                    showEntityDropdown = false
                                    // Reset allocations when customer changes
                                    allocations = emptyList()
                                    editableAllocations = emptyMap()
                                }
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    Text(
                                        text = entity.firmName,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.Black
                                    )
                                    if (entity.contactPerson.isNotBlank()) {
                                        Text(
                                            text = entity.contactPerson,
                                            fontSize = 12.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Cash Out Amount Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cashOutAmount,
                        onValueChange = { 
                            cashOutAmount = it
                            // Reset allocations when amount changes
                            allocations = emptyList()
                            editableAllocations = emptyMap()
                        },
                        label = { Text("Cash Out Amount (₹)", color = Color(0xFF9CA3AF)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            cursorColor = Color(0xFF10B981),
                            focusedBorderColor = Color(0xFF10B981),
                            unfocusedBorderColor = Color(0xFF4B5563)
                        )
                    )
                    
                    Button(
                        onClick = {
                            if (selectedEntity == null) {
                                errorMessage = "Please select a customer first"
                                return@Button
                            }
                            
                            val amount = cashOutAmount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                isCalculating = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val calculatedAllocations = purchaseViewModel.calculateCashOutAllocations(
                                            amount,
                                            selectedEntity!!.id
                                        )
                                        if (calculatedAllocations.isEmpty()) {
                                            errorMessage = "No pending purchases found for ${selectedEntity!!.firmName}"
                                        } else {
                                            allocations = calculatedAllocations
                                            // Initialize editable allocations
                                            editableAllocations = calculatedAllocations.mapIndexed { index, allocation ->
                                                index to allocation.allocatedAmount.toString()
                                            }.toMap()
                                        }
                                    } catch (e: Exception) {
                                        errorMessage = "Error calculating allocations: ${e.message}"
                                    } finally {
                                        isCalculating = false
                                    }
                                }
                            } else {
                                errorMessage = "Please enter a valid amount greater than 0"
                            }
                        },
                        enabled = !isCalculating && cashOutAmount.isNotBlank() && selectedEntity != null,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (isCalculating) "Calculating..." else "Calculate")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFF10B981),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF4B5563)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Allocations List
                if (allocations.isNotEmpty()) {
                    Text(
                        text = "Purchase Allocations (Editable)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Column headers
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF374151))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Firm Name",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1.5f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Date",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Pending",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(0.8f),
                            fontSize = 14.sp
                        )
                        Text(
                            "Allocated",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                            fontSize = 14.sp
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        itemsIndexed(allocations) { index, allocation ->
                            val pendingAmount = allocation.grandTotal - allocation.previousAmountPaid
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                backgroundColor = Color(0xFF374151),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = allocation.firmName,
                                        color = Color(0xFFF9FAFB),
                                        modifier = Modifier.weight(1.5f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = allocation.purchaseDate,
                                        color = Color(0xFF9CA3AF),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", pendingAmount)}",
                                        color = Color(0xFFFBBF24),
                                        modifier = Modifier.weight(0.8f),
                                        fontSize = 13.sp
                                    )
                                    
                                    OutlinedTextField(
                                        value = editableAllocations[index] ?: allocation.allocatedAmount.toString(),
                                        onValueChange = { newValue ->
                                            editableAllocations = editableAllocations + (index to newValue)
                                        },
                                        modifier = Modifier.weight(1f).height(50.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        colors = TextFieldDefaults.outlinedTextFieldColors(
                                            textColor = Color(0xFF10B981),
                                            cursorColor = Color(0xFF10B981),
                                            focusedBorderColor = Color(0xFF10B981),
                                            unfocusedBorderColor = Color(0xFF6B7280)
                                        ),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Total summary
                    val totalAllocated = editableAllocations.values.mapNotNull { it.toDoubleOrNull() }.sum()
                    val originalAmount = cashOutAmount.toDoubleOrNull() ?: 0.0
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = when {
                            totalAllocated != originalAmount -> Color(0xFFEF4444)
                            else -> Color(0xFF374151)
                        }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Total Allocated:",
                                color = Color(0xFFF9FAFB),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "₹${String.format("%.2f", totalAllocated)}",
                                    color = Color(0xFF10B981),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (totalAllocated != originalAmount) {
                                    Text(
                                        "Must equal ₹${String.format("%.2f", originalAmount)}",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9CA3AF)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amount = cashOutAmount.toDoubleOrNull()
                            if (amount != null && amount > 0 && allocations.isNotEmpty()) {
                                // Update allocations with edited values
                                val updatedAllocations = allocations.mapIndexed { index, allocation ->
                                    val editedAmount = editableAllocations[index]?.toDoubleOrNull() ?: allocation.allocatedAmount
                                    val newAmountPaid = allocation.previousAmountPaid + editedAmount
                                    
                                    val newPaymentStatus = when {
                                        newAmountPaid >= allocation.grandTotal -> com.humblecoders.plantmanagement.data.PaymentStatus.PAID
                                        newAmountPaid > 0 -> com.humblecoders.plantmanagement.data.PaymentStatus.PARTIALLY_PAID
                                        else -> com.humblecoders.plantmanagement.data.PaymentStatus.PENDING
                                    }
                                    
                                    allocation.copy(
                                        allocatedAmount = editedAmount,
                                        newAmountPaid = newAmountPaid,
                                        newPaymentStatus = newPaymentStatus
                                    )
                                }
                                
                                val totalAllocated = updatedAllocations.sumOf { it.allocatedAmount }
                                
                                // Validate that total allocated equals cash out amount
                                if (totalAllocated != amount) {
                                    errorMessage = "Total allocated amount must equal cash out amount (₹${String.format("%.2f", amount)})"
                                } else {
                                    purchaseViewModel.processCashOut(amount, updatedAllocations, notes)
                                    onDismiss()
                                }
                            } else {
                                errorMessage = "Please calculate allocations first"
                            }
                        },
                        enabled = allocations.isNotEmpty() && !purchaseViewModel.purchaseState.isCashingOut,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (purchaseViewModel.purchaseState.isCashingOut) "Processing..." else "Save Cash Out")
                    }
                }
            }
        }
    }
}

