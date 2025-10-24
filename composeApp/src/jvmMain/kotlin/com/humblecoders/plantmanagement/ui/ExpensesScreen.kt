// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/ExpensesScreen.kt
package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import com.humblecoders.plantmanagement.data.Expense
import com.humblecoders.plantmanagement.data.ExpenseCategory
import com.humblecoders.plantmanagement.viewmodels.ExpenseSortField
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import com.humblecoders.plantmanagement.viewmodels.ExpenseState
import com.humblecoders.plantmanagement.ui.components.AddExpenseDialog
import com.humblecoders.plantmanagement.ui.components.ExpenseCategoryManagementDialog
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.utils.toComposeImageBitmap
import com.humblecoders.plantmanagement.viewmodels.ExpenseViewModel
import com.humblecoders.plantmanagement.repositories.ExpenseSummary
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO

@Composable
fun ExpensesScreen(
    expenseViewModel: ExpenseViewModel,
    userRole: com.humblecoders.plantmanagement.data.UserRole?,
    onBack: () -> Unit
) {
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showCategoryManagementDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<Expense?>(null) }

    val expenseState = expenseViewModel.expenseState
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        expenseViewModel.loadExpenses()
        expenseViewModel.loadCategories()
        expenseViewModel.loadSummary()
        expenseViewModel.listenToExpenses()
    }

    LaunchedEffect(expenseState.successMessage, expenseState.error) {
        if (expenseState.successMessage != null || expenseState.error != null) {
            // Close delete dialog if deletion was successful
            if (expenseState.successMessage != null && expenseState.successMessage.contains("deleted successfully")) {
                showDeleteConfirmDialog = null
            }
            kotlinx.coroutines.delay(3000)
            expenseViewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF9CA3AF)
                    )
                }
                Text(
                    text = "Expenses",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )

                if (expenseState.isRetrying) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFF59E0B),
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Text(
                            text = "Retrying...",
                            fontSize = 12.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = { showCategoryManagementDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF3B82F6),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Category,
                        contentDescription = "Manage Categories",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Categories")
                }

                Button(
                    onClick = { showAddExpenseDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Expense",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Expense")
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            expenseViewModel.generatePdf()
                                .fold(
                                    onSuccess = { pdfBytes ->
                                        val fileName = "expense_report_${System.currentTimeMillis()}.pdf"
                                        val file = java.io.File(System.getProperty("user.home"), "Downloads/$fileName")
                                        file.writeBytes(pdfBytes)

                                        try {
                                            val desktop = java.awt.Desktop.getDesktop()
                                            if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                                                desktop.open(file)
                                            }
                                        } catch (e: Exception) {
                                            println("Could not open PDF file: ${e.message}")
                                        }

                                        expenseViewModel.showPdfSuccessMessage()
                                    },
                                    onFailure = { error ->
                                        expenseViewModel.clearMessages()
                                    }
                                )
                        }
                    },
                    enabled = !expenseState.isLoading && !expenseState.isRetrying,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF10B981),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        Icons.Default.Download,
                        contentDescription = "Download PDF",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Download PDF")
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

        if (expenseState.error != null && !expenseState.isRetrying) {
            Card(
                backgroundColor = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = expenseState.error!!,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                expenseViewModel.loadExpenses()
                                expenseViewModel.loadCategories()
                                expenseViewModel.loadSummary()
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Retry", fontSize = 12.sp)
                        }
                        Button(
                            onClick = { expenseViewModel.clearMessages() },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF6B7280),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Dismiss", fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        ExpenseSummaryCard(summary = expenseViewModel.getFilteredSummary())

        Spacer(modifier = Modifier.height(16.dp))

        ExpenseFilterCard(expenseViewModel = expenseViewModel, expenseState = expenseState)

        Spacer(modifier = Modifier.height(16.dp))

        if (expenseState.isLoading || expenseState.isRetrying) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFEF4444),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (expenseState.isRetrying) "Retrying..." else "Loading expenses and categories...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
        } else if (expenseState.categories.isEmpty() && expenseState.expenses.isNotEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "Categories not loaded",
                        color = Color(0xFFF59E0B),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Category names cannot be displayed. Please retry loading.",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Button(
                        onClick = { expenseViewModel.loadCategories() },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        )
                    ) {
                        Text("Retry Loading Categories")
                    }
                }
            }
        } else {
            val filteredExpenses = expenseViewModel.getFilteredAndSortedExpenses()

            if (filteredExpenses.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Receipt,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (expenseState.expenses.isEmpty()) "No expenses found" else "No expenses match your filters",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (expenseState.expenses.isEmpty()) "Add your first expense to get started" else "Try adjusting your search or filter criteria",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredExpenses.forEach { expense ->
                        ExpenseCard(
                            expense = expense,
                            categories = expenseState.categories,
                            userRole = userRole,
                            onDelete = { showDeleteConfirmDialog = expense }
                        )
                    }
                }
            }
        }
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            expenseViewModel = expenseViewModel,
            onDismiss = { showAddExpenseDialog = false }
        )
    }

    if (showCategoryManagementDialog) {
        ExpenseCategoryManagementDialog(
            expenseViewModel = expenseViewModel,
            expenseState = expenseState,
            onDismiss = { showCategoryManagementDialog = false }
        )
    }

    if (showDeleteConfirmDialog != null) {
        DeleteExpenseConfirmationDialog(
            expense = showDeleteConfirmDialog!!,
            categories = expenseState.categories,
            expenseState = expenseState,
            onConfirm = {
                expenseViewModel.deleteExpense(showDeleteConfirmDialog!!.id)
                // Don't close dialog immediately - let it close after success
            },
            onDismiss = { 
                if (!expenseState.isDeletingExpense) {
                    showDeleteConfirmDialog = null 
                }
            }
        )
    }
}

@Composable
private fun ExpenseSummaryCard(summary: com.humblecoders.plantmanagement.repositories.ExpenseSummary) {
    Column {
        Text(
            text = "Summary (Filtered)",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF9FAFB),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TrendingDown,
                    contentDescription = "Total Expenses",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total Expenses",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Rs.${String.format("%.2f", summary.totalExpenses)}",
                    color = Color(0xFFEF4444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ExpenseCard(
    expense: Expense,
    categories: List<ExpenseCategory>,
    userRole: com.humblecoders.plantmanagement.data.UserRole?,
    onDelete: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = expense.date?.let {
        dateFormat.format(Date(it.seconds * 1000))
    } ?: "Unknown Date"

    val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown Category"
    var showImageDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
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
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = categoryName,
                        color = Color(0xFFF9FAFB),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (expense.documentUrls.isNotEmpty()) {
                        Icon(
                            Icons.Default.Image,
                            contentDescription = "Has Image",
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = formattedDate,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                if (expense.notes.isNotBlank()) {
                    Text(
                        text = expense.notes,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Rs.${String.format("%.2f", expense.amount)}",
                    color = Color(0xFFEF4444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // View Image Button - Only show if expense has an image
                    if (expense.documentUrls.isNotEmpty()) {
                        OutlinedButton(
                            onClick = { showImageDialog = true },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF3B82F6),
                                backgroundColor = Color(0xFF1E3A8A).copy(alpha = 0.1f)
                            ),
                            border = BorderStroke(1.dp, Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "View Image",
                                tint = Color(0xFF3B82F6),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "View",
                                fontSize = 12.sp,
                                color = Color(0xFF3B82F6),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Only show delete button for admin users
                    if (userRole == com.humblecoders.plantmanagement.data.UserRole.ADMIN) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
    
    if (showImageDialog && expense.documentUrls.isNotEmpty()) {
        DocumentViewDialog(
            documentUrls = expense.documentUrls,
            onDismiss = { showImageDialog = false }
        )
    }
}

@Composable
private fun DeleteExpenseConfirmationDialog(
    expense: Expense,
    categories: List<ExpenseCategory>,
    expenseState: ExpenseState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown Category"

    Dialog(onDismissRequest = { 
        if (!expenseState.isDeletingExpense) {
            onDismiss()
        }
    }) {
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
                    text = "Delete Expense",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Are you sure you want to delete this expense?",
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "Category: ${categoryName}",
                    fontSize = 14.sp,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = "Amount: Rs.${String.format("%.2f", expense.amount)}",
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
                        enabled = !expenseState.isDeletingExpense,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        if (expenseState.isDeletingExpense) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (expenseState.isDeletingExpense) "Deleting..." else "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseFilterCard(
    expenseViewModel: ExpenseViewModel,
    expenseState: com.humblecoders.plantmanagement.viewmodels.ExpenseState
) {
    Card(
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Filter & Search",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = expenseState.searchQuery,
                    onValueChange = { expenseViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search", color = Color(0xFF9CA3AF)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                    },
                    modifier = Modifier.width(200.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFFEF4444),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFFEF4444)
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text("From:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                DatePicker(
                    selectedDate = try {
                        java.time.LocalDate.parse(expenseState.filterDateFrom, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.now()
                    },
                    onDateSelected = { date ->
                        expenseViewModel.updateDateFilter(date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), expenseState.filterDateTo)
                    },
                    modifier = Modifier.width(140.dp),
                    label = ""
                )

                Text("To:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                DatePicker(
                    selectedDate = try {
                        java.time.LocalDate.parse(expenseState.filterDateTo, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
                    } catch (e: Exception) {
                        java.time.LocalDate.now()
                    },
                    onDateSelected = { date ->
                        expenseViewModel.updateDateFilter(expenseState.filterDateFrom, date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE))
                    },
                    modifier = Modifier.width(140.dp),
                    label = ""
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { sortExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF374151)
                        )
                    ) {
                        Text("Sort by: ${expenseState.sortBy.name.lowercase().replaceFirstChar { it.uppercase() }}")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            expenseViewModel.updateSortBy(ExpenseSortField.DATE)
                            sortExpanded = false
                        }) {
                            Text("Date")
                        }
                        DropdownMenuItem(onClick = {
                            expenseViewModel.updateSortBy(ExpenseSortField.CATEGORY)
                            sortExpanded = false
                        }) {
                            Text("Category")
                        }
                        DropdownMenuItem(onClick = {
                            expenseViewModel.updateSortBy(ExpenseSortField.AMOUNT)
                            sortExpanded = false
                        }) {
                            Text("Amount")
                        }
                    }
                }

                IconButton(
                    onClick = { expenseViewModel.toggleSortDirection() }
                ) {
                    Icon(
                        imageVector = if (expenseState.sortDirection == SortDirection.ASCENDING) {
                            Icons.Default.ArrowDropUp
                        } else {
                            Icons.Default.ArrowDropDown
                        },
                        contentDescription = "Sort Direction",
                        tint = Color(0xFFEF4444)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                if (expenseState.searchQuery.isNotBlank() ||
                    expenseState.filterDateFrom.isNotBlank() ||
                    expenseState.filterDateTo.isNotBlank()) {
                    OutlinedButton(
                        onClick = {
                            expenseViewModel.updateSearchQuery("")
                            expenseViewModel.updateDateFilter("", "")
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFEF4444)
                        )
                    ) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear Filters", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Clear Filters")
                    }
                }
            }
        }
    }
}

@Composable
private fun ImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Image",
                        fontSize = 18.sp,
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

                // Load and display image from URL
                var isLoading by remember { mutableStateOf(true) }
                var loadError by remember { mutableStateOf(false) }
                var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }

                LaunchedEffect(imageUrl) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val url = java.net.URL(imageUrl)
                            val connection = url.openConnection()
                            connection.connect()
                            val inputStream = connection.getInputStream()
                            val bufferedImage = ImageIO.read(inputStream)
                            imageBitmap = bufferedImage.toComposeImageBitmap()
                            isLoading = false
                        } catch (e: Exception) {
                            loadError = true
                            isLoading = false
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> CircularProgressIndicator(color = Color(0xFFEF4444))
                        loadError -> Text(
                            "Failed to load image",
                            color = Color(0xFFEF4444)
                        )
                        imageBitmap != null -> Image(
                            bitmap = imageBitmap!!,
                            contentDescription = "Expense Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            // Open image URL in browser
                            try {
                                val fixedUrl = fixImageUrl(imageUrl)
                                java.awt.Desktop.getDesktop().browse(java.net.URI(fixedUrl))
                            } catch (e: Exception) {
                                println("Error opening URL: ${e.message}")
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF06B6D4)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Open in browser", modifier = Modifier.size(14.dp), tint = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open in browser", fontSize = 11.sp, color = Color(0xFF06B6D4))
                    }

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentViewDialog(
    documentUrls: List<String>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Expense Documents",
                        fontSize = 18.sp,
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

                // Display documents
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(documentUrls) { documentUrl ->
                        DocumentItem(
                            documentUrl = documentUrl,
                            onClick = {
                                try {
                                    val fixedUrl = fixImageUrl(documentUrl)
                                    java.awt.Desktop.getDesktop().browse(java.net.URI(fixedUrl))
                                } catch (e: Exception) {
                                    println("Error opening URL: ${e.message}")
                                    e.printStackTrace()
                                }
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEF4444)
                    )
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun DocumentItem(
    documentUrl: String,
    onClick: () -> Unit
) {
    val isPdf = documentUrl.lowercase().contains(".pdf")
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isPdf) {
                Icon(
                    Icons.Default.PictureAsPdf,
                    contentDescription = "PDF Document",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "PDF Document",
                    color = Color(0xFFF9FAFB),
                    fontWeight = FontWeight.Medium
                )
            } else {
                // Load and display image thumbnail
                var imageBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
                var isLoading by remember { mutableStateOf(true) }
                
                LaunchedEffect(documentUrl) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            val url = java.net.URL(documentUrl)
                            val connection = url.openConnection()
                            connection.connect()
                            val inputStream = connection.getInputStream()
                            val bufferedImage = ImageIO.read(inputStream)
                            imageBitmap = bufferedImage.toComposeImageBitmap()
                            isLoading = false
                        } catch (e: Exception) {
                            isLoading = false
                        }
                    }
                }
                
                if (isLoading) {
                    Box(
                        modifier = Modifier.size(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!,
                        contentDescription = "Document Thumbnail",
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Image Document",
                        tint = Color(0xFF3B82F6),
                        modifier = Modifier.size(32.dp)
                    )
                }
                Text(
                    text = "Image Document",
                    color = Color(0xFFF9FAFB),
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            Icon(
                Icons.Default.OpenInBrowser,
                contentDescription = "Open in Browser",
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun fixImageUrl(url: String): String {
    return try {
        if (url.contains("/o/") && url.contains("?alt=media")) {
            val parts = url.split("/o/")
            if (parts.size == 2) {
                val pathAndQuery = parts[1].split("?alt=media")
                if (pathAndQuery.isNotEmpty()) {
                    val path = pathAndQuery[0]
                    // Decode, then re-encode properly
                    val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
                    val properlyEncodedPath = java.net.URLEncoder.encode(decodedPath, "UTF-8")
                        .replace("+", "%20")
                    return "${parts[0]}/o/${properlyEncodedPath}?alt=media"
                }
            }
        }
        url
    } catch (e: Exception) {
        println("Error fixing URL: ${e.message}")
        url
    }
}