package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
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
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.data.UserRole
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import com.humblecoders.plantmanagement.viewmodels.SortField
import com.humblecoders.plantmanagement.utils.PdfExportUtils

@Composable
fun EntityScreen(
    entityViewModel: EntityViewModel, 
    userRole: UserRole? = null,
    cashTransactionViewModel: com.humblecoders.plantmanagement.viewmodels.CashTransactionViewModel? = null,
    saleViewModel: com.humblecoders.plantmanagement.viewmodels.SaleViewModel? = null,
    purchaseViewModel: com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel? = null,
    onNavigateToCustomerDetail: (Entity) -> Unit = {}
) {
    val entityState = entityViewModel.entityState
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var entityToEdit by remember { mutableStateOf<Entity?>(null) }
    var selectedEntityForLedger by remember { mutableStateOf<Entity?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var entityToDelete by remember { mutableStateOf<Entity?>(null) }
    var showCashTransactionDialog by remember { mutableStateOf(false) }
    var selectedEntityForCashTransaction by remember { mutableStateOf<Entity?>(null) }
    var showCashTransactionHistory by remember { mutableStateOf(false) }
    
    val isAdmin = userRole == UserRole.ADMIN

    // Clear messages after showing
    LaunchedEffect(entityState.successMessage, entityState.error) {
        if (entityState.successMessage != null || entityState.error != null) {
            kotlinx.coroutines.delay(3000)
            entityViewModel.clearMessages()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Entity Management",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF9FAFB)
            )
            
            // Cash Transaction History Button
            if (cashTransactionViewModel != null) {
                Button(
                    onClick = { showCashTransactionHistory = true },
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color(0xFF10B981)
                    )
                ) {
                    Text("Cash Transaction History", color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Success/Error messages
        if (entityState.successMessage != null) {
            Card(
                backgroundColor = Color(0xFF10B981),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = entityState.successMessage,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        if (entityState.error != null) {
            Card(
                backgroundColor = Color(0xFFEF4444),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Text(
                    text = entityState.error,
                    color = Color.White,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // Add Entity Card
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
                        text = "Add New Entity",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )

                    Button(
                        onClick = { showAddDialog = true },
                        enabled = !entityState.isAdding && !entityState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Add Entity", color = Color(0xFF111827))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Entity List Card
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
                        text = "Entity List",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Search field
                        OutlinedTextField(
                            value = entityState.searchQuery,
                            onValueChange = { entityViewModel.updateSearchQuery(it) },
                            placeholder = { Text("Search entities...", color = Color(0xFF9CA3AF)) },
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

                        // Sort dropdown
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            OutlinedButton(
                                onClick = { expanded = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color.Black
                                )
                            ) {
                                Text("Sort by: ${entityState.sortBy.name.replace("_", " ")}")
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                DropdownMenuItem(onClick = {
                                    entityViewModel.updateSortBy(SortField.FIRM_NAME)
                                    expanded = false
                                }) {
                                    Text("Firm Name")
                                }
                                DropdownMenuItem(onClick = {
                                    entityViewModel.updateSortBy(SortField.CITY)
                                    expanded = false
                                }) {
                                    Text("City")
                                }
                                DropdownMenuItem(onClick = {
                                    entityViewModel.updateSortBy(SortField.GSTIN)
                                    expanded = false
                                }) {
                                    Text("GSTIN")
                                }
                            }
                        }

                        // Sort direction button
                        IconButton(
                            onClick = { entityViewModel.toggleSortDirection() }
                        ) {
                            Icon(
                                imageVector = if (entityState.sortDirection == SortDirection.ASCENDING) {
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

                // Print/Download Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val filteredEntities = entityViewModel.getFilteredAndSortedEntities()
                            PdfExportUtils.exportCustomers(filteredEntities)
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
                        Text("Print/Download Customers")
                    }
                }

                // Entity table
                EntityTable(
                    entities = entityViewModel.getFilteredAndSortedEntities(),
                    onEntityClick = { entity ->
                        if (saleViewModel != null && purchaseViewModel != null && cashTransactionViewModel != null) {
                            onNavigateToCustomerDetail(entity)
                        } else {
                            selectedEntityForLedger = entity
                        }
                    },
                    onEditClick = {
                        entityToEdit = it
                        showEditDialog = true
                    },
                    onDeleteClick = {
                        entityToDelete = it
                        showDeleteConfirmDialog = true
                    },
                    onCashTransactionClick = { entity ->
                        selectedEntityForCashTransaction = entity
                        showCashTransactionDialog = true
                    },
                    isAdmin = isAdmin,
                    showCashTransaction = cashTransactionViewModel != null
                )
            }
        }
        }

        // Loading indicator overlay
        if (entityState.isLoading) {
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
                                entityState.isAdding -> "Adding entity..."
                                entityState.isUpdating -> "Updating entity..."
                                entityState.isDeleting -> "Deleting entity..."
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

    // Add Entity Dialog
    if (showAddDialog) {
        AddEntityDialog(
            onDismiss = { showAddDialog = false },
            onSave = { entity ->
                entityViewModel.addEntity(entity)
                showAddDialog = false
            }
        )
    }

    // Edit Entity Dialog
    if (showEditDialog && entityToEdit != null) {
        EditEntityDialog(
            entity = entityToEdit!!,
            onDismiss = {
                showEditDialog = false
                entityToEdit = null
            },
            onSave = { entity ->
                entityViewModel.updateEntity(entityToEdit!!.id, entity)
                showEditDialog = false
                entityToEdit = null
            }
        )
    }

    // Entity Ledger Dialog (Placeholder for now)
    if (selectedEntityForLedger != null) {
        EntityLedgerDialog(
            entity = selectedEntityForLedger!!,
            onDismiss = { selectedEntityForLedger = null }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && entityToDelete != null) {
        DeleteConfirmationDialog(
            itemName = entityToDelete!!.firmName,
            itemType = "entity",
            onConfirm = {
                entityViewModel.deleteEntity(entityToDelete!!.id)
                showDeleteConfirmDialog = false
                entityToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                entityToDelete = null
            }
        )
    }
    
    // Cash Transaction Dialog
    if (showCashTransactionDialog && selectedEntityForCashTransaction != null && cashTransactionViewModel != null) {
        com.humblecoders.plantmanagement.ui.components.CashTransactionDialog(
            customer = selectedEntityForCashTransaction!!,
            cashTransactionViewModel = cashTransactionViewModel,
            onDismiss = {
                showCashTransactionDialog = false
                selectedEntityForCashTransaction = null
            }
        )
    }
    
    // Cash Transaction History Screen
    if (showCashTransactionHistory && cashTransactionViewModel != null) {
        CashTransactionHistoryScreen(
            cashTransactionViewModel = cashTransactionViewModel,
            entityViewModel = entityViewModel,
            onBack = { showCashTransactionHistory = false }
        )
    }
}

@Composable
fun EntityTable(
    entities: List<Entity>,
    onEntityClick: (Entity) -> Unit,
    onEditClick: (Entity) -> Unit,
    onDeleteClick: (Entity) -> Unit,
    onCashTransactionClick: (Entity) -> Unit,
    isAdmin: Boolean,
    showCashTransaction: Boolean = false
) {
    Column {
        // Header row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(12.dp)
        ) {
            Text("Firm Name", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.25f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Contact Person", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("City", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.15f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("GSTIN", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Balance", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Actions", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.20f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color(0xFF374151))

        // Data rows
        entities.forEach { entity ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF1F2937))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entity.firmName,
                    color = Color(0xFF06B6D4),
                    modifier = Modifier.weight(0.25f).clickable { onEntityClick(entity) },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(entity.contactPerson, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.20f), fontSize = 14.sp)
                Text(entity.city, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.15f), fontSize = 14.sp)
                Text(entity.gstin, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.20f), fontSize = 14.sp)

                Text(
                    text = buildString {
                        val label = if (entity.balance >= 0.0) "To receive" else "To give"
                        append(label)
                        append(" • ")
                        append("₹ ")
                        append(kotlin.math.abs(entity.balance))
                    },
                    color = if (entity.balance >= 0.0) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.weight(0.20f),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.weight(0.20f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Cash Transaction Button (always visible if enabled)
                    if (showCashTransaction) {
                        Button(
                            onClick = { onCashTransactionClick(entity) },
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF10B981),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(6.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Cash", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    
                    if (isAdmin) {
                        IconButton(
                            onClick = { onEditClick(entity) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = { onDeleteClick(entity) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(18.dp)
                            )
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
                                modifier = Modifier.size(16.dp)
                            )
                            Text("Admin Only", color = Color(0xFF6B7280), fontSize = 12.sp)
                        }
                    }
                }
            }
            Divider(color = Color(0xFF374151))
        }

        if (entities.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No entities found", color = Color(0xFF9CA3AF))
            }
        }
    }
}

@Composable
fun AddEntityDialog(
    onDismiss: () -> Unit,
    onSave: (Entity) -> Unit
) {
    var firmName by remember { mutableStateOf("") }
    var contactPerson by remember { mutableStateOf("") }
    var contactNo by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var state by remember { mutableStateOf("") }
    var gstin by remember { mutableStateOf("") }
    var balanceInput by remember { mutableStateOf("") }
    var isToReceive by remember { mutableStateOf(true) }

    val focusRequesters = remember { List(6) { FocusRequester() } }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(550.dp)
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
                        "Add New Entity",
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
                            value = firmName,
                            onValueChange = { firmName = it },
                            label = { Text("Firm Name", color = Color(0xFF9CA3AF)) },
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
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Contact Person", color = Color(0xFF9CA3AF)) },
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
                        OutlinedTextField(
                            value = contactNo,
                            onValueChange = { contactNo = it },
                            label = { Text("Contact Number", color = Color(0xFF9CA3AF)) },
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
                    }

                    item {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", color = Color(0xFF9CA3AF)) },
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
                    }

                    item {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[4])
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                        focusRequesters[5].requestFocus()
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
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[5]),
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

                    // Initial Balance Controls
                    item {
                        Text(
                            text = "Initial Balance",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { isToReceive = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isToReceive) Color(0xFF10B981) else Color.Black
                                )
                            ) { Text("To receive") }
                            OutlinedButton(
                                onClick = { isToReceive = false },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (!isToReceive) Color(0xFFEF4444) else Color.Black
                                )
                            ) { Text("To give") }
                            OutlinedTextField(
                                value = balanceInput,
                                onValueChange = { input ->
                                    // Allow only numbers and optional dot
                                    if (input.isEmpty() || input.matches(Regex("[0-9]*\\.?[0-9]*"))) {
                                        balanceInput = input
                                    }
                                },
                                label = { Text("Amount", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.width(180.dp),
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
                            val raw = balanceInput.toDoubleOrNull() ?: 0.0
                            val signed = if (isToReceive) kotlin.math.abs(raw) else -kotlin.math.abs(raw)
                            onSave(Entity(
                                firmName = firmName,
                                contactPerson = contactPerson,
                                contactNo = contactNo,
                                city = city,
                                state = state,
                                gstin = gstin,
                                balance = signed
                            ))
                        },
                        enabled = firmName.isNotBlank() && contactPerson.isNotBlank() && 
                                 contactNo.isNotBlank() && city.isNotBlank() && 
                                 state.isNotBlank() && gstin.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Add Entity", color = Color(0xFF111827))
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
fun EditEntityDialog(
    entity: Entity,
    onDismiss: () -> Unit,
    onSave: (Entity) -> Unit
) {
    var firmName by remember { mutableStateOf(entity.firmName) }
    var contactPerson by remember { mutableStateOf(entity.contactPerson) }
    var contactNo by remember { mutableStateOf(entity.contactNo) }
    var city by remember { mutableStateOf(entity.city) }
    var state by remember { mutableStateOf(entity.state) }
    var gstin by remember { mutableStateOf(entity.gstin) }
    var isToReceive by remember { mutableStateOf(entity.balance >= 0.0) }
    var balanceInput by remember { mutableStateOf(kotlin.run {
        val abs = if (entity.balance < 0) -entity.balance else entity.balance
        if (abs == 0.0) "" else abs.toString()
    }) }

    val focusRequesters = remember { List(6) { FocusRequester() } }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(550.dp)
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
                        "Edit Entity",
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
                            value = firmName,
                            onValueChange = { firmName = it },
                            label = { Text("Firm Name", color = Color(0xFF9CA3AF)) },
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
                            value = contactPerson,
                            onValueChange = { contactPerson = it },
                            label = { Text("Contact Person", color = Color(0xFF9CA3AF)) },
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
                        OutlinedTextField(
                            value = contactNo,
                            onValueChange = { contactNo = it },
                            label = { Text("Contact Number", color = Color(0xFF9CA3AF)) },
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
                    }

                    item {
                        OutlinedTextField(
                            value = city,
                            onValueChange = { city = it },
                            label = { Text("City", color = Color(0xFF9CA3AF)) },
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
                    }

                    item {
                        OutlinedTextField(
                            value = state,
                            onValueChange = { state = it },
                            label = { Text("State", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[4])
                                .onKeyEvent { event ->
                                    if (event.key == Key.Enter && event.type == KeyEventType.KeyDown) {
                                        focusRequesters[5].requestFocus()
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
                            value = gstin,
                            onValueChange = { gstin = it },
                            label = { Text("GSTIN", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[5]),
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

                    // Balance Controls
                    item {
                        Text(
                            text = "Balance",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = { isToReceive = true },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (isToReceive) Color(0xFF10B981) else Color.Black
                                )
                            ) { Text("To receive") }
                            OutlinedButton(
                                onClick = { isToReceive = false },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (!isToReceive) Color(0xFFEF4444) else Color.Black
                                )
                            ) { Text("To give") }
                            OutlinedTextField(
                                value = balanceInput,
                                onValueChange = { input ->
                                    if (input.isEmpty() || input.matches(Regex("[0-9]*\\.?[0-9]*"))) {
                                        balanceInput = input
                                    }
                                },
                                label = { Text("Amount", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.width(180.dp),
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
                            val raw = balanceInput.toDoubleOrNull() ?: 0.0
                            val signed = if (isToReceive) kotlin.math.abs(raw) else -kotlin.math.abs(raw)
                            onSave(Entity(
                                firmName = firmName,
                                contactPerson = contactPerson,
                                contactNo = contactNo,
                                city = city,
                                state = state,
                                gstin = gstin,
                                balance = signed
                            ))
                        },
                        enabled = firmName.isNotBlank() && contactPerson.isNotBlank() && 
                                 contactNo.isNotBlank() && city.isNotBlank() && 
                                 state.isNotBlank() && gstin.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        Text("Update Entity", color = Color(0xFF111827))
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
fun EntityLedgerDialog(
    entity: Entity,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("${entity.firmName} - Account Ledger", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.width(800.dp).height(500.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Entity details
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Entity Information", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Contact Person: ${entity.contactPerson}", color = Color(0xFF9CA3AF))
                        Text("Contact No: ${entity.contactNo}", color = Color(0xFF9CA3AF))
                        Text("City: ${entity.city}", color = Color(0xFF9CA3AF))
                        Text("State: ${entity.state}", color = Color(0xFF9CA3AF))
                        Text("GSTIN: ${entity.gstin}", color = Color(0xFF9CA3AF))
                    }
                }

                // Placeholder for financial summary
                Card(
                    backgroundColor = Color(0xFF111827),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Financial Summary", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Net Amount Due (Cr): ₹ 0.00", color = Color(0xFF10B981))
                        Text("Net Purchases to be Paid (Dr): ₹ 0.00", color = Color(0xFFEF4444))
                        Text("Net Difference to be Paid (Dr): ₹ 0.00", color = Color(0xFFEF4444))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Net Outstanding Balance: ₹ 0.00", color = Color(0xFF06B6D4), fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    "Transaction history will be implemented in future modules",
                    color = Color(0xFF9CA3AF),
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
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
fun DeleteConfirmationDialog(
    itemName: String,
    itemType: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = Color(0xFFFBBF24),
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    "Delete $itemType?",
                    color = Color(0xFFF9FAFB),
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Are you sure you want to delete:",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp
                )
                Text(
                    "\"$itemName\"",
                    color = Color(0xFFF9FAFB),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "This action cannot be undone.",
                    color = Color(0xFFEF4444),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color(0xFFEF4444)
                )
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF9CA3AF))
            }
        }
    )
}
