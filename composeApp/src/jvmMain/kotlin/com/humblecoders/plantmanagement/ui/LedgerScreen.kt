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
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.*

data class CustomerLedgerSummary(
    val customer: Entity,
    val pendingPortalAmount: Double,
    val pendingDifferenceAmount: Double,
    val pendingPurchaseAmount: Double,
    val netBalance: Double
)

enum class LedgerSortField {
    CUSTOMER_NAME,
    PENDING_PORTAL,
    PENDING_DIFFERENCE,
    PENDING_PURCHASE,
    NET_BALANCE
}

enum class LedgerSortDirection {
    ASCENDING,
    DESCENDING
}

@Composable
fun LedgerScreen(
    entityViewModel: EntityViewModel,
    saleViewModel: SaleViewModel,
    purchaseViewModel: PurchaseViewModel,
    cashTransactionViewModel: CashTransactionViewModel
) {
    val entityState = entityViewModel.entityState
    val saleState = saleViewModel.saleState
    val purchaseState = purchaseViewModel.purchaseState
    val cashTransactionState = cashTransactionViewModel.cashTransactionState
    
    var searchQuery by remember { mutableStateOf("") }
    var sortField by remember { mutableStateOf(LedgerSortField.NET_BALANCE) }
    var sortDirection by remember { mutableStateOf(LedgerSortDirection.DESCENDING) }
    
    // Calculate ledger summaries for all customers
    val ledgerSummaries = remember(entityState.entities, saleState.sales, purchaseState.purchases, cashTransactionState.transactions) {
        entityState.entities.map { customer ->
            val customerSales = saleState.sales.filter { it.customerId == customer.id }
            val customerPurchases = purchaseState.purchases.filter { it.customerId == customer.id }
            val customerCashTransactions = cashTransactionState.transactions.filter { it.customerId == customer.id }
            
            val pendingPortalAmount = customerSales.sumOf { sale ->
                sale.totalPortalAmount - sale.portalAmountPaid
            }
            
            val pendingDifferenceAmount = customerSales.sumOf { sale ->
                sale.differenceAmount - sale.differenceAmountPaid
            }
            
            val pendingPurchaseAmount = customerPurchases.sumOf { purchase ->
                purchase.totalAmount - purchase.amountPaid
            }
            
            val cashTransactionImpact = customerCashTransactions.sumOf { transaction ->
                when (transaction.transactionType) {
                    CashTransactionType.RECEIVE -> -transaction.amount
                    CashTransactionType.GIVE -> transaction.amount
                }
            }
            
            val netBalance = pendingPortalAmount + pendingDifferenceAmount - pendingPurchaseAmount + cashTransactionImpact
            
            CustomerLedgerSummary(
                customer = customer,
                pendingPortalAmount = pendingPortalAmount,
                pendingDifferenceAmount = pendingDifferenceAmount,
                pendingPurchaseAmount = pendingPurchaseAmount,
                netBalance = netBalance
            )
        }
    }
    
    // Filter and sort the summaries
    val filteredAndSortedSummaries = remember(ledgerSummaries, searchQuery, sortField, sortDirection) {
        val filtered = if (searchQuery.isBlank()) {
            ledgerSummaries
        } else {
            ledgerSummaries.filter { 
                it.customer.firmName.contains(searchQuery, ignoreCase = true) ||
                it.customer.contactPerson.contains(searchQuery, ignoreCase = true)
            }
        }
        
        val sorted = when (sortField) {
            LedgerSortField.CUSTOMER_NAME -> filtered.sortedBy { it.customer.firmName }
            LedgerSortField.PENDING_PORTAL -> filtered.sortedBy { it.pendingPortalAmount }
            LedgerSortField.PENDING_DIFFERENCE -> filtered.sortedBy { it.pendingDifferenceAmount }
            LedgerSortField.PENDING_PURCHASE -> filtered.sortedBy { it.pendingPurchaseAmount }
            LedgerSortField.NET_BALANCE -> filtered.sortedBy { it.netBalance }
        }
        
        if (sortDirection == LedgerSortDirection.DESCENDING) {
            sorted.reversed()
        } else {
            sorted
        }
    }
    
    // Load all data
    LaunchedEffect(Unit) {
        // Data is automatically loaded by ViewModels through their listeners
        // No need to manually call getAll methods
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
            Text(
                text = "Customer Ledger",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        // Data is automatically refreshed by ViewModels
                        // This is just a placeholder for future refresh functionality
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF374151),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Refresh")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Search and Sort Controls
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Search & Sort",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Search
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Search by customer name", color = Color(0xFF9CA3AF)) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        cursorColor = Color(0xFF06B6D4),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151)
                    )
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Sort Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Sort Field Dropdown
                    var expanded by remember { mutableStateOf(false) }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        OutlinedButton(
                            onClick = { expanded = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF374151)
                            )
                        ) {
                            Text(
                                text = when (sortField) {
                                    LedgerSortField.CUSTOMER_NAME -> "Customer Name"
                                    LedgerSortField.PENDING_PORTAL -> "Pending Portal"
                                    LedgerSortField.PENDING_DIFFERENCE -> "Pending Difference"
                                    LedgerSortField.PENDING_PURCHASE -> "Pending Purchase"
                                    LedgerSortField.NET_BALANCE -> "Net Balance"
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Sort Field")
                        }
                        
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            LedgerSortField.values().forEach { field ->
                                DropdownMenuItem(
                                    onClick = {
                                        sortField = field
                                        expanded = false
                                    }
                                ) {
                                    Text(
                                        text = when (field) {
                                            LedgerSortField.CUSTOMER_NAME -> "Customer Name"
                                            LedgerSortField.PENDING_PORTAL -> "Pending Portal"
                                            LedgerSortField.PENDING_DIFFERENCE -> "Pending Difference"
                                            LedgerSortField.PENDING_PURCHASE -> "Pending Purchase"
                                            LedgerSortField.NET_BALANCE -> "Net Balance"
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    // Sort Direction Button
                    Button(
                        onClick = { 
                            sortDirection = if (sortDirection == LedgerSortDirection.ASCENDING) {
                                LedgerSortDirection.DESCENDING
                            } else {
                                LedgerSortDirection.ASCENDING
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (sortDirection == LedgerSortDirection.ASCENDING) {
                                Icons.Default.ArrowUpward
                            } else {
                                Icons.Default.ArrowDownward
                            },
                            contentDescription = "Sort Direction"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (sortDirection == LedgerSortDirection.ASCENDING) "Asc" else "Desc")
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Summary Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SummaryCard(
                title = "Total Customers",
                value = filteredAndSortedSummaries.size.toString(),
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )
            
            SummaryCard(
                title = "Total Net Balance",
                value = "₹ ${String.format("%.2f", filteredAndSortedSummaries.sumOf { it.netBalance })}",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
            
            SummaryCard(
                title = "Total Pending Portal",
                value = "₹ ${String.format("%.2f", filteredAndSortedSummaries.sumOf { it.pendingPortalAmount })}",
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Ledger Table
        Card(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Customer Financial Summary",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                if (entityState.isLoading || saleState.isLoading || purchaseState.isLoading || cashTransactionState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(
                                color = Color(0xFF06B6D4),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Loading ledger data...",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    // Table Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF374151))
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Customer",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(2f)
                        )
                        Text(
                            text = "Pending Portal",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Pending Diff",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Pending Purchase",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "Net Balance",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    // Table Content
                    LazyColumn(
                        modifier = Modifier.height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        items(filteredAndSortedSummaries) { summary ->
                            LedgerRow(summary = summary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun LedgerRow(summary: CustomerLedgerSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF111827))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Customer Name
        Column(modifier = Modifier.weight(2f)) {
            Text(
                text = summary.customer.firmName,
                color = Color(0xFFF9FAFB),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp
            )
            if (summary.customer.contactPerson.isNotBlank()) {
                Text(
                    text = summary.customer.contactPerson,
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp
                )
            }
        }
        
        // Pending Portal Amount
        Text(
            text = "₹ ${String.format("%.2f", summary.pendingPortalAmount)}",
            color = if (summary.pendingPortalAmount > 0) Color(0xFFF59E0B) else Color(0xFF9CA3AF),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        
        // Pending Difference Amount
        Text(
            text = "₹ ${String.format("%.2f", summary.pendingDifferenceAmount)}",
            color = if (summary.pendingDifferenceAmount > 0) Color(0xFF3B82F6) else Color(0xFF9CA3AF),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        
        // Pending Purchase Amount
        Text(
            text = "₹ ${String.format("%.2f", summary.pendingPurchaseAmount)}",
            color = if (summary.pendingPurchaseAmount > 0) Color(0xFFEF4444) else Color(0xFF9CA3AF),
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        
        // Net Balance
        Text(
            text = "₹ ${String.format("%.2f", summary.netBalance)}",
            color = if (summary.netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
    }
}
