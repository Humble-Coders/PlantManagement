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
import com.humblecoders.plantmanagement.data.CashReport
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.data.CashReportType
import com.humblecoders.plantmanagement.viewmodels.CashReportSortField
import com.humblecoders.plantmanagement.viewmodels.CashReportTypeFilter
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import com.humblecoders.plantmanagement.viewmodels.CashReportState
import com.humblecoders.plantmanagement.ui.components.AddCashTransactionDialog
import com.humblecoders.plantmanagement.ui.components.CategoryManagementDialog
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.viewmodels.CashReportViewModel
import com.humblecoders.plantmanagement.viewmodels.UserBalanceViewModel
import com.humblecoders.plantmanagement.data.UserBalance
import com.humblecoders.plantmanagement.data.BalanceTransfer
import com.humblecoders.plantmanagement.data.BalanceTransferType
import com.humblecoders.plantmanagement.data.UserCashOutTransaction
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import com.humblecoders.plantmanagement.utils.toComposeImageBitmap

@Composable
fun CashReportsScreen(
    cashReportViewModel: CashReportViewModel,
    userBalanceViewModel: UserBalanceViewModel,
    userRole: com.humblecoders.plantmanagement.data.UserRole?,
    onBack: () -> Unit
) {
    var showAddTransactionDialog by remember { mutableStateOf(false) }
    var showCategoryManagementDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf<CashReport?>(null) }
    var showTransferBalanceDialog by remember { mutableStateOf(false) }
    var showTransferHistoryDialog by remember { mutableStateOf(false) }
    var showUserCashOutFilter by remember { mutableStateOf(false) }

    val cashReportState = cashReportViewModel.cashReportState
    val coroutineScope = rememberCoroutineScope()

    // Load data when screen opens
    LaunchedEffect(Unit) {
        cashReportViewModel.loadCashReports()
        cashReportViewModel.loadCategories()
        cashReportViewModel.loadSummary()
        cashReportViewModel.listenToCashReports()
        userBalanceViewModel.loadSharedUserBalance()
    }

    // Clear messages after showing
    LaunchedEffect(cashReportState.successMessage, cashReportState.error) {
        if (cashReportState.successMessage != null || cashReportState.error != null) {
            // Close delete dialog if deletion was successful
            if (cashReportState.successMessage != null && cashReportState.successMessage.contains("deleted successfully")) {
                showDeleteConfirmDialog = null
            }
            kotlinx.coroutines.delay(3000)
            cashReportViewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Header
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
                        text = "Cash Reports",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    
                    // Retry indicator
                    if (cashReportState.isRetrying) {
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

                    // User Balance Management Buttons (Available to both Admin and Accountant with USER role)
                    if (userRole == com.humblecoders.plantmanagement.data.UserRole.ADMIN || userRole == com.humblecoders.plantmanagement.data.UserRole.USER) {
                        Button(
                            onClick = { showTransferBalanceDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF8B5CF6),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = "Transfer Balance",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Transfer")
                        }

                        Button(
                            onClick = { showTransferHistoryDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFFF59E0B),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Transfer History",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("History")
                        }
                    }

                    Button(
                        onClick = { showAddTransactionDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Transaction",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Transaction")
                    }

                    Button(
                        onClick = { 
                            coroutineScope.launch {
                                val accountantBalance = userBalanceViewModel.userBalance?.currentBalance ?: 0.0
                                cashReportViewModel.generatePdf(accountantBalance)
                                    .fold(
                                        onSuccess = { pdfBytes ->
                                            // Save PDF to file
                                            val fileName = "cash_report_${System.currentTimeMillis()}.pdf"
                                            val file = java.io.File(System.getProperty("user.home"), "Downloads/$fileName")
                                            file.writeBytes(pdfBytes)
                                            
                                            // Open the PDF file
                                            try {
                                                val desktop = java.awt.Desktop.getDesktop()
                                                if (desktop.isSupported(java.awt.Desktop.Action.OPEN)) {
                                                    desktop.open(file)
                                                }
                                            } catch (e: Exception) {
                                                println("Could not open PDF file: ${e.message}")
                                            }
                                            
                                            // Show success message via ViewModel
                                            cashReportViewModel.showPdfSuccessMessage()
                                        },
                                        onFailure = { error ->
                                            // Error will be handled by the ViewModel state
                                            cashReportViewModel.clearMessages()
                                        }
                                    )
                            }
                        },
                        enabled = !cashReportState.isLoading && !cashReportState.isRetrying,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
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

        if (cashReportState.error != null && !cashReportState.isRetrying) {
            Card(
                backgroundColor = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = cashReportState.error!!,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                cashReportViewModel.loadCashReports()
                                cashReportViewModel.loadCategories()
                                cashReportViewModel.loadSummary()
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
                            onClick = { cashReportViewModel.clearMessages() },
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

        // Summary Cards
        CashReportSummaryCards(summary = cashReportViewModel.getFilteredSummary())

        Spacer(modifier = Modifier.height(16.dp))

        // User Balance Card
        UserBalanceCard(
            userBalance = userBalanceViewModel.userBalance,
            userRole = userRole,
            isLoading = userBalanceViewModel.isLoading,
            errorMessage = userBalanceViewModel.errorMessage
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Filtering and Search UI
        CashReportFilterCard(
            cashReportViewModel = cashReportViewModel, 
            cashReportState = cashReportState,
            userBalanceViewModel = userBalanceViewModel,
            showUserCashOutFilter = showUserCashOutFilter,
            onToggleUserCashOutFilter = { showUserCashOutFilter = !showUserCashOutFilter }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Transactions List
        if (cashReportState.isLoading || cashReportState.isRetrying) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = if (cashReportState.isRetrying) "Retrying..." else "Loading transactions and categories...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
        } else if (cashReportState.categories.isEmpty() && cashReportState.cashReports.isNotEmpty()) {
            // Show message when categories are not loaded but transactions exist
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
                        onClick = { cashReportViewModel.loadCategories() },
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
            val filteredReports = if (showUserCashOutFilter) {
                // Show only accountant transactions (transactions with accountantTransaction = true)
                cashReportViewModel.getFilteredAndSortedReports().filter { it.accountantTransaction }
            } else {
                cashReportViewModel.getFilteredAndSortedReports()
            }
            
            if (filteredReports.isEmpty()) {
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
                            text = if (cashReportState.cashReports.isEmpty()) "No cash transactions found" else "No transactions match your filters",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (cashReportState.cashReports.isEmpty()) "Add your first transaction to get started" else "Try adjusting your search or filter criteria",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredReports.forEach { transaction ->
                        CashReportTransactionCard(
                            transaction = transaction,
                            categories = cashReportState.categories,
                            userRole = userRole,
                            onDelete = { showDeleteConfirmDialog = transaction }
                        )
                    }
                }
            }
        }
    }

    // Add Transaction Dialog
    if (showAddTransactionDialog) {
        AddCashTransactionDialog(
            cashReportViewModel = cashReportViewModel,
            userBalanceViewModel = userBalanceViewModel,
            userRole = userRole,
            onDismiss = { showAddTransactionDialog = false }
        )
    }

    // Category Management Dialog
    if (showCategoryManagementDialog) {
        CategoryManagementDialog(
            cashReportViewModel = cashReportViewModel,
            cashReportState = cashReportState,
            onDismiss = { showCategoryManagementDialog = false }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog != null) {
        DeleteConfirmationDialog(
            transaction = showDeleteConfirmDialog!!,
            categories = cashReportState.categories,
            cashReportState = cashReportState,
            onConfirm = {
                cashReportViewModel.deleteCashReport(showDeleteConfirmDialog!!.id)
                // Don't close dialog immediately - let it close after success
            },
            onDismiss = { 
                if (!cashReportState.isDeletingCashReport) {
                    showDeleteConfirmDialog = null 
                }
            }
        )
    }

    // Transfer Balance Dialog
    if (showTransferBalanceDialog) {
        TransferBalanceDialog(
            onDismiss = { showTransferBalanceDialog = false },
            onTransfer = { amount, transferType, notes ->
                coroutineScope.launch {
                    userBalanceViewModel.transferBalanceToSharedUserBalance(amount, transferType, notes)
                    showTransferBalanceDialog = false
                }
            }
        )
    }

    // Transfer History Dialog
    if (showTransferHistoryDialog) {
        TransferHistoryDialog(
            balanceTransfers = userBalanceViewModel.balanceTransfers,
            onDismiss = { showTransferHistoryDialog = false }
        )
    }
}

@Composable
private fun CashReportSummaryCards(summary: com.humblecoders.plantmanagement.repositories.CashReportSummary) {
    Column {
        // Header indicating filtered data
        Text(
            text = "Summary (Filtered)",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFFF9FAFB),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
        // Total Cash In
        Card(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = "Cash In",
                    tint = Color(0xFF10B981),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total Cash In",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Rs.${String.format("%.2f", summary.totalCashIn)}",
                    color = Color(0xFF10B981),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Total Cash Out
        Card(
            modifier = Modifier.weight(1f),
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
                    contentDescription = "Cash Out",
                    tint = Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Total Cash Out",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Rs.${String.format("%.2f", summary.totalCashOut)}",
                    color = Color(0xFFEF4444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Net Change
        Card(
            modifier = Modifier.weight(1f),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    if (summary.netChange >= 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                    contentDescription = "Net Change",
                    tint = if (summary.netChange >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Net Change",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Rs.${String.format("%.2f", summary.netChange)}",
                    color = if (summary.netChange >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        }
    }
}

@Composable
private fun CashReportTransactionCard(
    transaction: CashReport,
    categories: List<CashReportCategory>,
    userRole: com.humblecoders.plantmanagement.data.UserRole?,
    onDelete: () -> Unit
) {
    var showImageDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedDate = transaction.date?.let { 
        dateFormat.format(Date(it.seconds * 1000)) 
    } ?: "Unknown Date"
    
    // Find category name from categoryId
    val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Unknown Category"

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
                        when (transaction.transactionType) {
                            CashReportType.CASH_IN -> Icons.Default.TrendingUp
                            CashReportType.CASH_OUT -> Icons.Default.TrendingDown
                        },
                        contentDescription = null,
                        tint = when (transaction.transactionType) {
                            CashReportType.CASH_IN -> Color(0xFF10B981)
                            CashReportType.CASH_OUT -> Color(0xFFEF4444)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    
                    // Accountant Transaction Badge
                    if (transaction.accountantTransaction) {
                        Card(
                            backgroundColor = Color(0xFF8B5CF6),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                text = "ACCOUNTANT",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = categoryName,
                        color = Color(0xFFF9FAFB),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    if (transaction.documentUrls.isNotEmpty()) {
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
                
                if (transaction.notes.isNotBlank()) {
                    Text(
                        text = transaction.notes,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = when (transaction.transactionType) {
                        CashReportType.CASH_IN -> "+"
                        CashReportType.CASH_OUT -> "-"
                    } + "Rs.${String.format("%.2f", transaction.amount)}",
                    color = when (transaction.transactionType) {
                        CashReportType.CASH_IN -> Color(0xFF10B981)
                        CashReportType.CASH_OUT -> Color(0xFFEF4444)
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // View Image Button - Only show if transaction has an image
                    if (transaction.documentUrls.isNotEmpty()) {
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
    
    if (showImageDialog && transaction.documentUrls.isNotEmpty()) {
        DocumentViewDialog(
            documentUrls = transaction.documentUrls,
            onDismiss = { showImageDialog = false }
        )
    }
}

@Composable
private fun DeleteConfirmationDialog(
    transaction: CashReport,
    categories: List<CashReportCategory>,
    cashReportState: CashReportState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    // Find category name from categoryId
    val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Unknown Category"
    Dialog(onDismissRequest = { 
        if (!cashReportState.isDeletingCashReport) {
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
                    text = "Delete Transaction",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "Are you sure you want to delete this transaction?",
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
                    text = "Amount: Rs.${String.format("%.2f", transaction.amount)}",
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
                        enabled = !cashReportState.isDeletingCashReport,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        if (cashReportState.isDeletingCashReport) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (cashReportState.isDeletingCashReport) "Deleting..." else "Delete")
                    }
                }
            }
        }
    }
}

@Composable
private fun CashReportFilterCard(
    cashReportViewModel: CashReportViewModel,
    cashReportState: com.humblecoders.plantmanagement.viewmodels.CashReportState,
    userBalanceViewModel: UserBalanceViewModel,
    showUserCashOutFilter: Boolean,
    onToggleUserCashOutFilter: () -> Unit
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
                // Search field
                OutlinedTextField(
                    value = cashReportState.searchQuery,
                    onValueChange = { cashReportViewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search", color = Color(0xFF9CA3AF)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                    },
                    modifier = Modifier.width(200.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF10B981)
                    ),
                    singleLine = true
                )

                // Type filter
                var typeFilterExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { typeFilterExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF374151)
                        )
                    ) {
                        Text("Filter: ${cashReportState.filterType.name.replace("_", " ")}")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = typeFilterExpanded,
                        onDismissRequest = { typeFilterExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateTypeFilter(CashReportTypeFilter.ALL)
                            typeFilterExpanded = false
                        }) {
                            Text("All Transactions")
                        }
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateTypeFilter(CashReportTypeFilter.CASH_IN)
                            typeFilterExpanded = false
                        }) {
                            Text("Cash In Only")
                        }
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateTypeFilter(CashReportTypeFilter.CASH_OUT)
                            typeFilterExpanded = false
                        }) {
                            Text("Cash Out Only")
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // User Cash Out Filter Toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Checkbox(
                        checked = showUserCashOutFilter,
                        onCheckedChange = { onToggleUserCashOutFilter() },
                        colors = CheckboxDefaults.colors(
                            checkedColor = Color(0xFF10B981),
                            uncheckedColor = Color(0xFF6B7280)
                        )
                    )
                    Text(
                        text = "Show Accountant Transactions Only",
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Date range filters
                Text("From:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                DatePicker(
                    selectedDate = try { 
                        java.time.LocalDate.parse(cashReportState.filterDateFrom, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) 
                    } catch (e: Exception) { 
                        java.time.LocalDate.now() 
                    },
                    onDateSelected = { date -> 
                        cashReportViewModel.updateDateFilter(date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE), cashReportState.filterDateTo) 
                    },
                    modifier = Modifier.width(140.dp),
                    label = ""
                )

                Text("To:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                DatePicker(
                    selectedDate = try { 
                        java.time.LocalDate.parse(cashReportState.filterDateTo, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE) 
                    } catch (e: Exception) { 
                        java.time.LocalDate.now() 
                    },
                    onDateSelected = { date -> 
                        cashReportViewModel.updateDateFilter(cashReportState.filterDateFrom, date.format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)) 
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
                // Sort dropdown
                var sortExpanded by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { sortExpanded = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF374151)
                        )
                    ) {
                        Text("Sort by: ${cashReportState.sortBy.name.lowercase().replaceFirstChar { it.uppercase() }}")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateSortBy(CashReportSortField.DATE)
                            sortExpanded = false
                        }) {
                            Text("Date")
                        }
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateSortBy(CashReportSortField.CATEGORY)
                            sortExpanded = false
                        }) {
                            Text("Category")
                        }
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateSortBy(CashReportSortField.AMOUNT)
                            sortExpanded = false
                        }) {
                            Text("Amount")
                        }
                        DropdownMenuItem(onClick = {
                            cashReportViewModel.updateSortBy(CashReportSortField.TYPE)
                            sortExpanded = false
                        }) {
                            Text("Type")
                        }
                    }
                }

                // Sort direction toggle
                IconButton(
                    onClick = { cashReportViewModel.toggleSortDirection() }
                ) {
                    Icon(
                        imageVector = if (cashReportState.sortDirection == SortDirection.ASCENDING) {
                            Icons.Default.ArrowDropUp
                        } else {
                            Icons.Default.ArrowDropDown
                        },
                        contentDescription = "Sort Direction",
                        tint = Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                // Clear filters button
                if (cashReportState.searchQuery.isNotBlank() || 
                    cashReportState.filterDateFrom.isNotBlank() || 
                    cashReportState.filterDateTo.isNotBlank() || 
                    cashReportState.filterType != CashReportTypeFilter.ALL) {
                    OutlinedButton(
                        onClick = {
                            cashReportViewModel.updateSearchQuery("")
                            cashReportViewModel.updateDateFilter("", "")
                            cashReportViewModel.updateTypeFilter(CashReportTypeFilter.ALL)
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
                        text = "Transaction Image",
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
                            val bufferedImage = javax.imageio.ImageIO.read(inputStream)
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
                        isLoading -> CircularProgressIndicator(color = Color(0xFF10B981))
                        loadError -> Text(
                            "Failed to load image",
                            color = Color(0xFFEF4444)
                        )
                        imageBitmap != null -> androidx.compose.foundation.Image(
                            bitmap = imageBitmap!!,
                            contentDescription = "Transaction Image",
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
                            backgroundColor = Color(0xFF10B981)
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
                        text = "Transaction Documents",
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
                        backgroundColor = Color(0xFF10B981)
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
                            color = Color(0xFF10B981),
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

@Composable
private fun UserBalanceCard(
    userBalance: UserBalance?,
    userRole: com.humblecoders.plantmanagement.data.UserRole?,
    isLoading: Boolean,
    errorMessage: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        elevation = 8.dp,
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shared Accountant Balance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
                
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp
                )
            } else {
                Text(
                    text = "₹${String.format("%.2f", userBalance?.currentBalance ?: 0.0)}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if ((userBalance?.currentBalance ?: 0.0) >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = if (userRole == com.humblecoders.plantmanagement.data.UserRole.ADMIN) {
                        "Admin can transfer balance to/from this shared accountant balance"
                    } else {
                        "Accountants can perform transfers and cash out transactions from this balance"
                    },
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun TransferBalanceDialog(
    onDismiss: () -> Unit,
    onTransfer: (Double, BalanceTransferType, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var transferType by remember { mutableStateOf(BalanceTransferType.ADD) }
    var notes by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Transfer Balance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount", color = Color(0xFF9CA3AF)) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF6B7280)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { transferType = BalanceTransferType.ADD },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (transferType == BalanceTransferType.ADD) Color(0xFF10B981) else Color(0xFF6B7280)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add", color = Color.White)
                    }
                    
                    Button(
                        onClick = { transferType = BalanceTransferType.DEDUCT },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (transferType == BalanceTransferType.DEDUCT) Color(0xFFEF4444) else Color(0xFF6B7280)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Deduct", color = Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)", color = Color(0xFF9CA3AF)) },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        focusedBorderColor = Color(0xFF10B981),
                        unfocusedBorderColor = Color(0xFF6B7280)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter a valid amount",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF6B7280)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel", color = Color.White)
                    }
                    
                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue != null && amountValue > 0) {
                                onTransfer(amountValue, transferType, notes)
                            } else {
                                showError = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Transfer", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransferHistoryDialog(
    balanceTransfers: List<BalanceTransfer>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transfer History",
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
                
                LazyColumn(
                    modifier = Modifier.heightIn(max = 400.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(balanceTransfers) { transfer ->
                        Card(
                            backgroundColor = Color(0xFF374151),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = transfer.transferType.name,
                                        color = if (transfer.transferType == BalanceTransferType.ADD) Color(0xFF10B981) else Color(0xFFEF4444),
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "₹${String.format("%.2f", transfer.amount)}",
                                        color = Color(0xFFF9FAFB),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                if (transfer.notes.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = transfer.notes,
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 12.sp
                                    )
                                }
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = transfer.createdAt?.let { 
                                        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(it.toDate())
                                    } ?: "Unknown date",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
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

