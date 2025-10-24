package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.launch
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.data.CashReportType
import com.humblecoders.plantmanagement.viewmodels.CashReportViewModel
import com.humblecoders.plantmanagement.viewmodels.UserBalanceViewModel
import com.humblecoders.plantmanagement.data.UserRole
import java.time.LocalDate
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import javax.imageio.ImageIO
import com.humblecoders.plantmanagement.ui.components.DocumentUploadComponent

@Composable
fun AddCashTransactionDialog(
    cashReportViewModel: CashReportViewModel,
    userBalanceViewModel: UserBalanceViewModel? = null,
    userRole: UserRole? = null,
    onDismiss: () -> Unit
) {
    var transactionType by remember { mutableStateOf(CashReportType.CASH_IN) }
    var selectedCategory by remember { mutableStateOf<CashReportCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedDocuments by remember { mutableStateOf<List<File>>(emptyList()) }

    val cashReportState = cashReportViewModel.cashReportState
    val coroutineScope = rememberCoroutineScope()

    // Load categories when dialog opens
    LaunchedEffect(Unit) {
        cashReportViewModel.loadCategories()
    }

    // Load categories when transaction type changes and filter by type
    LaunchedEffect(transactionType) {
        cashReportViewModel.loadCategories() // Load all categories
        selectedCategory = null // Reset selected category when type changes
    }

    // Clear messages after showing
    LaunchedEffect(cashReportState.successMessage, cashReportState.error) {
        if (cashReportState.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            cashReportViewModel.clearMessages()
            onDismiss()
        }
        if (cashReportState.error != null) {
            kotlinx.coroutines.delay(3000)
            cashReportViewModel.clearMessages()
        }
    }

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
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Cash Transaction",
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

                // Success/Error messages
                if (cashReportState.successMessage != null) {
                    Card(
                        backgroundColor = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cashReportState.successMessage!!,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (cashReportState.error != null) {
                    Card(
                        backgroundColor = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = cashReportState.error!!,
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CashReportType.values().forEach { type ->
                        val isSelected = transactionType == type
                        OutlinedButton(
                            onClick = { transactionType = type },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                backgroundColor = if (isSelected) {
                                    when (type) {
                                        CashReportType.CASH_IN -> Color(0xFF10B981)
                                        CashReportType.CASH_OUT -> Color(0xFFEF4444)
                                    }
                                } else Color(0xFF374151)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    when (type) {
                                        CashReportType.CASH_IN -> Icons.Default.TrendingUp
                                        CashReportType.CASH_OUT -> Icons.Default.TrendingDown
                                    },
                                    contentDescription = null,
                                    tint = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = when (type) {
                                        CashReportType.CASH_IN -> "Cash In"
                                        CashReportType.CASH_OUT -> "Cash Out"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = when (transactionType) {
                        CashReportType.CASH_IN -> "Money coming into the company"
                        CashReportType.CASH_OUT -> "Money going out of the company"
                    },
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection
                Text(
                    text = "Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SearchableCategoryDropdown(
                    selectedCategory = selectedCategory,
                    categories = cashReportState.categories.filter { it.type == transactionType },
                    transactionType = transactionType,
                    onCategorySelected = { category ->
                        selectedCategory = category
                    },
                    onAddNewCategory = { categoryName ->
                        cashReportViewModel.addCategory(categoryName, transactionType)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Input
                Text(
                    text = "Amount",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount (Rs.)", color = Color(0xFF9CA3AF)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFF10B981),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF)
                    ),
                    leadingIcon = {
                        Text(
                            text = "Rs.",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Date Selection
                Text(
                    text = "Date",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = selectedDate.toString(),
                    onValueChange = { },
                    label = { Text("Transaction Date", color = Color(0xFF9CA3AF)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFF10B981),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF)
                    ),
                    leadingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = "Select Date",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Notes
                Text(
                    text = "Notes",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFF10B981),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFF10B981),
                        unfocusedLabelColor = Color(0xFF9CA3AF)
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Document Upload Section
                Text(
                    text = "Receipt/Bill Documents (Optional)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                DocumentUploadComponent(
                    selectedDocuments = selectedDocuments,
                    onDocumentsSelected = { files -> selectedDocuments = files },
                    modifier = Modifier.fillMaxWidth(),
                    label = "Receipt/Bill Documents (Optional)"
                )

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
                            
                            if (selectedCategory == null || selectedCategory!!.id.isBlank()) {
                                errorMessage = "Please select a category"
                                return@Button
                            }
                            
                            // Check if this is an accountant performing cash out transaction
                            val isAccountantCashOut = userRole == UserRole.USER && transactionType == CashReportType.CASH_OUT
                            
                            if (isAccountantCashOut && userBalanceViewModel != null) {
                                // Accountant performing cash out - subtract from accountant balance
                                coroutineScope.launch {
                                    userBalanceViewModel.processUserCashOutTransaction(
                                        amount = transactionAmount,
                                        notes = notes
                                    )
                                    
                                    // Wait a bit for the transaction to complete
                                    kotlinx.coroutines.delay(1000)
                                    
                                    // Check if there was an error
                                    if (userBalanceViewModel.errorMessage != null) {
                                        errorMessage = userBalanceViewModel.errorMessage
                                    } else {
                                        // Also add to cash report with accountant marking
                                        cashReportViewModel.addCashReport(
                                            transactionType = transactionType,
                                            categoryId = selectedCategory!!.id, // Use the selected category
                                            amount = transactionAmount,
                                            date = selectedDate,
                                            notes = notes,
                                            documentFiles = selectedDocuments
                                        )
                                    }
                                }
                            } else {
                                // Normal transaction (admin or cash in)
                                cashReportViewModel.addCashReport(
                                    transactionType = transactionType,
                                    categoryId = selectedCategory!!.id,
                                    amount = transactionAmount,
                                    date = selectedDate,
                                    notes = notes,
                                    documentFiles = selectedDocuments
                                )
                            }
                        },
                        enabled = amount.isNotBlank() && selectedCategory != null && !cashReportState.isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = when (transactionType) {
                                CashReportType.CASH_IN -> Color(0xFF10B981)
                                CashReportType.CASH_OUT -> Color(0xFFEF4444)
                            },
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (cashReportState.isProcessing) "Adding..." else "Add Transaction")
                    }
                }
            }
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        Dialog(onDismissRequest = { showDatePicker = false }) {
            Card(
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Transaction Date",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DatePicker(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981)
                        )
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
}

