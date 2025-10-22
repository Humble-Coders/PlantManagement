package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.viewmodels.PendingBillViewModel
import com.humblecoders.plantmanagement.viewmodels.PendingBillSortField
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PendingBillHistoryScreen(
    pendingBillViewModel: PendingBillViewModel,
    userRole: UserRole? = null,
    onBack: () -> Unit = {}
) {
    val pendingBillState = pendingBillViewModel.pendingBillState
    
    // Filter bills with BILLED status
    val billedBills = pendingBillState.pendingBills.filter { it.status == PendingBillStatus.BILLED }
    
    // Get filtered and sorted bills using ViewModel
    val filteredBills = pendingBillViewModel.getFilteredAndSortedPendingBills()
        .filter { it.status == PendingBillStatus.BILLED }
    
    // State for date pickers
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF111827))
            .padding(24.dp)
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
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(
                        Color(0xFF374151),
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF9CA3AF)
                    )
                }
                Text(
                    text = "Pending Bill History",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredBills.size} billed bills",
                    fontSize = 16.sp,
                    color = Color(0xFF9CA3AF)
                )
                
                IconButton(
                    onClick = { pendingBillViewModel.toggleAdvancedFilters() },
                    modifier = Modifier.background(
                        Color(0xFF374151),
                        RoundedCornerShape(8.dp)
                    )
                ) {
                    Icon(
                        Icons.Default.FilterList,
                        contentDescription = "Filters",
                        tint = Color(0xFF9CA3AF)
                    )
                }
            }
        }
        
        // Error message
        if (pendingBillState.error != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF7F1D1D),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Error,
                        contentDescription = "Error",
                        tint = Color(0xFFFCA5A5),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = pendingBillState.error,
                        color = Color(0xFFFCA5A5),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { pendingBillViewModel.clearError() }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        // Success message
        if (pendingBillState.successMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF064E3B),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = "Success",
                        tint = Color(0xFF6EE7B7),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = pendingBillState.successMessage,
                        color = Color(0xFF6EE7B7),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(
                        onClick = { pendingBillViewModel.clearSuccessMessage() }
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Loading state
        if (pendingBillState.isLoading) {
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
                        strokeWidth = 3.dp
                    )
                    Text(
                        text = "Loading bill history...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            // Use LazyColumn for everything to avoid nesting scrollable components
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            // Filters section
                if (pendingBillState.showAdvancedFilters) {
                    item {
                        AdvancedFiltersSection(
                            pendingBillViewModel = pendingBillViewModel,
                            onShowFromDatePicker = { showFromDatePicker = true },
                            onShowToDatePicker = { showToDatePicker = true }
                        )
                    }
                }
                
                if (filteredBills.isEmpty()) {
                    // Empty state
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            backgroundColor = Color(0xFF1F2937),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(40.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.History,
                                    contentDescription = "No History",
                                    tint = Color(0xFF6B7280),
                                    modifier = Modifier.size(64.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "No Billed Bills Found",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (pendingBillState.searchQuery.isNotBlank() || 
                                        pendingBillState.filterDateFrom.isNotBlank() || 
                                        pendingBillState.filterDateTo.isNotBlank() ||
                                        pendingBillState.filterStatus != null) {
                                        "Try adjusting your filters"
                                    } else {
                                        "Bills will appear here once they are cleared"
                                    },
                                    fontSize = 14.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }
                    }
                } else {
                    // Bills list
                    items(filteredBills) { bill ->
                        BilledBillCard(bill = bill)
                    }
                }
            }
        }
    }
    
    // From Date picker dialog
    if (showFromDatePicker) {
        Dialog(onDismissRequest = { showFromDatePicker = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select From Date",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    
                    DatePicker(
                        selectedDate = if (pendingBillState.filterDateFrom.isNotBlank()) {
                            try {
                                LocalDate.parse(pendingBillState.filterDateFrom)
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        } else {
                            LocalDate.now()
                        },
                        onDateSelected = { date ->
                            pendingBillViewModel.updateDateFilter(
                                date.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                pendingBillState.filterDateTo
                            )
                        },
                        colors = com.humblecoders.plantmanagement.ui.components.DatePickerDefaults.colors()
                    )
                    
                    Button(
                        onClick = { showFromDatePicker = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }
    
    // To Date picker dialog
    if (showToDatePicker) {
        Dialog(onDismissRequest = { showToDatePicker = false }) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Select To Date",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    
                    DatePicker(
                        selectedDate = if (pendingBillState.filterDateTo.isNotBlank()) {
                            try {
                                LocalDate.parse(pendingBillState.filterDateTo)
                            } catch (e: Exception) {
                                LocalDate.now()
                            }
                        } else {
                            LocalDate.now()
                        },
                        onDateSelected = { date ->
                            pendingBillViewModel.updateDateFilter(
                                pendingBillState.filterDateFrom,
                                date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                            )
                        },
                        colors = com.humblecoders.plantmanagement.ui.components.DatePickerDefaults.colors()
                    )
                    
                    Button(
                        onClick = { showToDatePicker = false },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun AdvancedFiltersSection(
    pendingBillViewModel: PendingBillViewModel,
    onShowFromDatePicker: () -> Unit,
    onShowToDatePicker: () -> Unit
) {
    val pendingBillState = pendingBillViewModel.pendingBillState
    
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    backgroundColor = Color(0xFF1F2937),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                    text = "Advanced Filters",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )
                
                Button(
                    onClick = { pendingBillViewModel.clearAllFilters() },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280)),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Clear All", color = Color.White, fontSize = 12.sp)
                }
            }
                        
                        // Search
                        OutlinedTextField(
                value = pendingBillState.searchQuery,
                onValueChange = { pendingBillViewModel.updateSearchQuery(it) },
                            label = { Text("Search bills...", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF10B981)
                            ),
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color(0xFF9CA3AF)
                                )
                            }
                        )
                        
            // Date range - From Date
            Text(
                text = "From Date",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
                            OutlinedTextField(
                value = pendingBillState.filterDateFrom,
                onValueChange = { },
                label = { Text("From Date", color = Color(0xFF9CA3AF)) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowFromDatePicker() },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                    cursorColor = Color(0xFF10B981),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color(0xFF9CA3AF)
                ),
                leadingIcon = {
                    IconButton(onClick = { onShowFromDatePicker() }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select From Date",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
            )
            
            // Date range - To Date
            Text(
                text = "To Date",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFF9FAFB),
                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            OutlinedTextField(
                value = pendingBillState.filterDateTo,
                onValueChange = { },
                label = { Text("To Date", color = Color(0xFF9CA3AF)) },
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onShowToDatePicker() },
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                    cursorColor = Color(0xFF10B981),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                    focusedLabelColor = Color(0xFF10B981),
                    unfocusedLabelColor = Color(0xFF9CA3AF)
                ),
                leadingIcon = {
                    IconButton(onClick = { onShowToDatePicker() }) {
                        Icon(
                            Icons.Default.DateRange,
                            contentDescription = "Select To Date",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }
            )
            
            // Status filter
            var showStatusDropdown by remember { mutableStateOf(false) }
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { showStatusDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color(0xFF111827),
                        contentColor = Color(0xFFF9FAFB)
                    )
                ) {
                    Text(
                        pendingBillState.filterStatus?.name?.replace("_", " ") ?: "All Statuses",
                        color = Color(0xFFF9FAFB)
                    )
                    Icon(
                        Icons.Default.ArrowDropDown,
                        contentDescription = "Status Filter",
                        tint = Color(0xFF9CA3AF)
                    )
                }
                
                DropdownMenu(
                    expanded = showStatusDropdown,
                    onDismissRequest = { showStatusDropdown = false },
                    modifier = Modifier.background(Color(0xFF1F2937))
                ) {
                    DropdownMenuItem(
                        onClick = {
                            pendingBillViewModel.updateStatusFilter(null)
                            showStatusDropdown = false
                        }
                    ) {
                        Text("All Statuses", color = Color.White)
                    }
                    PendingBillStatus.values().forEach { status ->
                        DropdownMenuItem(
                            onClick = {
                                pendingBillViewModel.updateStatusFilter(status)
                                showStatusDropdown = false
                            }
                        ) {
                            Text(status.name.replace("_", " "), color = Color.White)
                        }
                    }
                }
                        }
                        
                        // Sort options
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Sort field dropdown
                            var showSortFieldDropdown by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                OutlinedButton(
                                    onClick = { showSortFieldDropdown = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        backgroundColor = Color(0xFF111827),
                                        contentColor = Color(0xFFF9FAFB)
                                    )
                                ) {
                        Text(
                            pendingBillState.sortBy.name.replace("_", " "),
                            color = Color(0xFFF9FAFB)
                        )
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = "Sort Field",
                                        tint = Color(0xFF9CA3AF)
                                    )
                                }
                                
                    DropdownMenu(
                        expanded = showSortFieldDropdown,
                        onDismissRequest = { showSortFieldDropdown = false },
                        modifier = Modifier.background(Color(0xFF1F2937))
                    ) {
                        PendingBillSortField.values().forEach { field ->
                            DropdownMenuItem(
                                onClick = {
                                    pendingBillViewModel.updateSortField(field)
                                    showSortFieldDropdown = false
                                }
                            ) {
                                Text(field.name.replace("_", " "), color = Color.White)
                            }
                        }
                    }
                            }
                            
                            // Sort direction button
                            Button(
                                onClick = {
                        val newDirection = if (pendingBillState.sortDirection == SortDirection.ASCENDING) {
                                        SortDirection.DESCENDING
                                    } else {
                                        SortDirection.ASCENDING
                                    }
                        pendingBillViewModel.updateSortDirection(newDirection)
                                },
                                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                            if (pendingBillState.sortDirection == SortDirection.ASCENDING) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                        contentDescription = "Sort Direction",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                            if (pendingBillState.sortDirection == SortDirection.ASCENDING) "Ascending" else "Descending",
                                        color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BilledBillCard(bill: PendingBill) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF1F2937),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = bill.firmName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "Bill #${bill.billNumber}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = bill.billDate,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "BILLED",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF10B981)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Quantity details
                Column {
                    Text(
                        text = "Quantity",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${String.format("%.2f", bill.quantityKg)} kg",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "${bill.numberOfBags} bags",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                // Portal amount
                Column {
                    Text(
                        text = "Portal Amount",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "₹${String.format("%.2f", bill.totalPortalAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "₹${String.format("%.2f", bill.portalAmount)} + GST",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                // Revenue amount
                Column {
                    Text(
                        text = "Revenue Amount",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "₹${String.format("%.2f", bill.totalRevenueAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "₹${String.format("%.2f", bill.revenueAmount)} + GST",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                // Difference amount
                Column {
                    Text(
                        text = "Difference",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "₹${String.format("%.2f", bill.differenceAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (bill.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444)
                    )

                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Portal batch
                Column {
                    Text(
                        text = "Portal Batch",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = bill.portalBatchNumber,
                        fontSize = 14.sp,
                        color = Color(0xFFF9FAFB)
                    )
                }
                
                // Rate per kg
                Column {
                    Text(
                        text = "Rate per Kg",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "₹${String.format("%.2f", bill.originalRatePerKg)}",
                        fontSize = 14.sp,
                        color = Color(0xFFF9FAFB)
                    )
                }
                
                // Cleared quantity
                Column {
                    Text(
                        text = "Cleared Quantity",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${String.format("%.2f", bill.clearedQuantity)} kg",
                        fontSize = 14.sp,
                        color = Color(0xFF10B981)
                    )
                }
                
                // Created date
                Column {
                    Text(
                        text = "Created",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = bill.createdAt?.let { 
                            java.time.LocalDateTime.ofEpochSecond(
                                it.seconds, 
                                it.nanos, 
                                java.time.ZoneOffset.UTC
                            ).format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                        } ?: "Unknown",
                        fontSize = 14.sp,
                        color = Color(0xFFF9FAFB)
                    )
                }
            }
            
            // Notes (if any)
            if (bill.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Notes: ${bill.notes}",
                    fontSize = 12.sp,
                    color = Color(0xFF9CA3AF),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}