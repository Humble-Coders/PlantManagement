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
import com.humblecoders.plantmanagement.data.UserRole
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
import com.humblecoders.plantmanagement.ui.components.SearchableCustomerDropdown
import com.humblecoders.plantmanagement.ui.components.SearchableItemDropdown
import com.humblecoders.plantmanagement.services.FirebaseStorageService
import com.humblecoders.plantmanagement.utils.FileDialogUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.ImageBitmap
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

@Composable
fun PurchaseScreen(
    purchaseViewModel: PurchaseViewModel,
    entityViewModel: EntityViewModel,
    inventoryViewModel: InventoryViewModel,
    userRole: UserRole? = null
) {
    val purchaseState = purchaseViewModel.purchaseState
    val entityState = entityViewModel.entityState
    val inventoryState = inventoryViewModel.inventoryState
    var showAddDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    var purchaseToView by remember { mutableStateOf<Purchase?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var purchaseToDelete by remember { mutableStateOf<Purchase?>(null) }
    var showCashOutDialog by remember { mutableStateOf(false) }
    var showCashOutHistoryDialog by remember { mutableStateOf(false) }
    var isUploadingDocument by remember { mutableStateOf(false) }
    var uploadingPurchaseId by remember { mutableStateOf<String?>(null) }
    
    val isAdmin = userRole == UserRole.ADMIN
    val storageService = remember { FirebaseStorageService(com.humblecoders.plantmanagement.FirebaseCredentialsHolder.credentials) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(purchaseState.successMessage, purchaseState.error) {
        if (purchaseState.successMessage != null || purchaseState.error != null) {
            kotlinx.coroutines.delay(3000)
            purchaseViewModel.clearMessages()
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

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { showCashOutDialog = true },
                            enabled = !purchaseState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF10B981),
                                disabledBackgroundColor = Color(0xFF9CA3AF)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = "Cash Out",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cash Out", color = Color.White)
                        }
                        
                        OutlinedButton(
                            onClick = { showCashOutHistoryDialog = true },
                            enabled = !purchaseState.isLoading,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF10B981)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = "Cash Out History",
                                tint = Color(0xFF10B981),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cash Out History", color = Color(0xFF10B981))
                        }
                        
                        Button(
                            onClick = { showAddDialog = true },
                            enabled = !purchaseState.isAdding && !purchaseState.isLoading,
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = Color(0xFF06B6D4),
                                disabledBackgroundColor = Color(0xFF9CA3AF)
                            )
                        ) {
                            Text("Add Purchase", color = Color(0xFF111827))
                        }
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

                // Print/Download Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = {
                            val filteredPurchases = purchaseViewModel.getFilteredAndSortedPurchases()
                            printPurchases(filteredPurchases)
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
                        Text("Print/Download Purchases")
                    }
                }

                PurchaseTable(
                    purchases = purchaseViewModel.getFilteredAndSortedPurchases(),
                    onDeleteClick = {
                        purchaseToDelete = it
                        showDeleteConfirmDialog = true
                    },
                    onViewClick = {
                        purchaseToView = it
                        showViewDialog = true
                    },
                    isAdmin = isAdmin,
                    isUploadingDocument = isUploadingDocument,
                    uploadingPurchaseId = uploadingPurchaseId,
                    onUploadDocument = { purchase ->
                        // Handle file picker on main thread first
                        val selectedFiles = FileDialogUtils.showOpenDialog(
                            title = "Select Document to Upload",
                            allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "txt"),
                            allowMultiple = false
                        )
                        
                        if (selectedFiles.isNotEmpty()) {
                            val selectedFile = selectedFiles.first()
                            
                            // Now handle upload in coroutine
                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                try {
                                    isUploadingDocument = true
                                    uploadingPurchaseId = purchase.id
                                    
                                    val uploadResult = storageService.uploadDocument(selectedFile, "purchases")
                                    
                                    if (uploadResult.isSuccess) {
                                        val documentUrl = uploadResult.getOrNull() ?: ""
                                        val updatedPurchase = purchase.copy(imageUrl = documentUrl)
                                        purchaseViewModel.updatePurchase(purchase.id, updatedPurchase)
                                    } else {
                                        println("Upload failed: ${uploadResult.exceptionOrNull()?.message}")
                                    }
                                } catch (e: Exception) {
                                    println("Upload error: ${e.message}")
                                    e.printStackTrace()
                                } finally {
                                    isUploadingDocument = false
                                    uploadingPurchaseId = null
                                }
                            }
                        }
                    }
                )
            }
        }
        }

        // Loading indicator overlay
        if (purchaseState.isLoading) {
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
                                purchaseState.isAdding -> "Adding purchase..."
                                purchaseState.isUpdating -> "Updating purchase..."
                                purchaseState.isDeleting -> "Deleting purchase..."
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

    if (showViewDialog && purchaseToView != null) {
        ViewPurchaseDialog(
            purchase = purchaseToView!!,
            onDismiss = {
                showViewDialog = false
                purchaseToView = null
            }
        )
    }
    
    // Delete Confirmation Dialog
    if (showDeleteConfirmDialog && purchaseToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "Purchase from ${purchaseToDelete!!.firmName} on ${purchaseToDelete!!.purchaseDate}",
            itemType = "purchase",
            onConfirm = {
                purchaseViewModel.deletePurchase(purchaseToDelete!!.id)
                showDeleteConfirmDialog = false
                purchaseToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                purchaseToDelete = null
            }
        )
    }
    
    // Cash Out Dialog
    if (showCashOutDialog) {
        com.humblecoders.plantmanagement.ui.components.CashOutDialog(
            purchaseViewModel = purchaseViewModel,
            entityViewModel = entityViewModel,
            onDismiss = { showCashOutDialog = false }
        )
    }
    
    // Cash Out History Dialog
    if (showCashOutHistoryDialog) {
        com.humblecoders.plantmanagement.ui.components.CashOutHistoryDialog(
            purchaseViewModel = purchaseViewModel,
            onDismiss = { showCashOutHistoryDialog = false }
        )
    }
}

@Composable
fun PurchaseTable(
    purchases: List<Purchase>,
    onDeleteClick: (Purchase) -> Unit,
    onViewClick: (Purchase) -> Unit,
    isAdmin: Boolean,
    isUploadingDocument: Boolean,
    uploadingPurchaseId: String?,
    onUploadDocument: (Purchase) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF374151))
                .padding(12.dp)
        ) {
            Text("Date", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.10f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Entity", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.16f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Items", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.14f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Grand Total", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.10f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Pending", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.10f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Status", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.10f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Upload", color = Color(0xFF9CA3AF), modifier = Modifier.weight(0.10f), fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                Text(purchase.purchaseDate, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.10f), fontSize = 14.sp)
                Text(purchase.firmName, color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.16f), fontSize = 14.sp)

                Column(modifier = Modifier.weight(0.14f)) {
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

                Text("₹ ${String.format("%.2f", purchase.grandTotal)}", color = Color(0xFFF9FAFB), modifier = Modifier.weight(0.12f), fontSize = 14.sp, fontWeight = FontWeight.Bold)

                // Pending Amount Column
                val pendingAmount = purchase.grandTotal - purchase.amountPaid
                if (purchase.paymentStatus == PaymentStatus.PARTIALLY_PAID || purchase.paymentStatus == PaymentStatus.PENDING) {
                    Text(
                        "₹ ${String.format("%.2f", pendingAmount)}",
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.weight(0.12f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        "-",
                        color = Color(0xFF6B7280),
                        modifier = Modifier.weight(0.12f),
                        fontSize = 13.sp
                    )
                }

                Text(
                    text = purchase.paymentStatus.name.replace("_", " "),
                    color = when (purchase.paymentStatus) {
                        PaymentStatus.PAID -> Color(0xFF10B981)
                        PaymentStatus.PENDING -> Color(0xFFF59E0B)
                        PaymentStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                    },
                    modifier = Modifier.weight(0.10f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                // Upload column
                Box(
                    modifier = Modifier.weight(0.10f),
                    contentAlignment = Alignment.Center
                ) {
                    if (purchase.imageUrl.isNotBlank()) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Document uploaded",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        IconButton(
                            onClick = { onUploadDocument(purchase) },
                            enabled = !isUploadingDocument,
                            modifier = Modifier.size(24.dp)
                        ) {
                            if (isUploadingDocument && uploadingPurchaseId == purchase.id) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF06B6D4),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.CloudUpload,
                                    contentDescription = "Upload document",
                                    tint = Color(0xFF06B6D4),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.weight(0.20f),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onViewClick(purchase) }) {
                        Text("View", color = Color(0xFF3B82F6), fontSize = 12.sp)
                    }
                    if (isAdmin) {
                        TextButton(onClick = { onDeleteClick(purchase) }) {
                            Text("Reverse", color = Color(0xFFEF4444), fontSize = 12.sp)
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
    var purchaseDate by remember { mutableStateOf(LocalDate.now()) }
    var gstRate by remember { mutableStateOf(0.0) }
    var amountPaid by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var purchaseItems by remember { mutableStateOf(listOf<PurchaseItem>()) }
    var selectedDocumentFile by remember { mutableStateOf<java.io.File?>(null) }
    var isUploadingDocument by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val storageService = remember { FirebaseStorageService(com.humblecoders.plantmanagement.FirebaseCredentialsHolder.credentials) }

    val totalAmount = purchaseItems.sumOf { it.totalPrice }
    val gstAmount = totalAmount * (gstRate / 100.0)
    val grandTotal = totalAmount + gstAmount
    val paidAmount = amountPaid.toDoubleOrNull() ?: 0.0
    val pendingAmount = grandTotal - paidAmount

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(750.dp)
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
                        "Log New Purchase",
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
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Entity Dropdown
                    item {
                        SearchableCustomerDropdown(
                            customers = customers,
                            selectedCustomerId = selectedEntityId,
                            onCustomerSelected = { selectedEntityId = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "Select Entity"
                        )
                    }

                    // Purchase Date
                    item {
                        DatePicker(
                            selectedDate = purchaseDate,
                            onDateSelected = { purchaseDate = it },
                            label = "Purchase Date",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Divider
                    item {
                        Divider(color = Color(0xFF374151))
                    }

                    // Items Section Header
                    item {
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
                    }

                    // Empty state or items list
                    if (purchaseItems.isEmpty()) {
                        item {
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
                    } else {
                        items(purchaseItems.size) { index ->
                            PurchaseItemCard(
                                item = purchaseItems[index],
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
                    }

                    // Divider
                    item {
                        Divider(color = Color(0xFF374151))
                    }

                    // Total Amount Display
                    item {
                        Card(
                            backgroundColor = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Amount:", color = Color(0xFF9CA3AF), fontSize = 14.sp)
                                Text("₹ ${String.format("%.2f", totalAmount)}", color = Color(0xFFF9FAFB), fontSize = 14.sp)
                            }
                        }
                    }

                    // GST Selection
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        }
                    }

                    // GST Amount Display
                    if (gstRate > 0) {
                        item {
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
                    }

                    // Grand Total Display
                    item {
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
                    }

                    // Amount Paid
                    item {
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
                    }

                    // Pending/Credit Amount Display
                    item {
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
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (optional)", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().height(80.dp),
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

                    // Image Selection
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Purchase Document (optional)", color = Color(0xFFF9FAFB), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        val selectedFiles = FileDialogUtils.showOpenDialog(
                                            title = "Select Document",
                                            allowedExtensions = listOf("jpg", "jpeg", "png", "gif", "webp", "pdf", "doc", "docx", "txt"),
                                            allowMultiple = false
                                        )
                                        if (selectedFiles.isNotEmpty()) {
                                            selectedDocumentFile = selectedFiles.first()
                                        }
                                    },
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF06B6D4)
                                    )
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = "Select Document", tint = Color(0xFF06B6D4))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Select Document", color = Color(0xFF06B6D4))
                                }
                                
                                if (selectedDocumentFile != null) {
                                    Card(
                                        backgroundColor = Color(0xFF111827),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                                Text(selectedDocumentFile!!.name, color = Color(0xFFF9FAFB), fontSize = 12.sp)
                                            }
                                            IconButton(
                                                onClick = { selectedDocumentFile = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }
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
                            coroutineScope.launch {
                                isUploadingDocument = true
                                var documentUrl = ""
                                
                                // Upload document if selected
                                if (selectedDocumentFile != null) {
                                    val uploadResult = storageService.uploadDocument(selectedDocumentFile!!, "purchases")
                                    if (uploadResult.isSuccess) {
                                        documentUrl = uploadResult.getOrNull() ?: ""
                                    }
                                }
                                
                                isUploadingDocument = false
                                
                                val selectedEntity = customers.find { it.id == selectedEntityId }
                                val paidAmount = amountPaid.toDoubleOrNull() ?: 0.0

                                val paymentStatus = when {
                                    paidAmount >= grandTotal -> PaymentStatus.PAID
                                    paidAmount > 0 -> PaymentStatus.PARTIALLY_PAID
                                    else -> PaymentStatus.PENDING
                                }

                                onSave(
                                    Purchase(
                                        customerId = selectedEntityId,
                                        firmName = selectedEntity?.firmName ?: "",
                                        purchaseDate = purchaseDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        items = purchaseItems,
                                        totalAmount = totalAmount,
                                        gstRate = gstRate,
                                        gstAmount = gstAmount,
                                        grandTotal = grandTotal,
                                        paymentStatus = paymentStatus,
                                        amountPaid = paidAmount,
                                        notes = notes,
                                        imageUrl = documentUrl
                                    )
                                )
                            }
                        },
                        enabled = selectedEntityId.isNotBlank() && purchaseItems.isNotEmpty() && !isUploadingDocument,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color(0xFF06B6D4),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        if (isUploadingDocument) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color(0xFF111827),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text(if (isUploadingDocument) "Uploading..." else "Log Purchase", color = Color(0xFF111827))
                    }
                }
            }

        }
    }
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

            // Inventory Item Searchable Dropdown
            SearchableItemDropdown(
                items = inventoryItems,
                selectedItemId = selectedInventoryItemId,
                onItemSelected = { itemId ->
                    selectedInventoryItemId = itemId
                },
                placeholder = "Select Item",
                modifier = Modifier.fillMaxWidth()
            )

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
fun ViewPurchaseDialog(
    purchase: Purchase,
    onDismiss: () -> Unit
) {
    val pdfService = remember { com.humblecoders.plantmanagement.services.PdfGeneratorService() }
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(650.dp)
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
                        "Purchase Details",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Download PDF Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isGeneratingPdf = true
                                    }
                                    try {
                                        val defaultFileName = "Purchase_${purchase.firmName.replace(" ", "_")}_${purchase.purchaseDate}.pdf"
                                        
                                        val outputFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            FileDialogUtils.showSaveDialog(
                                                title = "Save Purchase Bill",
                                                defaultFilename = defaultFileName,
                                                allowedExtensions = listOf("pdf")
                                            )?.let { selectedFile ->
                                                // Ensure .pdf extension
                                                val finalFile = if (!selectedFile.name.endsWith(".pdf", ignoreCase = true)) {
                                                    java.io.File(selectedFile.parent, "${selectedFile.name}.pdf")
                                                } else {
                                                    selectedFile
                                                }
                                                
                                                // Delete existing file if it exists (auto-replace)
                                                if (finalFile.exists()) {
                                                    finalFile.delete()
                                                }
                                                
                                                finalFile
                                            }
                                        }
                                        
                                        if (outputFile != null) {
                                            val pdfResult = pdfService.generatePurchaseBill(purchase, outputFile)
                                            
                                            if (pdfResult.isSuccess) {
                                                // Open the PDF after saving
                                                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                                    java.awt.Desktop.getDesktop().open(outputFile)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        println("Error saving PDF: ${e.message}")
                                        e.printStackTrace()
                                    } finally {
                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            isGeneratingPdf = false
                                        }
                                    }
                                }
                            },
                            enabled = !isGeneratingPdf,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFF10B981)
                            )
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF10B981),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    Icons.Default.Build,
                                    contentDescription = "Download PDF",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isGeneratingPdf) "Generating..." else "Download PDF", color = Color(0xFF10B981), fontSize = 12.sp)
                        }
                        
//                        // Print Button
//                        OutlinedButton(
//                            onClick = {
//                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
//                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
//                                        isGeneratingPdf = true
//                                    }
//                                    try {
//                                        val tempFile = java.io.File.createTempFile("purchase_bill_", ".pdf")
//                                        val pdfResult = pdfService.generatePurchaseBill(purchase, tempFile)
//
//                                        if (pdfResult.isSuccess) {
//                                            // Print the PDF
//                                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
//                                                java.awt.Desktop.getDesktop().print(tempFile)
//                                            }
//                                        }
//                                    } catch (e: Exception) {
//                                        println("Error printing PDF: ${e.message}")
//                                        e.printStackTrace()
//                                    } finally {
//                                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
//                                            isGeneratingPdf = false
//                                        }
//                                    }
//                                }
//                            },
//                            enabled = !isGeneratingPdf,
//                            colors = ButtonDefaults.outlinedButtonColors(
//                                contentColor = Color(0xFF06B6D4)
//                            )
//                        ) {
//                            Icon(
//                                Icons.Default.Send,
//                                contentDescription = "Print",
//                                tint = Color(0xFF06B6D4),
//                                modifier = Modifier.size(16.dp)
//                            )
//                            Spacer(modifier = Modifier.width(4.dp))
//                            Text("Print", color = Color(0xFF06B6D4), fontSize = 12.sp)
//                        }
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    item {
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
                    }

                    item {
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
                    }

                    item {
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
                    }

                    if (purchase.notes.isNotBlank()) {
                        item {
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

                    // Document Display
                    if (purchase.imageUrl.isNotBlank()) {
                        item {
                            Card(
                                backgroundColor = Color(0xFF111827),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Purchase Document", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF06B6D4))
                                        OutlinedButton(
                                            onClick = {
                                                try {
                                                    // Fix URL encoding before opening in browser
                                                    val fixedUrl = fixImageUrl(purchase.imageUrl)
                                                    java.awt.Desktop.getDesktop().browse(java.net.URI(fixedUrl))
                                                } catch (e: Exception) {
                                                    println("Error opening document: ${e.message}")
                                                    e.printStackTrace()
                                                }
                                            },
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = Color(0xFF06B6D4)
                                            )
                                        ) {
                                            Text("View Document", color = Color(0xFF06B6D4), fontSize = 12.sp)
                                        }
                                    }
                                    Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    // Display document (image or PDF)
                                    val isPdf = purchase.imageUrl.lowercase().contains(".pdf")
                                    if (isPdf) {
                                        // Show PDF icon and info
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(
                                                Icons.Default.PictureAsPdf,
                                                contentDescription = "PDF Document",
                                                tint = Color(0xFFEF4444),
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = "PDF Document",
                                                    color = Color(0xFFF9FAFB),
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "Click 'View Document' to open in browser",
                                                    color = Color(0xFF9CA3AF),
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    } else {
                                        // Display image
                                        NetworkImage(
                                            url = purchase.imageUrl,
                                            contentDescription = "Purchase Document",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Footer Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF06B6D4))
                    ) {
                        Text("Close", color = Color(0xFF111827))
                    }
                }
            }
        }
    }
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

@Composable
fun NetworkImage(
    url: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    var imageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    
    LaunchedEffect(url) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            var attempts = 0
            val maxAttempts = 3
            
            while (attempts < maxAttempts && imageBitmap == null && !hasError) {
                try {
                    attempts++
                    
                    // Validate and clean URL
                    var cleanUrl = url.trim()
                    if (cleanUrl.isEmpty()) {
                        throw Exception("Empty image URL")
                    }
                    
                    // Fix URLs with unencoded spaces (legacy URLs)
                    // Extract the path part after /o/ and before ?alt=media
                    if (cleanUrl.contains("/o/") && cleanUrl.contains("?alt=media")) {
                        val parts = cleanUrl.split("/o/")
                        if (parts.size == 2) {
                            val pathAndQuery = parts[1].split("?alt=media")
                            if (pathAndQuery.isNotEmpty()) {
                                val path = pathAndQuery[0]
                                // Decode, then re-encode properly
                                val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
                                val properlyEncodedPath = java.net.URLEncoder.encode(decodedPath, "UTF-8")
                                    .replace("+", "%20")
                                cleanUrl = "${parts[0]}/o/${properlyEncodedPath}?alt=media"
                            }
                        }
                    }
                    
                    println("Loading image (attempt $attempts/$maxAttempts)")
                    println("Original URL: $url")
                    if (url != cleanUrl) {
                        println("Fixed URL: $cleanUrl")
                    }
                    
                    // Create URL connection with proper settings
                    val urlObj = URL(cleanUrl)
                    val connection = urlObj.openConnection() as java.net.HttpURLConnection
                    connection.connectTimeout = 15000 // 15 seconds
                    connection.readTimeout = 15000 // 15 seconds
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                    connection.setRequestProperty("Accept", "image/webp,image/apng,image/*,*/*;q=0.8")
                    connection.instanceFollowRedirects = true
                    connection.doInput = true
                    
                    connection.connect()
                    
                    val responseCode = connection.responseCode
                    println("HTTP Response: $responseCode ${connection.responseMessage}")
                    
                    if (responseCode == 200) {
                        val inputStream = connection.inputStream
                        val bufferedImage: BufferedImage = ImageIO.read(inputStream)
                        inputStream.close()
                        
                        if (bufferedImage != null) {
                            imageBitmap = bufferedImage.toImageBitmap()
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                isLoading = false
                            }
                            println("Image loaded successfully!")
                        } else {
                            throw Exception("ImageIO returned null - unsupported format")
                        }
                    } else if (responseCode == 400) {
                        // For 400 errors, log the full response
                        val errorStream = connection.errorStream
                        val errorMsg = errorStream?.bufferedReader()?.readText() ?: "No error details"
                        errorStream?.close()
                        println("400 Error details: $errorMsg")
                        throw Exception("Bad Request (400) - Invalid URL format or permissions")
                    } else {
                        throw Exception("HTTP $responseCode: ${connection.responseMessage}")
                    }
                    
                    connection.disconnect()
                } catch (e: Exception) {
                    println("Error loading image (attempt $attempts/$maxAttempts): ${e.message}")
                    errorMessage = e.message ?: "Unknown error"
                    
                    if (attempts >= maxAttempts) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            hasError = true
                            isLoading = false
                        }
                    } else {
                        // Wait before retry
                        kotlinx.coroutines.delay(1000)
                    }
                }
            }
        }
    }
    
    Box(
        modifier = modifier.background(Color(0xFF1F2937), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        when {
            isLoading -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = Color(0xFF06B6D4),
                    strokeWidth = 3.dp
                )
            }
            hasError -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Failed to load image", color = Color(0xFF9CA3AF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (errorMessage.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(errorMessage, color = Color(0xFF6B7280), fontSize = 10.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = {
                            // Open image URL in browser
                            try {
                                val fixedUrl = fixImageUrl(url)
                                java.awt.Desktop.getDesktop().browse(java.net.URI(fixedUrl))
                            } catch (e: Exception) {
                                println("Error opening URL: ${e.message}")
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF06B6D4)
                        )
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Open in browser", modifier = Modifier.size(14.dp), tint = Color(0xFF06B6D4))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Open in browser", fontSize = 11.sp, color = Color(0xFF06B6D4))
                    }
                }
            }
            imageBitmap != null -> {
                androidx.compose.foundation.Image(
                    bitmap = imageBitmap!!,
                    contentDescription = contentDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit
                )
            }
        }
    }
}

private fun printPurchases(purchases: List<Purchase>) {
    try {
        val now = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        )

        // Build compact HTML mirroring the table view
        val html = buildString {
            append("""
                <html>
                <head>
                  <meta charset='UTF-8'/>
                  <style>
                    body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Arial, sans-serif; color: #111; }
                    .header { margin-bottom: 12px; }
                    .muted { color: #666; font-size: 12px; }
                    table { width: 100%; border-collapse: collapse; font-size: 12px; }
                    th { background: #f0f2f5; text-align: left; padding: 8px; border-bottom: 1px solid #e5e7eb; }
                    td { padding: 8px; border-bottom: 1px solid #f3f4f6; vertical-align: top; }
                    .right { text-align: right; }
                    .items { margin: 0; padding-left: 16px; color: #374151; }
                  </style>
                </head>
                <body>
                  <div class='header'>
                    <h2 style='margin:0 0 4px 0;'>Purchase Records</h2>
                    <div class='muted'>Generated on: $now • Total Records: ${purchases.size}</div>
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th style='width:14%'>Date</th>
                        <th style='width:26%'>Entity</th>
                        <th style='width:12%'>Items</th>
                        <th style='width:12%'>Total</th>
                        <th style='width:12%'>Paid</th>
                        <th style='width:12%'>Status</th>
                      </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            purchases.forEach { purchase ->
                val totalAmount = String.format("%.2f", purchase.grandTotal)
                val amountPaid = String.format("%.2f", purchase.amountPaid)
                val status = when (purchase.paymentStatus) {
                    com.humblecoders.plantmanagement.data.PaymentStatus.PAID -> "Paid"
                    com.humblecoders.plantmanagement.data.PaymentStatus.PARTIALLY_PAID -> "Partial"
                    else -> "Pending"
                }

                append("""
                    <tr>
                      <td>${purchase.purchaseDate}</td>
                      <td>${purchase.firmName}</td>
                      <td>${purchase.items.size}</td>
                      <td class='right'>Rs. $totalAmount</td>
                      <td class='right'>Rs. $amountPaid</td>
                      <td>$status</td>
                    </tr>
                """.trimIndent())

                if (purchase.items.isNotEmpty()) {
                    val itemsHtml = purchase.items.joinToString(separator = "") { item ->
                        """
                          <li>${item.itemName} - ${item.quantity} ${item.unit} @ Rs. ${String.format("%.2f", item.pricePerUnit)}</li>
                        """.trimIndent()
                    }
                    append("""
                        <tr>
                          <td colspan='6'>
                            <ul class='items'>
                              $itemsHtml
                            </ul>
                          </td>
                        </tr>
                    """.trimIndent())
                }
            }

            append("""
                    </tbody>
                  </table>
                </body>
                </html>
            """.trimIndent())
        }

        // Write to PDF using OpenHTMLToPDF
        val fileName = "purchase_records_${System.currentTimeMillis()}.pdf"
        val file = java.io.File(System.getProperty("user.home"), "Downloads/$fileName")

        java.io.FileOutputStream(file).use { os ->
            val builder = com.openhtmltopdf.pdfboxout.PdfRendererBuilder()
            builder.withHtmlContent(html, null)
            builder.toStream(os)
            builder.run()
        }

        println("Purchase records saved to: ${file.absolutePath}")

        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file)
            }
        } catch (e: Exception) {
            println("Could not open file automatically: ${e.message}")
        }

    } catch (e: Exception) {
        println("Error printing purchases (PDF): ${e.message}")
        e.printStackTrace()
    }
}

private fun BufferedImage.toImageBitmap(): ImageBitmap {
    val baos = ByteArrayOutputStream()
    ImageIO.write(this, "png", baos)
    val bytes = baos.toByteArray()
    
    return org.jetbrains.skia.Image.makeFromEncoded(bytes).asImageBitmap()
}

private fun fixImageUrl(url: String): String {
    return try {
        if (url.contains("/o/") && url.contains("?alt=media")) {
            val parts = url.split("/o/")
            if (parts.size == 2) {
                val pathAndQuery = parts[1].split("?alt=media")
                if (pathAndQuery.isNotEmpty()) {
                    val path = pathAndQuery[0]
                    // Decode, then re-encode properly
                    val decodedPath = java.net.URLDecoder.decode(path, "UTF-8")
                    val properlyEncodedPath = java.net.URLEncoder.encode(decodedPath, "UTF-8")
                        .replace("+", "%20")
                    return "${parts[0]}/o/${properlyEncodedPath}?alt=media"
                }
            }
        }
        url
    } catch (e: Exception) {
        println("Error fixing URL: ${e.message}")
        url
    }
}