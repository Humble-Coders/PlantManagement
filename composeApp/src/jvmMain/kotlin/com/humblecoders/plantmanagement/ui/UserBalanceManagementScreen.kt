package com.humblecoders.plantmanagement.ui

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
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.UserBalanceViewModel
import com.humblecoders.plantmanagement.services.UserBalancePdfService
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun UserBalanceManagementScreen(
    userBalanceViewModel: UserBalanceViewModel,
    onBack: () -> Unit
) {
    var showTransferDialog by remember { mutableStateOf(false) }
    var showBalanceTransferHistoryDialog by remember { mutableStateOf(false) }
    var showUserCashOutHistoryDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }

    val userBalance = userBalanceViewModel.userBalance
    val balanceTransfers = userBalanceViewModel.balanceTransfers
    val userCashOutTransactions = userBalanceViewModel.userCashOutTransactions
    val isLoading = userBalanceViewModel.isLoading
    val errorMessage = userBalanceViewModel.errorMessage

    LaunchedEffect(Unit) {
        userBalanceViewModel.loadSharedUserBalance()
        userBalanceViewModel.loadAllBalanceTransfers()
        // User cash out transactions are already loaded via the listener in ViewModel
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Shared User Balance Management",
                style = MaterialTheme.typography.h5,
                fontWeight = FontWeight.Bold
            )
            
            Row {
                Button(
                    onClick = { showExportDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Export PDF")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export PDF")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Current Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFFF0F9FF)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Current Shared User Balance",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Rs. ${String.format("%.2f", userBalance?.currentBalance ?: 0.0)}",
                    style = MaterialTheme.typography.h4,
                    fontWeight = FontWeight.Bold,
                    color = if ((userBalance?.currentBalance ?: 0.0) >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showTransferDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
            ) {
                Text("Transfer Balance")
            }
            
            Button(
                onClick = { showBalanceTransferHistoryDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF8B5CF6))
            ) {
                Text("Transfer History")
            }
            
            Button(
                onClick = { showUserCashOutHistoryDialog = true },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFF59E0B))
            ) {
                Text("Cash Out History")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        errorMessage?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFFEE2E2)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFDC2626)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Loading Indicator
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }

    // Transfer Balance Dialog
    if (showTransferDialog) {
        TransferBalanceDialog1(
            onDismiss = { showTransferDialog = false },
            onTransfer = { amount, transferType, notes ->
                userBalanceViewModel.transferBalanceToSharedUserBalance(amount, transferType, notes)
                showTransferDialog = false
            }
        )
    }

    // Balance Transfer History Dialog
    if (showBalanceTransferHistoryDialog) {
        BalanceTransferHistoryDialog(
            transfers = balanceTransfers,
            onDismiss = { showBalanceTransferHistoryDialog = false }
        )
    }

    // User Cash Out History Dialog
    if (showUserCashOutHistoryDialog) {
        UserCashOutHistoryDialog(
            transactions = userCashOutTransactions,
            onDismiss = { showUserCashOutHistoryDialog = false }
        )
    }

    // Export PDF Dialog
    if (showExportDialog) {
        ExportUserBalancePdfDialog(
            userBalance = userBalance,
            balanceTransfers = balanceTransfers,
            userCashOutTransactions = userCashOutTransactions,
            onDismiss = { showExportDialog = false }
        )
    }
}

@Composable
fun TransferBalanceDialog1(
    onDismiss: () -> Unit,
    onTransfer: (Double, BalanceTransferType, String) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var transferType by remember { mutableStateOf(BalanceTransferType.ADD) }
    var notes by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Transfer Balance to Shared User Balance",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Transfer Type Selection
                Text(
                    text = "Transfer Type",
                    style = MaterialTheme.typography.h6,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { transferType = BalanceTransferType.ADD },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (transferType == BalanceTransferType.ADD) 
                                Color(0xFF10B981) else Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Add",
                            color = if (transferType == BalanceTransferType.ADD) 
                                Color.White else Color.Black
                        )
                    }
                    
                    Button(
                        onClick = { transferType = BalanceTransferType.DEDUCT },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (transferType == BalanceTransferType.DEDUCT) 
                                Color(0xFFEF4444) else Color(0xFFE5E7EB)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            "Deduct",
                            color = if (transferType == BalanceTransferType.DEDUCT) 
                                Color.White else Color.Black
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Amount Input
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Notes Input
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color.Black,
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color.Gray
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280))
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            val amountValue = amount.toDoubleOrNull()
                            if (amountValue != null && amountValue > 0) {
                                onTransfer(amountValue, transferType, notes)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
                    ) {
                        Text("Transfer")
                    }
                }
            }
        }
    }
}

@Composable
fun BalanceTransferHistoryDialog(
    transfers: List<BalanceTransfer>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Balance Transfer History",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (transfers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No balance transfers found")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transfers) { transfer ->
                            BalanceTransferCard(transfer = transfer)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun BalanceTransferCard(transfer: BalanceTransfer) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
            backgroundColor = Color(0xFFF9FAFB)
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
                    fontWeight = FontWeight.Bold,
                    color = if (transfer.transferType == BalanceTransferType.ADD) 
                        Color(0xFF10B981) else Color(0xFFEF4444)
                )
                
                Text(
                    text = "Rs. ${String.format("%.2f", transfer.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = if (transfer.transferType == BalanceTransferType.ADD) 
                        Color(0xFF10B981) else Color(0xFFEF4444)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "Admin: ${transfer.fromAdminEmail}",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF6B7280)
            )
            
            if (transfer.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${transfer.notes}",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF6B7280)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = transfer.createdAt?.let { 
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(it.seconds * 1000))
                } ?: "Unknown date",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun UserCashOutHistoryDialog(
    transactions: List<UserCashOutTransaction>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "User Cash Out History",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No cash out transactions found")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(transactions) { transaction ->
                            UserCashOutTransactionCard(transaction = transaction)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close")
                }
            }
        }
    }
}

@Composable
fun UserCashOutTransactionCard(transaction: UserCashOutTransaction) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
            backgroundColor = Color(0xFFF9FAFB)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cash Out",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
                
                Text(
                    text = "Rs. ${String.format("%.2f", transaction.amount)}",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = "User: ${transaction.userEmail}",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF6B7280)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Previous: Rs. ${String.format("%.2f", transaction.previousBalance)}",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF6B7280)
                )
                
                Text(
                    text = "New: Rs. ${String.format("%.2f", transaction.newBalance)}",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF6B7280)
                )
            }
            
            if (transaction.notes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Notes: ${transaction.notes}",
                    style = MaterialTheme.typography.body2,
                    color = Color(0xFF6B7280)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = transaction.createdAt?.let { 
                    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        .format(Date(it.seconds * 1000))
                } ?: "Unknown date",
                style = MaterialTheme.typography.body2,
                color = Color(0xFF6B7280)
            )
        }
    }
}

@Composable
fun ExportUserBalancePdfDialog(
    userBalance: UserBalance?,
    balanceTransfers: List<BalanceTransfer>,
    userCashOutTransactions: List<UserCashOutTransaction>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Export User Balance Report",
                    style = MaterialTheme.typography.h5,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "This will generate a PDF report containing:",
                    style = MaterialTheme.typography.body1
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("• Current shared user balance")
                Text("• Balance transfer history")
                Text("• User cash out transactions")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280))
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = {
                            exportToPdf(userBalance, balanceTransfers, userCashOutTransactions)
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                    ) {
                        Text("Export")
                    }
                }
            }
        }
    }
}

private fun exportToPdf(
    userBalance: UserBalance?,
    balanceTransfers: List<BalanceTransfer>,
    userCashOutTransactions: List<UserCashOutTransaction>
) {
    try {
        val pdfService = UserBalancePdfService()
        val pdfBytes = pdfService.generateUserBalanceReportPdf(
            userBalance = userBalance,
            balanceTransfers = balanceTransfers,
            userCashOutTransactions = userCashOutTransactions,
            filterInfo = "Shared User Balance Report"
        )
        
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Save User Balance Report"
        fileChooser.selectedFile = File("UserBalanceReport_${System.currentTimeMillis()}.pdf")
        fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
        
        if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
            val selectedFile = fileChooser.selectedFile
            selectedFile.writeBytes(pdfBytes)
            println("PDF exported successfully to: ${selectedFile.absolutePath}")
        }
    } catch (e: Exception) {
        println("Error exporting PDF: ${e.message}")
    }
}
