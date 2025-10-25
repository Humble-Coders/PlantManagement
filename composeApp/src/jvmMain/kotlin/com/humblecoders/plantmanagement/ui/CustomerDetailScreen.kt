package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.CashTransaction
import com.humblecoders.plantmanagement.data.CashTransactionType
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.data.PaymentStatus
import com.humblecoders.plantmanagement.data.Purchase
import com.humblecoders.plantmanagement.data.Sale
import com.humblecoders.plantmanagement.data.SaleStatus
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.ui.components.CashOutDialog
import com.humblecoders.plantmanagement.ui.components.CashInRevenueDialog
import com.humblecoders.plantmanagement.ui.components.CashInOutDifferenceDialog
import com.humblecoders.plantmanagement.viewmodels.CashTransactionViewModel
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun CustomerDetailScreen(
    customer: Entity,
    saleViewModel: SaleViewModel,
    purchaseViewModel: PurchaseViewModel,
    cashTransactionViewModel: CashTransactionViewModel,
    entityViewModel: EntityViewModel,
    onBack: () -> Unit
) {
    val saleState = saleViewModel.saleState
    val purchaseState = purchaseViewModel.purchaseState
    val cashTransactionState = cashTransactionViewModel.cashTransactionState
    
    var selectedTransactionType by remember { mutableStateOf(TransactionType.ALL) }
    var showFilters by remember { mutableStateOf(false) }
    
    // Filter and sort state
    var sortBy by remember { mutableStateOf(TransactionSortField.DATE) }
    var sortDirection by remember { mutableStateOf(SortDirection.DESCENDING) }
    var filterDateFrom by remember { mutableStateOf("") }
    var filterDateTo by remember { mutableStateOf("") }
    
    // Loading state management
    var isRefreshing by remember { mutableStateOf(false) }
    var lastRefreshTime by remember { mutableStateOf(System.currentTimeMillis()) }
    
    // Dialog state variables
    var showCashOutDialog by remember { mutableStateOf(false) }
    var showCashInRevenueDialog by remember { mutableStateOf(false) }
    var showDifferenceDialog by remember { mutableStateOf(false) }
    
    // Calculate financial summaries
    val financialSummary = remember(customer.id, saleState.sales, purchaseState.purchases, cashTransactionState.transactions) {
        calculateFinancialSummary(customer.id, saleState.sales, purchaseState.purchases, cashTransactionState.transactions)
    }
    
    // Refresh function with proper async handling and buffering
    val refreshData = {
        if (!isRefreshing) {
            isRefreshing = true
            lastRefreshTime = System.currentTimeMillis()
            
            // Use coroutines for proper async handling
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    // Refresh all data asynchronously
                    saleViewModel.refreshSales()
                    purchaseViewModel.refreshPurchases()
                    cashTransactionViewModel.refreshCashTransactions()
                    
                    // Add minimum delay for better UX (buffering)
                    delay(500)
                } catch (e: Exception) {
                    println("Error during refresh: ${e.message}")
                } finally {
                    isRefreshing = false
                }
            }
        }
    }
    
    // Check if data is stale (older than 2 minutes)
    val isDataStale = remember(lastRefreshTime) {
        System.currentTimeMillis() - lastRefreshTime > 120000 // 2 minutes
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                    text = "Customer Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = refreshData,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (isDataStale) Color(0xFFF59E0B) else Color(0xFF374151),
                        contentColor = Color.White
                    ),
                    enabled = !isRefreshing && !saleState.isLoading && !purchaseState.isLoading && !cashTransactionState.isLoading
                ) {
                    if (isRefreshing || saleState.isLoading || purchaseState.isLoading || cashTransactionState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refreshing...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isDataStale) "Refresh (Stale)" else "Refresh")
                    }
                }
                
                Button(
                    onClick = { showFilters = !showFilters },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF374151),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filters")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Filters")
                }
                
                Button(
                    onClick = { 
                        printCustomerTransactions(
                            customer = customer,
                            transactionType = selectedTransactionType,
                            sales = saleState.sales.filter { it.customerId == customer.id },
                            purchases = purchaseState.purchases.filter { it.customerId == customer.id },
                            cashTransactions = cashTransactionState.transactions.filter { it.customerId == customer.id },
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print Transactions")
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Quick Action Buttons for Cash Dialogs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { showCashOutDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFEF4444),
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cash Out (Purchase)", fontSize = 14.sp)
            }
            
            Button(
                onClick = { showCashInRevenueDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF10B981),
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Cash In (Sales)", fontSize = 14.sp)
            }
            
            Button(
                onClick = { showDifferenceDialog = true },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFF06B6D4),
                    contentColor = Color.White
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("Difference", fontSize = 14.sp)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Customer Information Card
        CustomerInfoCard(customer = customer)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Financial Summary Cards
        FinancialSummaryCards(financialSummary = financialSummary)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Transaction Type Selector
        TransactionTypeSelector(
            selectedType = selectedTransactionType,
            onTypeSelected = { selectedTransactionType = it }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Filters (if shown)
        if (showFilters) {
            TransactionFilters(
                sortBy = sortBy,
                sortDirection = sortDirection,
                filterDateFrom = filterDateFrom,
                filterDateTo = filterDateTo,
                onSortByChanged = { sortBy = it },
                onSortDirectionChanged = { sortDirection = it },
                onDateFromChanged = { filterDateFrom = it },
                onDateToChanged = { filterDateTo = it },
                onClearFilters = {
                    sortBy = TransactionSortField.DATE
                    sortDirection = SortDirection.DESCENDING
                    filterDateFrom = ""
                    filterDateTo = ""
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Data freshness indicator
        if (isDataStale && !isRefreshing) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFFF59E0B).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = "Stale Data",
                        tint = Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Data may be outdated. Click refresh to get latest information.",
                        color = Color(0xFFF59E0B),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        // Transaction Records
        TransactionRecordsSection(
            transactionType = selectedTransactionType,
            sales = saleState.sales.filter { it.customerId == customer.id },
            purchases = purchaseState.purchases.filter { it.customerId == customer.id },
            cashTransactions = cashTransactionState.transactions.filter { it.customerId == customer.id },
            isLoading = saleState.isLoading || purchaseState.isLoading || cashTransactionState.isLoading || isRefreshing,
            sortBy = sortBy,
            sortDirection = sortDirection,
            filterDateFrom = filterDateFrom,
            filterDateTo = filterDateTo
        )
        }
    }
    
    // Cash Out Dialog (from Purchase Module)
    if (showCashOutDialog) {
        CashOutDialog(
            purchaseViewModel = purchaseViewModel,
            entityViewModel = entityViewModel,
            onDismiss = { showCashOutDialog = false },
            preselectedCustomer = customer
        )
    }
    
    // Cash In Revenue Dialog (from Sales Module)
    if (showCashInRevenueDialog) {
        CashInRevenueDialog(
            saleViewModel = saleViewModel,
            entityViewModel = entityViewModel,
            onDismiss = { showCashInRevenueDialog = false },
            preselectedCustomer = customer
        )
    }
    
    // Cash In/Out Difference Dialog (from Sales Module)
    if (showDifferenceDialog) {
        CashInOutDifferenceDialog(
            saleViewModel = saleViewModel,
            entityViewModel = entityViewModel,
            onDismiss = { showDifferenceDialog = false },
            preselectedCustomer = customer
        )
    }
}

@Composable
fun CustomerInfoCard(customer: Entity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = "Customer Information",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("Firm Name", customer.firmName)
                    InfoRow("Contact Person", customer.contactPerson)
                    InfoRow("Contact Number", customer.contactNo)
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    InfoRow("City", customer.city)
                    InfoRow("State", customer.state)
                    InfoRow("GSTIN", customer.gstin)
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = "$label:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF9CA3AF),
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value.ifEmpty { "N/A" },
            fontSize = 14.sp,
            color = Color(0xFFF9FAFB)
        )
    }
}

@Composable
fun FinancialSummaryCards(financialSummary: FinancialSummary) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // First row - 4 cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancialCard(
                title = "Pending Portal Amount",
                amount = financialSummary.pendingPortalAmount,
                color = Color(0xFF10B981), // Green
                modifier = Modifier.weight(1f)
            )
            
            FinancialCard(
                title = "Pending Purchase Amount",
                amount = financialSummary.pendingPurchaseAmount,
                color = Color(0xFFEF4444), // Red
                modifier = Modifier.weight(1f)
            )
            
            FinancialCard(
                title = "Cash In",
                amount = financialSummary.cashInAmount,
                color = Color(0xFF10B981), // Green
                modifier = Modifier.weight(1f)
            )
            
            FinancialCard(
                title = "Cash Out",
                amount = financialSummary.cashOutAmount,
                color = Color(0xFFEF4444), // Red
                modifier = Modifier.weight(1f)
            )
        }
        
        // Second row - Difference amounts (3 cards)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancialCard(
                title = "Positive Difference",
                amount = financialSummary.positiveDifferenceAmount,
                color = Color(0xFF10B981), // Green
                modifier = Modifier.weight(1f)
            )
            
            FinancialCard(
                title = "Negative Difference",
                amount = financialSummary.negativeDifferenceAmount,
                color = Color(0xFFEF4444), // Red
                modifier = Modifier.weight(1f)
            )
            
            FinancialCard(
                title = "Net Difference",
                amount = financialSummary.pendingDifferenceAmount,
                color = if (financialSummary.pendingDifferenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444), // Green if positive, Red if negative
                modifier = Modifier.weight(1f)
            )
        }
        
        // Third row - Net Balance
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FinancialCard(
                title = "Net Balance",
                amount = financialSummary.netBalance,
                color = if (financialSummary.netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444), // Green if positive, Red if negative
                modifier = Modifier.weight(1f)
            )
            
            // Add empty space to maintain layout
            Spacer(modifier = Modifier.weight(3f))
        }
    }
}

@Composable
fun FinancialCard(
    title: String,
    amount: Double,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp),
        elevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF9CA3AF),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "₹ ${String.format("%.2f", amount)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun TransactionTypeSelector(
    selectedType: TransactionType,
    onTypeSelected: (TransactionType) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        TransactionType.values().forEach { type ->
            Button(
                onClick = { onTypeSelected(type) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = if (selectedType == type) Color(0xFF06B6D4) else Color(0xFF374151),
                    contentColor = Color.White
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Text(type.displayName, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun TransactionFilters(
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String,
    onSortByChanged: (TransactionSortField) -> Unit,
    onSortDirectionChanged: (SortDirection) -> Unit,
    onDateFromChanged: (String) -> Unit,
    onDateToChanged: (String) -> Unit,
    onClearFilters: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Filter & Sort Transactions",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            // Date Range Filters
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("From:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                DatePicker(
                    selectedDate = try { 
                        LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE) 
                    } catch (e: Exception) { 
                        LocalDate.now().minusMonths(1) 
                    },
                    onDateSelected = { date -> 
                        onDateFromChanged(date.format(DateTimeFormatter.ISO_LOCAL_DATE)) 
                    },
                    modifier = Modifier.weight(1f),
                    label = ""
                )
                
                Text("To:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                DatePicker(
                    selectedDate = try { 
                        LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE) 
                    } catch (e: Exception) { 
                        LocalDate.now() 
                    },
                    onDateSelected = { date -> 
                        onDateToChanged(date.format(DateTimeFormatter.ISO_LOCAL_DATE)) 
                    },
                    modifier = Modifier.weight(1f),
                    label = ""
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Sort Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                        Text("Sort by: ${sortBy.displayName}")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false }
                    ) {
                        DropdownMenuItem(onClick = {
                            onSortByChanged(TransactionSortField.DATE)
                            sortExpanded = false
                        }) {
                            Text("Date")
                        }
                        DropdownMenuItem(onClick = {
                            onSortByChanged(TransactionSortField.AMOUNT)
                            sortExpanded = false
                        }) {
                            Text("Amount")
                        }
                        DropdownMenuItem(onClick = {
                            onSortByChanged(TransactionSortField.TYPE)
                            sortExpanded = false
                        }) {
                            Text("Type")
                        }
                        DropdownMenuItem(onClick = {
                            onSortByChanged(TransactionSortField.STATUS)
                            sortExpanded = false
                        }) {
                            Text("Status")
                        }
                    }
                }

                // Sort direction toggle
                IconButton(
                    onClick = { 
                        onSortDirectionChanged(
                            if (sortDirection == SortDirection.ASCENDING) 
                                SortDirection.DESCENDING 
                            else 
                                SortDirection.ASCENDING
                        )
                    }
                ) {
                    Icon(
                        imageVector = if (sortDirection == SortDirection.ASCENDING) {
                            Icons.Default.ArrowDropUp
                        } else {
                            Icons.Default.ArrowDropDown
                        },
                        contentDescription = "Sort Direction",
                        tint = Color(0xFF06B6D4)
                    )
                }
                
                Spacer(modifier = Modifier.weight(1f))
                
                // Clear filters button
                if (filterDateFrom.isNotBlank() || filterDateTo.isNotBlank() || 
                    sortBy != TransactionSortField.DATE || sortDirection != SortDirection.DESCENDING) {
                    OutlinedButton(
                        onClick = onClearFilters,
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
fun TransactionRecordsSection(
    transactionType: TransactionType,
    sales: List<Sale>,
    purchases: List<Purchase>,
    cashTransactions: List<CashTransaction>,
    isLoading: Boolean = false,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Transaction Records",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            if (isLoading) {
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
                            text = "Loading transactions...",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Fetching latest data from Firebase",
                            color = Color(0xFF6B7280),
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "This may take a moment",
                            color = Color(0xFF6B7280),
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                when (transactionType) {
                    TransactionType.ALL -> {
                        // Show all transactions combined
                        CombinedTransactionList(
                            sales = sales,
                            purchases = purchases,
                            cashTransactions = cashTransactions,
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                    TransactionType.SALES -> {
                        SalesTransactionList(
                            sales = sales,
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                    TransactionType.PURCHASES -> {
                        PurchasesTransactionList(
                            purchases = purchases,
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                    TransactionType.CASH_IN -> {
                        CashInTransactionList(
                            cashTransactions = cashTransactions.filter { it.transactionType == CashTransactionType.RECEIVE },
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                    TransactionType.CASH_OUT -> {
                        CashOutTransactionList(
                            cashTransactions = cashTransactions.filter { it.transactionType == CashTransactionType.GIVE },
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                    TransactionType.CASH_TRANSACTIONS -> {
                        AllCashTransactionList(
                            cashTransactions = cashTransactions,
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                    TransactionType.DIFFERENCES -> {
                        DifferenceTransactionList(
                            sales = sales,
                            sortBy = sortBy,
                            sortDirection = sortDirection,
                            filterDateFrom = filterDateFrom,
                            filterDateTo = filterDateTo
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CombinedTransactionList(
    sales: List<Sale>,
    purchases: List<Purchase>,
    cashTransactions: List<CashTransaction>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val allTransactions = remember(sales, purchases, cashTransactions, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        val salesTransactions = sales.map { TransactionItem.SaleItem(it) }
        val purchaseTransactions = purchases.map { TransactionItem.PurchaseItem(it) }
        val cashTransactionsItems = cashTransactions.map { TransactionItem.CashTransactionItem(it) }
        
        val allItems = salesTransactions + purchaseTransactions + cashTransactionsItems
        
        // Apply date filtering
        val filteredItems = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                allItems.filter { transaction ->
                    val transactionDate = when (transaction) {
                        is TransactionItem.SaleItem -> {
                            try {
                                // Try multiple date formats for saleDate
                                when {
                                    transaction.sale.saleDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                                        // yyyy-mm-dd format
                                        LocalDate.parse(transaction.sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                    }
                                    transaction.sale.saleDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                                        // dd/MM/yyyy format
                                        LocalDate.parse(transaction.sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    }
                                    else -> {
                                        // Try parsing as ISO date first, then fallback to dd/MM/yyyy
                                        try {
                                            LocalDate.parse(transaction.sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                        } catch (e: Exception) {
                                            LocalDate.parse(transaction.sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        }
                        is TransactionItem.PurchaseItem -> {
                            try {
                                // Try multiple date formats for purchaseDate
                                when {
                                    transaction.purchase.purchaseDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                                        // yyyy-mm-dd format
                                        LocalDate.parse(transaction.purchase.purchaseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                    }
                                    transaction.purchase.purchaseDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                                        // dd/MM/yyyy format
                                        LocalDate.parse(transaction.purchase.purchaseDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                    }
                                    else -> {
                                        // Try parsing as ISO date first, then fallback to dd/MM/yyyy
                                        try {
                                            LocalDate.parse(transaction.purchase.purchaseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                        } catch (e: Exception) {
                                            LocalDate.parse(transaction.purchase.purchaseDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        }
                        is TransactionItem.CashTransactionItem -> {
                            transaction.cashTransaction.createdAt?.let { timestamp ->
                                try {
                                    // Convert Firebase Timestamp to LocalDate
                                    java.time.LocalDate.ofEpochDay(timestamp.seconds / 86400)
                                } catch (e: Exception) {
                                    LocalDate.now()
                                }
                            } ?: LocalDate.now()
                        }
                    }
                    transactionDate.isAfter(fromDate.minusDays(1)) && transactionDate.isBefore(toDate.plusDays(1))
                }
            } catch (e: Exception) {
                allItems
            }
        } else {
            allItems
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredItems.sortedBy { it.date }
                } else {
                    filteredItems.sortedByDescending { it.date }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredItems.sortedBy { 
                        when (it) {
                            is TransactionItem.SaleItem -> it.sale.totalRevenueAmount
                            is TransactionItem.PurchaseItem -> it.purchase.grandTotal
                            is TransactionItem.CashTransactionItem -> it.cashTransaction.amount
                        }
                    }
                } else {
                    filteredItems.sortedByDescending { 
                        when (it) {
                            is TransactionItem.SaleItem -> it.sale.totalRevenueAmount
                            is TransactionItem.PurchaseItem -> it.purchase.grandTotal
                            is TransactionItem.CashTransactionItem -> it.cashTransaction.amount
                        }
                    }
                }
            }
            TransactionSortField.TYPE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredItems.sortedBy { it.type }
                } else {
                    filteredItems.sortedByDescending { it.type }
                }
            }
            TransactionSortField.STATUS -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredItems.sortedBy { it.status }
                } else {
                    filteredItems.sortedByDescending { it.status }
                }
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        allTransactions.forEach { transaction ->
            TransactionItemCard(transaction = transaction)
        }
    }
}

@Composable
fun SalesTransactionList(
    sales: List<Sale>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val filteredAndSortedSales = remember(sales, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        // Apply date filtering
        val filteredSales = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                sales.filter { sale ->
                    try {
                        // Try multiple date formats for saleDate
                        val saleDate = when {
                            sale.saleDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                                // yyyy-mm-dd format
                                LocalDate.parse(sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            }
                            sale.saleDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                                // dd/MM/yyyy format
                                LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            }
                            else -> {
                                // Try parsing as ISO date first, then fallback to dd/MM/yyyy
                                try {
                                    LocalDate.parse(sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                } catch (e: Exception) {
                                    LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                }
                            }
                        }
                        saleDate.isAfter(fromDate.minusDays(1)) && saleDate.isBefore(toDate.plusDays(1))
                    } catch (e: Exception) {
                        // If date parsing fails, include the record
                        true
                    }
                }
            } catch (e: Exception) {
                sales
            }
        } else {
            sales
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredSales.sortedBy { it.saleDate }
                } else {
                    filteredSales.sortedByDescending { it.saleDate }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredSales.sortedBy { it.totalRevenueAmount }
                } else {
                    filteredSales.sortedByDescending { it.totalRevenueAmount }
                }
            }
            TransactionSortField.TYPE -> {
                filteredSales // Sales are all the same type
            }
            TransactionSortField.STATUS -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredSales.sortedBy { it.saleStatus.name }
                } else {
                    filteredSales.sortedByDescending { it.saleStatus.name }
                }
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredAndSortedSales.forEach { sale ->
            TransactionItemCard(transaction = TransactionItem.SaleItem(sale))
        }
    }
}

@Composable
fun PurchasesTransactionList(
    purchases: List<Purchase>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val filteredAndSortedPurchases = remember(purchases, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        // Apply date filtering
        val filteredPurchases = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                purchases.filter { purchase ->
                    try {
                        // Try multiple date formats for purchaseDate
                        val purchaseDate = when {
                            purchase.purchaseDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                                // yyyy-mm-dd format
                                LocalDate.parse(purchase.purchaseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            }
                            purchase.purchaseDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                                // dd/MM/yyyy format
                                LocalDate.parse(purchase.purchaseDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            }
                            else -> {
                                // Try parsing as ISO date first, then fallback to dd/MM/yyyy
                                try {
                                    LocalDate.parse(purchase.purchaseDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                } catch (e: Exception) {
                                    LocalDate.parse(purchase.purchaseDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                }
                            }
                        }
                        purchaseDate.isAfter(fromDate.minusDays(1)) && purchaseDate.isBefore(toDate.plusDays(1))
                    } catch (e: Exception) {
                        // If date parsing fails, include the record
                        true
                    }
                }
            } catch (e: Exception) {
                purchases
            }
        } else {
            purchases
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredPurchases.sortedBy { it.purchaseDate }
                } else {
                    filteredPurchases.sortedByDescending { it.purchaseDate }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredPurchases.sortedBy { it.grandTotal }
                } else {
                    filteredPurchases.sortedByDescending { it.grandTotal }
                }
            }
            TransactionSortField.TYPE -> {
                filteredPurchases // Purchases are all the same type
            }
            TransactionSortField.STATUS -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredPurchases.sortedBy { it.paymentStatus.name }
                } else {
                    filteredPurchases.sortedByDescending { it.paymentStatus.name }
                }
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredAndSortedPurchases.forEach { purchase ->
            TransactionItemCard(transaction = TransactionItem.PurchaseItem(purchase))
        }
    }
}

@Composable
fun CashInTransactionList(
    cashTransactions: List<CashTransaction>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val filteredAndSortedTransactions = remember(cashTransactions, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        // Apply date filtering
        val filteredTransactions = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                cashTransactions.filter { transaction ->
                    transaction.createdAt?.let { timestamp ->
                        try {
                            // Convert Firebase Timestamp to LocalDate
                            val transactionDate = java.time.LocalDate.ofEpochDay(timestamp.seconds / 86400)
                            transactionDate.isAfter(fromDate.minusDays(1)) && transactionDate.isBefore(toDate.plusDays(1))
                        } catch (e: Exception) {
                            // If timestamp conversion fails, include the record
                            true
                        }
                    } ?: true // Include records without createdAt timestamp
                }
            } catch (e: Exception) {
                cashTransactions
            }
        } else {
            cashTransactions
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.createdAt }
                } else {
                    filteredTransactions.sortedByDescending { it.createdAt }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.amount }
                } else {
                    filteredTransactions.sortedByDescending { it.amount }
                }
            }
            TransactionSortField.TYPE -> {
                filteredTransactions // All are cash in transactions
            }
            TransactionSortField.STATUS -> {
                filteredTransactions // All are completed
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredAndSortedTransactions.forEach { transaction ->
            TransactionItemCard(transaction = TransactionItem.CashTransactionItem(transaction))
        }
    }
}

@Composable
fun CashOutTransactionList(
    cashTransactions: List<CashTransaction>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val filteredAndSortedTransactions = remember(cashTransactions, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        // Apply date filtering
        val filteredTransactions = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                cashTransactions.filter { transaction ->
                    transaction.createdAt?.let { timestamp ->
                        try {
                            // Convert Firebase Timestamp to LocalDate
                            val transactionDate = java.time.LocalDate.ofEpochDay(timestamp.seconds / 86400)
                            transactionDate.isAfter(fromDate.minusDays(1)) && transactionDate.isBefore(toDate.plusDays(1))
                        } catch (e: Exception) {
                            // If timestamp conversion fails, include the record
                            true
                        }
                    } ?: true // Include records without createdAt timestamp
                }
            } catch (e: Exception) {
                cashTransactions
            }
        } else {
            cashTransactions
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.createdAt }
                } else {
                    filteredTransactions.sortedByDescending { it.createdAt }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.amount }
                } else {
                    filteredTransactions.sortedByDescending { it.amount }
                }
            }
            TransactionSortField.TYPE -> {
                filteredTransactions // All are cash out transactions
            }
            TransactionSortField.STATUS -> {
                filteredTransactions // All are completed
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredAndSortedTransactions.forEach { transaction ->
            TransactionItemCard(transaction = TransactionItem.CashTransactionItem(transaction))
        }
    }
}

@Composable
fun AllCashTransactionList(
    cashTransactions: List<CashTransaction>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val filteredAndSortedTransactions = remember(cashTransactions, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        // Apply date filtering
        val filteredTransactions = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                cashTransactions.filter { transaction ->
                    transaction.createdAt?.let { timestamp ->
                        try {
                            // Convert Firebase Timestamp to LocalDate
                            val transactionDate = java.time.LocalDate.ofEpochDay(timestamp.seconds / 86400)
                            transactionDate.isAfter(fromDate.minusDays(1)) && transactionDate.isBefore(toDate.plusDays(1))
                        } catch (e: Exception) {
                            // If timestamp conversion fails, include the record
                            true
                        }
                    } ?: true // Include records without createdAt timestamp
                }
            } catch (e: Exception) {
                cashTransactions
            }
        } else {
            cashTransactions
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.createdAt }
                } else {
                    filteredTransactions.sortedByDescending { it.createdAt }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.amount }
                } else {
                    filteredTransactions.sortedByDescending { it.amount }
                }
            }
            TransactionSortField.TYPE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.transactionType.name }
                } else {
                    filteredTransactions.sortedByDescending { it.transactionType.name }
                }
            }
            TransactionSortField.STATUS -> {
                filteredTransactions // All are completed
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        filteredAndSortedTransactions.forEach { transaction ->
            TransactionItemCard(transaction = TransactionItem.CashTransactionItem(transaction))
        }
    }
}

@Composable
fun DifferenceTransactionList(
    sales: List<Sale>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    val salesWithDifferences = remember(sales, sortBy, sortDirection, filterDateFrom, filterDateTo) {
        val filteredSales = sales.filter { it.differenceAmount != 0.0 }
        
        // Apply date filtering
        val dateFilteredSales = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                filteredSales.filter { sale ->
                    try {
                        // Try multiple date formats for saleDate
                        val saleDate = when {
                            sale.saleDate.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) -> {
                                // yyyy-mm-dd format
                                LocalDate.parse(sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                            }
                            sale.saleDate.matches(Regex("\\d{2}/\\d{2}/\\d{4}")) -> {
                                // dd/MM/yyyy format
                                LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            }
                            else -> {
                                // Try parsing as ISO date first, then fallback to dd/MM/yyyy
                                try {
                                    LocalDate.parse(sale.saleDate, DateTimeFormatter.ISO_LOCAL_DATE)
                                } catch (e: Exception) {
                                    LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                                }
                            }
                        }
                        saleDate.isAfter(fromDate.minusDays(1)) && saleDate.isBefore(toDate.plusDays(1))
                    } catch (e: Exception) {
                        // If date parsing fails, include the record
                        true
                    }
                }
            } catch (e: Exception) {
                filteredSales
            }
        } else {
            filteredSales
        }
        
        // Apply sorting
        when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    dateFilteredSales.sortedBy { it.saleDate }
                } else {
                    dateFilteredSales.sortedByDescending { it.saleDate }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    dateFilteredSales.sortedBy { it.differenceAmount }
                } else {
                    dateFilteredSales.sortedByDescending { it.differenceAmount }
                }
            }
            TransactionSortField.TYPE -> {
                dateFilteredSales // All are sales with differences
            }
            TransactionSortField.STATUS -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    dateFilteredSales.sortedBy { it.differenceStatus.name }
                } else {
                    dateFilteredSales.sortedByDescending { it.differenceStatus.name }
                }
            }
        }
    }
    
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        salesWithDifferences.forEach { sale ->
            TransactionItemCard(transaction = TransactionItem.SaleItem(sale))
        }
    }
}

@Composable
fun TransactionItemCard(transaction: TransactionItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row with type and main amount
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = transaction.type,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = transaction.typeColor
                    )
                    Text(
                        text = transaction.description,
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = transaction.amount,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = transaction.amountColor
                    )
                    Text(
                        text = transaction.status,
                        fontSize = 11.sp,
                        color = transaction.statusColor
                    )
                }
            }
            
            // Date row
            Text(
                text = transaction.date,
                fontSize = 11.sp,
                color = Color(0xFF6B7280),
                modifier = Modifier.padding(top = 4.dp)
            )
            
            // Additional details based on transaction type
            when (transaction) {
                is TransactionItem.SaleItem -> {
                    val sale = transaction.sale
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Portal amount details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Portal Amount:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "₹${String.format("%.2f", sale.totalPortalAmount)}",
                                fontSize = 11.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pending Portal:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "₹${String.format("%.2f", sale.totalPortalAmount - sale.portalAmountPaid)}",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        
                        // Difference amount details
                        if (sale.differenceAmount != 0.0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Difference:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", sale.differenceAmount)}",
                                    fontSize = 11.sp,
                                    color = if (sale.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Pending Difference:",
                                    fontSize = 11.sp,
                                    color = Color(0xFF9CA3AF)
                                )
                                Text(
                                    text = "₹${String.format("%.2f", if (sale.differenceAmount > 0) sale.differenceAmount - sale.differenceAmountPaid else sale.differenceAmount + sale.differenceAmountPaid)}",
                                    fontSize = 11.sp,
                                    color = if (sale.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
                is TransactionItem.PurchaseItem -> {
                    val purchase = transaction.purchase
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Purchase amount details
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Amount:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "₹${String.format("%.2f", purchase.grandTotal)}",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Pending Amount:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "₹${String.format("%.2f", purchase.grandTotal - purchase.amountPaid)}",
                                fontSize = 11.sp,
                                color = Color(0xFFF59E0B)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Items: ${purchase.items.size}",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "GST: ₹${String.format("%.2f", purchase.gstAmount)}",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }
                is TransactionItem.CashTransactionItem -> {
                    val cash = transaction.cashTransaction
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        // Show module information
                        val moduleInfo = when {
                            cash.note.contains("Cash Out from Purchase Module") -> "Cash Out from Purchase Module"
                            cash.note.contains("Cash In from Sale Module") -> "Cash In from Sale Module"
                            cash.note.contains("Cash In from Customer Module") -> "Cash In from Customer Module"
                            cash.note.contains("Cash Out from Customer Module") -> "Cash Out from Customer Module"
                            else -> "Cash Transaction"
                        }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Module:",
                                fontSize = 11.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = moduleInfo,
                                fontSize = 11.sp,
                                color = when (moduleInfo) {
                                    "Cash In from Sale Module", "Cash In from Customer Module" -> Color(0xFF10B981)
                                    "Cash Out from Purchase Module", "Cash Out from Customer Module" -> Color(0xFFEF4444)
                                    else -> Color(0xFF9CA3AF)
                                },
                                modifier = Modifier.weight(1f),
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data classes and enums
data class FinancialSummary(
    val pendingPortalAmount: Double = 0.0,
    val pendingDifferenceAmount: Double = 0.0,
    val pendingPurchaseAmount: Double = 0.0,
    val netBalance: Double = 0.0,
    val cashInAmount: Double = 0.0,
    val cashOutAmount: Double = 0.0,
    val positiveDifferenceAmount: Double = 0.0,
    val negativeDifferenceAmount: Double = 0.0
)

enum class TransactionType(val displayName: String) {
    ALL("All"),
    SALES("Sales"),
    PURCHASES("Purchases"),
    CASH_IN("Cash In"),
    CASH_OUT("Cash Out"),
    CASH_TRANSACTIONS("Cash Transactions"),
    DIFFERENCES("Differences")
}

enum class TransactionSortField(val displayName: String) {
    DATE("Date"),
    AMOUNT("Amount"),
    TYPE("Type"),
    STATUS("Status")
}

sealed class TransactionItem {
    data class SaleItem(val sale: Sale) : TransactionItem()
    data class PurchaseItem(val purchase: Purchase) : TransactionItem()
    data class CashTransactionItem(val cashTransaction: CashTransaction) : TransactionItem()
    
    val type: String
        get() = when (this) {
            is SaleItem -> "Sale"
            is PurchaseItem -> "Purchase"
            is CashTransactionItem -> when (cashTransaction.transactionType) {
                CashTransactionType.RECEIVE -> "Cash In"
                CashTransactionType.GIVE -> "Cash Out"
            }
        }
    
    val typeColor: Color
        get() = when (this) {
            is SaleItem -> Color(0xFF10B981)
            is PurchaseItem -> Color(0xFF3B82F6)
            is CashTransactionItem -> when (cashTransaction.transactionType) {
                CashTransactionType.RECEIVE -> Color(0xFF10B981)
                CashTransactionType.GIVE -> Color(0xFFEF4444)
            }
        }
    
    val description: String
        get() = when (this) {
            is SaleItem -> "Bill: ${sale.billNumber}"
            is PurchaseItem -> "Purchase"
            is CashTransactionItem -> cashTransaction.note
        }
    
    val date: String
        get() = when (this) {
            is SaleItem -> sale.saleDate
            is PurchaseItem -> purchase.purchaseDate
            is CashTransactionItem -> cashTransaction.createdAt?.let { 
                java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(it.toDate())
            } ?: "N/A"
        }
    
    val amount: String
        get() = when (this) {
            is SaleItem -> "Rs ${String.format("%.2f", sale.totalRevenueAmount)}"
            is PurchaseItem -> "Rs ${String.format("%.2f", purchase.grandTotal)}"
            is CashTransactionItem -> "Rs ${String.format("%.2f", cashTransaction.amount)}"
        }
    
    val amountColor: Color
        get() = when (this) {
            is SaleItem -> Color(0xFF10B981)
            is PurchaseItem -> Color(0xFFEF4444)
            is CashTransactionItem -> when (cashTransaction.transactionType) {
                CashTransactionType.RECEIVE -> Color(0xFF10B981)
                CashTransactionType.GIVE -> Color(0xFFEF4444)
            }
        }
    
    val status: String
        get() = when (this) {
            is SaleItem -> when (sale.saleStatus) {
                SaleStatus.PENDING -> "Pending"
                SaleStatus.PARTIALLY_PAID -> "Partially Paid"
                SaleStatus.PAID -> "Paid"
            }
            is PurchaseItem -> when (purchase.paymentStatus) {
                PaymentStatus.PENDING -> "Pending"
                PaymentStatus.PARTIALLY_PAID -> "Partially Paid"
                PaymentStatus.PAID -> "Paid"
            }
            is CashTransactionItem -> "Completed"
        }
    
    val statusColor: Color
        get() = when (this) {
            is SaleItem -> when (sale.saleStatus) {
                SaleStatus.PENDING -> Color(0xFFF59E0B)
                SaleStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                SaleStatus.PAID -> Color(0xFF10B981)
            }
            is PurchaseItem -> when (purchase.paymentStatus) {
                PaymentStatus.PENDING -> Color(0xFFF59E0B)
                PaymentStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                PaymentStatus.PAID -> Color(0xFF10B981)
            }
            is CashTransactionItem -> Color(0xFF10B981)
        }
}

// Print function for customer transactions
private fun printCustomerTransactions(
    customer: Entity,
    transactionType: TransactionType,
    sales: List<Sale>,
    purchases: List<Purchase>,
    cashTransactions: List<CashTransaction>,
    sortBy: TransactionSortField,
    sortDirection: SortDirection,
    filterDateFrom: String,
    filterDateTo: String
) {
    try {
        val now = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        )

        // Get filtered and sorted transactions based on type
        val allTransactions = when (transactionType) {
            TransactionType.ALL -> {
                val salesTransactions = sales.map { TransactionItem.SaleItem(it) }
                val purchaseTransactions = purchases.map { TransactionItem.PurchaseItem(it) }
                val cashTransactionsItems = cashTransactions.map { TransactionItem.CashTransactionItem(it) }
                salesTransactions + purchaseTransactions + cashTransactionsItems
            }
            TransactionType.SALES -> sales.map { TransactionItem.SaleItem(it) }
            TransactionType.PURCHASES -> purchases.map { TransactionItem.PurchaseItem(it) }
            TransactionType.CASH_IN -> cashTransactions.filter { it.transactionType == CashTransactionType.RECEIVE }.map { TransactionItem.CashTransactionItem(it) }
            TransactionType.CASH_OUT -> cashTransactions.filter { it.transactionType == CashTransactionType.GIVE }.map { TransactionItem.CashTransactionItem(it) }
            TransactionType.CASH_TRANSACTIONS -> cashTransactions.map { TransactionItem.CashTransactionItem(it) }
            TransactionType.DIFFERENCES -> sales.filter { it.differenceAmount != 0.0 }.map { TransactionItem.SaleItem(it) }
        }

        // Apply date filtering
        val filteredTransactions = if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) {
            try {
                val fromDate = LocalDate.parse(filterDateFrom, DateTimeFormatter.ISO_LOCAL_DATE)
                val toDate = LocalDate.parse(filterDateTo, DateTimeFormatter.ISO_LOCAL_DATE)
                
                allTransactions.filter { transaction ->
                    val transactionDate = when (transaction) {
                        is TransactionItem.SaleItem -> {
                            try {
                                LocalDate.parse(transaction.sale.saleDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        }
                        is TransactionItem.PurchaseItem -> {
                            try {
                                LocalDate.parse(transaction.purchase.purchaseDate, DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        }
                        is TransactionItem.CashTransactionItem -> {
                            transaction.cashTransaction.createdAt?.let { 
                                java.time.LocalDate.ofEpochDay(it.seconds / 86400)
                            } ?: LocalDate.now()
                        }
                    }
                    transactionDate.isAfter(fromDate.minusDays(1)) && transactionDate.isBefore(toDate.plusDays(1))
                }
            } catch (e: Exception) {
                allTransactions
            }
        } else {
            allTransactions
        }

        // Apply sorting
        val sortedTransactions = when (sortBy) {
            TransactionSortField.DATE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.date }
                } else {
                    filteredTransactions.sortedByDescending { it.date }
                }
            }
            TransactionSortField.AMOUNT -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { 
                        when (it) {
                            is TransactionItem.SaleItem -> it.sale.totalRevenueAmount
                            is TransactionItem.PurchaseItem -> it.purchase.grandTotal
                            is TransactionItem.CashTransactionItem -> it.cashTransaction.amount
                        }
                    }
                } else {
                    filteredTransactions.sortedByDescending { 
                        when (it) {
                            is TransactionItem.SaleItem -> it.sale.totalRevenueAmount
                            is TransactionItem.PurchaseItem -> it.purchase.grandTotal
                            is TransactionItem.CashTransactionItem -> it.cashTransaction.amount
                        }
                    }
                }
            }
            TransactionSortField.TYPE -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.type }
                } else {
                    filteredTransactions.sortedByDescending { it.type }
                }
            }
            TransactionSortField.STATUS -> {
                if (sortDirection == SortDirection.ASCENDING) {
                    filteredTransactions.sortedBy { it.status }
                } else {
                    filteredTransactions.sortedByDescending { it.status }
                }
            }
        }

        // Build HTML content
        val html = buildString {
            append("""
                <html>
                <head>
                  <meta charset='UTF-8'/>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; color: #111; }
                    .header { margin-bottom: 12px; }
                    .customer-info { background: #f8f9fa; padding: 12px; border-radius: 6px; margin-bottom: 16px; }
                    .muted { color: #666; font-size: 12px; }
                    table { width: 100%; border-collapse: collapse; font-size: 12px; }
                    th { background: #f0f2f5; text-align: left; padding: 8px; border-bottom: 1px solid #e5e7eb; }
                    td { padding: 8px; border-bottom: 1px solid #f3f4f6; vertical-align: top; }
                    .right { text-align: right; }
                    .type-sale { color: #10B981; font-weight: bold; }
                    .type-purchase { color: #3B82F6; font-weight: bold; }
                    .type-cash-in { color: #10B981; font-weight: bold; }
                    .type-cash-out { color: #EF4444; font-weight: bold; }
                    .amount-positive { color: #10B981; }
                    .amount-negative { color: #EF4444; }
                    .status-paid { color: #10B981; }
                    .status-pending { color: #F59E0B; }
                    .status-partial { color: #3B82F6; }
                  </style>
                </head>
                <body>
                  <div class='header'>
                    <h2 style='margin:0 0 4px 0;'>Customer Transaction Report</h2>
                    <div class='muted'>Generated on: $now • Total Records: ${sortedTransactions.size}</div>
                  </div>
                  
                  <div class='customer-info'>
                    <h3 style='margin:0 0 8px 0;'>${customer.firmName}</h3>
                    <div style='font-size: 12px; color: #666;'>
                      Contact: ${customer.contactPerson} | Phone: ${customer.contactNo}<br/>
                      Location: ${customer.city}, ${customer.state} | GSTIN: ${customer.gstin}
                    </div>
                  </div>
                  
                  <div class='muted' style='margin-bottom: 8px;'>
                    Filter: ${transactionType.displayName} | 
                    Sort: ${sortBy.displayName} (${if (sortDirection == SortDirection.ASCENDING) "Ascending" else "Descending"}) |
                    ${if (filterDateFrom.isNotBlank() && filterDateTo.isNotBlank()) "Date Range: $filterDateFrom to $filterDateTo" else "All Dates"}
                  </div>
                  
                  <table>
                    <thead>
                      <tr>
                        <th style='width:12%'>Date</th>
                        <th style='width:15%'>Type</th>
                        <th style='width:25%'>Description</th>
                        <th style='width:15%'>Amount</th>
                        <th style='width:15%'>Status</th>
                        <th style='width:18%'>Details</th>
                      </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            sortedTransactions.forEach { transaction ->
                val typeClass = when (transaction.type) {
                    "Sale" -> "type-sale"
                    "Purchase" -> "type-purchase"
                    "Cash In" -> "type-cash-in"
                    "Cash Out" -> "type-cash-out"
                    else -> ""
                }
                
                val amountClass = when {
                    transaction.amount.contains("+") || transaction.type == "Sale" || transaction.type == "Cash In" -> "amount-positive"
                    transaction.amount.contains("-") || transaction.type == "Purchase" || transaction.type == "Cash Out" -> "amount-negative"
                    else -> ""
                }
                
                val statusClass = when (transaction.status) {
                    "Paid" -> "status-paid"
                    "Pending" -> "status-pending"
                    "Partially Paid" -> "status-partial"
                    else -> ""
                }

                append("""
                    <tr>
                      <td>${transaction.date}</td>
                      <td class='$typeClass'>${transaction.type}</td>
                      <td>${transaction.description}</td>
                      <td class='right $amountClass'>${transaction.amount}</td>
                      <td class='$statusClass'>${transaction.status}</td>
                      <td>
                """.trimIndent())

                // Add specific details based on transaction type
                when (transaction) {
                    is TransactionItem.SaleItem -> {
                        val sale = transaction.sale
                        append("""
                            Bill: ${sale.billNumber}<br/>
                            Qty: ${String.format("%.2f", sale.quantityKg)} kg<br/>
                            Rate: Rs${String.format("%.2f", sale.discountedRatePerKg)}/kg<br/>
                            <br/>
                            <strong>Portal Amount:</strong> Rs${String.format("%.2f", sale.totalPortalAmount)}<br/>
                            <strong>Pending Portal:</strong> Rs${String.format("%.2f", sale.totalPortalAmount - sale.portalAmountPaid)}<br/>
                        """.trimIndent())
                        
                        // Add difference details if there's a difference
                        if (sale.differenceAmount != 0.0) {
                            val differenceColor = if (sale.differenceAmount >= 0) "#10B981" else "#EF4444"
                            val pendingDifference = if (sale.differenceAmount > 0) sale.differenceAmount - sale.differenceAmountPaid else sale.differenceAmount + sale.differenceAmountPaid
                            append("""
                                <br/>
                                <strong style="color: $differenceColor">Difference:</strong> <span style="color: $differenceColor">Rs${String.format("%.2f", sale.differenceAmount)}</span><br/>
                                <strong style="color: $differenceColor">Pending Difference:</strong> <span style="color: $differenceColor">Rs${String.format("%.2f", pendingDifference)}</span>
                            """.trimIndent())
                        }
                    }
                    is TransactionItem.PurchaseItem -> {
                        val purchase = transaction.purchase
                        append("""
                            Items: ${purchase.items.size}<br/>
                            GST: Rs${String.format("%.2f", purchase.gstAmount)}<br/>
                            <br/>
                            <strong>Total Amount:</strong> Rs${String.format("%.2f", purchase.grandTotal)}<br/>
                            <strong>Pending Amount:</strong> Rs${String.format("%.2f", purchase.grandTotal - purchase.amountPaid)}
                        """.trimIndent())
                    }
                    is TransactionItem.CashTransactionItem -> {
                        val cash = transaction.cashTransaction
                        val moduleInfo = when {
                            cash.note.contains("Cash Out from Purchase Module") -> "Cash Out from Purchase Module"
                            cash.note.contains("Cash In from Sale Module") -> "Cash In from Sale Module"
                            cash.note.contains("Cash In from Customer Module") -> "Cash In from Customer Module"
                            cash.note.contains("Cash Out from Customer Module") -> "Cash Out from Customer Module"
                            else -> "Cash Transaction"
                        }
                        
                        val moduleColor = when (moduleInfo) {
                            "Cash In from Sale Module", "Cash In from Customer Module" -> "#10B981"
                            "Cash Out from Purchase Module", "Cash Out from Customer Module" -> "#EF4444"
                            else -> "#666"
                        }
                        
                        append("""
                            <strong style="color: $moduleColor">Module:</strong> <span style="color: $moduleColor">$moduleInfo</span>
                        """.trimIndent())
                    }
                }

                append("""
                      </td>
                    </tr>
                """.trimIndent())
            }

            append("""
                    </tbody>
                  </table>
                </body>
                </html>
            """.trimIndent())
        }

        // Generate PDF filename
        val fileName = "customer_transactions_${customer.firmName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
        val file = java.io.File(System.getProperty("user.home"), "Downloads/$fileName")

        // Write to PDF using OpenHTMLToPDF
        java.io.FileOutputStream(file).use { os ->
            val builder = com.openhtmltopdf.pdfboxout.PdfRendererBuilder()
            builder.withHtmlContent(html, null)
            builder.toStream(os)
            builder.run()
        }

        println("Customer transactions report saved to: ${file.absolutePath}")

        // Open the PDF file
        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file)
            }
        } catch (e: Exception) {
            println("Could not open PDF file automatically: ${e.message}")
        }

    } catch (e: Exception) {
        println("Error printing customer transactions: ${e.message}")
        e.printStackTrace()
        }
}

// Helper function to calculate financial summary
fun calculateFinancialSummary(
    customerId: String,
    sales: List<Sale>,
    purchases: List<Purchase>,
    cashTransactions: List<CashTransaction>
): FinancialSummary {
    val customerSales = sales.filter { it.customerId == customerId }
    val customerPurchases = purchases.filter { it.customerId == customerId }
    val customerCashTransactions = cashTransactions.filter { it.customerId == customerId }
    
    // Calculate pending portal amount from sales
    val pendingPortalAmount = customerSales.sumOf { sale ->
        sale.totalPortalAmount - sale.portalAmountPaid
    }
    
    // Calculate positive and negative difference amounts separately
    val positiveDifferenceAmount = customerSales.sumOf { sale ->
        if (sale.differenceAmount > 0) {
            sale.differenceAmount - sale.differenceAmountPaid // Positive: we owe customer, paid amount reduces what we owe
        } else {
            0.0
        }
    }
    
    val negativeDifferenceAmount = customerSales.sumOf { sale ->
        if (sale.differenceAmount < 0) {
            sale.differenceAmount + sale.differenceAmountPaid // Negative: customer owes us, paid amount reduces what they owe
        } else {
            0.0
        }
    }
    
    // Calculate total pending difference amount
    val pendingDifferenceAmount = positiveDifferenceAmount + negativeDifferenceAmount
    
    // Calculate pending purchase amount (using grand total which includes GST)
    val pendingPurchaseAmount = customerPurchases.sumOf { purchase ->
        purchase.grandTotal - purchase.amountPaid
    }
    
    // Calculate cash in and cash out amounts (excluding purchase module cash outs and difference cash)
    val cashInAmount = customerCashTransactions
        .filter { 
            !it.note.contains("Cash Out from Purchase Module") && 
            !it.note.contains("Difference Cash")
        }
        .filter { it.transactionType == CashTransactionType.RECEIVE }
        .sumOf { it.amount }
    
    val cashOutAmount = customerCashTransactions
        .filter { 
            !it.note.contains("Cash Out from Purchase Module") && 
            !it.note.contains("Difference Cash")
        }
        .filter { it.transactionType == CashTransactionType.GIVE }
        .sumOf { it.amount }
    
    // Calculate net cash transaction impact (excluding cash out from purchase module and difference cash)
    val cashTransactionImpact = customerCashTransactions
        .filter { 
            !it.note.contains("Cash Out from Purchase Module") && 
            !it.note.contains("Difference Cash")
        } // Exclude purchase module cash outs and difference cash transactions
        .sumOf { transaction ->
            when (transaction.transactionType) {
                CashTransactionType.RECEIVE -> -transaction.amount  // Customer paid us (reduces what they owe)
                CashTransactionType.GIVE -> transaction.amount        // We paid customer (increases what they owe)
            }
        }
    
    // Calculate net balance including cash transactions (excluding purchase module cash outs and difference cash)
    val netBalance = pendingPortalAmount + pendingDifferenceAmount - pendingPurchaseAmount + cashTransactionImpact
    
    return FinancialSummary(
        pendingPortalAmount = pendingPortalAmount,
        pendingDifferenceAmount = pendingDifferenceAmount,
        pendingPurchaseAmount = pendingPurchaseAmount,
        netBalance = netBalance,
        cashInAmount = cashInAmount,
        cashOutAmount = cashOutAmount,
        positiveDifferenceAmount = positiveDifferenceAmount,
        negativeDifferenceAmount = negativeDifferenceAmount
    )
}
