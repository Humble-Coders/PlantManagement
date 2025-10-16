package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
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
import com.humblecoders.plantmanagement.data.CashTransaction
import com.humblecoders.plantmanagement.data.CashTransactionType
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.viewmodels.CashTransactionViewModel
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CashTransactionHistoryScreen(
    cashTransactionViewModel: CashTransactionViewModel,
    entityViewModel: EntityViewModel,
    onBack: () -> Unit
) {
    val cashTransactionState = cashTransactionViewModel.cashTransactionState
    val entities = entityViewModel.entityState.entities
    
    var selectedCustomer by remember { mutableStateOf<Entity?>(null) }
    var selectedTransactionType by remember { mutableStateOf<CashTransactionType?>(null) }
    var showCustomerDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }
    
    // Load transactions on screen load
    LaunchedEffect(Unit) {
        cashTransactionViewModel.getCashTransactions() // Load initial data
        cashTransactionViewModel.listenToCashTransactions()
    }
    
    // Clear messages after showing
    LaunchedEffect(cashTransactionState.successMessage, cashTransactionState.error) {
        if (cashTransactionState.successMessage != null || cashTransactionState.error != null) {
            kotlinx.coroutines.delay(3000)
            cashTransactionViewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
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
                    text = "Cash Transaction History",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }
            
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filters",
                    tint = Color(0xFF9CA3AF)
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

        // Filters Section
        if (showFilters) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Filters",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFF9FAFB),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Customer Filter
                    Text(
                        text = "Customer",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showCustomerDropdown = true },
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
                                    text = selectedCustomer?.firmName ?: "All Customers",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF9FAFB)
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown, 
                                    contentDescription = null,
                                    tint = Color(0xFFF9FAFB)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showCustomerDropdown,
                            onDismissRequest = { showCustomerDropdown = false },
                            modifier = Modifier
                                .widthIn(max = 300.dp)
                                .background(Color.White)
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    selectedCustomer = null
                                    showCustomerDropdown = false
                                    cashTransactionViewModel.getCashTransactions(
                                        customerId = null,
                                        transactionType = selectedTransactionType
                                    )
                                    cashTransactionViewModel.listenToCashTransactions(
                                        customerId = null,
                                        transactionType = selectedTransactionType
                                    )
                                }
                            ) {
                                Text("All Customers", color = Color.Black)
                            }
                            entities.forEach { entity ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedCustomer = entity
                                        showCustomerDropdown = false
                                        cashTransactionViewModel.getCashTransactions(
                                            customerId = entity.id,
                                            transactionType = selectedTransactionType
                                        )
                                        cashTransactionViewModel.listenToCashTransactions(
                                            customerId = entity.id,
                                            transactionType = selectedTransactionType
                                        )
                                    }
                                ) {
                                    Text(entity.firmName, color = Color.Black)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Transaction Type Filter
                    Text(
                        text = "Transaction Type",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { showTypeDropdown = true },
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
                                    text = when (selectedTransactionType) {
                                        CashTransactionType.RECEIVE -> "Receive Money"
                                        CashTransactionType.GIVE -> "Give Money"
                                        null -> "All Types"
                                    },
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF9FAFB)
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown, 
                                    contentDescription = null,
                                    tint = Color(0xFFF9FAFB)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showTypeDropdown,
                            onDismissRequest = { showTypeDropdown = false },
                            modifier = Modifier
                                .widthIn(max = 200.dp)
                                .background(Color.White)
                        ) {
                            DropdownMenuItem(
                                onClick = {
                                    selectedTransactionType = null
                                    showTypeDropdown = false
                                    cashTransactionViewModel.getCashTransactions(
                                        customerId = selectedCustomer?.id,
                                        transactionType = null
                                    )
                                    cashTransactionViewModel.listenToCashTransactions(
                                        customerId = selectedCustomer?.id,
                                        transactionType = null
                                    )
                                }
                            ) {
                                Text("All Types", color = Color.Black)
                            }
                            CashTransactionType.values().forEach { type ->
                                DropdownMenuItem(
                                    onClick = {
                                        selectedTransactionType = type
                                        showTypeDropdown = false
                                        cashTransactionViewModel.getCashTransactions(
                                            customerId = selectedCustomer?.id,
                                            transactionType = type
                                        )
                                        cashTransactionViewModel.listenToCashTransactions(
                                            customerId = selectedCustomer?.id,
                                            transactionType = type
                                        )
                                    }
                                ) {
                                    Text(
                                        text = when (type) {
                                            CashTransactionType.RECEIVE -> "Receive Money"
                                            CashTransactionType.GIVE -> "Give Money"
                                        },
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Transactions List
        if (cashTransactionState.isLoading) {
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
                        text = "Loading transactions...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                }
            }
        } else if (cashTransactionState.transactions.isEmpty()) {
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
                        text = "No cash transactions found",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cashTransactionState.transactions) { transaction ->
                    CashTransactionCard(transaction = transaction)
                }
            }
        }
    }
}

@Composable
fun CashTransactionCard(transaction: CashTransaction) {
    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val formattedDate = transaction.createdAt?.let { 
        dateFormat.format(Date(it.seconds * 1000)) 
    } ?: "Unknown Date"
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.customerName,
                        color = Color(0xFFF9FAFB),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = formattedDate,
                        color = Color(0xFF9CA3AF),
                        fontSize = 12.sp
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = when (transaction.transactionType) {
                            CashTransactionType.RECEIVE -> "Received"
                            CashTransactionType.GIVE -> "Given"
                        },
                        color = when (transaction.transactionType) {
                            CashTransactionType.RECEIVE -> Color(0xFF10B981)
                            CashTransactionType.GIVE -> Color(0xFFEF4444)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "₹${String.format("%.2f", transaction.amount)}",
                        color = Color(0xFFF9FAFB),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            if (transaction.note.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = transaction.note,
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Balance change info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Previous Balance:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
                Text(
                    text = "₹${String.format("%.2f", transaction.previousBalance)}",
                    color = if (transaction.previousBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "New Balance:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
                Text(
                    text = "₹${String.format("%.2f", transaction.newBalance)}",
                    color = if (transaction.newBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
