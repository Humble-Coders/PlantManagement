package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.humblecoders.plantmanagement.data.Entity
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import com.humblecoders.plantmanagement.viewmodels.SortField

@Composable
fun EntityScreen(entityViewModel: EntityViewModel) {
    val entityState = entityViewModel.entityState
    var showAddDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var entityToEdit by remember { mutableStateOf<Entity?>(null) }
    var selectedEntityForLedger by remember { mutableStateOf<Entity?>(null) }

    // Clear messages after showing
    LaunchedEffect(entityState.successMessage, entityState.error) {
        if (entityState.successMessage != null || entityState.error != null) {
            kotlinx.coroutines.delay(3000)
            entityViewModel.clearMessages()
        }
    }

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
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4)
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

                // Entity table
                EntityTable(
                    entities = entityViewModel.getFilteredAndSortedEntities(),
                    onEntityClick = { selectedEntityForLedger = it },
                    onEditClick = {
                        entityToEdit = it
                        showEditDialog = true
                    },
                    onDeleteClick = { entityViewModel.deleteEntity(it.id) }
                )
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
}

@Composable
fun EntityTable(
    entities: List<Entity>,
    onEntityClick: (Entity) -> Unit,
    onEditClick: (Entity) -> Unit,
    onDeleteClick: (Entity) -> Unit
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(onClick = { onEditClick(entity) }) {
                        Text("Edit", color = Color(0xFF10B981))
                    }
                    TextButton(onClick = { onDeleteClick(entity) }) {
                        Text("Delete", color = Color(0xFFEF4444))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Add New Entity", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.width(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                // Initial Balance Controls
                Text(
                    text = "Initial Balance",
                    color = Color(0xFFF9FAFB),
                    fontWeight = FontWeight.SemiBold
                )
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
        },
        confirmButton = {
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
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
            ) {
                Text("Add Entity", color = Color(0xFF111827))
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

    AlertDialog(
        onDismissRequest = onDismiss,
        backgroundColor = Color(0xFF1F2937),
        title = {
            Text("Edit Entity", color = Color(0xFFF9FAFB), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.width(500.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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

                // Balance Controls
                Text(
                    text = "Balance",
                    color = Color(0xFFF9FAFB),
                    fontWeight = FontWeight.SemiBold
                )
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
        },
        confirmButton = {
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
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
            ) {
                Text("Update Entity", color = Color(0xFF111827))
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
