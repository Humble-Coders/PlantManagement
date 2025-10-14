package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import com.humblecoders.plantmanagement.viewmodels.InventoryViewModel
import com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel
import com.humblecoders.plantmanagement.viewmodels.PurchaseSortField
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.humblecoders.plantmanagement.ui.components.DatePicker

@Composable
fun PurchaseScreen(
    purchaseViewModel: PurchaseViewModel,
    entityViewModel: EntityViewModel,
    inventoryViewModel: InventoryViewModel
) {
    val purchaseState = purchaseViewModel.purchaseState
    val entityState = entityViewModel.entityState
    val inventoryState = inventoryViewModel.inventoryState
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var purchaseToEdit by remember { mutableStateOf<Purchase?>(null) }
    var showViewDialog by remember { mutableStateOf(false) }
    var purchaseToView by remember { mutableStateOf<Purchase?>(null) }

    LaunchedEffect(purchaseState.successMessage, purchaseState.error) {
        if (purchaseState.successMessage != null || purchaseState.error != null) {
            kotlinx.coroutines.delay(3000)
            purchaseViewModel.clearMessages()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Purchase Management",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (purchaseState.successMessage != null) {
            Card(
                backgroundColor = Color(0xFF10B981),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = purchaseState.successMessage,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (purchaseState.error != null) {
            Card(
                backgroundColor = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = purchaseState.error,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Log New Purchase",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4)
                        )
                    ) {
                        Text("Add Purchase", color = Color(0xFF111827))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(12.dp),
            elevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Purchase Records",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = purchaseState.searchQuery,
                            onValueChange = { purchaseViewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search records...", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                            },
                            modifier = Modifier.width(200.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF06B6D4)
                            ),
                            singleLine = true
                        )

                        Text("From:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        DatePicker(
                            selectedDate = try { LocalDate.parse(purchaseState.filterDateFrom) } catch (e: Exception) { LocalDate.now() },
                            onDateSelected = { date -> purchaseViewModel.updateDateFilter(date.format(DateTimeFormatter.ISO_LOCAL_DATE), purchaseState.filterDateTo) },
                            modifier = Modifier.width(140.dp),
                            label = ""
                        )

                        Text("To:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                        DatePicker(
                            selectedDate = try { LocalDate.parse(purchaseState.filterDateTo) } catch (e: Exception) { LocalDate.now() },
                            onDateSelected = { date -> purchaseViewModel.updateDateFilter(purchaseState.filterDateFrom, date.format(DateTimeFormatter.ISO_LOCAL_DATE)) },
                            modifier = Modifier.width(140.dp),
                            label = ""
                        )

                        var sortExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { sortExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Sort by: ${purchaseState.sortBy.name}")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = sortExpanded,
                                onDismissRequest = { sortExpanded = false }
                            ) {
                                DropdownMenuItem(onClick = {
                                    purchaseViewModel.updateSortBy(PurchaseSortField.DATE)
                                    sortExpanded = false
                                }) {
                                    Text("Date")
                                }
                                DropdownMenuItem(onClick = {
                            purchaseViewModel.updateSortBy(PurchaseSortField.ENTITY)
                                    sortExpanded = false
                                }) {
                            Text("Entity")
                                }
                                DropdownMenuItem(onClick = {
                                    purchaseViewModel.updateSortBy(PurchaseSortField.STATUS)
                                    sortExpanded = false
                                }) {
                                    Text("Status")
                                }
                            }
                        }

                        IconButton(
                            onClick = { purchaseViewModel.toggleSortDirection() }
                        ) {
                            Icon(
                                imageVector = if (purchaseState.sortDirection == SortDirection.ASCENDING) {
                                    Icons.Default.ArrowDropUp
                                } else {
                                    Icons.Default.ArrowDropDown
                                },
                                contentDescription = "Sort Direction",
                                tint = Color(0xFF06B6D4)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                PurchaseTable(
                    purchases = purchaseViewModel.getFilteredAndSortedPurchases(),
                    onEditClick = {
                        purchaseToEdit = it
                        showEditDialog = true
                    },
                    onDeleteClick = { purchaseViewModel.deletePurchase(it.id) },
                    onViewClick = {
                        purchaseToView = it
                        showViewDialog = true
                    }
                )
            }
        }
    }

    if (showAddDialog) {
        AddPurchaseDialog(
            customers = entityState.entities,
            inventoryItems = inventoryState.items,
            onDismiss = { showAddDialog = false },
            onSave = { purchase ->
                purchaseViewModel.addPurchase(purchase)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && purchaseToEdit != null) {
        EditPurchaseDialog(
            purchase = purchaseToEdit!!,
            customers = entityState.entities,
            inventoryItems = inventoryState.items,
            onDismiss = {
                showEditDialog = false
                purchaseToEdit = null
            },
            onSave = { purchase ->
                purchaseViewModel.updatePurchase(purchaseToEdit!!.id, purchase)
                showEditDialog = false
                purchaseToEdit = null
            }
        )
    }

    if (showViewDialog && purchaseToView != null) {
        ViewPurchaseDialog(
            purchase = purchaseToView!!,
            onDismiss = {
                showViewDialog = false
                purchaseToView = null
            }
        )
    }
}

@Composable
fun PurchaseTable(
    purchases: List<Purchase>,
    onEditClick: (Purchase) -> Unit,
    onDeleteClick: (Purchase) -> Unit,
    onViewClick: (Purchase) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(12.dp)
        ) {
            Text("Date", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Entity", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Items", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.18f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Grand Total", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Status", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Actions", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color(0xFF374151))

        purchases.forEach { purchase ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(purchase.purchaseDate, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.12f), fontSize = 14.sp)
                Text(purchase.firmName, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.20f), fontSize = 14.sp)

                Column(modifier = Modifier.weight(0.18f)) {
                    purchase.items.take(2).forEach { item ->
                        Text(
                            "${item.itemName} (${String.format("%.2f", item.quantity)} ${item.unit})",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }
                    if (purchase.items.size > 2) {
                        Text(
                            "+${purchase.items.size - 2} more",
                            color= Color(0xFF06B6D4),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text("₹ ${String.format("%.2f", purchase.grandTotal)}", color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.15f), fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Text(
                    text = purchase.paymentStatus.name.replace("_", " "),
                    color = when (purchase.paymentStatus) {
                        PaymentStatus.PAID -> Color(0xFF10B981)
                        PaymentStatus.PENDING -> Color(0xFFF59E0B)
                        PaymentStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                    },
                    modifier = Modifier.weight(0.15f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.weight(0.20f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onViewClick(purchase) }) {
                        Text("View", color = Color(0xFF3B82F6), fontSize = 12.sp)
                    }
                    TextButton(onClick = { onEditClick(purchase) }) {
                        Text("Edit", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                    TextButton(onClick = { onDeleteClick(purchase) }) {
                        Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                    }
                }
            }
            Divider(color = Color(0xFF374151))
        }

        if (purchases.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No purchase records found", color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun AddPurchaseDialog(
    customers: List<Entity>,
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit
) {
    var selectedEntityId by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var gstRate by remember { mutableStateOf(0.0) }
    var amountPaid by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var purchaseItems by remember { mutableStateOf(listOf<PurchaseItem>()) }

    val totalAmount = purchaseItems.sumOf { it.totalPrice }
    val gstAmount = totalAmount * (gstRate / 100.0)
    val grandTotal = totalAmount + gstAmount
    val paidAmount = amountPaid.toDoubleOrNull() ?: 0.0
    val pendingAmount = grandTotal - paidAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Log New Purchase", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Box(
                modifier = Modifier
                    .width(700.dp)
                    .height(750.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Entity Dropdown
                var entityExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { entityExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (selectedEntityId.isBlank()) "Select Entity"
                            else customers.find { it.id == selectedEntityId }?.firmName ?: "Select Entity",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = entityExpanded,
                        onDismissRequest = { entityExpanded = false }
                    ) {
                        customers.forEach { entity ->
                            DropdownMenuItem(onClick = {
                                selectedEntityId = entity.id
                                entityExpanded = false
                            }) {
                                Text(entity.firmName)
                            }
                        }
                    }
                }

                // Purchase Date
                DatePicker(
                    selectedDate = try { LocalDate.parse(purchaseDate) } catch (e: Exception) { LocalDate.now() },
                    onDateSelected = { date -> 
                        purchaseDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    },
                    label = "Purchase Date",
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(color = Color(0xFF374151))

                // Items Section
                Row(
                        modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Purchase Items",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4)
                    )
                    Button(
                        onClick = {
                            purchaseItems = purchaseItems + PurchaseItem()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", color = Color.White, fontSize = 12.sp)
                    }
                }

                // Items List
                if (purchaseItems.isEmpty()) {
                    Card(
                        backgroundColor = Color(0xFF111827),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No items added. Click 'Add Item' to start.",
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.padding(16.dp),
                            fontSize = 12.sp
                        )
                    }
                }

                purchaseItems.forEachIndexed { index, item ->
                    PurchaseItemCard(
                        item = item,
                        index = index,
                        inventoryItems = inventoryItems,
                        onItemChanged = { updatedItem ->
                            purchaseItems = purchaseItems.toMutableList().apply {
                                set(index, updatedItem)
                            }
                        },
                        onRemove = {
                            purchaseItems = purchaseItems.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    )
                }

                Divider(color = Color(0xFF374151))

                // Total Amount Display
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total Amount:", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                            Text("₹ ${String.format("%.2f", totalAmount)}", color = Color(0xFFF9FAFB), fontSize = 14.sp)
                        }
                    }
                }

                // GST Selection
                Text("GST Rate", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { gstRate = 0.0 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gstRate == 0.0) Color(0xFF06B6D4) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("0%")
                    }
                    OutlinedButton(
                        onClick = { gstRate = 5.0 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gstRate == 5.0) Color(0xFF06B6D4) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("5%")
                    }
                    OutlinedButton(
                        onClick = { gstRate = 18.0 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gstRate == 18.0) Color(0xFF06B6D4) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("18%")
                    }
                }

                // GST Amount Display
                if (gstRate > 0) {
                    Card(
                        backgroundColor = Color(0xFF111827),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST Amount (${gstRate.toInt()}%):", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                            Text("₹ ${String.format("%.2f", gstAmount)}", color = Color(0xFFF9FAFB), fontSize = 14.sp)
                        }
                    }
                }

                // Grand Total Display
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total:", color = Color(0xFF9CA3AF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("₹ ${String.format("%.2f", grandTotal)}", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                // Amount Paid
                OutlinedTextField(
                    value = amountPaid,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) amountPaid = it },
                    label = { Text("Amount Paid (₹)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )

                // Pending/Credit Amount Display
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (pendingAmount >= 0) "Pending Amount:" else "Credit Amount:",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₹ ${String.format("%.2f", kotlin.math.abs(pendingAmount))}",
                            color = if (pendingAmount >= 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    maxLines = 3
                )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedEntity = customers.find { it.id == selectedEntityId }
                    val paidAmount = amountPaid.toDoubleOrNull() ?: 0.0
                    
                    // Auto-calculate payment status
                    val paymentStatus = when {
                        paidAmount >= grandTotal -> PaymentStatus.PAID
                        paidAmount > 0 -> PaymentStatus.PARTIALLY_PAID
                        else -> PaymentStatus.PENDING
                    }

                    onSave(
                        Purchase(
                            customerId = selectedEntityId,
                            firmName = selectedEntity?.firmName ?: "",
                            purchaseDate = purchaseDate,
                            items = purchaseItems,
                            totalAmount = totalAmount,
                            gstRate = gstRate,
                            gstAmount = gstAmount,
                            grandTotal = grandTotal,
                            paymentStatus = paymentStatus,
                            amountPaid = paidAmount,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
            ) {
                Text("Log Purchase", color = Color(0xFF111827))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9CA3AF))
            }
        }
    )
}

@Composable
fun PurchaseItemCard(
    item: PurchaseItem,
    index: Int,
    inventoryItems: List<InventoryItem>,
    onItemChanged: (PurchaseItem) -> Unit,
    onRemove: () -> Unit
) {
    var selectedInventoryItemId by remember { mutableStateOf(item.inventoryItemId) }
    var quantity by remember { mutableStateOf(if (item.quantity > 0) item.quantity.toString() else "") }
    var pricePerUnit by remember { mutableStateOf(if (item.pricePerUnit > 0) item.pricePerUnit.toString() else "") }

    val selectedInventoryItem = inventoryItems.find { it.id == selectedInventoryItemId }
    val totalPrice = (quantity.toDoubleOrNull() ?: 0.0) * (pricePerUnit.toDoubleOrNull() ?: 0.0)

    LaunchedEffect(selectedInventoryItemId, quantity, pricePerUnit) {
        if (selectedInventoryItem != null) {
            onItemChanged(
                PurchaseItem(
                    inventoryItemId = selectedInventoryItemId,
                    itemName = selectedInventoryItem.name,
                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                    unit = selectedInventoryItem.unit,
                    pricePerUnit = pricePerUnit.toDoubleOrNull() ?: 0.0,
                    totalPrice = totalPrice
                )
            )
        }
    }

    Card(
        backgroundColor = Color(0xFF111827),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                        modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                    "Item ${index + 1}",
                    color = Color(0xFF06B6D4),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444))
                }
            }

            // Inventory Item Dropdown
                var itemExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { itemExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                    Text(
                        text = selectedInventoryItem?.name ?: "Select Item",
                        modifier = Modifier.weight(1f),
                        color = Color.Black
                    )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = itemExpanded,
                        onDismissRequest = { itemExpanded = false }
                    ) {
                    inventoryItems.forEach { invItem ->
                            DropdownMenuItem(onClick = {
                            selectedInventoryItemId = invItem.id
                                itemExpanded = false
                            }) {
                            Text("${invItem.name} (${invItem.unit})")
                            }
                        }
                    }
                }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantity = it },
                    label = { Text("Quantity (${selectedInventoryItem?.unit ?: "unit"})", color = Color(0xFF9CA3AF), fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF1F2937),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )

                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) pricePerUnit = it },
                    label = { Text("Price/Unit (₹)", color = Color(0xFF9CA3AF), fontSize = 11.sp) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF1F2937),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                Text("Item Total:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                Text("₹ ${String.format("%.2f", totalPrice)}", color = Color(0xFF10B981), fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun EditPurchaseDialog(
    purchase: Purchase,
    customers: List<Entity>,
    inventoryItems: List<InventoryItem>,
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit
) {
    var selectedEntityId by remember { mutableStateOf(purchase.customerId) }
    var purchaseDate by remember { mutableStateOf(purchase.purchaseDate) }
    var gstRate by remember { mutableStateOf(purchase.gstRate) }
    var amountPaid by remember { mutableStateOf(purchase.amountPaid.toString()) }
    var notes by remember { mutableStateOf(purchase.notes) }
    var purchaseItems by remember { mutableStateOf(purchase.items) }

    val totalAmount = purchaseItems.sumOf { it.totalPrice }
    val gstAmount = totalAmount * (gstRate / 100.0)
    val grandTotal = totalAmount + gstAmount
    val paidAmount = amountPaid.toDoubleOrNull() ?: 0.0
    val pendingAmount = grandTotal - paidAmount

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Edit Purchase", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Box(
                modifier = Modifier
                    .width(700.dp)
                    .height(750.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(end = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Entity Dropdown (Disabled)
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9CA3AF),
                            disabledContentColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text(
                            text = customers.find { it.id == selectedEntityId }?.firmName ?: "Select Entity",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                // Purchase Date
                DatePicker(
                    selectedDate = try { LocalDate.parse(purchaseDate) } catch (e: Exception) { LocalDate.now() },
                    onDateSelected = { date -> 
                        purchaseDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    },
                    label = "Purchase Date",
                    modifier = Modifier.fillMaxWidth()
                )

                Divider(color = Color(0xFF374151))

                // Items Section
                Row(
                        modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Purchase Items",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF06B6D4)
                    )
                    Button(
                        onClick = {
                            purchaseItems = purchaseItems + PurchaseItem()
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Item", tint = Color.White)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Item", color = Color.White, fontSize = 12.sp)
                    }
                }

                purchaseItems.forEachIndexed { index, item ->
                    PurchaseItemCard(
                        item = item,
                        index = index,
                        inventoryItems = inventoryItems,
                        onItemChanged = { updatedItem ->
                            purchaseItems = purchaseItems.toMutableList().apply {
                                set(index, updatedItem)
                            }
                        },
                        onRemove = {
                            purchaseItems = purchaseItems.toMutableList().apply {
                                removeAt(index)
                            }
                        }
                    )
                }

                Divider(color = Color(0xFF374151))

                // Total Amount Display
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                            modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                            Text("Total Amount:", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                            Text("₹ ${String.format("%.2f", totalAmount)}", color = Color(0xFFF9FAFB), fontSize = 14.sp)
                        }
                    }
                }

                // GST Selection
                Text("GST Rate", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { gstRate = 0.0 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gstRate == 0.0) Color(0xFF06B6D4) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("0%")
                    }
                    OutlinedButton(
                        onClick = { gstRate = 5.0 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gstRate == 5.0) Color(0xFF06B6D4) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("5%")
                    }
                    OutlinedButton(
                        onClick = { gstRate = 18.0 },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (gstRate == 18.0) Color(0xFF06B6D4) else Color.Black
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("18%")
                    }
                }

                // GST Amount Display
                if (gstRate > 0) {
                    Card(
                        backgroundColor = Color(0xFF111827),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("GST Amount (${gstRate.toInt()}%):", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                            Text("₹ ${String.format("%.2f", gstAmount)}", color = Color(0xFFF9FAFB), fontSize = 14.sp)
                        }
                    }
                }

                // Grand Total Display
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Grand Total:", color = Color(0xFF9CA3AF), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("₹ ${String.format("%.2f", grandTotal)}", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }

                // Amount Paid
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) amountPaid = it },
                        label = { Text("Amount Paid (₹)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF111827),
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Color(0xFF374151),
                            cursorColor = Color(0xFF06B6D4)
                        ),
                        singleLine = true
                )

                // Pending/Credit Amount Display
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            if (pendingAmount >= 0) "Pending Amount:" else "Credit Amount:",
                            color = Color(0xFF9CA3AF),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "₹ ${String.format("%.2f", kotlin.math.abs(pendingAmount))}",
                            color = if (pendingAmount >= 0) Color(0xFFF59E0B) else Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    maxLines = 3
                )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedEntity = customers.find { it.id == selectedEntityId }
                    val paidAmount = amountPaid.toDoubleOrNull() ?: 0.0
                    
                    // Auto-calculate payment status
                    val paymentStatus = when {
                        paidAmount >= grandTotal -> PaymentStatus.PAID
                        paidAmount > 0 -> PaymentStatus.PARTIALLY_PAID
                        else -> PaymentStatus.PENDING
                    }

                    onSave(
                        Purchase(
                            customerId = selectedEntityId,
                            firmName = selectedEntity?.firmName ?: purchase.firmName,
                            purchaseDate = purchaseDate,
                            items = purchaseItems,
                            totalAmount = totalAmount,
                            gstRate = gstRate,
                            gstAmount = gstAmount,
                            grandTotal = grandTotal,
                            paymentStatus = paymentStatus,
                            amountPaid = paidAmount,
                            notes = notes
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
            ) {
                Text("Update Purchase", color = Color(0xFF111827))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9CA3AF))
            }
        }
    )
}

@Composable
fun ViewPurchaseDialog(
    purchase: Purchase,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Purchase Details", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.width(600.dp).height(600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Purchase Information", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow("Purchase Date", purchase.purchaseDate)
                        DetailRow("Entity", purchase.firmName)
                    }
                }

                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Items Purchased", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))

                        purchase.items.forEach { item ->
                            Card(
                                backgroundColor = Color(0xFF1F2937),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.itemName, color = Color(0xFF06B6D4), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Quantity:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                        Text("${String.format("%.2f", item.quantity)} ${item.unit}", color = Color(0xFFF9FAFB), fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Price/Unit:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                                        Text("₹ ${String.format("%.2f", item.pricePerUnit)}", color = Color(0xFFF9FAFB), fontSize = 12.sp)
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("Total:", color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("₹ ${String.format("%.2f", item.totalPrice)}", color = Color(0xFF10B981), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                    }
                }

                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Payment Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                        DetailRow("Total Amount", "₹ ${String.format("%.2f", purchase.totalAmount)}")
                        if (purchase.gstRate > 0) {
                            DetailRow("GST (${purchase.gstRate.toInt()}%)", "₹ ${String.format("%.2f", purchase.gstAmount)}")
                        }
                        DetailRow("Grand Total", "₹ ${String.format("%.2f", purchase.grandTotal)}", valueColor = Color(0xFF06B6D4))
                        DetailRow(
                            "Payment Status",
                            purchase.paymentStatus.name.replace("_", " "),
                            valueColor = when (purchase.paymentStatus) {
                                PaymentStatus.PAID -> Color(0xFF10B981)
                                PaymentStatus.PENDING -> Color(0xFFF59E0B)
                                PaymentStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                            }
                        )
                        DetailRow("Amount Paid", "₹ ${String.format("%.2f", purchase.amountPaid)}")
                        if (purchase.grandTotal - purchase.amountPaid > 0) {
                            DetailRow("Balance", "₹ ${String.format("%.2f", purchase.grandTotal - purchase.amountPaid)}", valueColor = Color(0xFFEF4444))
                        }
                    }
                }

                        if (purchase.notes.isNotBlank()) {
                    Card(
                        backgroundColor = Color(0xFF111827),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                            Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                            Text(purchase.notes, color = Color(0xFFF9FAFB), fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
            ) {
                Text("Close", color = Color(0xFF111827))
            }
        }
    )
}

@Composable
fun DetailRow(label: String, value: String, valueColor: Color = Color(0xFFF9FAFB)) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "$label:", color = Color(0xFF9CA3AF), fontSize = 14.sp)
        Text(text = value, color = valueColor, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}