// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/ui/SaleScreen.kt
package com.humblecoders.plantmanagement.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.humblecoders.plantmanagement.ui.components.AddSaleDialog
import com.humblecoders.plantmanagement.viewmodels.EntityViewModel
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import com.humblecoders.plantmanagement.viewmodels.SaleSortField
import com.humblecoders.plantmanagement.viewmodels.SortDirection
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.ui.components.ViewSaleDialog
// Add these imports at the top of SaleScreen.kt
import com.humblecoders.plantmanagement.ui.components.CashInRevenueDialog
import com.humblecoders.plantmanagement.ui.components.CashInOutDifferenceDialog
import com.humblecoders.plantmanagement.ui.components.CashInHistoryDialog
import com.humblecoders.plantmanagement.utils.PdfExportUtils

@Composable
fun SaleScreen(
    saleViewModel: SaleViewModel,
    entityViewModel: EntityViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    storageService: com.humblecoders.plantmanagement.services.FirebaseStorageService,
    userRole: UserRole? = null
) {
    val saleState = saleViewModel.saleState
    val entityState = entityViewModel.entityState
    var showAddDialog by remember { mutableStateOf(false) }
    var showViewDialog by remember { mutableStateOf(false) }
    var saleToView by remember { mutableStateOf<Sale?>(null) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var saleToDelete by remember { mutableStateOf<Sale?>(null) }
    var showCashInRevenueDialog by remember { mutableStateOf(false) }
    var showCashInOutDifferenceDialog by remember { mutableStateOf(false) }
    var showCashInHistoryDialog by remember { mutableStateOf(false) }

    val isAdmin = userRole == UserRole.ADMIN

    LaunchedEffect(saleState.successMessage, saleState.error) {
        if (saleState.successMessage != null || saleState.error != null) {
            kotlinx.coroutines.delay(3000)
            saleViewModel.clearMessages()
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
                    text = "Sale Management",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFF9FAFB)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (saleState.successMessage != null) {
                Card(
                    backgroundColor = Color(0xFF10B981),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = saleState.successMessage,
                        color = Color.White,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (saleState.error != null) {
                Card(
                    backgroundColor = Color(0xFFEF4444),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = saleState.error,
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
                            text = "Add New Sale",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        // Add this Row with buttons
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showCashInRevenueDialog = true },
                                enabled = !saleState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF3B82F6),
                                    disabledBackgroundColor = Color(0xFF9CA3AF)
                                )
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = "Cash In", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Cash In Revenue", color = Color.White, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { showCashInOutDifferenceDialog = true },
                                enabled = !saleState.isLoading,
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = Color(0xFF8B5CF6),
                                    disabledBackgroundColor = Color(0xFF9CA3AF)
                                )
                            ) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Cash In/Out Difference", tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Difference", color = Color.White, fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = { showCashInHistoryDialog = true },
                                enabled = !saleState.isLoading,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                            ) {
                                Icon(Icons.Default.History, contentDescription = "History", tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("History", color = Color(0xFF10B981), fontSize = 13.sp)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = { showAddDialog = true },
                                    enabled = !saleState.isAdding && !saleState.isLoading,
                                    colors = ButtonDefaults.buttonColors(
                                        backgroundColor = Color(0xFF10B981),
                                        disabledBackgroundColor = Color(0xFF9CA3AF)
                                    )
                                ) {
                                    Text("Add Sale", color = Color.White)
                                }
                                
                                OutlinedButton(
                                    onClick = {
                                        val filteredSales = saleViewModel.getFilteredAndSortedSales()
                                        PdfExportUtils.exportSales(filteredSales)
                                    },
                                    enabled = saleState.sales.isNotEmpty(),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFF10B981)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Icon(Icons.Default.Print, contentDescription = "Print", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Print Sales", color = Color(0xFF10B981))
                                }
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
                            text = "Sale Records",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFF9FAFB)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = saleState.searchQuery,
                                onValueChange = { saleViewModel.updateSearchQuery(it) },
                                placeholder = { Text("Search sales...", color = Color(0xFF9CA3AF)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF9CA3AF))
                                },
                                modifier = Modifier.width(200.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    textColor = Color(0xFFF9FAFB),
                                    backgroundColor = Color(0xFF111827),
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color(0xFF374151),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                singleLine = true
                            )

                            Text("From:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                            DatePicker(
                                selectedDate = try { LocalDate.parse(saleState.filterDateFrom) } catch (e: Exception) { LocalDate.now() },
                                onDateSelected = { date -> saleViewModel.updateDateFilter(date.format(DateTimeFormatter.ISO_LOCAL_DATE), saleState.filterDateTo) },
                                modifier = Modifier.width(140.dp),
                                label = ""
                            )

                            Text("To:", color = Color(0xFF9CA3AF), fontSize = 12.sp)
                            DatePicker(
                                selectedDate = try { LocalDate.parse(saleState.filterDateTo) } catch (e: Exception) { LocalDate.now() },
                                onDateSelected = { date -> saleViewModel.updateDateFilter(saleState.filterDateFrom, date.format(DateTimeFormatter.ISO_LOCAL_DATE)) },
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
                                    Text("Sort: ${saleState.sortBy.name}")
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }

                                DropdownMenu(
                                    expanded = sortExpanded,
                                    onDismissRequest = { sortExpanded = false }
                                ) {
                                    DropdownMenuItem(onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.DATE)
                                        sortExpanded = false
                                    }) {
                                        Text("Date")
                                    }
                                    DropdownMenuItem(onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.ENTITY)
                                        sortExpanded = false
                                    }) {
                                        Text("Entity")
                                    }
                                    DropdownMenuItem(onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.BILL_NUMBER)
                                        sortExpanded = false
                                    }) {
                                        Text("Bill Number")
                                    }
                                    DropdownMenuItem(onClick = {
                                        saleViewModel.updateSortBy(SaleSortField.STATUS)
                                        sortExpanded = false
                                    }) {
                                        Text("Status")
                                    }
                                }
                            }

                            IconButton(
                                onClick = { saleViewModel.toggleSortDirection() }
                            ) {
                                Icon(
                                    imageVector = if (saleState.sortDirection == SortDirection.ASCENDING) {
                                        Icons.Default.ArrowDropUp
                                    } else {
                                        Icons.Default.ArrowDropDown
                                    },
                                    contentDescription = "Sort Direction",
                                    tint = Color(0xFF10B981)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    SaleTable(
                        sales = saleViewModel.getFilteredAndSortedSales(),
                        onDeleteClick = {
                            saleToDelete = it
                            showDeleteConfirmDialog = true
                        },
                        onViewClick = {
                            saleToView = it
                            showViewDialog = true
                        },
                        isAdmin = isAdmin
                    )
                }
            }
        }

        if (saleState.isLoading) {
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
                            color = Color(0xFF10B981),
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = when {
                                saleState.isAdding -> "Adding sale..."
                                saleState.isUpdating -> "Updating sale..."
                                saleState.isDeleting -> "Deleting sale..."
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
        AddSaleDialog(
            customers = entityState.entities,
            saleViewModel = saleViewModel,
            inventoryViewModel = inventoryViewModel,
            storageService = storageService,
            onDismiss = { showAddDialog = false },
            onSave = { sale ->
                saleViewModel.addSale(sale)
                showAddDialog = false
            }
        )
    }

    if (showViewDialog && saleToView != null) {
        ViewSaleDialog(
            sale = saleToView!!,
            onDismiss = {
                showViewDialog = false
                saleToView = null
            }
        )
    }

    if (showDeleteConfirmDialog && saleToDelete != null) {
        DeleteConfirmationDialog(
            itemName = "Sale #${saleToDelete!!.billNumber} to ${saleToDelete!!.firmName}",
            itemType = "sale",
            onConfirm = {
                saleViewModel.deleteSale(saleToDelete!!.id)
                showDeleteConfirmDialog = false
                saleToDelete = null
            },
            onDismiss = {
                showDeleteConfirmDialog = false
                saleToDelete = null
            }
        )
    }
    if (showCashInRevenueDialog) {
        CashInRevenueDialog(
            saleViewModel = saleViewModel,
            entityViewModel = entityViewModel,
            onDismiss = { showCashInRevenueDialog = false }
        )
    }

    if (showCashInOutDifferenceDialog) {
        CashInOutDifferenceDialog(
            saleViewModel = saleViewModel,
            entityViewModel = entityViewModel,
            onDismiss = { showCashInOutDifferenceDialog = false }
        )
    }

    if (showCashInHistoryDialog) {
        CashInHistoryDialog(
            saleViewModel = saleViewModel,
            onDismiss = { showCashInHistoryDialog = false }
        )
    }
}

@Composable
fun SaleTable(
    sales: List<Sale>,
    onDeleteClick: (Sale) -> Unit,
    onViewClick: (Sale) -> Unit,
    isAdmin: Boolean
) {
    // Single horizontal scroll for the entire table
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier.horizontalScroll(scrollState)
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .background(Color(0xFF374151))
                .padding(12.dp)
        ) {
            Text("Date", color = Color(0xFF9CA3AF), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Customer", color = Color(0xFF9CA3AF), modifier = Modifier.width(120.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Bill #", color = Color(0xFF9CA3AF), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Portal Batch", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Quantity", color = Color(0xFF9CA3AF), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Extra Qty", color = Color(0xFF9CA3AF), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Original Rate", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Discounted   Rate", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Total Portal Amount", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Revenue Amount", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Portal Pending", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Difference Amount", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Difference Pending", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Sale Status", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Diff Status", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Billing Status", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Deduct Inventory", color = Color(0xFF9CA3AF), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("Actions", color = Color(0xFF9CA3AF), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        Divider(color = Color(0xFF374151))

        // Data Rows
        sales.forEach { sale ->
            Row(
                modifier = Modifier
                    .background(Color(0xFF1F2937))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(sale.saleDate, color = Color(0xFFF9FAFB), modifier = Modifier.width(80.dp), fontSize = 12.sp)
                Text(sale.firmName, color = Color(0xFFF9FAFB), modifier = Modifier.width(120.dp), fontSize = 12.sp)
                Text(sale.billNumber, color = Color(0xFF10B981), modifier = Modifier.width(80.dp), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(sale.portalBatchNumber, color = Color(0xFFF9FAFB), modifier = Modifier.width(100.dp), fontSize = 12.sp)
                Text("${String.format("%.2f", sale.quantityKg)} kg", color = Color(0xFFF9FAFB), modifier = Modifier.width(80.dp), fontSize = 12.sp)
                Text("${String.format("%.2f", sale.extraQuantityKg)} kg", color = Color(0xFFF9FAFB), modifier = Modifier.width(80.dp), fontSize = 12.sp)
                Text("₹${String.format("%.2f", sale.originalRatePerKg)}", color = Color(0xFFF9FAFB), modifier = Modifier.width(100.dp), fontSize = 12.sp)
                Text("₹${String.format("%.2f", sale.discountedRatePerKg)}", color = Color(0xFFF9FAFB), modifier = Modifier.width(100.dp), fontSize = 12.sp)
                Text("₹${String.format("%.2f", sale.totalPortalAmount)}", color = Color(0xFFF9FAFB), modifier = Modifier.width(100.dp), fontSize = 12.sp)
                Text("₹${String.format("%.2f", sale.totalRevenueAmount)}", color = Color(0xFFF9FAFB), modifier = Modifier.width(100.dp), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                
                Text(
                    "₹${String.format("%.2f", sale.totalPortalAmount - sale.portalAmountPaid)}",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.width(100.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "${if (sale.differenceAmount >= 0) "+" else ""}₹${String.format("%.2f", sale.differenceAmount)}",
                    color = if (sale.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.width(100.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = "₹${String.format("%.2f", if (sale.differenceAmount < 0) sale.differenceAmount + sale.differenceAmountPaid else sale.differenceAmount - sale.differenceAmountPaid)}",
                    color = Color(0xFFEF4444),
                    modifier = Modifier.width(100.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = sale.saleStatus.name.replace("_", " "),
                    color = when (sale.saleStatus) {
                        SaleStatus.PAID -> Color(0xFF10B981)
                        SaleStatus.PENDING -> Color(0xFFF59E0B)
                        SaleStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = sale.differenceStatus.name.replace("_", " "),
                    color = when (sale.differenceStatus) {
                        com.humblecoders.plantmanagement.data.DifferenceStatus.PAID -> Color(0xFF10B981)
                        com.humblecoders.plantmanagement.data.DifferenceStatus.PENDING -> Color(0xFFF59E0B)
                        com.humblecoders.plantmanagement.data.DifferenceStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = sale.billingStatus.name.replace("_", " "),
                    color = when (sale.billingStatus) {
                        BillingStatus.BILLED -> Color(0xFF10B981)
                        BillingStatus.PENDING_BILLED -> Color(0xFFF59E0B)
                    },
                    modifier = Modifier.width(100.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (sale.deductFromInventory) "Yes" else "No",
                    color = if (sale.deductFromInventory) Color(0xFF10B981) else Color(0xFFEF4444),
                    modifier = Modifier.width(80.dp),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.width(100.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    TextButton(onClick = { onViewClick(sale) }) {
                        Text("View", color = Color(0xFF3B82F6), fontSize = 10.sp)
                    }
                    if (isAdmin) {
                        IconButton(onClick = { onDeleteClick(sale) }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Divider(color = Color(0xFF374151))
        }

        if (sales.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("No sale records found", color = Color(0xFF9CA3AF))
            }
        }
    }
}