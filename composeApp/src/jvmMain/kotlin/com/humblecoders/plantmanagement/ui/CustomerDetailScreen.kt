package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
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
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.*
import kotlinx.coroutines.launch

@Composable
fun CustomerDetailScreen(
    customer: Entity,
    saleViewModel: SaleViewModel,
    purchaseViewModel: PurchaseViewModel,
    cashTransactionViewModel: CashTransactionViewModel,
    onBack: () -> Unit
) {
    val saleState = saleViewModel.saleState
    val purchaseState = purchaseViewModel.purchaseState
    val cashTransactionState = cashTransactionViewModel.cashTransactionState
    
    var selectedTransactionType by remember { mutableStateOf(TransactionType.ALL) }
    var showFilters by remember { mutableStateOf(false) }
    
    // Calculate financial summaries
    val financialSummary = remember(customer.id, saleState.sales, purchaseState.purchases, cashTransactionState.transactions) {
        calculateFinancialSummary(customer.id, saleState.sales, purchaseState.purchases, cashTransactionState.transactions)
    }
    
    // Load data for this customer
    LaunchedEffect(customer.id) {
        saleViewModel.getSalesByCustomerId(customer.id)
        purchaseViewModel.getPurchasesByCustomerId(customer.id)
        cashTransactionViewModel.getCashTransactionsByCustomerId(customer.id)
    }
    
    // Refresh data when transaction type changes
    LaunchedEffect(selectedTransactionType) {
        // Reload data to ensure we have the latest transactions
        saleViewModel.getSalesByCustomerId(customer.id)
        purchaseViewModel.getPurchasesByCustomerId(customer.id)
        cashTransactionViewModel.getCashTransactionsByCustomerId(customer.id)
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
                    text = "Customer Details",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { 
                        // Refresh all data
                        saleViewModel.getSalesByCustomerId(customer.id)
                        purchaseViewModel.getPurchasesByCustomerId(customer.id)
                        cashTransactionViewModel.getCashTransactionsByCustomerId(customer.id)
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
                    onClick = { /* TODO: Implement print functionality */ },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Print, contentDescription = "Print")
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Print")
                }
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
                onFiltersApplied = { /* TODO: Apply filters */ }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // Transaction Records
        TransactionRecordsSection(
            customerId = customer.id,
            transactionType = selectedTransactionType,
            sales = saleState.sales.filter { it.customerId == customer.id },
            purchases = purchaseState.purchases.filter { it.customerId == customer.id },
            cashTransactions = cashTransactionState.transactions.filter { it.customerId == customer.id },
            isLoading = saleState.isLoading || purchaseState.isLoading || cashTransactionState.isLoading
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FinancialCard(
            title = "Pending Portal Amount",
            amount = financialSummary.pendingPortalAmount,
            color = Color(0xFFF59E0B),
            modifier = Modifier.weight(1f)
        )
        
        FinancialCard(
            title = "Pending Difference Amount",
            amount = financialSummary.pendingDifferenceAmount,
            color = Color(0xFF8B5CF6),
            modifier = Modifier.weight(1f)
        )
        
        FinancialCard(
            title = "Pending Purchase Amount",
            amount = financialSummary.pendingPurchaseAmount,
            color = Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
        
        FinancialCard(
            title = "Net Balance",
            amount = financialSummary.netBalance,
            color = if (financialSummary.netBalance >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
            modifier = Modifier.weight(1f)
        )
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
    onFiltersApplied: () -> Unit
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
                text = "Filters",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Date range filter
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("From Date", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.weight(1f)
                )
                
                OutlinedTextField(
                    value = "",
                    onValueChange = { },
                    label = { Text("To Date", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.weight(1f)
                )
                
                Button(
                    onClick = onFiltersApplied,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF10B981),
                        contentColor = Color.White
                    )
                ) {
                    Text("Apply")
                }
            }
        }
    }
}

@Composable
fun TransactionRecordsSection(
    customerId: String,
    transactionType: TransactionType,
    sales: List<Sale>,
    purchases: List<Purchase>,
    cashTransactions: List<CashTransaction>,
    isLoading: Boolean = false
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
                    }
                }
            } else {
                when (transactionType) {
                    TransactionType.ALL -> {
                        // Show all transactions combined
                        CombinedTransactionList(
                            sales = sales,
                            purchases = purchases,
                            cashTransactions = cashTransactions
                        )
                    }
                    TransactionType.SALES -> {
                        SalesTransactionList(sales = sales)
                    }
                    TransactionType.PURCHASES -> {
                        PurchasesTransactionList(purchases = purchases)
                    }
                    TransactionType.CASH_IN -> {
                        CashInTransactionList(cashTransactions = cashTransactions.filter { it.transactionType == CashTransactionType.RECEIVE })
                    }
                    TransactionType.CASH_OUT -> {
                        CashOutTransactionList(cashTransactions = cashTransactions.filter { it.transactionType == CashTransactionType.GIVE })
                    }
                    TransactionType.CASH_TRANSACTIONS -> {
                        AllCashTransactionList(cashTransactions = cashTransactions)
                    }
                    TransactionType.DIFFERENCES -> {
                        DifferenceTransactionList(sales = sales)
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
    cashTransactions: List<CashTransaction>
) {
    val allTransactions = remember(sales, purchases, cashTransactions) {
        val salesTransactions = sales.map { TransactionItem.SaleItem(it) }
        val purchaseTransactions = purchases.map { TransactionItem.PurchaseItem(it) }
        val cashTransactionsItems = cashTransactions.map { TransactionItem.CashTransactionItem(it) }
        
        (salesTransactions + purchaseTransactions + cashTransactionsItems)
            .sortedByDescending { it.date }
    }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allTransactions) { transaction ->
            TransactionItemCard(transaction = transaction)
        }
    }
}

@Composable
fun SalesTransactionList(sales: List<Sale>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(sales.sortedByDescending { it.saleDate }) { sale ->
            TransactionItemCard(transaction = TransactionItem.SaleItem(sale))
        }
    }
}

@Composable
fun PurchasesTransactionList(purchases: List<Purchase>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(purchases.sortedByDescending { it.purchaseDate }) { purchase ->
            TransactionItemCard(transaction = TransactionItem.PurchaseItem(purchase))
        }
    }
}

@Composable
fun CashInTransactionList(cashTransactions: List<CashTransaction>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cashTransactions.sortedByDescending { it.createdAt }) { transaction ->
            TransactionItemCard(transaction = TransactionItem.CashTransactionItem(transaction))
        }
    }
}

@Composable
fun CashOutTransactionList(cashTransactions: List<CashTransaction>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cashTransactions.sortedByDescending { it.createdAt }) { transaction ->
            TransactionItemCard(transaction = TransactionItem.CashTransactionItem(transaction))
        }
    }
}

@Composable
fun AllCashTransactionList(cashTransactions: List<CashTransaction>) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(cashTransactions.sortedByDescending { it.createdAt }) { transaction ->
            TransactionItemCard(transaction = TransactionItem.CashTransactionItem(transaction))
        }
    }
}

@Composable
fun DifferenceTransactionList(sales: List<Sale>) {
    val salesWithDifferences = sales.filter { it.differenceAmount != 0.0 }
    
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(salesWithDifferences.sortedByDescending { it.saleDate }) { sale ->
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
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
                Text(
                    text = transaction.date,
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
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
    }
}

// Data classes and enums
data class FinancialSummary(
    val pendingPortalAmount: Double = 0.0,
    val pendingDifferenceAmount: Double = 0.0,
    val pendingPurchaseAmount: Double = 0.0,
    val netBalance: Double = 0.0
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
            is PurchaseItem -> "Purchase: ${purchase.id}"
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
            is SaleItem -> "₹ ${String.format("%.2f", sale.totalRevenueAmount)}"
            is PurchaseItem -> "₹ ${String.format("%.2f", purchase.totalAmount)}"
            is CashTransactionItem -> "₹ ${String.format("%.2f", cashTransaction.amount)}"
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
    
    // Calculate pending difference amount from sales
    val pendingDifferenceAmount = customerSales.sumOf { sale ->
        sale.differenceAmount - sale.differenceAmountPaid
    }
    
    // Calculate pending purchase amount
    val pendingPurchaseAmount = customerPurchases.sumOf { purchase ->
        purchase.totalAmount - purchase.amountPaid
    }
    
    // Calculate net cash transaction impact
    val cashTransactionImpact = customerCashTransactions.sumOf { transaction ->
        when (transaction.transactionType) {
            CashTransactionType.RECEIVE -> -transaction.amount  // Customer paid us (reduces what they owe)
            CashTransactionType.GIVE -> transaction.amount        // We paid customer (increases what they owe)
        }
    }
    
    // Calculate net balance including cash transactions
    val netBalance = pendingPortalAmount + pendingDifferenceAmount - pendingPurchaseAmount + cashTransactionImpact
    
    return FinancialSummary(
        pendingPortalAmount = pendingPortalAmount,
        pendingDifferenceAmount = pendingDifferenceAmount,
        pendingPurchaseAmount = pendingPurchaseAmount,
        netBalance = netBalance
    )
}
