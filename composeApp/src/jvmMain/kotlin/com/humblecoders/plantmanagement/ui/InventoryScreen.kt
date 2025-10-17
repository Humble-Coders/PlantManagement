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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import com.humblecoders.plantmanagement.data.CategoryType
import com.humblecoders.plantmanagement.data.InventoryItem
import com.humblecoders.plantmanagement.data.UserRole
import com.humblecoders.plantmanagement.viewmodels.InventoryViewModel
import com.humblecoders.plantmanagement.utils.PdfExportUtils

@Composable
fun InventoryScreen(inventoryViewModel: InventoryViewModel, userRole: UserRole? = null) {
    val inventoryState = inventoryViewModel.inventoryState
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var itemToEdit by remember { mutableStateOf<InventoryItem?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var itemToDelete by remember { mutableStateOf<InventoryItem?>(null) }
    
    val isAdmin = userRole == UserRole.ADMIN

    LaunchedEffect(inventoryState.successMessage, inventoryState.error) {
        if (inventoryState.successMessage != null || inventoryState.error != null) {
            kotlinx.coroutines.delay(3000)
            inventoryViewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                text = "Inventory Management",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (inventoryState.successMessage != null) {
            Card(
                backgroundColor = Color(0xFF10B981),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = inventoryState.successMessage,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (inventoryState.error != null) {
            Card(
                backgroundColor = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = inventoryState.error,
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
                        text = "Add New Item",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        enabled = !inventoryState.isAdding && !inventoryState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Add Item", color = Color(0xFF111827))
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
                        text = "Inventory List",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inventoryState.searchQuery,
                            onValueChange = { inventoryViewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search items...", color = Color(0xFF9CA3AF)) },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                            },
                            modifier = Modifier.width(250.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF06B6D4)
                            ),
                            singleLine = true
                        )

                        var categoryExpanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { categoryExpanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Black
                                )
                            ) {
                                Text(
                                    text = inventoryState.filterCategory?.name?.replace("_", " ") ?: "All Categories"
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = categoryExpanded,
                                onDismissRequest = { categoryExpanded = false }
                            ) {
                                DropdownMenuItem(onClick = {
                                    inventoryViewModel.updateFilterCategory(null)
                                    categoryExpanded = false
                                }) {
                                    Text("All Categories")
                                }
                                DropdownMenuItem(onClick = {
                                    inventoryViewModel.updateFilterCategory(CategoryType.RAW_MATERIAL)
                                    categoryExpanded = false
                                }) {
                                    Text("Raw Material")
                                }
                                DropdownMenuItem(onClick = {
                                    inventoryViewModel.updateFilterCategory(CategoryType.OTHER)
                                    categoryExpanded = false
                                }) {
                                    Text("Other")
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Print/Download Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val filteredItems = inventoryViewModel.getFilteredItems()
                            PdfExportUtils.exportInventoryItems(filteredItems)
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF10B981),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Print,
                            contentDescription = "Print",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Print/Download Inventory")
                    }
                }

                InventoryTable(
                    items = inventoryViewModel.getFilteredItems(),
                    onEditClick = {
                        itemToEdit = it
                        showEditDialog = true
                    },
                    onDeleteClick = {
                        itemToDelete = it
                        showDeleteConfirmDialog = true
                    },
                    isAdmin = isAdmin
                )
            }
        }
        }

        // Loading indicator overlay
        if (inventoryState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    backgroundColor = Color(0xFF1F2937),
                    shape = RoundedCornerShape(16.dp),
                    elevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF06B6D4),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = when {
                                inventoryState.isAdding -> "Adding item..."
                                inventoryState.isUpdating -> "Updating item..."
                                inventoryState.isDeleting -> "Deleting item..."
                                else -> "Processing..."
                            },
                            color = Color(0xFFF9FAFB),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddInventoryItemDialog(
            onDismiss = { showAddDialog = false },
            onSave = { item ->
                inventoryViewModel.addInventoryItem(item)
                showAddDialog = false
            }
        )
    }

    if (showEditDialog && itemToEdit != null) {
        EditInventoryItemDialog(
            item = itemToEdit!!,
            onDismiss = {
                showEditDialog = false
                itemToEdit = null
            },
            onSave = { item ->
                inventoryViewModel.updateInventoryItem(itemToEdit!!.id, item)
                showEditDialog = false
                itemToEdit = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && itemToDelete != null) {
        DeleteConfirmationDialog(
            itemName = itemToDelete!!.name,
            itemType = "inventory item",
            onConfirm = {
                inventoryViewModel.deleteInventoryItem(itemToDelete!!.id)
                showDeleteConfirmDialog = false
                itemToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                itemToDelete = null
            }
        )
    }
}

@Composable
fun InventoryTable(
    items: List<InventoryItem>,
    onEditClick: (InventoryItem) -> Unit,
    onDeleteClick: (InventoryItem) -> Unit,
    isAdmin: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(12.dp)
        ) {
            Text("Name", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.30f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Quantity", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Avg. Purchase Price", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Unit", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Category", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Actions", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color(0xFF374151))

        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(item.name, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.30f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(String.format("%.2f", item.quantity), color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.20f), fontSize = 14.sp)
                Text(String.format("₹ %.2f", item.averagePurchasePrice), color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.20f), fontSize = 14.sp)
                Text(item.unit, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.15f), fontSize = 14.sp)
                Text(
                    text = item.categoryType.name.replace("_", " "),
                    color = if (item.categoryType == CategoryType.RAW_MATERIAL) Color(0xFF10B981) else Color(0xFF3B82F6),
                    modifier = Modifier.weight(0.20f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.weight(0.15f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isAdmin) {
                        TextButton(onClick = { onEditClick(item) }) {
                            Text("Edit", color = Color(0xFF10B981), fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDeleteClick(item) }) {
                            Text("Delete", color = Color(0xFFEF4444), fontSize = 12.sp)
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = Color(0xFF6B7280),
                                modifier = Modifier.size(14.dp)
                            )
                            Text("Locked", color = Color(0xFF6B7280), fontSize = 11.sp)
                        }
                    }
                }
            }
            Divider(color = Color(0xFF374151))
        }

        if (items.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No inventory items found", color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun AddInventoryItemDialog(
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(CategoryType.RAW_MATERIAL) }
    var selectedUnitType by remember { mutableStateOf("kg") }
    var customUnit by remember { mutableStateOf("") }

    val focusRequesters = remember { List(3) { FocusRequester() } }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(550.dp)
                .height(600.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                        "Add Inventory Item",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[0])
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                focusRequesters[1].requestFocus()
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
                    }

                    item {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantity = it },
                            label = { Text("Quantity", color = Color(0xFF9CA3AF)) },
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
                    }

                    item {
                        Text("Unit Type", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedUnitType = "kg" },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedUnitType == "kg") Color(0xFF06B6D4) else Color.Black
                                )
                            ) {
                                Text("kg")
                            }
                            OutlinedButton(
                                onClick = { selectedUnitType = "other" },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedUnitType == "other") Color(0xFF06B6D4) else Color.Black
                                )
                            ) {
                                Text("Other")
                            }
                        }
                    }

                    if (selectedUnitType == "other") {
                        item {
                            OutlinedTextField(
                                value = customUnit,
                                onValueChange = { customUnit = it },
                                label = { Text("Specify Unit (e.g., metre, litre)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[2]),
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
                    }

                    item {
                        Text("Category Type", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedCategory = CategoryType.RAW_MATERIAL },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedCategory == CategoryType.RAW_MATERIAL) Color(0xFF10B981) else Color.Black
                                )
                            ) {
                                Text("Raw Material")
                            }
                            OutlinedButton(
                                onClick = { selectedCategory = CategoryType.OTHER },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedCategory == CategoryType.OTHER) Color(0xFF3B82F6) else Color.Black
                                )
                            ) {
                                Text("Other")
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF9CA3AF))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val finalUnit = if (selectedUnitType == "kg") "kg" else customUnit
                            onSave(
                                InventoryItem(
                                    name = name,
                                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                                    unit = finalUnit,
                                    categoryType = selectedCategory
                                )
                            )
                        },
                        enabled = name.isNotBlank() && quantity.isNotBlank() && 
                                 (selectedUnitType == "kg" || customUnit.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Add Item", color = Color(0xFF111827))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}

@Composable
fun EditInventoryItemDialog(
    item: InventoryItem,
    onDismiss: () -> Unit,
    onSave: (InventoryItem) -> Unit
) {
    var name by remember { mutableStateOf(item.name) }
    var quantity by remember { mutableStateOf(item.quantity.toString()) }
    var selectedCategory by remember { mutableStateOf(item.categoryType) }
    var selectedUnitType by remember { mutableStateOf(if (item.unit == "kg") "kg" else "other") }
    var customUnit by remember { mutableStateOf(if (item.unit == "kg") "" else item.unit) }

    val focusRequesters = remember { List(3) { FocusRequester() } }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(550.dp)
                .height(600.dp),
            backgroundColor = Color(0xFF1F2937),
            shape = RoundedCornerShape(16.dp),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
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
                        "Edit Inventory Item",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF9CA3AF)
                        )
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
                        OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Item Name", color = Color(0xFF9CA3AF)) },
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[0])
                        .onKeyEvent { event ->
                            if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                focusRequesters[1].requestFocus()
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
                    }

                    item {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantity = it },
                            label = { Text("Quantity", color = Color(0xFF9CA3AF)) },
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
                    }

                    item {
                        Text("Unit Type", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedUnitType = "kg" },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedUnitType == "kg") Color(0xFF06B6D4) else Color.Black
                                )
                            ) {
                                Text("kg")
                            }
                            OutlinedButton(
                                onClick = { selectedUnitType = "other" },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedUnitType == "other") Color(0xFF06B6D4) else Color.Black
                                )
                            ) {
                                Text("Other")
                            }
                        }
                    }

                    if (selectedUnitType == "other") {
                        item {
                            OutlinedTextField(
                                value = customUnit,
                                onValueChange = { customUnit = it },
                                label = { Text("Specify Unit (e.g., metre, litre)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[2]),
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
                    }

                    item {
                        Text("Category Type", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold)
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { selectedCategory = CategoryType.RAW_MATERIAL },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedCategory == CategoryType.RAW_MATERIAL) Color(0xFF10B981) else Color.Black
                                )
                            ) {
                                Text("Raw Material")
                            }
                            OutlinedButton(
                                onClick = { selectedCategory = CategoryType.OTHER },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (selectedCategory == CategoryType.OTHER) Color(0xFF3B82F6) else Color.Black
                                )
                            ) {
                                Text("Other")
                            }
                        }
                    }
                }

                // Footer Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color(0xFF9CA3AF))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = {
                            val finalUnit = if (selectedUnitType == "kg") "kg" else customUnit
                            onSave(
                                InventoryItem(
                                    name = name,
                                    quantity = quantity.toDoubleOrNull() ?: 0.0,
                                    unit = finalUnit,
                                    categoryType = selectedCategory
                                )
                            )
                        },
                        enabled = name.isNotBlank() && quantity.isNotBlank() && 
                                 (selectedUnitType == "kg" || customUnit.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Update Item", color = Color(0xFF111827))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}