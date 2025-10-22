package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.ui.components.ViewSaleDialog
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import com.humblecoders.plantmanagement.viewmodels.SaleSortField
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun RemindersScreen(
    saleViewModel: SaleViewModel,
    entityViewModel: EntityViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    storageService: com.humblecoders.plantmanagement.services.FirebaseStorageService,
    userRole: UserRole? = null
) {
    val saleState = saleViewModel.saleState
    val entityState = entityViewModel.entityState
    var showViewDialog by remember { mutableStateOf(false) }
    var saleToView by remember { mutableStateOf<Sale?>(null) }
    
    // Days crossed filter
    var daysCrossed by remember { mutableStateOf(7) }
    var daysCrossedInput by remember { mutableStateOf("7") }
    val focusRequester = remember { FocusRequester() }
    
    // Sort dropdown state
    var showSortDropdown by remember { mutableStateOf(false) }

    val isAdmin = userRole == UserRole.ADMIN

    LaunchedEffect(saleState.successMessage, saleState.error) {
        if (saleState.successMessage != null || saleState.error != null) {
            kotlinx.coroutines.delay(3000)
            saleViewModel.clearMessages()
        }
    }

    // Filter sales for reminders - only pending portal amounts and within days crossed
    val filteredSales = remember(saleState.sales, daysCrossed, saleState.searchQuery, saleState.sortBy, saleState.sortDirection) {
        val currentDate = LocalDate.now()
        var filtered = saleState.sales.filter { sale ->
            // Only show sales with pending portal amount
            val pendingPortalAmount = sale.totalPortalAmount - sale.portalAmountPaid
            val hasPendingAmount = pendingPortalAmount > 0.01 // Small tolerance for floating point
            
            if (!hasPendingAmount) return@filter false
            
            // Check if sale date is within the specified days crossed
            try {
                val saleDate = LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val daysBetween = ChronoUnit.DAYS.between(saleDate, currentDate)
                daysBetween >= daysCrossed
            } catch (e: Exception) {
                false // Skip invalid dates
            }
        }
        
        // Apply search filter
        if (saleState.searchQuery.isNotEmpty()) {
            filtered = filtered.filter { sale ->
                sale.firmName.contains(saleState.searchQuery, ignoreCase = true) ||
                sale.billNumber.contains(saleState.searchQuery, ignoreCase = true)
            }
        }
        
        // Apply sorting
        filtered = when (saleState.sortBy) {
            SaleSortField.DATE -> {
                filtered.sortedBy { sale ->
                    try {
                        LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                    } catch (e: Exception) {
                        LocalDate.MIN
                    }
                }
            }
            SaleSortField.ENTITY -> filtered.sortedBy { it.firmName }
            SaleSortField.BILL_NUMBER -> filtered.sortedBy { it.billNumber }
            SaleSortField.STATUS -> filtered.sortedBy { (it.totalPortalAmount - it.portalAmountPaid) }
            else -> filtered
        }
        
        if (saleState.sortDirection == SortDirection.DESCENDING) {
            filtered = filtered.reversed()
        }
        
        filtered
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Payment Reminders",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                }
            }

            // Success Message
            if (saleState.successMessage != null) {
                item {
                    Card(
                        backgroundColor = Color(0xFF10B981),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = saleState.successMessage,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Error Message
            if (saleState.error != null) {
                item {
                    Card(
                        backgroundColor = Color(0xFFEF4444),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = saleState.error,
                            color = Color.White,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }

            // Days Crossed Filter Section
            item {
                Card(
                    backgroundColor = Color(0xFF1F2937),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Filter by Days Crossed",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF9FAFB),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedTextField(
                                value = daysCrossedInput,
                                onValueChange = { 
                                    daysCrossedInput = it
                                    val newDays = it.toIntOrNull() ?: 7
                                    if (newDays in 1..365) {
                                        daysCrossed = newDays
                                    }
                                },
                                label = { Text("Days Crossed", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier
                                    .width(150.dp)
                                    .focusRequester(focusRequester),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF06B6D4),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF06B6D4),
                                    focusedLabelColor = Color(0xFF06B6D4),
                                    unfocusedLabelColor = Color(0xFF9CA3AF)
                                ),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true
                            )
                            
                            Text(
                                text = "days",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                            
                            Spacer(modifier = Modifier.weight(1f))
                        
                            Text(
                                text = "Showing ${filteredSales.size} sales with pending payments",
                                color = Color(0xFF9CA3AF),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            // Search, Filter and Sort Section (Compact)
            item {
                Card(
                    backgroundColor = Color(0xFF1F2937),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search Field
                        OutlinedTextField(
                            value = saleState.searchQuery,
                            onValueChange = { saleViewModel.updateSearchQuery(it) },
                            label = { Text("Search", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier
                                .weight(1f)
                                .widthIn(min = 180.dp, max = 380.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF06B6D4),
                                focusedLabelColor = Color(0xFF06B6D4),
                                unfocusedLabelColor = Color(0xFF9CA3AF)
                            ),
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )


                        // Sort Field
                        Box {
                            OutlinedTextField(
                                value = when (saleState.sortBy) {
                                    SaleSortField.DATE -> "Date"
                                    SaleSortField.ENTITY -> "Firm"
                                    SaleSortField.BILL_NUMBER -> "Bill"
                                    SaleSortField.STATUS -> "Status"
                                    else -> "Date"
                                },
                                onValueChange = { },
                                readOnly = true,
                                label = { Text("Sort", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier
                                    .width(110.dp)
                                    .clickable { showSortDropdown = true },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF06B6D4),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF06B6D4),
                                    focusedLabelColor = Color(0xFF06B6D4),
                                    unfocusedLabelColor = Color(0xFF9CA3AF)
                                ),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { showSortDropdown = true }) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDropDown,
                                            contentDescription = "Sort Options",
                                            tint = Color(0xFF9CA3AF),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            )
                            
                            DropdownMenu(
                                expanded = showSortDropdown,
                                onDismissRequest = { showSortDropdown = false }
                            ) {
                                DropdownMenuItem(
                                    onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.DATE)
                                        showSortDropdown = false
                                    }
                                ) {
                                    Text("Date")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.ENTITY)
                                        showSortDropdown = false
                                    }
                                ) {
                                    Text("Firm")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.BILL_NUMBER)
                                        showSortDropdown = false
                                    }
                                ) {
                                    Text("Bill")
                                }
                                DropdownMenuItem(
                                    onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.STATUS)
                                        showSortDropdown = false
                                    }
                                ) {
                                    Text("Status")
                                }
                            }
                        }

                        // Sort Direction Button
                        IconButton(
                            onClick = { saleViewModel.toggleSortDirection() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (saleState.sortDirection == SortDirection.ASCENDING) 
                                    Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                contentDescription = "Sort Direction",
                                tint = Color(0xFF06B6D4),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Clear Button
                        IconButton(
                            onClick = { 
                                saleViewModel.updateSearchQuery("")
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear",
                                tint = Color(0xFF9CA3AF),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Sales List or Empty State
            if (filteredSales.isEmpty()) {
                item {
                    Card(
                        backgroundColor = Color(0xFF1F2937),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = "No Reminders",
                                modifier = Modifier.size(64.dp),
                                tint = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No payment reminders found",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "All sales are up to date with payments",
                                fontSize = 14.sp,
                                color = Color(0xFF6B7280)
                            )
                        }
                    }
                }
            } else {
                items(filteredSales.size) { index ->
                    val sale = filteredSales[index]
                    ReminderSaleCard(
                        sale = sale,
                        onViewClick = { 
                            saleToView = sale
                            showViewDialog = true
                        }
                    )
                }
            }
        }
    }

    // View Sale Dialog
    if (showViewDialog && saleToView != null) {
        ViewSaleDialog(
            sale = saleToView!!,
            onDismiss = { 
                showViewDialog = false
                saleToView = null
            }
        )
    }
}

@Composable
fun ReminderSaleCard(
    sale: Sale,
    onViewClick: () -> Unit
) {
    val pendingPortalAmount = sale.totalPortalAmount - sale.portalAmountPaid
    val daysCrossed = try {
        val currentDate = LocalDate.now()
        val saleDate = LocalDate.parse(sale.saleDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        ChronoUnit.DAYS.between(saleDate, currentDate).toInt()
    } catch (e: Exception) {
        0
    }

    Card(
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp,
        modifier = Modifier.fillMaxWidth()
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
                        text = sale.firmName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "Bill: ${sale.billNumber} | Date: ${sale.saleDate}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Days Crossed Badge
                    Card(
                        backgroundColor = if (daysCrossed > 14) Color(0xFFEF4444) else Color(0xFFF59E0B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "${daysCrossed}d",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    
                    Button(
                        onClick = onViewClick,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            contentColor = Color.White
                        )
                    ) {
                        Text("View")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Pending Amount",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "₹ ${String.format("%.2f", pendingPortalAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                }
                
                Column {
                    Text(
                        text = "Total Amount",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "₹ ${String.format("%.2f", sale.totalPortalAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                }
                
                Column {
                    Text(
                        text = "Quantity",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "${String.format("%.2f", sale.quantityKg)} kg",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                }
            }
        }
    }
}
