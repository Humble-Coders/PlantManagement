// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/components/AddExpenseDialog.kt
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
import com.humblecoders.plantmanagement.data.ExpenseCategory
import com.humblecoders.plantmanagement.viewmodels.ExpenseViewModel
import java.time.LocalDate
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import javax.imageio.ImageIO
import com.humblecoders.plantmanagement.ui.components.DocumentUploadComponent

@Composable
fun AddExpenseDialog(
    expenseViewModel: ExpenseViewModel,
    onDismiss: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf<ExpenseCategory?>(null) }
    var amount by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var notes by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var selectedDocuments by remember { mutableStateOf<List<File>>(emptyList()) }
    var isUploadingDocuments by remember { mutableStateOf(false) }

    val expenseState = expenseViewModel.expenseState

    LaunchedEffect(Unit) {
        expenseViewModel.loadCategories()
    }

    LaunchedEffect(expenseState.successMessage, expenseState.error) {
        if (expenseState.successMessage != null) {
            kotlinx.coroutines.delay(2000)
            expenseViewModel.clearMessages()
            onDismiss()
        }
        if (expenseState.error != null) {
            kotlinx.coroutines.delay(3000)
            expenseViewModel.clearMessages()
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Expense",
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

                if (expenseState.successMessage != null) {
                    Card(
                        backgroundColor = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = expenseState.successMessage!!,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                if (expenseState.error != null) {
                    Card(
                        backgroundColor = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = expenseState.error!!,
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

                Text(
                    text = "Category",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                SearchableExpenseCategoryDropdown(
                    selectedCategory = selectedCategory,
                    categories = expenseState.categories,
                    onCategorySelected = { category ->
                        selectedCategory = category
                    },
                    onAddNewCategory = { categoryName ->
                        expenseViewModel.addCategory(categoryName)
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

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
                        cursorColor = Color(0xFFEF4444),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFFEF4444),
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
                    label = { Text("Expense Date", color = Color(0xFF9CA3AF)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFFEF4444),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFFEF4444),
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
                        cursorColor = Color(0xFFEF4444),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFFEF4444),
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
                            val expenseAmount = amount.toDoubleOrNull()
                            if (expenseAmount == null || expenseAmount <= 0) {
                                errorMessage = "Please enter a valid amount greater than 0"
                                return@Button
                            }

                            if (selectedCategory == null || selectedCategory!!.id.isBlank()) {
                                errorMessage = "Please select a category"
                                return@Button
                            }

                            expenseViewModel.addExpense(
                                categoryId = selectedCategory!!.id,
                                amount = expenseAmount,
                                date = selectedDate,
                                notes = notes,
                                documentFiles = selectedDocuments
                            )
                        },
                        enabled = amount.isNotBlank() && selectedCategory != null && !expenseState.isProcessing,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (expenseState.isProcessing) "Adding..." else "Add Expense")
                    }
                }
            }
        }
    }

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
                        text = "Select Expense Date",
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
                            backgroundColor = Color(0xFFEF4444)
                        )
                    ) {
                        Text("OK", color = Color.White)
                    }
                }
            }
        }
    }
}


