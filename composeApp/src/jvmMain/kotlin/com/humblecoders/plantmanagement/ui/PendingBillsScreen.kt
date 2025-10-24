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
import com.humblecoders.plantmanagement.viewmodels.PendingBillViewModel
import com.humblecoders.plantmanagement.ui.components.AddPendingBillDialog
import com.humblecoders.plantmanagement.ui.components.AddSaleDialog
import com.humblecoders.plantmanagement.ui.components.SearchableCustomerDropdown
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PendingBillsScreen(
    pendingBillViewModel: PendingBillViewModel,
    entityViewModel: com.humblecoders.plantmanagement.viewmodels.EntityViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    saleViewModel: com.humblecoders.plantmanagement.viewmodels.SaleViewModel,
    storageService: com.humblecoders.plantmanagement.services.FirebaseStorageService,
    userRole: UserRole? = null
) {
    val pendingBillState = pendingBillViewModel.pendingBillState
    var showClearBillDialog by remember { mutableStateOf(false) }
    var pendingBillToClear by remember { mutableStateOf<PendingBill?>(null) }
    var showAddPendingBillDialog by remember { mutableStateOf(false) }
    var showHistoryScreen by remember { mutableStateOf(false) }
    
    // Filter pending bills with PENDING_BILLED status
    val pendingBills = pendingBillState.pendingBills.filter { it.status == PendingBillStatus.PENDING_BILLED }
    
    // Auto-close dialogs when operations complete
    LaunchedEffect(pendingBillState.isAdding, pendingBillState.isUpdating) {
        if (!pendingBillState.isAdding && !pendingBillState.isUpdating) {
            // Check if we have a success message, which means operation completed successfully
            if (pendingBillState.successMessage != null) {
                showAddPendingBillDialog = false
                showClearBillDialog = false
                pendingBillToClear = null
            }
        }
    }
    
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
            Text(
                text = "Pending Bills",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
            Text(
                text = "${pendingBills.size} pending bills",
                fontSize = 16.sp,
                color = Color(0xFF9CA3AF)
            )
                
                Button(
                    onClick = { showAddPendingBillDialog = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add Pending Bill",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Button(
                    onClick = { showHistoryScreen = true },
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280)),
                    modifier = Modifier.height(40.dp)
                ) {
                    Icon(Icons.Default.History, contentDescription = "History", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "History",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
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
                        text = "Loading pending bills...",
                        color = Color(0xFF9CA3AF),
                        fontSize = 16.sp
                    )
                }
            }
        } else if (pendingBills.isEmpty()) {
            // Empty state
            Card(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Receipt,
                        contentDescription = "No pending bills",
                        tint = Color(0xFF6B7280),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No Pending Bills",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF9CA3AF)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "All bills have been cleared",
                        fontSize = 14.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }
        } else {
            // Pending bills list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(pendingBills) { pendingBill ->
                    PendingBillCard(
                        pendingBill = pendingBill,
                        onClearBillClick = {
                            pendingBillToClear = pendingBill
                            showClearBillDialog = true
                        }
                    )
                }
            }
        }
    }
    
    // Clear Bill Dialog
    if (showClearBillDialog && pendingBillToClear != null) {
        ClearBillDialog(
            pendingBill = pendingBillToClear!!,
            customers = entityViewModel.entityState.entities,
            pendingBillViewModel = pendingBillViewModel,
            saleViewModel = saleViewModel,
            inventoryViewModel = inventoryViewModel,
            storageService = storageService,
            onDismiss = {
                showClearBillDialog = false
                pendingBillToClear = null
            },
            onClearBill = { clearedQuantity, customerName ->
                pendingBillViewModel.clearBill(pendingBillToClear!!.id, clearedQuantity, customerName)
                // Dialog will be closed automatically when operation completes
                // We'll handle this in the success/error state handling
            }
        )
    }
    
    // Add Pending Bill Dialog
    if (showAddPendingBillDialog) {
        AddPendingBillDialog(
            customers = entityViewModel.entityState.entities,
            pendingBillViewModel = pendingBillViewModel,
            inventoryViewModel = inventoryViewModel,
            storageService = storageService,
            onDismiss = { showAddPendingBillDialog = false },
            onSave = { pendingBill ->
                pendingBillViewModel.addPendingBill(pendingBill)
                // Dialog will be closed automatically when operation completes
                // We'll handle this in the success/error state handling
            }
        )
    }
    
    // History Screen
    if (showHistoryScreen) {
        PendingBillHistoryScreen(
            pendingBillViewModel = pendingBillViewModel,
            userRole = userRole,
            onBack = { showHistoryScreen = false }
        )
    }
}

@Composable
fun PendingBillCard(
    pendingBill: PendingBill,
    onClearBillClick: () -> Unit
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
            // Header row with customer name and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pendingBill.firmName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    Text(
                        text = "Bill #${pendingBill.billNumber}",
                        fontSize = 14.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
                
                Text(
                    text = pendingBill.billDate,
                    fontSize = 14.sp,
                    color = Color(0xFF9CA3AF)
                )
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
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.2f", pendingBill.clearedQuantity)} / ${String.format("%.2f", pendingBill.quantityKg)} kg",
                        fontSize = 16.sp,
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Portal amount
                Column {
                    Text(
                        text = "Portal Amount",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "₹ ${String.format("%.2f", pendingBill.totalPortalAmount)}",
                        fontSize = 16.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                // Status
                Column {
                    Text(
                        text = "Status",
                        fontSize = 12.sp,
                        color = Color(0xFF9CA3AF),
                        fontWeight = FontWeight.Medium
                    )
                    Card(
                        backgroundColor = Color(0xFFF59E0B),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "PENDING",
                            fontSize = 12.sp,
                            color = Color(0xFF111827),
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onClearBillClick,
                    colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                    modifier = Modifier.weight(1f).height(36.dp)
                ) {
                    Text(
                        text = "Clear Bill",
                        color = Color(0xFFF9FAFB),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun ClearBillDialog(
    pendingBill: PendingBill,
    customers: List<Entity>,
    pendingBillViewModel: PendingBillViewModel,
    saleViewModel: com.humblecoders.plantmanagement.viewmodels.SaleViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    storageService: com.humblecoders.plantmanagement.services.FirebaseStorageService,
    onDismiss: () -> Unit,
    onClearBill: (Double, String) -> Unit
) {
    var clearedQuantity by remember { mutableStateOf("") }
    var selectedCustomer by remember { mutableStateOf<Entity?>(null) }
    var errorMessage by remember { mutableStateOf("") }
    var showAddSaleDialog by remember { mutableStateOf(false) }
    
    // Set default customer to the bill's customer
    LaunchedEffect(pendingBill.customerId) {
        selectedCustomer = customers.find { it.id == pendingBill.customerId }
    }
    
    // Monitor sale state to close dialog when operation completes
    LaunchedEffect(saleViewModel.saleState.isAdding, saleViewModel.saleState.error, saleViewModel.saleState.successMessage) {
        if (showAddSaleDialog && !saleViewModel.saleState.isAdding) {
            // Sale operation completed (either success or error)
            if (saleViewModel.saleState.successMessage != null) {
                // Success - proceed with clearing the bill
                val clearedQuantityValue = clearedQuantity.toDoubleOrNull() ?: 0.0
                onClearBill(clearedQuantityValue, selectedCustomer?.firmName ?: pendingBill.firmName)
            }
            // Close the dialog regardless of success or error
            showAddSaleDialog = false
            onDismiss()
        }
    }
    
    Dialog(onDismissRequest = { 
        if (!pendingBillViewModel.pendingBillState.isUpdating && !saleViewModel.saleState.isAdding) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .width(400.dp),
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
                    Text(
                        "Clear Bill",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(
                        onClick = { 
                            if (!saleViewModel.saleState.isAdding) {
                                onDismiss()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Pending bill details
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = pendingBill.firmName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )
                        Text(
                            text = "Bill #${pendingBill.billNumber}",
                            fontSize = 14.sp,
                            color = Color(0xFF9CA3AF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Quantity: ${String.format("%.2f", pendingBill.quantityKg)} kg",
                                fontSize = 14.sp,
                                color = Color(0xFF9CA3AF)
                            )
                            Text(
                                text = "Cleared: ${String.format("%.2f", pendingBill.clearedQuantity)} kg",
                                fontSize = 14.sp,
                                color = Color(0xFF10B981)
                            )
                        }
                        Text(
                            text = "Remaining: ${String.format("%.2f", pendingBill.quantityKg - pendingBill.clearedQuantity)} kg",
                            fontSize = 14.sp,
                            color = Color(0xFFF59E0B)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity input
                OutlinedTextField(
                    value = clearedQuantity,
                    onValueChange = { 
                        clearedQuantity = it
                        errorMessage = ""
                        
                        // Real-time validation
                        val quantity = it.toDoubleOrNull()
                        val remainingQuantity = pendingBill.quantityKg - pendingBill.clearedQuantity
                        
                        when {
                            it.isNotEmpty() && (quantity == null || quantity <= 0) -> {
                                errorMessage = "Please enter a valid quantity"
                            }
                            it.isNotEmpty() && quantity != null && quantity > remainingQuantity -> {
                                errorMessage = "Cannot clear more than remaining quantity (${String.format("%.2f", remainingQuantity)} kg)"
                            }
                        }
                    },
                    label = { Text("Quantity to Clear (kg)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = if (errorMessage.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF10B981),
                        unfocusedBorderColor = if (errorMessage.isNotEmpty()) Color(0xFFEF4444) else Color(0xFF374151),
                        cursorColor = Color(0xFF10B981)
                    ),
                    singleLine = true,
                    isError = errorMessage.isNotEmpty()
                )
                
                // Helpful hint showing remaining quantity
                Text(
                    text = "Remaining quantity: ${String.format("%.2f", pendingBill.quantityKg - pendingBill.clearedQuantity)} kg",
                    fontSize = 12.sp,
                    color = Color(0xFF6B7280),
                    modifier = Modifier.padding(start = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Customer selection
                Text(
                    text = "Select Customer for Sale",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFF9FAFB)
                )
                
                SearchableCustomerDropdown(
                    customers = customers,
                    selectedCustomerId = selectedCustomer?.id ?: "",
                    onCustomerSelected = { customerId ->
                        selectedCustomer = customers.find { it.id == customerId }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Select Customer"
                )
                
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = { 
                            if (!saleViewModel.saleState.isAdding) {
                                onDismiss()
                            }
                        }
                    ) {
                        Text("Cancel", color = Color(0xFF9CA3AF))
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Button(
                        onClick = {
                            val quantity = clearedQuantity.toDoubleOrNull()
                            val remainingQuantity = pendingBill.quantityKg - pendingBill.clearedQuantity
                            
                            when {
                                quantity == null || quantity <= 0 -> {
                                    errorMessage = "Please enter a valid quantity"
                                }
                                quantity > remainingQuantity -> {
                                    errorMessage = "Cannot clear more than remaining quantity (${String.format("%.2f", remainingQuantity)} kg)"
                                }
                                selectedCustomer == null -> {
                                    errorMessage = "Please select a customer"
                                }
                                else -> {
                                    showAddSaleDialog = true
                                }
                            }
                        },
                        enabled = !pendingBillViewModel.pendingBillState.isUpdating && 
                                clearedQuantity.isNotBlank() && errorMessage.isEmpty() && selectedCustomer != null,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pendingBillViewModel.pendingBillState.isUpdating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFFF9FAFB),
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                if (pendingBillViewModel.pendingBillState.isUpdating) "Clearing..." else "Clear Bill", 
                                color = Color(0xFFF9FAFB), 
                                fontWeight = FontWeight.Bold
                            )
                        }
                }
            }
        }
    }
}

    // Add Sale Dialog with pre-filled values
    if (showAddSaleDialog) {
        val clearedQuantityValue = clearedQuantity.toDoubleOrNull() ?: 0.0
        val preFilledSale = Sale(
            customerId = selectedCustomer?.id ?: pendingBill.customerId,
            firmName = selectedCustomer?.firmName ?: pendingBill.firmName,
            saleDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE),
            billNumber = pendingBill.billNumber,
            portalBatchNumber = pendingBill.portalBatchNumber,
            quantityKg = clearedQuantityValue,
            numberOfBags = (clearedQuantityValue / 25.0).toInt(),
            deductFromInventory = pendingBill.deductFromInventory, // Use the same setting as the pending bill
            originalRatePerKg = pendingBill.originalRatePerKg,
            portalAmount = pendingBill.portalAmount * (clearedQuantityValue / pendingBill.quantityKg),
            gstAmount = pendingBill.gstAmount * (clearedQuantityValue / pendingBill.quantityKg),
            totalPortalAmount = pendingBill.totalPortalAmount * (clearedQuantityValue / pendingBill.quantityKg),
            discountType = pendingBill.discountType,
            discountedRatePerKg = pendingBill.discountedRatePerKg,
            extraQuantityKg = pendingBill.extraQuantityKg * (clearedQuantityValue / pendingBill.quantityKg),
            revenueAmount = pendingBill.revenueAmount * (clearedQuantityValue / pendingBill.quantityKg),
            totalRevenueAmount = pendingBill.totalRevenueAmount * (clearedQuantityValue / pendingBill.quantityKg),
            differenceAmount = pendingBill.differenceAmount * (clearedQuantityValue / pendingBill.quantityKg),
            portalAmountPaid = pendingBill.portalAmountPaid * (clearedQuantityValue / pendingBill.quantityKg),
            differenceAmountPaid = 0.0,
            truckNumber = pendingBill.truckNumber,
            fareAmount = pendingBill.fareAmount * (clearedQuantityValue / pendingBill.quantityKg),
            farePaidBy = pendingBill.farePaidBy,
            notes = pendingBill.notes,
            imageUrls = emptyList(),
            clearedInventory = clearedQuantityValue
        )
        
        AddSaleDialog(
            customers = customers,
            saleViewModel = saleViewModel,
            inventoryViewModel = inventoryViewModel,
            storageService = storageService,
            onDismiss = { showAddSaleDialog = false },
            onSave = { sale ->
                saleViewModel.addSale(sale, skipInventoryDeduction = true)
                // Don't close dialog immediately - wait for sale operation to complete
            },
            preFilledSale = preFilledSale,
            isClearingBill = true
        )
    }
}