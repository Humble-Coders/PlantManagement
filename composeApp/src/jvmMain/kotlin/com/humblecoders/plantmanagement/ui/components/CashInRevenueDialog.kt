// Create new file: composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/components/CashInRevenueDialog.kt

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
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.data.SaleAllocation
import com.humblecoders.plantmanagement.ui.components.SearchableCustomerDropdown
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import kotlinx.coroutines.launch

@Composable
fun CashInRevenueDialog(
    saleViewModel: SaleViewModel,
    entityViewModel: EntityViewModel,
    onDismiss: () -> Unit,
    preselectedCustomer: Entity? = null
) {
    var selectedEntity by remember(preselectedCustomer) { mutableStateOf<Entity?>(preselectedCustomer) }
    var showEntityDropdown by remember { mutableStateOf(false) }
    var cashInAmount by remember { mutableStateOf("") }
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
                        text = "Cash In - Revenue",
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
                SearchableCustomerDropdown(
                    customers = entities,
                    selectedCustomerId = selectedEntity?.id ?: "",
                    onCustomerSelected = { entityId ->
                        selectedEntity = entities.find { it.id == entityId }
                        allocations = emptyList()
                        editableAllocations = emptyMap()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Select Customer",
                    label = "Select Customer"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Cash In Amount
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = cashInAmount,
                        onValueChange = {
                            cashInAmount = it
                            allocations = emptyList()
                            editableAllocations = emptyMap()
                        },
                        label = { Text("Cash In Amount (₹)", color = Color(0xFF9CA3AF)) },
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

                            val amount = cashInAmount.toDoubleOrNull()
                            if (amount != null && amount > 0) {
                                isCalculating = true
                                errorMessage = null
                                coroutineScope.launch {
                                    try {
                                        val calculatedAllocations = saleViewModel.calculateCashInRevenueAllocations(
                                            amount,
                                            selectedEntity!!.id
                                        )
                                        if (calculatedAllocations.isEmpty()) {
                                            errorMessage = "No pending sales found for ${selectedEntity!!.firmName}"
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
                        enabled = !isCalculating && cashInAmount.isNotBlank() && selectedEntity != null,
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
                        Text("Pending", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(0.8f), fontSize = 14.sp)
                        Text("Allocated", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    }

                    LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        itemsIndexed(allocations) { index, allocation ->
                            val pendingAmount = allocation.totalPortalAmount - allocation.previousAmountPaid

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
                    val originalAmount = cashInAmount.toDoubleOrNull() ?: 0.0

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
                            val amount = cashInAmount.toDoubleOrNull()
                            if (amount != null && amount > 0 && allocations.isNotEmpty()) {
                                val updatedAllocations = allocations.mapIndexed { index, allocation ->
                                    val editedAmount = editableAllocations[index]?.toDoubleOrNull() ?: allocation.allocatedAmount
                                    val newAmountPaid = allocation.previousAmountPaid + editedAmount

                                    val newPaymentStatus = when {
                                        newAmountPaid >= allocation.totalPortalAmount -> com.humblecoders.plantmanagement.data.SaleStatus.PAID
                                        newAmountPaid > 0 -> com.humblecoders.plantmanagement.data.SaleStatus.PARTIALLY_PAID
                                        else -> com.humblecoders.plantmanagement.data.SaleStatus.PENDING
                                    }

                                    allocation.copy(
                                        allocatedAmount = editedAmount,
                                        newAmountPaid = newAmountPaid,
                                        newPaymentStatus = newPaymentStatus
                                    )
                                }

                                val totalAllocated = updatedAllocations.sumOf { it.allocatedAmount }

                                if (totalAllocated != amount) {
                                    errorMessage = "Total allocated amount must equal cash in amount (₹${String.format("%.2f", amount)})"
                                } else {
                                    saleViewModel.processCashInRevenue(amount, updatedAllocations, notes)
                                    onDismiss()
                                }
                            } else {
                                errorMessage = "Please calculate allocations first"
                            }
                        },
                        enabled = allocations.isNotEmpty() && !saleViewModel.saleState.isCashingIn,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (saleViewModel.saleState.isCashingIn) "Processing..." else "Save Cash In")
                    }
                }
            }
        }
    }
}