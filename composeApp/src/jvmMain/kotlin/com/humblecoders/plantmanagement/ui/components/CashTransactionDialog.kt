package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
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
import com.humblecoders.plantmanagement.data.CashTransactionType
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.viewmodels.CashTransactionViewModel

@Composable
fun CashTransactionDialog(
    customer: Entity,
    cashTransactionViewModel: CashTransactionViewModel,
    onDismiss: () -> Unit
) {
    var transactionType by remember { mutableStateOf(CashTransactionType.RECEIVE) }
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val cashTransactionState = cashTransactionViewModel.cashTransactionState
    
    // Calculate new balance preview
    val currentBalance = customer.balance
    val transactionAmount = amount.toDoubleOrNull() ?: 0.0
    val newBalance = when (transactionType) {
        CashTransactionType.RECEIVE -> currentBalance - transactionAmount  // Customer pays us (we owe them less, balance decreases)
        CashTransactionType.GIVE -> currentBalance + transactionAmount      // We pay customer (they owe us more, balance increases)
    }

    // Clear messages after showing
    LaunchedEffect(cashTransactionState.successMessage, cashTransactionState.error) {
        if (cashTransactionState.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            cashTransactionViewModel.clearMessages()
            onDismiss() // Close dialog after success message
        }
        if (cashTransactionState.error != null) {
            kotlinx.coroutines.delay(3000)
            cashTransactionViewModel.clearMessages()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF1F2937),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash Transaction",
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

                // Customer Info
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF374151),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Customer Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF9FAFB),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Text(
                            text = "Firm: ${customer.firmName}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                        if (customer.contactPerson.isNotBlank()) {
                            Text(
                                text = "Contact: ${customer.contactPerson}",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Current Balance: ₹${String.format("%.2f", currentBalance)}",
                            color = if (currentBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Success/Error messages
                if (cashTransactionState.successMessage != null) {
                    Card(
                        backgroundColor = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cashTransactionState.successMessage!!,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (cashTransactionState.error != null) {
                    Card(
                        backgroundColor = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cashTransactionState.error!!,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

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

                // Transaction Type Selection
                Text(
                    text = "Transaction Type",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CashTransactionType.values().forEach { type ->
                        val isSelected = transactionType == type
                        OutlinedButton(
                            onClick = { transactionType = type },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                backgroundColor = if (isSelected) Color(0xFF10B981) else Color(0xFF374151)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = when (type) {
                                    CashTransactionType.RECEIVE -> "Receive"
                                    CashTransactionType.GIVE -> "Give"
                                },
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
                
                // Transaction type descriptions
                Text(
                    text = when (transactionType) {
                        CashTransactionType.RECEIVE -> "Customer pays us money"
                        CashTransactionType.GIVE -> "We pay customer money"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (₹)", color = Color(0xFF9CA3AF)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFF10B981),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF4B5563)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notes
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
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

                // Balance Preview
                if (amount.isNotBlank() && transactionAmount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        backgroundColor = Color(0xFF374151),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Balance Preview",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFF9FAFB),
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Current Balance:",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "₹${String.format("%.2f", currentBalance)}",
                                    color = if (currentBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Transaction Amount:",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "₹${String.format("%.2f", transactionAmount)}",
                                    color = Color(0xFFF9FAFB),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Divider(
                                color = Color(0xFF4B5563),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "New Balance:",
                                    color = Color(0xFFF9FAFB),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "₹${String.format("%.2f", newBalance)}",
                                    color = if (newBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Text(
                                text = if (newBalance >= 0) "Customer owes us ₹${String.format("%.2f", newBalance)}" else "We owe customer ₹${String.format("%.2f", kotlin.math.abs(newBalance))}",
                                color = if (newBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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
                            val transactionAmount = amount.toDoubleOrNull()
                            if (transactionAmount == null || transactionAmount <= 0) {
                                errorMessage = "Please enter a valid amount greater than 0"
                                return@Button
                            }
                            
                            cashTransactionViewModel.processCashTransaction(
                                customerId = customer.id,
                                amount = transactionAmount,
                                transactionType = transactionType,
                                note = note
                            )
                        },
                        enabled = amount.isNotBlank() && !cashTransactionState.isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (cashTransactionState.isProcessing) "Processing..." else "Save Transaction")
                    }
                }
            }
        }
    }
}
