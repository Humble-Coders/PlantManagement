package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Search
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
import com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel
import com.humblecoders.plantmanagement.viewmodels.PurchaseSortField
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.humblecoders.plantmanagement.ui.components.DatePicker

@Composable
fun PurchaseScreen(
    purchaseViewModel: PurchaseViewModel,
    entityViewModel: EntityViewModel
) {
    val purchaseState = purchaseViewModel.purchaseState
    val entityState = entityViewModel.entityState
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
                                    purchaseViewModel.updateSortBy(PurchaseSortField.CUSTOMER)
                                    sortExpanded = false
                                }) {
                                    Text("Customer")
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

// Continue in PurchaseScreen.kt

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
            Text("Customer", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.18f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Item", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Qty", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.10f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Price/Unit", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Total", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Status", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Actions", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text(purchase.firmName, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.18f), fontSize = 14.sp)
                Text(purchase.itemName, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.12f), fontSize = 14.sp)
                Text("${String.format("%.2f", purchase.quantity)} ${purchase.unit}", color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.10f), fontSize = 14.sp)
                Text("₹ ${String.format("%.2f", purchase.pricePerUnit)}", color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.12f), fontSize = 14.sp)
                Text("₹ ${String.format("%.2f", purchase.totalAmount)}", color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.12f), fontSize = 14.sp)

                Text(
                    text = purchase.paymentStatus.name.replace("_", " "),
                    color = when (purchase.paymentStatus) {
                        PaymentStatus.PAID -> Color(0xFF10B981)
                        PaymentStatus.PENDING -> Color(0xFFF59E0B)
                        PaymentStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                    },
                    modifier = Modifier.weight(0.12f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.weight(0.12f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onEditClick(purchase) }) {
                        Text("Edit", color = Color(0xFF10B981), fontSize = 12.sp)
                    }
                    TextButton(onClick = { onViewClick(purchase) }) {
                        Text("View", color = Color(0xFF3B82F6), fontSize = 12.sp)
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
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit
) {
    var selectedCustomerId by remember { mutableStateOf("") }
    var purchaseDate by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var selectedItem by remember { mutableStateOf("RICE") }
    var quantity by remember { mutableStateOf("") }
    var pricePerUnit by remember { mutableStateOf("") }
    var paymentStatus by remember { mutableStateOf(PaymentStatus.PENDING) }
    var amountPaid by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    val itemNames = listOf("RICE", "RICE1", "PREMIX", "PACKAGING", "OTHERS", "FORTIFIED RICE")
    val focusRequesters = remember { List(6) { FocusRequester() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Log New Purchase", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.width(600.dp).height(600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Dropdown
                var customerExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { customerExpanded = true },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[0]),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = if (selectedCustomerId.isBlank()) "Select Customer"
                            else customers.find { it.id == selectedCustomerId }?.firmName ?: "Select Customer",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = customerExpanded,
                        onDismissRequest = { customerExpanded = false }
                    ) {
                        customers.forEach { customer ->
                            DropdownMenuItem(onClick = {
                                selectedCustomerId = customer.id
                                customerExpanded = false
                            }) {
                                Text(customer.firmName)
                            }
                        }
                    }
                }

                // Purchase Date
                DatePicker(
                    selectedDate = try { LocalDate.parse(purchaseDate) } catch (e: Exception) { LocalDate.now() },
                    onDateSelected = { date -> 
                        purchaseDate = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                        focusRequesters[2].requestFocus()
                    },
                    label = "Purchase Date",
                    modifier = Modifier.fillMaxWidth()
                )

                // Item Name Dropdown
                var itemExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { itemExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = selectedItem, modifier = Modifier.weight(1f), color = Color.Black)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = itemExpanded,
                        onDismissRequest = { itemExpanded = false }
                    ) {
                        itemNames.forEach { item ->
                            DropdownMenuItem(onClick = {
                                selectedItem = item
                                itemExpanded = false
                            }) {
                                Text(item)
                            }
                        }
                    }
                }

                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantity = it },
                    label = { Text("Quantity (kg)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[2])
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                focusRequesters[3].requestFocus()
                                true
                            } else false
                        },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )

                // Price Per Unit
                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) pricePerUnit = it },
                    label = { Text("Price Per Unit (₹)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[3])
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                focusRequesters[4].requestFocus()
                                true
                            } else false
                        },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )

                // Total Amount Display
                val totalAmount = (quantity.toDoubleOrNull() ?: 0.0) * (pricePerUnit.toDoubleOrNull() ?: 0.0)
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", color = Color(0xFF9CA3AF))
                        Text("₹ ${String.format("%.2f", totalAmount)}", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                    }
                }

                // Payment Status
                Text("Payment Status", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { paymentStatus = PaymentStatus.PENDING },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (paymentStatus == PaymentStatus.PENDING) Color(0xFFF59E0B) else Color.Black
                        )
                    ) {
                        Text("Pending")
                    }
                    OutlinedButton(
                        onClick = { paymentStatus = PaymentStatus.PAID },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (paymentStatus == PaymentStatus.PAID) Color(0xFF10B981) else Color.Black
                        )
                    ) {
                        Text("Paid")
                    }
                    OutlinedButton(
                        onClick = { paymentStatus = PaymentStatus.PARTIALLY_PAID },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (paymentStatus == PaymentStatus.PARTIALLY_PAID) Color(0xFF3B82F6) else Color.Black
                        )
                    ) {
                        Text("Partially Paid")
                    }
                }

                // Amount Paid (if Partially Paid)
                if (paymentStatus == PaymentStatus.PARTIALLY_PAID) {
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) amountPaid = it },
                        label = { Text("Amount Paid (₹)", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[4]),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF111827),
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Color(0xFF374151),
                            cursorColor = Color(0xFF06B6D4)
                        ),
                        singleLine = true
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp).focusRequester(focusRequesters[5]),
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedCustomer = customers.find { it.id == selectedCustomerId }
                    val qtyValue = quantity.toDoubleOrNull() ?: 0.0
                    val priceValue = pricePerUnit.toDoubleOrNull() ?: 0.0
                    val totalValue = qtyValue * priceValue
                    val paidAmount = when (paymentStatus) {
                        PaymentStatus.PAID -> totalValue
                        PaymentStatus.PARTIALLY_PAID -> amountPaid.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    onSave(
                        Purchase(
                            customerId = selectedCustomerId,
                            firmName = selectedCustomer?.firmName ?: "",
                            purchaseDate = purchaseDate,
                            itemName = selectedItem,
                            quantity = qtyValue,
                            unit = "kg",
                            pricePerUnit = priceValue,
                            totalAmount = totalValue,
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

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}

@Composable
fun EditPurchaseDialog(
    purchase: Purchase,
    customers: List<Entity>,
    onDismiss: () -> Unit,
    onSave: (Purchase) -> Unit
) {
    var selectedCustomerId by remember { mutableStateOf(purchase.customerId) }
    var purchaseDate by remember { mutableStateOf(purchase.purchaseDate) }
    var selectedItem by remember { mutableStateOf(purchase.itemName) }
    var quantity by remember { mutableStateOf(purchase.quantity.toString()) }
    var pricePerUnit by remember { mutableStateOf(purchase.pricePerUnit.toString()) }
    var paymentStatus by remember { mutableStateOf(purchase.paymentStatus) }
    var amountPaid by remember { mutableStateOf(purchase.amountPaid.toString()) }
    var notes by remember { mutableStateOf(purchase.notes) }

    val itemNames = listOf("RICE", "RICE1", "PREMIX", "PACKAGING", "OTHERS", "FORTIFIED RICE")
    val focusRequesters = remember { List(6) { FocusRequester() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Edit Purchase", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.width(600.dp).height(600.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Customer Dropdown (Disabled in edit)
                var customerExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { customerExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF9CA3AF),
                            disabledContentColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text(
                            text = customers.find { it.id == selectedCustomerId }?.firmName ?: "Select Customer",
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
                        focusRequesters[1].requestFocus()
                    },
                    label = "Purchase Date",
                    modifier = Modifier.fillMaxWidth()
                )

                // Item Name Dropdown
                var itemExpanded by remember { mutableStateOf(false) }
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(
                        onClick = { itemExpanded = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Black
                        )
                    ) {
                        Text(text = selectedItem, modifier = Modifier.weight(1f), color = Color.Black)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }

                    DropdownMenu(
                        expanded = itemExpanded,
                        onDismissRequest = { itemExpanded = false }
                    ) {
                        itemNames.forEach { item ->
                            DropdownMenuItem(onClick = {
                                selectedItem = item
                                itemExpanded = false
                            }) {
                                Text(item)
                            }
                        }
                    }
                }

                // Quantity
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantity = it },
                    label = { Text("Quantity (kg)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[1])
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                focusRequesters[2].requestFocus()
                                true
                            } else false
                        },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )

                // Price Per Unit
                OutlinedTextField(
                    value = pricePerUnit,
                    onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) pricePerUnit = it },
                    label = { Text("Price Per Unit (₹)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[2])
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                focusRequesters[3].requestFocus()
                                true
                            } else false
                        },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        textColor = Color(0xFFF9FAFB),
                        backgroundColor = Color(0xFF111827),
                        focusedBorderColor = Color(0xFF06B6D4),
                        unfocusedBorderColor = Color(0xFF374151),
                        cursorColor = Color(0xFF06B6D4)
                    ),
                    singleLine = true
                )

                // Total Amount Display
                val totalAmount = (quantity.toDoubleOrNull() ?: 0.0) * (pricePerUnit.toDoubleOrNull() ?: 0.0)
                Card(
                    backgroundColor = Color(0xFF111827),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Amount:", color = Color(0xFF9CA3AF))
                        Text("₹ ${String.format("%.2f", totalAmount)}", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                    }
                }

                // Payment Status
                Text("Payment Status", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { paymentStatus = PaymentStatus.PENDING },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (paymentStatus == PaymentStatus.PENDING) Color(0xFFF59E0B) else Color.Black
                        )
                    ) {
                        Text("Pending")
                    }
                    OutlinedButton(
                        onClick = { paymentStatus = PaymentStatus.PAID },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (paymentStatus == PaymentStatus.PAID) Color(0xFF10B981) else Color.Black
                        )
                    ) {
                        Text("Paid")
                    }
                    OutlinedButton(
                        onClick = { paymentStatus = PaymentStatus.PARTIALLY_PAID },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (paymentStatus == PaymentStatus.PARTIALLY_PAID) Color(0xFF3B82F6) else Color.Black
                        )
                    ) {
                        Text("Partially Paid")
                    }
                }

                // Amount Paid
                if (paymentStatus == PaymentStatus.PARTIALLY_PAID) {
                    OutlinedTextField(
                        value = amountPaid,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) amountPaid = it },
                        label = { Text("Amount Paid (₹)", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[3]),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF111827),
                            focusedBorderColor = Color(0xFF06B6D4),
                            unfocusedBorderColor = Color(0xFF374151),
                            cursorColor = Color(0xFF06B6D4)
                        ),
                        singleLine = true
                    )
                }

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().height(100.dp).focusRequester(focusRequesters[4]),
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
        },
        confirmButton = {
            Button(
                onClick = {
                    val selectedCustomer = customers.find { it.id == selectedCustomerId }
                    val qtyValue = quantity.toDoubleOrNull() ?: 0.0
                    val priceValue = pricePerUnit.toDoubleOrNull() ?: 0.0
                    val totalValue = qtyValue * priceValue
                    val paidAmount = when (paymentStatus) {
                        PaymentStatus.PAID -> totalValue
                        PaymentStatus.PARTIALLY_PAID -> amountPaid.toDoubleOrNull() ?: 0.0
                        else -> 0.0
                    }

                    onSave(
                        Purchase(
                            customerId = selectedCustomerId,
                            firmName = selectedCustomer?.firmName ?: purchase.firmName,
                            purchaseDate = purchaseDate,
                            itemName = selectedItem,
                            quantity = qtyValue,
                            unit = "kg",
                            pricePerUnit = priceValue,
                            totalAmount = totalValue,
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

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
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
                modifier = Modifier.width(500.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        DetailRow("Purchase Date", purchase.purchaseDate)
                        DetailRow("Customer", purchase.firmName)
                        DetailRow("Item", purchase.itemName)
                        DetailRow("Quantity", "${String.format("%.2f", purchase.quantity)} ${purchase.unit}")
                        DetailRow("Price Per Unit", "₹ ${String.format("%.2f", purchase.pricePerUnit)}")
                        DetailRow("Total Amount", "₹ ${String.format("%.2f", purchase.totalAmount)}")
                        DetailRow(
                            "Payment Status",
                            purchase.paymentStatus.name.replace("_", " "),
                            valueColor = when (purchase.paymentStatus) {
                                PaymentStatus.PAID -> Color(0xFF10B981)
                                PaymentStatus.PENDING -> Color(0xFFF59E0B)
                                PaymentStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                            }
                        )
                        if (purchase.paymentStatus == PaymentStatus.PARTIALLY_PAID) {
                            DetailRow("Amount Paid", "₹ ${String.format("%.2f", purchase.amountPaid)}")
                        }
                        if (purchase.notes.isNotBlank()) {
                            Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                            Text("Notes:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
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