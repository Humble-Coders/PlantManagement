package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.PendingBillViewModel
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.services.FirebaseStorageService
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun AddPendingBillDialog(
    customers: List<Entity>,
    pendingBillViewModel: PendingBillViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    storageService: FirebaseStorageService,
    onDismiss: () -> Unit,
    onSave: (PendingBill) -> Unit
) {
    var selectedEntityId by remember { mutableStateOf("") }
    var billDate by remember { mutableStateOf(LocalDate.now()) }
    var billNumber by remember { mutableStateOf("") }
    var portalBatchNumber by remember { mutableStateOf("") }

    var quantityKg by remember { mutableStateOf("") }
    var numberOfBags by remember { mutableStateOf("") }
    var deductFromInventory by remember { mutableStateOf(true) }

    var originalRatePerKg by remember { mutableStateOf("") }

    var discountType by remember { mutableStateOf(DiscountType.NONE) }
    var discountedRatePerKg by remember { mutableStateOf("") }
    var extraQuantityKg by remember { mutableStateOf("") }

    var truckNumber by remember { mutableStateOf("") }
    var fareAmount by remember { mutableStateOf("") }
    var farePaidBy by remember { mutableStateOf(FarePaidBy.COMPANY) }

    var notes by remember { mutableStateOf("") }

    // Image upload state
    var selectedImages by remember { mutableStateOf<List<File>>(emptyList()) }
    var isUploadingImages by remember { mutableStateOf(false) }

    // Inventory validation state
    var inventoryError by remember { mutableStateOf("") }
    var availableInventory by remember { mutableStateOf(0.0) }

    // Auto-calculate between kg and bags
    LaunchedEffect(quantityKg) {
        if (quantityKg.isNotBlank()) {
            val kg = quantityKg.toDoubleOrNull()
            if (kg != null) {
                numberOfBags = (kg / 25.0).toInt().toString()
            }
        }
    }

    LaunchedEffect(numberOfBags) {
        if (numberOfBags.isNotBlank() && numberOfBags != (quantityKg.toDoubleOrNull()?.div(25.0) ?: 0.0).toInt().toString()) {
            val bags = numberOfBags.toIntOrNull()
            if (bags != null) {
                quantityKg = (bags * 25.0).toString()
            }
        }
    }

    // Get fortified rice inventory
    LaunchedEffect(Unit) {
        val fortifiedRiceItem = inventoryViewModel.inventoryState.items.find { 
            it.name.lowercase().contains("fortified rice") || 
            it.name.lowercase().contains("frk")
        }
        availableInventory = fortifiedRiceItem?.quantity ?: 0.0
    }

    // Validate inventory when quantity changes
    LaunchedEffect(quantityKg, deductFromInventory) {
        if (quantityKg.isNotBlank() && deductFromInventory) {
            val qty = quantityKg.toDoubleOrNull() ?: 0.0
            if (qty > availableInventory) {
                inventoryError = "Insufficient inventory. Available: ${String.format("%.2f", availableInventory)} kg"
            } else {
                inventoryError = ""
            }
        } else {
            inventoryError = ""
        }
    }

    // Calculate amounts
    val calculation = remember(originalRatePerKg, quantityKg, discountType, discountedRatePerKg, extraQuantityKg) {
        val qty = quantityKg.toDoubleOrNull() ?: 0.0
        val originalRate = originalRatePerKg.toDoubleOrNull() ?: 0.0
        val discountedRate = discountedRatePerKg.toDoubleOrNull() ?: 0.0
        val extraQty = extraQuantityKg.toDoubleOrNull() ?: 0.0

        // Portal amount is ALWAYS calculated from original rate
        val portalAmount = qty * originalRate
        
        // GST is 5% on portal amount
        val gstAmount = portalAmount * 0.05
        val totalPortalAmount = portalAmount + gstAmount

        // Revenue amount is calculated from discounted rate (when applicable)
        val revenueAmount = when (discountType) {
            DiscountType.NONE -> qty * originalRate
            DiscountType.DISCOUNT_PREMIUM -> qty * discountedRate
            DiscountType.INDIRECT_DISCOUNT -> (qty - extraQty) * originalRate
        }
        
        // Total revenue amount = revenue amount + GST (5% on portal amount)
        val totalRevenueAmount = revenueAmount + gstAmount
        val differenceAmount = totalRevenueAmount - totalPortalAmount

        SaleCalculation(
            portalAmount = portalAmount,
            gstAmount = gstAmount,
            totalPortalAmount = totalPortalAmount,
            revenueAmount = revenueAmount,
            totalRevenueAmount = totalRevenueAmount,
            differenceAmount = differenceAmount
        )
    }

    Dialog(onDismissRequest = { 
        if (!pendingBillViewModel.pendingBillState.isAdding) {
            onDismiss()
        }
    }) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .height(750.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Add Pending Bill",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }

                // Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Customer Selection
                    item {
                        Text("Customer", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827)
                                )
                            ) {
                                Text(
                                    text = if (selectedEntityId.isBlank()) "Select Customer"
                                    else customers.find { it.id == selectedEntityId }?.firmName ?: "Select Customer",
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF9FAFB)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF9CA3AF))
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                customers.forEach { customer ->
                                    DropdownMenuItem(
                                        onClick = {
                                            selectedEntityId = customer.id
                                            expanded = false
                                        }
                                    ) {
                                        Text(customer.firmName, color = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    // Basic Details
                    item {
                        Text("Basic Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DatePicker(
                                selectedDate = billDate,
                                onDateSelected = { billDate = it },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = billNumber,
                                onValueChange = { billNumber = it },
                                label = { Text("Bill Number", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = portalBatchNumber,
                            onValueChange = { portalBatchNumber = it },
                            label = { Text("Portal Batch Number", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF10B981)
                            ),
                            singleLine = true
                        )
                    }

                    // Quantity Details
                    item {
                        Text("Quantity Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = quantityKg,
                                onValueChange = { quantityKg = it },
                                label = { Text("Quantity (Kg)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier
                                    .weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = numberOfBags,
                                onValueChange = { numberOfBags = it },
                                label = { Text("Number of Bags", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier
                                    .weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Checkbox(
                                checked = deductFromInventory,
                                onCheckedChange = { deductFromInventory = it },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF10B981),
                                    uncheckedColor = Color(0xFF9CA3AF)
                                )
                            )
                            Text(
                                "Deduct from Inventory",
                                color = Color(0xFFF9FAFB),
                                fontSize = 14.sp
                            )
                        }
                    }

                    if (inventoryError.isNotEmpty()) {
                        item {
                            Card(
                                backgroundColor = Color(0xFF7F1D1D),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = "Warning",
                                        tint = Color(0xFFFCA5A5),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        inventoryError,
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    // Pricing Details
                    item {
                        Text("Pricing Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    item {
                        OutlinedTextField(
                            value = originalRatePerKg,
                            onValueChange = { originalRatePerKg = it },
                            label = { Text("Original Rate per Kg (₹)", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF10B981)
                            ),
                            singleLine = true
                        )
                    }

                    // Discount Section
                    item {
                        Text("Discount (Optional)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827)
                                )
                            ) {
                                Text(
                                    text = discountType.name.replace("_", " "),
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF9FAFB)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF9CA3AF))
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DiscountType.values().forEach { type ->
                                    DropdownMenuItem(
                                        onClick = {
                                            discountType = type
                                            expanded = false
                                        }
                                    ) {
                                        Text(type.name.replace("_", " "), color = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    if (discountType == DiscountType.DISCOUNT_PREMIUM) {
                        item {
                            OutlinedTextField(
                                value = discountedRatePerKg,
                                onValueChange = { discountedRatePerKg = it },
                                label = { Text("Discounted Rate per Kg (₹)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                        }
                    }

                    if (discountType == DiscountType.INDIRECT_DISCOUNT) {
                        item {
                            OutlinedTextField(
                                value = extraQuantityKg,
                                onValueChange = { extraQuantityKg = it },
                                label = { Text("Extra Quantity (Kg)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                        }
                    }

                    // Divider
                    item { Divider(color = Color(0xFF374151)) }

                    // Transport Section
                    item {
                        Text("Transport Details (Optional)", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = truckNumber,
                                onValueChange = { truckNumber = it },
                                label = { Text("Truck Number", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = fareAmount,
                                onValueChange = { fareAmount = it },
                                label = { Text("Fare Amount (₹)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )
                        }
                    }

                    item {
                        var expanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { expanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827)
                                )
                            ) {
                                Text(
                                    text = farePaidBy.name.replace("_", " "),
                                    modifier = Modifier.weight(1f),
                                    color = Color(0xFFF9FAFB)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = Color(0xFF9CA3AF))
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                FarePaidBy.values().forEach { type ->
                                    DropdownMenuItem(
                                        onClick = {
                                            farePaidBy = type
                                            expanded = false
                                        }
                                    ) {
                                        Text(type.name.replace("_", " "), color = Color.Black)
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF10B981),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF10B981)
                            ),
                            maxLines = 3
                        )
                    }

                    // Calculation Summary
                    item {
                        Card(
                            backgroundColor = Color(0xFF111827),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Calculation Summary",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Divider(color = Color(0xFF374151))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Portal Amount:", color = Color(0xFF9CA3AF))
                                    Text("₹ ${String.format("%.2f", calculation.portalAmount)}", color = Color(0xFFF9FAFB))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST (5%):", color = Color(0xFF9CA3AF))
                                    Text("₹ ${String.format("%.2f", calculation.gstAmount)}", color = Color(0xFFF9FAFB))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Portal Amount:", color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                                    Text("₹ ${String.format("%.2f", calculation.totalPortalAmount)}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Revenue Amount:", color = Color(0xFF9CA3AF))
                                    Text("₹ ${String.format("%.2f", calculation.revenueAmount)}", color = Color(0xFFF9FAFB))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total Revenue Amount:", color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                                    Text("₹ ${String.format("%.2f", calculation.totalRevenueAmount)}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Difference Amount:", color = Color(0xFF9CA3AF), fontWeight = FontWeight.Bold)
                                    Text("₹ ${String.format("%.2f", calculation.differenceAmount)}", 
                                        color = if (calculation.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444), 
                                        fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Footer with Save Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val qty = quantityKg.toDoubleOrNull() ?: 0.0
                            val bags = numberOfBags.toIntOrNull() ?: 0
                            val selectedEntity = customers.find { it.id == selectedEntityId }

                            if (selectedEntity != null) {
                                onSave(
                                    PendingBill(
                                        customerId = selectedEntityId,
                                        firmName = selectedEntity.firmName,
                                        billDate = billDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        billNumber = billNumber,
                                        portalBatchNumber = portalBatchNumber,
                                        quantityKg = qty,
                                        numberOfBags = bags,
                                        deductFromInventory = deductFromInventory,
                                        originalRatePerKg = originalRatePerKg.toDoubleOrNull() ?: 0.0,
                                        portalAmount = calculation.portalAmount,
                                        gstAmount = calculation.gstAmount,
                                        totalPortalAmount = calculation.totalPortalAmount,
                                        discountType = discountType,
                                        discountedRatePerKg = discountedRatePerKg.toDoubleOrNull() ?: 0.0,
                                        extraQuantityKg = extraQuantityKg.toDoubleOrNull() ?: 0.0,
                                        revenueAmount = calculation.revenueAmount,
                                        totalRevenueAmount = calculation.totalRevenueAmount,
                                        differenceAmount = calculation.differenceAmount,
                                        portalAmountPaid = 0.0, // No monetary transactions for pending bills
                                        differenceAmountPaid = 0.0, // Always 0 when adding a pending bill
                                        truckNumber = truckNumber,
                                        fareAmount = fareAmount.toDoubleOrNull() ?: 0.0,
                                        farePaidBy = farePaidBy,
                                        notes = notes,
                                        imageUrls = emptyList(),
                                        status = PendingBillStatus.PENDING_BILLED,
                                        clearedQuantity = 0.0, // Always 0 when adding a new pending bill
                                        createdAt = null
                                    )
                                )
                            }
                        },
                        enabled = !pendingBillViewModel.pendingBillState.isAdding && 
                                selectedEntityId.isNotBlank() && billNumber.isNotBlank() &&
                                portalBatchNumber.isNotBlank() && quantityKg.isNotBlank() &&
                                originalRatePerKg.isNotBlank() && inventoryError.isEmpty(),
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981)),
                        modifier = Modifier.height(40.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pendingBillViewModel.pendingBillState.isAdding) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            }
                            Text(
                                if (pendingBillViewModel.pendingBillState.isAdding) "Adding..." else "Add Pending Bill", 
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

// Data class for calculation results
data class SaleCalculation(
    val portalAmount: Double,
    val gstAmount: Double,
    val totalPortalAmount: Double,
    val revenueAmount: Double,
    val totalRevenueAmount: Double,
    val differenceAmount: Double
)
