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

// Update the MenuItem enum
enum class MenuItem {
    DASHBOARD,
    PRODUCTION,
    INVENTORY,
    PURCHASE,
    SALE,
    PENDING_BILLS,
    CUSTOMERS,
    CASH_REPORT,
    LEDGER,
    COMPANY_INFO,
    NOTES,
    REPORTS,
    PROFILE
}

// Update MainScreen composable signature
@Composable
fun MainScreen(
    authViewModel: AuthViewModel,
    entityViewModel: com.humblecoders.plantmanagement.viewmodels.EntityViewModel,
    purchaseViewModel: com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel
) {
    var selectedMenuItem by remember { mutableStateOf(MenuItem.DASHBOARD) }
    val authState = authViewModel.authState
    val user = authState.currentUser

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
    ) {
        SidebarMenu(
            selectedItem = selectedMenuItem,
            onItemSelected = { selectedMenuItem = it },
            currentUserRole = user?.role?.name ?: "USER"
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFF111827))
                .padding(24.dp)
        ) {
            when (selectedMenuItem) {
                MenuItem.DASHBOARD -> DashboardContent()
                MenuItem.INVENTORY -> InventoryScreen(inventoryViewModel)
                MenuItem.CUSTOMERS -> EntityScreen(entityViewModel)
                MenuItem.PURCHASE -> PurchaseScreen(purchaseViewModel, entityViewModel, inventoryViewModel)
            MenuItem.PROFILE -> ProfileContent(authViewModel)
                else -> PlaceholderContent(selectedMenuItem.name)
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
        MenuItemData(MenuItem.CUSTOMERS, "Customers", Icons.Default.Person),
        MenuItemData(MenuItem.CASH_REPORT, "Cash Report", Icons.Default.AccountBalance),
        MenuItemData(MenuItem.LEDGER, "Ledger", Icons.Default.Menu),
        MenuItemData(MenuItem.COMPANY_INFO, "Company Info", Icons.Default.Info),
        MenuItemData(MenuItem.NOTES, "Notes", Icons.Default.Create),
        MenuItemData(MenuItem.REPORTS, "Reports", Icons.Default.ThumbUp),
    )

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
            .background(Color(0xFF1F2937))
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // App Title
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

        // Menu Items
        menuItems.forEach { menuItem ->
            MenuItemRow(
                menuItemData = menuItem,
                isSelected = selectedItem == menuItem.item,
                onClick = { onItemSelected(menuItem.item) }
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Divider(
            color = Color(0xFF374151),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        // Profile at bottom
        MenuItemRow(
            menuItemData = MenuItemData(MenuItem.PROFILE, "Profile", Icons.Default.AccountCircle),
            isSelected = selectedItem == MenuItem.PROFILE,
            onClick = { onItemSelected(MenuItem.PROFILE) }
        )

        // Role badge
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
fun DashboardContent() {
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

        // Metrics Grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "FRK Produced",
                value = "0.00 kg",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "FRK in Stock",
                value = "0.00 kg",
                color = Color(0xFF3B82F6),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Billed Portal Amount",
                value = "₹ 0.00",
                color = Color(0xFF8B5CF6),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Total Sales Revenue",
                value = "₹ 0.00",
                color = Color(0xFF10B981),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            MetricCard(
                title = "Total Pending Portal Amount",
                value = "₹ 0.00",
                color = Color(0xFFF59E0B),
                modifier = Modifier.weight(1f)
            )

            MetricCard(
                title = "Net Outstanding Balance",
                value = "₹ 0.00",
                color = Color(0xFF06B6D4),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Additional Info Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            InfoCard(
                title = "Production Losses",
                items = listOf(
                    "Wastage: 0.00 kg",
                    "Burn: 0.00 kg",
                    "Regrind: 0.00 kg"
                ),
                modifier = Modifier.weight(1f)
            )

            InfoCard(
                title = "Cash Flow Summary",
                items = listOf(
                    "Cash In: ₹ 0.00",
                    "Cash Out: ₹ 0.00",
                    "Net Cash: ₹ 0.00"
                ),
                modifier = Modifier.weight(1f)
            )

            InfoCard(
                title = "Total Customers",
                items = listOf(
                    "0 customers"
                ),
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