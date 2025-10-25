package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.viewmodels.AuthViewModel

data class MenuItemData(
    val item: MenuItem,
    val title: String,
    val icon: ImageVector
)

enum class MenuItem {
    DASHBOARD,
    PRODUCTION,
    INVENTORY,
    PURCHASE,
    SALE,
    PENDING_BILLS,
    CUSTOMERS,
    CASH_REPORT,
    EXPENSES,
    LEDGER,
    REMINDERS,
    NOTES,
    REPORTS,
    USER_BALANCE_MANAGEMENT,
    USER_CASH_OUT,
    HISTORY,
    PROFILE
}

@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    entityViewModel: com.humblecoders.plantmanagement.viewmodels.EntityViewModel,
    purchaseViewModel: com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    cashTransactionViewModel: com.humblecoders.plantmanagement.viewmodels.CashTransactionViewModel,
    cashReportViewModel: com.humblecoders.plantmanagement.viewmodels.CashReportViewModel,
    productionViewModel: com.humblecoders.plantmanagement.viewmodels.ProductionViewModel,
    expenseViewModel: com.humblecoders.plantmanagement.viewmodels.ExpenseViewModel,
    saleViewModel: com.humblecoders.plantmanagement.viewmodels.SaleViewModel,
    pendingBillViewModel: com.humblecoders.plantmanagement.viewmodels.PendingBillViewModel,
    noteViewModel: com.humblecoders.plantmanagement.viewmodels.NoteViewModel,
    userBalanceViewModel: com.humblecoders.plantmanagement.viewmodels.UserBalanceViewModel,
    historyViewModel: com.humblecoders.plantmanagement.viewmodels.HistoryViewModel,
    storageService: com.humblecoders.plantmanagement.services.FirebaseStorageService
) {
    var selectedMenuItem by remember { mutableStateOf(MenuItem.DASHBOARD) }
    var selectedCustomer by remember { mutableStateOf<com.humblecoders.plantmanagement.data.Entity?>(null) }
    val authState = authViewModel.authState
    val user = authState.currentUser

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        SidebarMenu(
            selectedItem = selectedMenuItem,
            onItemSelected = { 
                selectedMenuItem = it
                selectedCustomer = null // Clear customer detail screen when navigating to other menu items
            },
            currentUserRole = user?.role?.name ?: "USER"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF111827))
                .padding(24.dp)
        ) {
            when {
                selectedCustomer != null -> {
                    CustomerDetailScreen(
                        customer = selectedCustomer!!,
                        saleViewModel = saleViewModel,
                        purchaseViewModel = purchaseViewModel,
                        cashTransactionViewModel = cashTransactionViewModel,
                        onBack = { selectedCustomer = null }
                    )
                }
                else -> {
                    when (selectedMenuItem) {
                        MenuItem.DASHBOARD -> DashboardContent(
                            productionViewModel = productionViewModel,
                            inventoryViewModel = inventoryViewModel,
                            cashReportViewModel = cashReportViewModel
                        )
                        MenuItem.PRODUCTION -> ProductionScreen(productionViewModel, inventoryViewModel, user?.role)
                        MenuItem.INVENTORY -> InventoryScreen(inventoryViewModel, user?.role)
                        MenuItem.CUSTOMERS -> EntityScreen(
                            entityViewModel, 
                            user?.role, 
                            cashTransactionViewModel,
                            saleViewModel,
                            purchaseViewModel,
                            onNavigateToCustomerDetail = { customer -> selectedCustomer = customer }
                        )
                        MenuItem.PURCHASE -> PurchaseScreen(purchaseViewModel, entityViewModel, inventoryViewModel, user?.role)
                        MenuItem.SALE -> SaleScreen(saleViewModel, entityViewModel, inventoryViewModel, storageService, user?.role)
                        MenuItem.PENDING_BILLS -> PendingBillsScreen(pendingBillViewModel, entityViewModel, inventoryViewModel, saleViewModel, storageService, user?.role)
                        MenuItem.CASH_REPORT -> CashReportsScreen(cashReportViewModel, userBalanceViewModel, user?.role) { }
                        MenuItem.EXPENSES -> ExpensesScreen(expenseViewModel, user?.role) { }
                        MenuItem.LEDGER -> LedgerScreen(entityViewModel, saleViewModel, purchaseViewModel, cashTransactionViewModel)
                        MenuItem.HISTORY -> HistoryScreen(historyViewModel)
                        MenuItem.REMINDERS -> RemindersScreen(saleViewModel, entityViewModel, inventoryViewModel, storageService, user?.role)
                        MenuItem.NOTES -> NotesScreen(noteViewModel, user?.role)
                        MenuItem.USER_BALANCE_MANAGEMENT -> UserBalanceManagementScreen(userBalanceViewModel) { }
                        MenuItem.USER_CASH_OUT -> UserCashOutScreen(userBalanceViewModel) { }
                        MenuItem.PROFILE -> ProfileContent(authViewModel)
                        else -> PlaceholderContent(selectedMenuItem.name)
                    }
                }
            }
        }
    }
}

@Composable
fun SidebarMenu(
    selectedItem: MenuItem,
    onItemSelected: (MenuItem) -> Unit,
    currentUserRole: String
) {
    val menuItems = listOf(
        MenuItemData(MenuItem.DASHBOARD, "Dashboard", Icons.Default.Home),
        MenuItemData(MenuItem.PRODUCTION, "Production", Icons.Default.Build),
        MenuItemData(MenuItem.INVENTORY, "Inventory", Icons.Default.List),
        MenuItemData(MenuItem.PURCHASE, "Purchase", Icons.Default.ShoppingCart),
        MenuItemData(MenuItem.SALE, "Sale", Icons.Default.Star),
        MenuItemData(MenuItem.PENDING_BILLS, "Pending Bills", Icons.Default.DateRange),
        MenuItemData(MenuItem.EXPENSES, "Expenses", Icons.Default.Receipt),
        MenuItemData(MenuItem.CUSTOMERS, "Customers", Icons.Default.Person),
        MenuItemData(MenuItem.CASH_REPORT, "Cash Report", Icons.Default.AccountBalance),
        MenuItemData(MenuItem.LEDGER, "Ledger", Icons.Default.Menu),
        MenuItemData(MenuItem.HISTORY, "History", Icons.Default.History),
        MenuItemData(MenuItem.REMINDERS, "Reminders", Icons.Default.Notifications),
        MenuItemData(MenuItem.NOTES, "Notes", Icons.Default.Create),
        MenuItemData(MenuItem.REPORTS, "Reports", Icons.Default.ThumbUp),
        MenuItemData(MenuItem.USER_BALANCE_MANAGEMENT, "User Balance Management", Icons.Default.AccountBalanceWallet),
        MenuItemData(MenuItem.USER_CASH_OUT, "My Cash Out", Icons.Default.MoneyOff),
    )

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF1F2937))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "PLANT MANAGEMENT",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF06B6D4)
            )
        }

        Divider(
            color = Color(0xFF374151),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        menuItems.forEach { menuItem ->
            // Role-based filtering
            val shouldShow = when (menuItem.item) {
                MenuItem.USER_BALANCE_MANAGEMENT -> false // Removed from admin menu
                MenuItem.USER_CASH_OUT -> false // Removed from user menu
                else -> true
            }
            
            if (shouldShow) {
                MenuItemRow(
                    menuItemData = menuItem,
                    isSelected = selectedItem == menuItem.item,
                    onClick = { onItemSelected(menuItem.item) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(
            color = Color(0xFF374151),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        MenuItemRow(
            menuItemData = MenuItemData(MenuItem.PROFILE, "Profile", Icons.Default.AccountCircle),
            isSelected = selectedItem == MenuItem.PROFILE,
            onClick = { onItemSelected(MenuItem.PROFILE) }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(
                    color = if (currentUserRole == "ADMIN") Color(0xFF10B981) else Color(0xFF3B82F6),
                    shape = RoundedCornerShape(4.dp)
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = currentUserRole,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
fun MenuItemRow(
    menuItemData: MenuItemData,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSelected) Color(0xFF06B6D4) else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = menuItemData.icon,
            contentDescription = menuItemData.title,
            tint = if (isSelected) Color(0xFF111827) else Color(0xFF9CA3AF),
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = menuItemData.title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) Color(0xFF111827) else Color(0xFFF9FAFB)
        )
    }

    Spacer(modifier = Modifier.height(4.dp))
}

@Composable
fun DashboardContent(
    productionViewModel: com.humblecoders.plantmanagement.viewmodels.ProductionViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    cashReportViewModel: com.humblecoders.plantmanagement.viewmodels.CashReportViewModel
) {
    val productionRecords = productionViewModel.productionState.productionRecords
    val inventoryItems = inventoryViewModel.getFilteredItems()
    val cashReports = cashReportViewModel.cashReportState.cashReports
    
    // Calculate total FRK produced
    val totalFrkProduced = productionRecords.sumOf { it.quantityProduced }
    
    // Find Fortified Rice in stock
    val fortifiedRiceItem = inventoryItems.find { 
        it.name.equals("Fortified Rice", ignoreCase = true) || 
        it.name.equals("FRK", ignoreCase = true) ||
        it.name.equals("Fortified Rice Kernels", ignoreCase = true)
    }
    val frkInStock = fortifiedRiceItem?.quantity ?: 0.0
    
    // Calculate total production losses
    val totalWastage = productionRecords.sumOf { it.wasteTracking?.wastage ?: 0.0 }
    val totalBurn = productionRecords.sumOf { it.wasteTracking?.burn ?: 0.0 }
    val totalRegrind = productionRecords.sumOf { it.wasteTracking?.regrind ?: 0.0 }
    val totalOthers = productionRecords.sumOf { it.wasteTracking?.others ?: 0.0 }
    val totalProductionLosses = totalWastage + totalBurn + totalRegrind + totalOthers
    
    // Calculate cash flow summary
    val totalCashIn = cashReports.filter { it.transactionType == com.humblecoders.plantmanagement.data.CashReportType.CASH_IN }.sumOf { it.amount }
    val totalCashOut = cashReports.filter { it.transactionType == com.humblecoders.plantmanagement.data.CashReportType.CASH_OUT }.sumOf { it.amount }
    val netCash = totalCashIn - totalCashOut
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Dashboard",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF9FAFB)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Total FRK Produced",
                value = "${String.format("%.2f", totalFrkProduced)} kg",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "FRK in Stock",
                value = "${String.format("%.2f", frkInStock)} kg",
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Production Losses",
                value = "${String.format("%.2f", totalProductionLosses)} kg",
                color = Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Net Cash Flow",
                value = "₹ ${String.format("%.2f", netCash)}",
                color = if (netCash >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                title = "Production Losses Breakdown",
                items = listOf(
                    "Wastage: ${String.format("%.2f", totalWastage)} kg",
                    "Burn: ${String.format("%.2f", totalBurn)} kg",
                    "Regrind: ${String.format("%.2f", totalRegrind)} kg",
                    "Others: ${String.format("%.2f", totalOthers)} kg"
                ),
                modifier = Modifier.weight(1f)
            )

            CashFlowInfoCard(
                title = "Cash Flow Summary",
                cashIn = totalCashIn,
                cashOut = totalCashOut,
                netCash = netCash,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color(0xFF9CA3AF)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF9FAFB)
            )

            Spacer(modifier = Modifier.height(12.dp))

            items.forEach { item ->
                Text(
                    text = item,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFEF4444),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CashFlowInfoCard(
    title: String,
    cashIn: Double,
    cashOut: Double,
    netCash: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF9FAFB)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Cash In - Green
            Text(
                text = "Cash In: ₹ ${String.format("%.2f", cashIn)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981), // Green color
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Cash Out - Red
            Text(
                text = "Cash Out: ₹ ${String.format("%.2f", cashOut)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFEF4444), // Red color
                modifier = Modifier.padding(vertical = 4.dp)
            )

            // Net Cash - Green if positive, Red if negative
            Text(
                text = "Net Cash: ₹ ${String.format("%.2f", netCash)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (netCash >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
fun ProfileContent(authViewModel: AuthViewModel) {
    val authState = authViewModel.authState
    val user = authState.currentUser

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Profile",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF9FAFB)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "User Information",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF06B6D4)
                )

                Divider(color = Color(0xFF374151))

                ProfileField(label = "Email", value = user?.email ?: "N/A")
                ProfileField(label = "Role", value = user?.role?.name ?: "N/A")
                ProfileField(label = "User ID", value = user?.uid ?: "N/A")

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = { authViewModel.signOut() },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFFEF4444),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Sign Out",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sign Out", fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
fun ProfileField(label: String, value: String) {
    Column {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color(0xFF9CA3AF)
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFFF9FAFB),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun PlaceholderContent(title: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = Color(0xFF374151)
            )
            Text(
                text = "$title Module",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF9CA3AF)
            )
            Text(
                text = "Coming Soon...",
                fontSize = 16.sp,
                color = Color(0xFF6B7280)
            )
        }
    }
}