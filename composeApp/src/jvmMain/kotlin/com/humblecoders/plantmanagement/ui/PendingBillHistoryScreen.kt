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
    
    // Get filtered and sorted bills using ViewModel (without enforcing status)
    val filteredBills = pendingBillViewModel.getFilteredAndSortedPendingBills()
    
    // State for date pickers
    var showFromDatePicker by remember { mutableStateOf(false) }
    var showToDatePicker by remember { mutableStateOf(false) }
    
    // State for clearance records dialog
    var showClearanceRecordsDialog by remember { mutableStateOf(false) }
    var selectedBillForClearance by remember { mutableStateOf<PendingBill?>(null) }
    
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
                    text = "${filteredBills.size} bills",
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
                                    text = "No Bills Found",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF9CA3AF)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (pendingBillState.searchQuery.isNotBlank() || 
                                        pendingBillState.filterDateFrom.isNotBlank() || 
                                        pendingBillState.filterDateTo.isNotBlank()) {
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
                        BilledBillCard(
                            bill = bill,
                            onViewClearanceRecords = { 
                                selectedBillForClearance = bill
                                showClearanceRecordsDialog = true
                            }
                        )
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
    
    // Clearance Records Dialog
    if (showClearanceRecordsDialog && selectedBillForClearance != null) {
        ClearanceRecordsDialog(
            pendingBill = selectedBillForClearance!!,
            clearanceRecords = pendingBillState.clearanceRecords.filter { 
                it.pendingBillId == selectedBillForClearance!!.id 
            },
            onDismiss = { 
                showClearanceRecordsDialog = false
                selectedBillForClearance = null
            }
        )
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
                        
                        // Compact filters row: Search, From, To, Sort Field, Sort Direction
                        var showSortFieldDropdown by remember { mutableStateOf(false) }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Search
                            OutlinedTextField(
                                value = pendingBillState.searchQuery,
                                onValueChange = { pendingBillViewModel.updateSearchQuery(it) },
                                label = { Text("Search bills...", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(2f),
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
                            // From Date
                            OutlinedTextField(
                                value = pendingBillState.filterDateFrom,
                                onValueChange = { },
                                label = { Text("From", color = Color(0xFF9CA3AF)) },
                                readOnly = true,
                                modifier = Modifier
                                    .weight(1f)
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
                            // To Date
                            OutlinedTextField(
                                value = pendingBillState.filterDateTo,
                                onValueChange = { },
                                label = { Text("To", color = Color(0xFF9CA3AF)) },
                                readOnly = true,
                                modifier = Modifier
                                    .weight(1f)
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
                            // Sort Field
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
                            // Sort Direction
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
                                modifier = Modifier
                                    .height(56.dp)
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
                                        if (pendingBillState.sortDirection == SortDirection.ASCENDING) "Asc" else "Desc",
                                        color = Color.White
                                    )
                                }
                            }
                        }
        }
    }
}

@Composable
fun BilledBillCard(
    bill: PendingBill,
    onViewClearanceRecords: () -> Unit
) {
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
                    val statusText = bill.status.name.replace("_", " ")
                    val statusColor = when (bill.status) {
                        PendingBillStatus.BILLED -> Color(0xFF10B981)
                        PendingBillStatus.PENDING_BILLED -> Color(0xFFF59E0B)
                    }
                    Text(
                        text = statusText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
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
            
            // View button (only show if there are clearance records)
            if (bill.clearedQuantity > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onViewClearanceRecords,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.Visibility,
                                contentDescription = "View",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "View Clearance Records",
                                color = Color.White,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ClearanceRecordCard(record: PendingBillClearanceRecord) {
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
                        text = record.customerName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    
                }
                
                Column(
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = record.clearanceDate,
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                    Text(
                        text = "CLEARED",
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
                // Quantity cleared
                Column {
                    Text(
                        text = "Quantity Cleared",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = "${String.format("%.2f", record.quantityCleared)} kg",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF10B981)
                    )
                }
                
                // Created date
                Column {
                    Text(
                        text = "Recorded On",
                        fontSize = 12.sp,
                        color = Color(0xFF6B7280)
                    )
                    Text(
                        text = record.createdAt?.let { 
                            java.time.LocalDateTime.ofEpochSecond(
                                it.seconds, 
                                it.nanos, 
                                java.time.ZoneOffset.UTC
                            ).format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"))
                        } ?: "Unknown",
                        fontSize = 14.sp,
                        color = Color(0xFFF9FAFB)
                    )
                }
            }
        }
    }
}

@Composable
fun ClearanceRecordsDialog(
    pendingBill: PendingBill,
    clearanceRecords: List<PendingBillClearanceRecord>,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(600.dp)
                .heightIn(max = 600.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Clearance Records",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "${pendingBill.firmName} - Bill #${pendingBill.billNumber}",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF)
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity Cards
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Total Quantity Card
                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF111827),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Total Quantity",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.2f", pendingBill.quantityKg)} kg",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF9FAFB)
                            )
                        }
                    }
                    
                    // Cleared Quantity Card
                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF111827),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Cleared Quantity",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.2f", pendingBill.clearedQuantity)} kg",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF10B981)
                            )
                        }
                    }
                    
                    // Remaining Quantity Card
                    Card(
                        modifier = Modifier.weight(1f),
                        backgroundColor = Color(0xFF111827),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Remaining",
                                fontSize = 12.sp,
                                color = Color(0xFF6B7280)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "${String.format("%.2f", pendingBill.quantityKg - pendingBill.clearedQuantity)} kg",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Clearance Records List
                if (clearanceRecords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No clearance records found",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(clearanceRecords) { record ->
                            ClearanceRecordCard(record = record)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Close", color = Color.White)
                }
            }
        }
    }
}