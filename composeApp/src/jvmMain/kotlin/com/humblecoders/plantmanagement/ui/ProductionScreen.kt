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
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.ProductionViewModel
import com.humblecoders.plantmanagement.viewmodels.InventoryViewModel
import com.humblecoders.plantmanagement.ui.components.DatePicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ProductionScreen(
    productionViewModel: ProductionViewModel,
    inventoryViewModel: InventoryViewModel,
    userRole: UserRole? = null
) {
    val productionState = productionViewModel.productionState
    val inventoryState = inventoryViewModel.inventoryState
    val isAdmin = userRole == UserRole.ADMIN
    
    // Local dialog state management
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var recordToEdit by remember { mutableStateOf<ProductionRecord?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var recordToDelete by remember { mutableStateOf<ProductionRecord?>(null) }

    LaunchedEffect(productionState.successMessage, productionState.error) {
        if (productionState.successMessage != null || productionState.error != null) {
            kotlinx.coroutines.delay(3000)
            productionViewModel.clearMessages()
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
                    text = "Production Management",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (productionState.successMessage != null) {
                Card(
                    backgroundColor = Color(0xFF10B981),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = productionState.successMessage,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (productionState.error != null) {
                Card(
                    backgroundColor = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = productionState.error,
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
                            text = "Add New Production Record",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )

                        Button(
                            onClick = { showAddDialog = true },
                            enabled = !productionState.isAdding && !productionState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF06B6D4),
                                disabledBackgroundColor = Color(0xFF9CA3AF)
                            )
                        ) {
                            Text("Add Production", color = Color(0xFF111827))
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
                            text = "Production Records",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )

                        OutlinedTextField(
                            value = productionState.searchQuery,
                            onValueChange = { productionViewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search records...", color = Color(0xFF9CA3AF)) },
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
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ProductionRecordsTable(
                        records = productionViewModel.getFilteredRecords(),
                        onEditClick = { 
                            recordToEdit = it
                            showEditDialog = true
                        },
                        onDeleteClick = { 
                            recordToDelete = it
                            showDeleteConfirmDialog = true
                        },
                        isAdmin = isAdmin
                    )
                }
            }
        }

        // Loading indicator overlay
        if (productionState.isLoading) {
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
                                productionState.isAdding -> "Adding production record..."
                                productionState.isUpdating -> "Updating production record..."
                                productionState.isDeleting -> "Deleting production record..."
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

    // Add Production Dialog
    if (showAddDialog) {
        AddProductionDialog(
            inventoryViewModel = inventoryViewModel,
            onDismiss = { showAddDialog = false },
            onSave = { productionInput ->
                productionViewModel.addProductionRecord(productionInput)
                showAddDialog = false
            }
        )
    }

    // Edit Production Dialog
    if (showEditDialog && recordToEdit != null) {
        EditProductionDialog(
            record = recordToEdit!!,
            inventoryViewModel = inventoryViewModel,
            onDismiss = {
                showEditDialog = false
                recordToEdit = null
            },
            onSave = { productionInput ->
                productionViewModel.updateProductionRecord(recordToEdit!!.id, productionInput)
                showEditDialog = false
                recordToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && recordToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "Production Record ${recordToDelete!!.batchNumber}",
            itemType = "production record",
            onConfirm = {
                productionViewModel.deleteProductionRecord(recordToDelete!!.id)
                showDeleteConfirmDialog = false
                recordToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                recordToDelete = null
            }
        )
    }
}

@Composable
fun ProductionRecordsTable(
    records: List<ProductionRecord>,
    onEditClick: (ProductionRecord) -> Unit,
    onDeleteClick: (ProductionRecord) -> Unit,
    isAdmin: Boolean
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(12.dp)
        ) {
            Text("Batch Number", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Quantity Produced", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Date", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Supervisor", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Raw Materials", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Waste", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.12f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Actions", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.14f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color(0xFF374151))

        records.forEach { record ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(record.batchNumber, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.15f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text("${String.format("%.2f", record.quantityProduced)} kg", color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.12f), fontSize = 14.sp)
                Text(
                    record.productionDate?.let { 
                        java.time.LocalDateTime.ofInstant(
                            java.time.Instant.ofEpochMilli(it.seconds * 1000),
                            java.time.ZoneId.systemDefault()
                        ).toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    } ?: "N/A", 
                    color = Color(0xFFF9FAFB), 
                    modifier = Modifier.weight(0.12f), 
                    fontSize = 14.sp
                )
                Text(record.supervisorName, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.15f), fontSize = 14.sp)
                Text(
                    "${record.rawMaterialsUsed.size} items", 
                    color = Color(0xFF10B981), 
                    modifier = Modifier.weight(0.20f), 
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    record.wasteTracking?.let { "${String.format("%.2f", it.getTotalWaste())} kg" } ?: "0.00 kg", 
                    color = Color(0xFFEF4444), 
                    modifier = Modifier.weight(0.12f), 
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.weight(0.14f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isAdmin) {
                        TextButton(onClick = { onEditClick(record) }) {
                            Text("Edit", color = Color(0xFF10B981), fontSize = 12.sp)
                        }
                        TextButton(onClick = { onDeleteClick(record) }) {
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

        if (records.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No production records found", color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun AddProductionDialog(
    inventoryViewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    onSave: (ProductionInput) -> Unit
) {
    var batchNumber by remember { mutableStateOf("") }
    var quantityProduced by remember { mutableStateOf("") }
    var supervisorName by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var rawMaterialsUsed by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var showWasteTracking by remember { mutableStateOf(false) }
    var wastage by remember { mutableStateOf("0") }
    var burn by remember { mutableStateOf("0") }
    var regrind by remember { mutableStateOf("0") }
    var others by remember { mutableStateOf("0") }

    val focusRequesters = remember { List(4) { FocusRequester() } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .height(700.dp),
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
                        "Add Production Record",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = batchNumber,
                                onValueChange = { batchNumber = it },
                                label = { Text("Batch Number", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[0])
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

                            OutlinedTextField(
                                value = quantityProduced,
                                onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantityProduced = it },
                                label = { Text("Quantity Produced (kg)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[1])
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
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = supervisorName,
                                onValueChange = { supervisorName = it },
                                label = { Text("Supervisor Name", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[2])
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

                            OutlinedTextField(
                                value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                onValueChange = { },
                                label = { Text("Production Date", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF06B6D4),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF06B6D4)
                                ),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color(0xFF06B6D4))
                                    }
                                }
                            )
                        }
                    }

                    item {
                        RawMaterialsSelection(
                            inventoryViewModel = inventoryViewModel,
                            rawMaterialsUsed = rawMaterialsUsed,
                            onRawMaterialsChanged = { rawMaterialsUsed = it },
                            quantityProduced = quantityProduced.toDoubleOrNull() ?: 0.0,
                            onShowWasteTracking = { showWasteTracking = it }
                        )
                    }

                    if (showWasteTracking) {
                        item {
                            WasteTrackingSection(
                                wastage = wastage,
                                burn = burn,
                                regrind = regrind,
                                others = others,
                                onWastageChange = { wastage = it },
                                onBurnChange = { burn = it },
                                onRegrindChange = { regrind = it },
                                onOthersChange = { others = it },
                                productionDifference = calculateProductionDifference(rawMaterialsUsed, quantityProduced.toDoubleOrNull() ?: 0.0)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[3]),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF06B6D4)
                            ),
                            minLines = 2,
                            maxLines = 4
                        )
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
                            val wasteTracking = if (showWasteTracking) {
                                WasteTracking(
                                    wastage = wastage.toDoubleOrNull() ?: 0.0,
                                    burn = burn.toDoubleOrNull() ?: 0.0,
                                    regrind = regrind.toDoubleOrNull() ?: 0.0,
                                    others = others.toDoubleOrNull() ?: 0.0
                                )
                            } else null

                            onSave(
                                ProductionInput(
                                    batchNumber = batchNumber,
                                    quantityProduced = quantityProduced.toDoubleOrNull() ?: 0.0,
                                    productionDate = selectedDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    supervisorName = supervisorName,
                                    notes = notes,
                                    rawMaterialsUsed = rawMaterialsUsed,
                                    wasteTracking = wasteTracking
                                )
                            )
                        },
                        enabled = batchNumber.isNotBlank() && quantityProduced.isNotBlank() && 
                                 supervisorName.isNotBlank() && rawMaterialsUsed.isNotEmpty() &&
                                 (!showWasteTracking || (wastage.isNotBlank() && burn.isNotBlank() && regrind.isNotBlank() && others.isNotBlank())),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Add Production", color = Color(0xFF111827))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDatePicker = false }) {
            Card(
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Select Production Date",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DatePicker(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4)
                        )
                    ) {
                        Text("OK", color = Color(0xFF111827))
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
fun RawMaterialsSelection(
    inventoryViewModel: InventoryViewModel,
    rawMaterialsUsed: Map<String, Double>,
    onRawMaterialsChanged: (Map<String, Double>) -> Unit,
    quantityProduced: Double,
    onShowWasteTracking: (Boolean) -> Unit
) {
    val rawMaterials = inventoryViewModel.getFilteredItems().filter { it.categoryType == CategoryType.RAW_MATERIAL }
    
    Column {
        Text(
            "Raw Materials Used",
            color = Color(0xFFF9FAFB),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        rawMaterials.forEach { material ->
            val currentQuantity = rawMaterialsUsed[material.id] ?: 0.0
            val isExceedingInventory = currentQuantity > material.quantity
            
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${material.name} (${material.unit})",
                            color = Color(0xFFF9FAFB),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Available: ${String.format("%.2f", material.quantity)} ${material.unit}",
                            color = Color(0xFF9CA3AF),
                            fontSize = 12.sp
                        )
                    }
                    
                    OutlinedTextField(
                        value = if (currentQuantity > 0) currentQuantity.toString() else "",
                        onValueChange = { value ->
                            val quantity = value.toDoubleOrNull() ?: 0.0
                            val updatedMap = if (quantity > 0) {
                                rawMaterialsUsed.toMutableMap().apply { put(material.id, quantity) }
                            } else {
                                rawMaterialsUsed.toMutableMap().apply { remove(material.id) }
                            }
                            onRawMaterialsChanged(updatedMap)
                            
                            // Check if waste tracking should be shown
                            val totalUsed = updatedMap.values.sum()
                            onShowWasteTracking(totalUsed > quantityProduced)
                        },
                        label = { Text("Quantity", color = Color(0xFF9CA3AF)) },
                        modifier = Modifier.width(120.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = Color(0xFFF9FAFB),
                            backgroundColor = Color(0xFF111827),
                            focusedBorderColor = if (isExceedingInventory) Color(0xFFEF4444) else Color(0xFF06B6D4),
                            unfocusedBorderColor = if (isExceedingInventory) Color(0xFFEF4444) else Color(0xFF374151),
                            cursorColor = Color(0xFF06B6D4)
                        ),
                        singleLine = true,
                        isError = isExceedingInventory
                    )
                }
                
                if (isExceedingInventory) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "⚠️ Cannot use more than available inventory (${String.format("%.2f", material.quantity)} ${material.unit})",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        if (rawMaterials.isEmpty()) {
            Text(
                "No raw materials found in inventory",
                color = Color(0xFF9CA3AF),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun WasteTrackingSection(
    wastage: String,
    burn: String,
    regrind: String,
    others: String,
    onWastageChange: (String) -> Unit,
    onBurnChange: (String) -> Unit,
    onRegrindChange: (String) -> Unit,
    onOthersChange: (String) -> Unit,
    productionDifference: Double
) {
    Column {
        Text(
            "Waste Tracking (Production Difference: ${String.format("%.2f", productionDifference)} kg)",
            color = Color(0xFFEF4444),
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = wastage,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) onWastageChange(it) },
                label = { Text("Wastage (kg)", color = Color(0xFF9CA3AF)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color(0xFFF9FAFB),
                    backgroundColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFEF4444),
                    unfocusedBorderColor = Color(0xFF374151),
                    cursorColor = Color(0xFFEF4444)
                ),
                singleLine = true,
                placeholder = { Text("0.00", color = Color(0xFF6B7280)) }
            )
            
            OutlinedTextField(
                value = burn,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) onBurnChange(it) },
                label = { Text("Burn (kg)", color = Color(0xFF9CA3AF)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color(0xFFF9FAFB),
                    backgroundColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFEF4444),
                    unfocusedBorderColor = Color(0xFF374151),
                    cursorColor = Color(0xFFEF4444)
                ),
                singleLine = true,
                placeholder = { Text("0.00", color = Color(0xFF6B7280)) }
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedTextField(
                value = regrind,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) onRegrindChange(it) },
                label = { Text("Regrind (kg)", color = Color(0xFF9CA3AF)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color(0xFFF9FAFB),
                    backgroundColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFEF4444),
                    unfocusedBorderColor = Color(0xFF374151),
                    cursorColor = Color(0xFFEF4444)
                ),
                singleLine = true,
                placeholder = { Text("0.00", color = Color(0xFF6B7280)) }
            )
            
            OutlinedTextField(
                value = others,
                onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) onOthersChange(it) },
                label = { Text("Others (kg)", color = Color(0xFF9CA3AF)) },
                modifier = Modifier.weight(1f),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = Color(0xFFF9FAFB),
                    backgroundColor = Color(0xFF111827),
                    focusedBorderColor = Color(0xFFEF4444),
                    unfocusedBorderColor = Color(0xFF374151),
                    cursorColor = Color(0xFFEF4444)
                ),
                singleLine = true,
                placeholder = { Text("0.00", color = Color(0xFF6B7280)) }
            )
        }
        
        val totalWaste = (wastage.toDoubleOrNull() ?: 0.0) + (burn.toDoubleOrNull() ?: 0.0) + 
                        (regrind.toDoubleOrNull() ?: 0.0) + (others.toDoubleOrNull() ?: 0.0)
        val isValidSum = kotlin.math.abs(totalWaste - productionDifference) < 0.01
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            "Total Waste: ${String.format("%.2f", totalWaste)} kg",
            color = if (isValidSum) Color(0xFF10B981) else Color(0xFFEF4444),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        
        if (!isValidSum) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "⚠️ Total waste must equal production difference (${String.format("%.2f", productionDifference)} kg)",
                color = Color(0xFFEF4444),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

fun calculateProductionDifference(rawMaterialsUsed: Map<String, Double>, quantityProduced: Double): Double {
    val totalRawMaterialsUsed = rawMaterialsUsed.values.sum()
    return totalRawMaterialsUsed - quantityProduced
}

@Composable
fun EditProductionDialog(
    record: ProductionRecord,
    inventoryViewModel: InventoryViewModel,
    onDismiss: () -> Unit,
    onSave: (ProductionInput) -> Unit
) {
    var batchNumber by remember { mutableStateOf(record.batchNumber) }
    var quantityProduced by remember { mutableStateOf(record.quantityProduced.toString()) }
    var supervisorName by remember { mutableStateOf(record.supervisorName) }
    var notes by remember { mutableStateOf(record.notes) }
    var selectedDate by remember { 
        mutableStateOf(
            record.productionDate?.let { 
                java.time.LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(it.seconds * 1000),
                    java.time.ZoneId.systemDefault()
                ).toLocalDate()
            } ?: LocalDate.now()
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    
    var rawMaterialsUsed by remember { mutableStateOf(record.rawMaterialsUsed) }
    var showWasteTracking by remember { mutableStateOf(record.wasteTracking != null) }
    var wastage by remember { mutableStateOf(record.wasteTracking?.wastage?.toString() ?: "0") }
    var burn by remember { mutableStateOf(record.wasteTracking?.burn?.toString() ?: "0") }
    var regrind by remember { mutableStateOf(record.wasteTracking?.regrind?.toString() ?: "0") }
    var others by remember { mutableStateOf(record.wasteTracking?.others?.toString() ?: "0") }

    val focusRequesters = remember { List(4) { FocusRequester() } }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .height(700.dp),
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
                        "Edit Production Record",
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = batchNumber,
                                onValueChange = { batchNumber = it },
                                label = { Text("Batch Number", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[0])
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

                            OutlinedTextField(
                                value = quantityProduced,
                                onValueChange = { if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) quantityProduced = it },
                                label = { Text("Quantity Produced (kg)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[1])
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
                    }

                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            OutlinedTextField(
                                value = supervisorName,
                                onValueChange = { supervisorName = it },
                                label = { Text("Supervisor Name", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[2])
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

                            OutlinedTextField(
                                value = selectedDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                                onValueChange = { },
                                label = { Text("Production Date", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF06B6D4),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF06B6D4)
                                ),
                                readOnly = true,
                                trailingIcon = {
                                    IconButton(onClick = { showDatePicker = true }) {
                                        Icon(Icons.Default.DateRange, contentDescription = "Select Date", tint = Color(0xFF06B6D4))
                                    }
                                }
                            )
                        }
                    }

                    item {
                        RawMaterialsSelection(
                            inventoryViewModel = inventoryViewModel,
                            rawMaterialsUsed = rawMaterialsUsed,
                            onRawMaterialsChanged = { rawMaterialsUsed = it },
                            quantityProduced = quantityProduced.toDoubleOrNull() ?: 0.0,
                            onShowWasteTracking = { showWasteTracking = it }
                        )
                    }

                    if (showWasteTracking) {
                        item {
                            WasteTrackingSection(
                                wastage = wastage,
                                burn = burn,
                                regrind = regrind,
                                others = others,
                                onWastageChange = { wastage = it },
                                onBurnChange = { burn = it },
                                onRegrindChange = { regrind = it },
                                onOthersChange = { others = it },
                                productionDifference = calculateProductionDifference(rawMaterialsUsed, quantityProduced.toDoubleOrNull() ?: 0.0)
                            )
                        }
                    }

                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[3]),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                textColor = Color(0xFFF9FAFB),
                                backgroundColor = Color(0xFF111827),
                                focusedBorderColor = Color(0xFF06B6D4),
                                unfocusedBorderColor = Color(0xFF374151),
                                cursorColor = Color(0xFF06B6D4)
                            ),
                            minLines = 2,
                            maxLines = 4
                        )
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
                            val wasteTracking = if (showWasteTracking) {
                                WasteTracking(
                                    wastage = wastage.toDoubleOrNull() ?: 0.0,
                                    burn = burn.toDoubleOrNull() ?: 0.0,
                                    regrind = regrind.toDoubleOrNull() ?: 0.0,
                                    others = others.toDoubleOrNull() ?: 0.0
                                )
                            } else null

                            onSave(
                                ProductionInput(
                                    batchNumber = batchNumber,
                                    quantityProduced = quantityProduced.toDoubleOrNull() ?: 0.0,
                                    productionDate = selectedDate.atStartOfDay().atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                                    supervisorName = supervisorName,
                                    notes = notes,
                                    rawMaterialsUsed = rawMaterialsUsed,
                                    wasteTracking = wasteTracking
                                )
                            )
                        },
                        enabled = batchNumber.isNotBlank() && quantityProduced.isNotBlank() && 
                                 supervisorName.isNotBlank() && rawMaterialsUsed.isNotEmpty() &&
                                 (!showWasteTracking || (wastage.isNotBlank() && burn.isNotBlank() && regrind.isNotBlank() && others.isNotBlank())),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Update Production", color = Color(0xFF111827))
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showDatePicker = false }) {
            Card(
                backgroundColor = Color(0xFF1F2937),
                shape = RoundedCornerShape(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Select Production Date",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    DatePicker(
                        selectedDate = selectedDate,
                        onDateSelected = { selectedDate = it }
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Button(
                        onClick = { showDatePicker = false },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4)
                        )
                    ) {
                        Text("OK", color = Color(0xFF111827))
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}
