// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/components/ExpenseCategoryManagementDialog.kt
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
import com.humblecoders.plantmanagement.data.ExpenseCategory
import com.humblecoders.plantmanagement.viewmodels.ExpenseViewModel
import com.humblecoders.plantmanagement.viewmodels.ExpenseState

@Composable
fun ExpenseCategoryManagementDialog(
    expenseViewModel: ExpenseViewModel,
    expenseState: ExpenseState,
    onDismiss: () -> Unit
) {
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showEditCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<ExpenseCategory?>(null) }
    var categoryToDelete by remember { mutableStateOf<ExpenseCategory?>(null) }

    LaunchedEffect(Unit) {
        expenseViewModel.loadCategories()
    }

    LaunchedEffect(expenseState.successMessage, expenseState.error) {
        if (expenseState.successMessage != null || expenseState.error != null) {
            kotlinx.coroutines.delay(3000)
            expenseViewModel.clearMessages()
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
                            enabled = !expenseState.isAddingCategory && !expenseState.isDeletingCategory,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFEF4444),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            if (expenseState.isAddingCategory) {
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
                            Text(if (expenseState.isAddingCategory) "Adding..." else "Add Category")
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

                if (expenseState.categories.isEmpty()) {
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
                        items(expenseState.categories) { category ->
                            ExpenseCategoryCard(
                                category = category,
                                expenseState = expenseState,
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

    if (showAddCategoryDialog) {
        AddEditExpenseCategoryDialog(
            category = null,
            expenseState = expenseState,
            onDismiss = { showAddCategoryDialog = false },
            onSave = { categoryName ->
                expenseViewModel.addCategory(categoryName)
                showAddCategoryDialog = false
            }
        )
    }

    if (showEditCategoryDialog && categoryToEdit != null) {
        AddEditExpenseCategoryDialog(
            category = categoryToEdit!!,
            expenseState = expenseState,
            onDismiss = {
                showEditCategoryDialog = false
                categoryToEdit = null
            },
            onSave = { categoryName ->
                if (categoryToEdit != null) {
                    expenseViewModel.updateCategory(categoryToEdit!!.id, categoryName)
                }
                showEditCategoryDialog = false
                categoryToEdit = null
            }
        )
    }

    if (showDeleteConfirmDialog && categoryToDelete != null) {
        DeleteExpenseCategoryConfirmDialog(
            category = categoryToDelete!!,
            expenseState = expenseState,
            onConfirm = {
                if (categoryToDelete != null) {
                    expenseViewModel.deleteCategory(categoryToDelete!!.id)
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
private fun ExpenseCategoryCard(
    category: ExpenseCategory,
    expenseState: ExpenseState,
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
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = category.name,
                    color = Color(0xFFF9FAFB),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
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
                    enabled = !expenseState.isDeletingCategory && !expenseState.isAddingCategory,
                    modifier = Modifier.size(32.dp)
                ) {
                    if (expenseState.isDeletingCategory) {
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
private fun AddEditExpenseCategoryDialog(
    category: ExpenseCategory?,
    expenseState: ExpenseState,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var categoryName by remember { mutableStateOf(category?.name ?: "") }
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
                        textColor = Color(0xFFF9FAFB),cursorColor = Color(0xFFEF4444),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF4B5563),
                        focusedLabelColor = Color(0xFFEF4444),
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
                                onSave(categoryName.trim())
                            }
                        },
                        enabled = categoryName.isNotBlank() && !expenseState.isAddingCategory && !expenseState.isDeletingCategory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        if (expenseState.isAddingCategory || expenseState.isDeletingCategory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(
                            when {
                                expenseState.isAddingCategory -> "Adding..."
                                expenseState.isDeletingCategory -> "Processing..."
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
private fun DeleteExpenseCategoryConfirmDialog(
    category: ExpenseCategory,
    expenseState: ExpenseState,
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
                        enabled = !expenseState.isDeletingCategory && !expenseState.isAddingCategory,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        if (expenseState.isDeletingCategory) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (expenseState.isDeletingCategory) "Deleting..." else "Delete")
                    }
                }
            }
        }
    }
}