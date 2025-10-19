package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.viewmodels.CashReportViewModel
import com.humblecoders.plantmanagement.viewmodels.CashReportState

@Composable
fun CategoryManagementDialog(
    cashReportViewModel: CashReportViewModel,
    cashReportState: CashReportState,
    onDismiss: () -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CashReportCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<CashReportCategory?>(null) }

    // Load categories when dialog opens
    LaunchedEffect(Unit) {
        cashReportViewModel.loadCategories()
    }

    // Clear messages after showing
    LaunchedEffect(cashReportState.successMessage, cashReportState.error) {
        if (cashReportState.successMessage != null || cashReportState.error != null) {
            kotlinx.coroutines.delay(3000)
            cashReportViewModel.clearMessages()
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
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage Categories",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            enabled = !cashReportState.isAddingCategory && !cashReportState.isDeletingCategory,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (cashReportState.isAddingCategory) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Add Category",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (cashReportState.isAddingCategory) "Adding..." else "Add Category")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
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
                            text = cashReportState.successMessage ?: "",
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
                            text = cashReportState.error ?: "",
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Categories List
                if (cashReportState.categories.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.Category,
                                contentDescription = null,
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No categories found",
                                color = Color(0xFF9CA3AF),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Add your first category to get started",
                                color = Color(0xFF6B7280),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(cashReportState.categories) { category ->
                            CategoryCard(
                                category = category,
                                cashReportState = cashReportState,
                                onEdit = {
                                    categoryToEdit = category
                                    showEditCategoryDialog = true
                                },
                                onDelete = {
                                    categoryToDelete = category
                                    showDeleteConfirmDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AddEditCategoryDialog(
            category = null,
            cashReportState = cashReportState,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { categoryName, categoryType ->
                cashReportViewModel.addCategory(categoryName, categoryType)
                showAddCategoryDialog = false
            }
        )
    }

    // Edit Category Dialog
    if (showEditCategoryDialog && categoryToEdit != null) {
        AddEditCategoryDialog(
            category = categoryToEdit!!,
            cashReportState = cashReportState,
            onDismiss = { 
                showEditCategoryDialog = false
                categoryToEdit = null
            },
            onSave = { categoryName, _ ->
                if (categoryToEdit != null) {
                    cashReportViewModel.updateCategory(categoryToEdit!!.id, categoryName)
                }
                showEditCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && categoryToDelete != null) {
        DeleteCategoryConfirmDialog(
            category = categoryToDelete!!,
            cashReportState = cashReportState,
            onConfirm = {
                if (categoryToDelete != null) {
                    cashReportViewModel.deleteCategory(categoryToDelete!!.id)
                }
                showDeleteConfirmDialog = false
                categoryToDelete = null
            },
            onDismiss = { 
                showDeleteConfirmDialog = false
                categoryToDelete = null
            }
        )
    }
}

@Composable
private fun CategoryCard(
    category: CashReportCategory,
    cashReportState: CashReportState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = "Category",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = category.name,
                        color = Color(0xFFF9FAFB),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = category.type.name.lowercase().replace("_", " "),
                        color = when (category.type) {
                            com.humblecoders.plantmanagement.data.CashReportType.CASH_IN -> Color(0xFF10B981)
                            com.humblecoders.plantmanagement.data.CashReportType.CASH_OUT -> Color(0xFFEF4444)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(18.dp)
                    )
                }
                
                IconButton(
                    onClick = onDelete,
                    enabled = !cashReportState.isDeletingCategory && !cashReportState.isAddingCategory,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (cashReportState.isDeletingCategory) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color(0xFFEF4444),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AddEditCategoryDialog(
    category: CashReportCategory?,
    cashReportState: CashReportState,
    onDismiss: () -> Unit,
    onSave: (String, com.humblecoders.plantmanagement.data.CashReportType) -> Unit
) {
    var categoryName by remember { mutableStateOf(category?.name ?: "") }
    var categoryType by remember { mutableStateOf(category?.type ?: com.humblecoders.plantmanagement.data.CashReportType.CASH_IN) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = if (category != null) "Edit Category" else "Add New Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Category Type Selection (only for new categories)
                if (category == null) {
                    Text(
                        text = "Category Type",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        com.humblecoders.plantmanagement.data.CashReportType.values().forEach { type ->
                            val isSelected = categoryType == type
                            OutlinedButton(
                                onClick = { categoryType = type },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isSelected) Color.White else Color(0xFF9CA3AF),
                                    backgroundColor = if (isSelected) {
                                        when (type) {
                                            com.humblecoders.plantmanagement.data.CashReportType.CASH_IN -> Color(0xFF10B981)
                                            com.humblecoders.plantmanagement.data.CashReportType.CASH_OUT -> Color(0xFFEF4444)
                                        }
                                    } else Color(0xFF374151)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = when (type) {
                                        com.humblecoders.plantmanagement.data.CashReportType.CASH_IN -> "Cash In"
                                        com.humblecoders.plantmanagement.data.CashReportType.CASH_OUT -> "Cash Out"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (errorMessage != null) {
                    Card(
                        backgroundColor = Color(0xFFEF4444),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Text(
                            text = errorMessage!!,
                            color = Color.White,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            if (categoryName.isBlank()) {
                                errorMessage = "Please enter a category name"
                            } else {
                                onSave(categoryName.trim(), categoryType)
                            }
                        },
                        enabled = categoryName.isNotBlank() && !cashReportState.isAddingCategory && !cashReportState.isDeletingCategory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        if (cashReportState.isAddingCategory || cashReportState.isDeletingCategory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                cashReportState.isAddingCategory -> "Adding..."
                                cashReportState.isDeletingCategory -> "Processing..."
                                category != null -> "Update"
                                else -> "Add"
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteCategoryConfirmDialog(
    category: CashReportCategory,
    cashReportState: CashReportState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(0.8f),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Delete Category",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Are you sure you want to delete this category?",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Category: ${category.name}",
                    fontSize = 14.sp,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "This action cannot be undone.",
                    fontSize = 12.sp,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = onConfirm,
                        enabled = !cashReportState.isDeletingCategory && !cashReportState.isAddingCategory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        if (cashReportState.isDeletingCategory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (cashReportState.isDeletingCategory) "Deleting..." else "Delete")
                    }
                }
            }
        }
    }
}
