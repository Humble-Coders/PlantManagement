// Create new file: composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/components/CashInOutDifferenceDialog.kt

package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import kotlinx.coroutines.launch

@Composable
fun CashInOutDifferenceDialog(
    saleViewModel: SaleViewModel,
    entityViewModel: EntityViewModel,
    onDismiss: () -> Unit
) {
    var selectedEntity by remember { mutableStateOf<Entity?>(null) }
    var showEntityDropdown by remember { mutableStateOf(false) }
    var transactionType by remember { mutableStateOf(DifferenceTransactionType.CASH_IN) }
    var amount by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var allocations by remember { mutableStateOf<List<SaleAllocation>>(emptyList()) }
    var editableAllocations by remember { mutableStateOf<Map<Int, String>>(emptyMap()) }
    var isCalculating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()
    val entities = entityViewModel.entityState.entities

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
                        text = "Cash In/Out - Difference Amount",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (errorMessage != null) {
                    Card(backgroundColor = Color(0xFFEF4444), modifier = Modifier.fillMaxWidth()) {
                        Text(text = errorMessage!!, color = Color.White, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Customer Selection
                Text("Select Customer", fontSize = 14.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(bottom = 4.dp))

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
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFFF9FAFB))
                        }
                    }

                    DropdownMenu(
                        expanded = showEntityDropdown,
                        onDismissRequest = { showEntityDropdown = false },
                        modifier = Modifier.width(300.dp).background(Color.White)
                    ) {
                        entities.forEach { entity ->
                            DropdownMenuItem(onClick = {
                                selectedEntity = entity
                                showEntityDropdown = false
                                allocations = emptyList()
                                editableAllocations = emptyMap()
                            }) {
                                Text(text = entity.firmName, fontWeight = FontWeight.SemiBold, color = Color.Black)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Transaction Type
                Text("Transaction Type", fontSize = 14.sp, color = Color(0xFF9CA3AF), modifier = Modifier.padding(bottom = 4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = {
                            transactionType = DifferenceTransactionType.CASH_IN
                            allocations = emptyList()
                            editableAllocations = emptyMap()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (transactionType == DifferenceTransactionType.CASH_IN) Color(0xFF10B981) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cash In (Positive Difference)")
                    }
                    OutlinedButton(
                        onClick = {
                            transactionType = DifferenceTransactionType.CASH_OUT
                            allocations = emptyList()
                            editableAllocations = emptyMap()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (transactionType == DifferenceTransactionType.CASH_OUT) Color(0xFFEF4444) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cash Out (Negative Difference)")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = amount,
                        onValueChange = {
                            amount = it
                            allocations = emptyList()
                            editableAllocations = emptyMap()
                        },
                        label = { Text("Amount (₹)", color = Color(0xFF9CA3AF)) },
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

                            val amt = amount.toDoubleOrNull()
                            if (amt != null && amt > 0) {
                            isCalculating = true
                            errorMessage = null
                            coroutineScope.launch {
                                try {
                                    val calculatedAllocations = saleViewModel.calculateCashInOutDifferenceAllocations(
                                        amt,
                                        selectedEntity!!.id,
                                        transactionType
                                    )
                                    if (calculatedAllocations.isEmpty()) {
                                        errorMessage = when (transactionType) {
                                            DifferenceTransactionType.CASH_IN ->
                                                "No sales with positive pending difference found for ${selectedEntity!!.firmName}"
                                            DifferenceTransactionType.CASH_OUT ->
                                                "No sales with negative pending difference found for ${selectedEntity!!.firmName}"
                                        }
                                    } else {
                                        allocations = calculatedAllocations
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
                        enabled = !isCalculating && amount.isNotBlank() && selectedEntity != null,
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
                        text = "Sale Allocations (Editable)",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().background(Color(0xFF374151)).padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Bill #", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("Date", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        Text("Difference", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f), fontSize = 14.sp)
                        Text("Pending", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f), fontSize = 14.sp)
                        Text("Allocated", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    }

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        itemsIndexed(allocations) { index, allocation ->
                            val pendingAmount = if (allocation.differenceAmount < 0) {
                                kotlin.math.abs(allocation.differenceAmount + allocation.previousAmountPaid)
                            } else {
                                kotlin.math.abs(allocation.differenceAmount - allocation.previousAmountPaid)
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                backgroundColor = Color(0xFF374151),
                                shape = RoundedCornerShape(8.dp)
                            ) {
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
                                        modifier = Modifier.weight(0.8f),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
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

                    val totalAllocated = editableAllocations.values.mapNotNull { it.toDoubleOrNull() }.sum()
                    val originalAmount = amount.toDoubleOrNull() ?: 0.0

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
                            Text("Total Allocated:", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9CA3AF)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val amt = amount.toDoubleOrNull()
                            if (amt != null && amt > 0 && allocations.isNotEmpty()) {
                                val updatedAllocations = allocations.mapIndexed { index, allocation ->
                                    val editedAmount = editableAllocations[index]?.toDoubleOrNull() ?: allocation.allocatedAmount
                                    val newAmountPaid = allocation.previousAmountPaid + editedAmount

                                    // Proper logic for positive/negative difference amounts
                                    val newDifferenceStatus = when {
                                        // If difference amount is 0, status should be PAID (nothing to pay)
                                        kotlin.math.abs(allocation.differenceAmount) == 0.0 -> DifferenceStatus.PAID
                                        // For positive difference (customer pays us)
                                        allocation.differenceAmount > 0 -> {
                                            when {
                                                newAmountPaid >= allocation.differenceAmount -> DifferenceStatus.PAID
                                                newAmountPaid > 0 -> DifferenceStatus.PARTIALLY_PAID
                                                else -> DifferenceStatus.PENDING
                                            }
                                        }
                                        // For negative difference (we pay customer)
                                        allocation.differenceAmount < 0 -> {
                                            when {
                                                newAmountPaid >= kotlin.math.abs(allocation.differenceAmount) -> DifferenceStatus.PAID
                                                newAmountPaid > 0 -> DifferenceStatus.PARTIALLY_PAID
                                                else -> DifferenceStatus.PENDING
                                            }
                                        }
                                        else -> DifferenceStatus.PENDING
                                    }

                                    allocation.copy(
                                        allocatedAmount = editedAmount,
                                        newAmountPaid = newAmountPaid,
                                        newPaymentStatus = SaleStatus.valueOf(newDifferenceStatus.name)  // CORRECT - Convert DifferenceStatus to SaleStatus
                                    )
                                }

                                val totalAllocated = updatedAllocations.sumOf { it.allocatedAmount }

                                if (totalAllocated != amt) {
                                    errorMessage = "Total allocated amount must equal ${transactionType.name.lowercase().replace("_", " ")} amount (₹${String.format("%.2f", amt)})"
                                } else {
                                    saleViewModel.processCashInOutDifference(amt, transactionType, updatedAllocations, notes)
                                    onDismiss()
                                }
                            } else {
                                errorMessage = "Please calculate allocations first"
                            }
                        },
                        enabled = allocations.isNotEmpty() && !saleViewModel.saleState.isCashingIn,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (transactionType == DifferenceTransactionType.CASH_IN) Color(0xFF10B981) else Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (saleViewModel.saleState.isCashingIn) "Processing..." else "Save ${transactionType.name.replace("_", " ")}")
                    }
                }
            }
        }
    }
}