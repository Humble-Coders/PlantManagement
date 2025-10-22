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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.*
import com.humblecoders.plantmanagement.viewmodels.SaleViewModel
import com.humblecoders.plantmanagement.ui.components.DatePicker
import com.humblecoders.plantmanagement.services.FirebaseStorageService
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun AddSaleDialog(
    customers: List<Entity>,
    saleViewModel: SaleViewModel,
    inventoryViewModel: com.humblecoders.plantmanagement.viewmodels.InventoryViewModel,
    storageService: FirebaseStorageService,
    onDismiss: () -> Unit,
    onSave: (Sale) -> Unit,
    preFilledSale: Sale? = null, // Optional pre-filled sale data
    isClearingBill: Boolean = false // Flag to indicate if this is for clearing a bill
) {
    var selectedEntityId by remember { mutableStateOf(preFilledSale?.customerId ?: "") }
    var saleDate by remember {
        mutableStateOf(
            preFilledSale?.saleDate?.let { LocalDate.parse(it) } ?: LocalDate.now()
        )
    }
    var billNumber by remember { mutableStateOf(preFilledSale?.billNumber ?: "") }
    var portalBatchNumber by remember { mutableStateOf(preFilledSale?.portalBatchNumber ?: "") }

    var quantityKg by remember {
        mutableStateOf(
            if (isClearingBill) preFilledSale?.quantityKg?.toString() ?: "" else ""
        )
    }
    var numberOfBags by remember {
        mutableStateOf(
            if (isClearingBill) preFilledSale?.numberOfBags?.toString() ?: "" else ""
        )
    }
    var deductFromInventory by remember {
        mutableStateOf(
            preFilledSale?.deductFromInventory ?: true
        )
    }

    var originalRatePerKg by remember {
        mutableStateOf(
            preFilledSale?.originalRatePerKg?.toString() ?: ""
        )
    }

    var discountType by remember {
        mutableStateOf(
            preFilledSale?.discountType ?: DiscountType.NONE
        )
    }
    var discountedRatePerKg by remember {
        mutableStateOf(
            preFilledSale?.discountedRatePerKg?.toString() ?: ""
        )
    }
    var extraQuantityKg by remember {
        mutableStateOf(
            preFilledSale?.extraQuantityKg?.toString() ?: ""
        )
    }

    var portalAmountPaid by remember {
        mutableStateOf(
            preFilledSale?.portalAmountPaid?.toString() ?: ""
        )
    }

    var truckNumber by remember { mutableStateOf(preFilledSale?.truckNumber ?: "") }
    var fareAmount by remember { mutableStateOf(preFilledSale?.fareAmount?.toString() ?: "") }
    var farePaidBy by remember { mutableStateOf(preFilledSale?.farePaidBy ?: FarePaidBy.COMPANY) }

    var notes by remember { mutableStateOf(preFilledSale?.notes ?: "") }

    // Image upload state
    var selectedImages by remember { mutableStateOf<List<File>>(emptyList()) }
    var isUploadingImages by remember { mutableStateOf(false) }

    // Inventory validation state
    var inventoryError by remember { mutableStateOf("") }
    var availableInventory by remember { mutableStateOf(0.0) }

    val focusRequesters = remember { List(10) { FocusRequester() } }

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
        if (numberOfBags.isNotBlank() && numberOfBags != (quantityKg.toDoubleOrNull()?.div(25.0)
                ?: 0.0).toInt().toString()
        ) {
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

    // Inventory validation
    LaunchedEffect(quantityKg, extraQuantityKg, deductFromInventory, discountType) {
        inventoryError = ""

        if (deductFromInventory) {
            val qty = quantityKg.toDoubleOrNull() ?: 0.0
            val extraQty = extraQuantityKg.toDoubleOrNull() ?: 0.0

            // Calculate total quantity needed
            val totalQuantityNeeded = if (discountType == DiscountType.INDIRECT_DISCOUNT) {
                qty + extraQty // For indirect discount, we need both main quantity + extra quantity
            } else {
                qty // For other discount types, only main quantity
            }

            if (totalQuantityNeeded > availableInventory) {
                inventoryError = "Insufficient inventory! Available: ${
                    String.format(
                        "%.2f",
                        availableInventory
                    )
                } kg, Required: ${String.format("%.2f", totalQuantityNeeded)} kg"
            }
        }
    }

    // Calculate amounts
    val calculation = remember(
        quantityKg,
        originalRatePerKg,
        discountType,
        discountedRatePerKg,
        extraQuantityKg
    ) {
        val qty = quantityKg.toDoubleOrNull() ?: 0.0
        val rate = originalRatePerKg.toDoubleOrNull() ?: 0.0
        val discRate = discountedRatePerKg.toDoubleOrNull() ?: 0.0
        val extraQty = extraQuantityKg.toDoubleOrNull() ?: 0.0

        saleViewModel.calculateSaleAmounts(qty, rate, discountType, discRate, extraQty)
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(850.dp)
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
                        "Add New Sale",
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
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Customer Selection
                    item {
                        var entityExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedButton(
                                onClick = { entityExpanded = true },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
                            ) {
                                Text(
                                    text = if (selectedEntityId.isBlank()) "Select Customer"
                                    else customers.find { it.id == selectedEntityId }?.firmName
                                        ?: "Select Customer",
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }

                            DropdownMenu(
                                expanded = entityExpanded,
                                onDismissRequest = { entityExpanded = false }) {
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
                    }

                    // Sale Date
                    item {
                        DatePicker(
                            selectedDate = saleDate,
                            onDateSelected = { saleDate = it },
                            label = "Sale Date",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Bill Number and Portal Batch Number
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = billNumber,
                                onValueChange = { billNumber = it },
                                label = { Text("Bill Number", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[0]),
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
                                value = portalBatchNumber,
                                onValueChange = { portalBatchNumber = it },
                                label = { Text("Portal Batch Number", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[1]),
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

                    // Quantity Section
                    item {
                        Text(
                            "Quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = quantityKg,
                                onValueChange = {
                                    if (!isClearingBill && (it.isEmpty() || it.matches(
                                            Regex("[0-9]*\\.?[0-9]*")
                                        ))
                                    ) quantityKg = it
                                },
                                label = { Text("Quantity (Kg)", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[2]),
                                readOnly = isClearingBill,
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
                                onValueChange = {
                                    if (!isClearingBill && (it.isEmpty() || it.matches(
                                            Regex("[0-9]*")
                                        ))
                                    ) numberOfBags = it
                                },
                                label = { Text("Number of Bags", color = Color(0xFF9CA3AF)) },
                                modifier = Modifier.weight(1f).focusRequester(focusRequesters[3]),
                                readOnly = isClearingBill,
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

                    // Deduct from Inventory checkbox (only show when not clearing a bill)
                    if (!isClearingBill) {
                        item {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Checkbox(
                                    checked = deductFromInventory,
                                    onCheckedChange = { deductFromInventory = it },
                                    colors = CheckboxDefaults.colors(checkedColor = Color(0xFF10B981))
                                )
                                Text(
                                    "Deduct from Inventory",
                                    color = Color(0xFFF9FAFB),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    // Divider
                    item { Divider(color = Color(0xFF374151)) }

                    // Pricing Section
                    item {
                        Text(
                            "Pricing & Discount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = originalRatePerKg,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) originalRatePerKg =
                                    it
                            },
                            label = { Text("Original Rate per Kg (₹)", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[4]),
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

                    // Discount Type Selection
                    item {
                        Text(
                            "Discount Type",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { discountType = DiscountType.NONE },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (discountType == DiscountType.NONE) Color(
                                        0xFF10B981
                                    ) else Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("No Discount")
                            }
                            OutlinedButton(
                                onClick = { discountType = DiscountType.DISCOUNT_PREMIUM },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (discountType == DiscountType.DISCOUNT_PREMIUM) Color(
                                        0xFF10B981
                                    ) else Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Discount/Premium")
                            }
                            OutlinedButton(
                                onClick = { discountType = DiscountType.INDIRECT_DISCOUNT },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (discountType == DiscountType.INDIRECT_DISCOUNT) Color(
                                        0xFF10B981
                                    ) else Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Indirect Discount")
                            }
                        }
                    }

                    // Discount/Premium Rate Input
                    if (discountType == DiscountType.DISCOUNT_PREMIUM) {
                        item {
                            OutlinedTextField(
                                value = discountedRatePerKg,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) discountedRatePerKg =
                                        it
                                },
                                label = {
                                    Text(
                                        "Discounted/Premium Rate per Kg (₹)",
                                        color = Color(0xFF9CA3AF)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .focusRequester(focusRequesters[5]),
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

                    // Extra Quantity Input
                    if (discountType == DiscountType.INDIRECT_DISCOUNT) {
                        item {
                            OutlinedTextField(
                                value = extraQuantityKg,
                                onValueChange = {
                                    if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) extraQuantityKg =
                                        it
                                },
                                label = {
                                    Text(
                                        "Extra Quantity to Send (Kg)",
                                        color = Color(0xFF9CA3AF)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth()
                                    .focusRequester(focusRequesters[6]),
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

                    // Calculations Display
                    item {
                        Card(
                            backgroundColor = Color(0xFF111827),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Calculations",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Portal Amount:",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "₹${String.format("%.2f", calculation.portalAmount)}",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST (5%):", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                    Text(
                                        "₹${String.format("%.2f", calculation.gstAmount)}",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Total Portal Amount:",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "₹${String.format("%.2f", calculation.totalPortalAmount)}",
                                        color = Color(0xFF10B981),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Revenue Amount:",
                                        color = Color(0xFF9CA3AF),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "₹${String.format("%.2f", calculation.revenueAmount)}",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("GST (5%):", color = Color(0xFF9CA3AF), fontSize = 13.sp)
                                    Text(
                                        "₹${String.format("%.2f", calculation.gstAmount)}",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 13.sp
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Total Revenue Amount:",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        "₹${
                                            String.format(
                                                "%.2f",
                                                calculation.totalRevenueAmount
                                            )
                                        }",
                                        color = Color(0xFF10B981),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Difference Amount:",
                                        color = Color(0xFFF9FAFB),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${if (calculation.differenceAmount >= 0) "+" else ""}₹${
                                            String.format(
                                                "%.2f",
                                                calculation.differenceAmount
                                            )
                                        }",
                                        color = if (calculation.differenceAmount >= 0) Color(
                                            0xFF10B981
                                        ) else Color(0xFFEF4444),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = if (calculation.differenceAmount >= 0) "(Customer pays us)" else "(We pay customer)",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 11.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                )
                            }
                        }
                    }

                    // Divider
                    item { Divider(color = Color(0xFF374151)) }

                    // Payment Section
                    item {
                        Text(
                            "Payment Details",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = portalAmountPaid,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) portalAmountPaid =
                                    it
                            },
                            label = { Text("Portal Amount Paid (₹)", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().focusRequester(focusRequesters[7]),
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


                    // Divider
                    item { Divider(color = Color(0xFF374151)) }

                    // Transport Section
                    item {
                        Text(
                            "Transport Details (Optional)",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF10B981)
                        )
                    }

                    item {
                        OutlinedTextField(
                            value = truckNumber,
                            onValueChange = { truckNumber = it },
                            label = { Text("Truck Number", color = Color(0xFF9CA3AF)) },
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

                    item {
                        OutlinedTextField(
                            value = fareAmount,
                            onValueChange = {
                                if (it.isEmpty() || it.matches(Regex("[0-9]*\\.?[0-9]*"))) fareAmount =
                                    it
                            },
                            label = { Text("Fare Amount (₹)", color = Color(0xFF9CA3AF)) },
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

                    item {
                        Text(
                            "Fare Paid By",
                            color = Color(0xFFF9FAFB),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }

                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = { farePaidBy = FarePaidBy.COMPANY },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (farePaidBy == FarePaidBy.COMPANY) Color(
                                        0xFF10B981
                                    ) else Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Company")
                            }
                            OutlinedButton(
                                onClick = { farePaidBy = FarePaidBy.CUSTOMER },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (farePaidBy == FarePaidBy.CUSTOMER) Color(
                                        0xFF10B981
                                    ) else Color.Black
                                ),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Customer")
                            }
                        }
                    }

                    // Image Upload Section
                    item {
                        Divider(color = Color(0xFF374151))
                    }

                    item {
                        ImageUploadComponent(
                            selectedImages = selectedImages,
                            onImagesSelected = { selectedImages = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Notes
                    item {
                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it },
                            label = { Text("Notes (Optional)", color = Color(0xFF9CA3AF)) },
                            modifier = Modifier.fillMaxWidth().height(80.dp)
                                .focusRequester(focusRequesters[9]),
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

                    // Inventory Error Display
                    if (inventoryError.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
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
                            // Check inventory validation before saving
                            if (inventoryError.isNotEmpty()) {
                                return@Button // Don't save if there's an inventory error
                            }

                            // Start image upload process
                            if (selectedImages.isNotEmpty()) {
                                isUploadingImages = true

                                // Upload images in background - we'll handle this with a coroutine scope
                                GlobalScope.launch {
                                    val imageUrls = mutableListOf<String>()

                                    for (imageFile in selectedImages) {
                                        val uploadResult =
                                            storageService.uploadImage(imageFile, "sales")
                                        if (uploadResult.isSuccess) {
                                            imageUrls.add(uploadResult.getOrNull() ?: "")
                                        }
                                    }

                                    isUploadingImages = false

                                    // Create and save sale with uploaded image URLs
                                    val selectedEntity =
                                        customers.find { it.id == selectedEntityId }
                                    val qty = quantityKg.toDoubleOrNull() ?: 0.0
                                    val bags = (qty / 25.0).toInt()
                                    val portalPaid = portalAmountPaid.toDoubleOrNull() ?: 0.0

                                    val saleStatus = when {
                                        portalPaid >= calculation.totalPortalAmount -> SaleStatus.PAID
                                        portalPaid > 0 -> SaleStatus.PARTIALLY_PAID
                                        else -> SaleStatus.PENDING
                                    }

                                    // Difference status: PAID if zero difference, PENDING otherwise
                                    val differenceStatus =
                                        if (kotlin.math.abs(calculation.differenceAmount) == 0.0) {
                                            DifferenceStatus.PAID
                                        } else {
                                            DifferenceStatus.PENDING
                                        }

                                    onSave(
                                        Sale(
                                            customerId = selectedEntityId,
                                            firmName = selectedEntity?.firmName ?: "",
                                            saleDate = saleDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                            billNumber = billNumber,
                                            portalBatchNumber = portalBatchNumber,
                                            quantityKg = qty,
                                            numberOfBags = bags,
                                            deductFromInventory = deductFromInventory,
                                            originalRatePerKg = originalRatePerKg.toDoubleOrNull()
                                                ?: 0.0,
                                            portalAmount = calculation.portalAmount,
                                            gstAmount = calculation.gstAmount,
                                            totalPortalAmount = calculation.totalPortalAmount,
                                            discountType = discountType,
                                            discountedRatePerKg = discountedRatePerKg.toDoubleOrNull()
                                                ?: 0.0,
                                            extraQuantityKg = extraQuantityKg.toDoubleOrNull()
                                                ?: 0.0,
                                            revenueAmount = calculation.revenueAmount,
                                            totalRevenueAmount = calculation.totalRevenueAmount,
                                            differenceAmount = calculation.differenceAmount,
                                            portalAmountPaid = portalPaid,
                                            saleStatus = saleStatus,
                                            differenceAmountPaid = 0.0, // Always 0 when adding a sale
                                            differenceStatus = differenceStatus,
                                            clearedInventory = qty,
                                            truckNumber = truckNumber,
                                            fareAmount = fareAmount.toDoubleOrNull() ?: 0.0,
                                            farePaidBy = farePaidBy,
                                            notes = notes,
                                            imageUrls = imageUrls
                                        )
                                    )
                                }
                            } else {
                                // No images to upload, save directly
                                val selectedEntity = customers.find { it.id == selectedEntityId }
                                val qty = quantityKg.toDoubleOrNull() ?: 0.0
                                val bags = (qty / 25.0).toInt()
                                val portalPaid = portalAmountPaid.toDoubleOrNull() ?: 0.0

                                val saleStatus = when {
                                    portalPaid >= calculation.totalPortalAmount -> SaleStatus.PAID
                                    portalPaid > 0 -> SaleStatus.PARTIALLY_PAID
                                    else -> SaleStatus.PENDING
                                }

                                // Difference status: PAID if zero difference, PENDING otherwise
                                val differenceStatus =
                                    if (kotlin.math.abs(calculation.differenceAmount) == 0.0) {
                                        DifferenceStatus.PAID
                                    } else {
                                        DifferenceStatus.PENDING
                                    }

                                onSave(
                                    Sale(
                                        customerId = selectedEntityId,
                                        firmName = selectedEntity?.firmName ?: "",
                                        saleDate = saleDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                                        billNumber = billNumber,
                                        portalBatchNumber = portalBatchNumber,
                                        quantityKg = qty,
                                        numberOfBags = bags,
                                        deductFromInventory = deductFromInventory,
                                        originalRatePerKg = originalRatePerKg.toDoubleOrNull()
                                            ?: 0.0,
                                        portalAmount = calculation.portalAmount,
                                        gstAmount = calculation.gstAmount,
                                        totalPortalAmount = calculation.totalPortalAmount,
                                        discountType = discountType,
                                        discountedRatePerKg = discountedRatePerKg.toDoubleOrNull()
                                            ?: 0.0,
                                        extraQuantityKg = extraQuantityKg.toDoubleOrNull() ?: 0.0,
                                        revenueAmount = calculation.revenueAmount,
                                        totalRevenueAmount = calculation.totalRevenueAmount,
                                        differenceAmount = calculation.differenceAmount,
                                        portalAmountPaid = portalPaid,
                                        saleStatus = saleStatus,
                                        differenceAmountPaid = 0.0, // Always 0 when adding a sale
                                        differenceStatus = differenceStatus,
                                        clearedInventory = qty,
                                        truckNumber = truckNumber,
                                        fareAmount = fareAmount.toDoubleOrNull() ?: 0.0,
                                        farePaidBy = farePaidBy,
                                        notes = notes,
                                        imageUrls = emptyList()
                                    )
                                )
                            }
                        },
                        enabled = selectedEntityId.isNotBlank() && billNumber.isNotBlank() &&
                                portalBatchNumber.isNotBlank() && quantityKg.isNotBlank() &&
                                originalRatePerKg.isNotBlank() && inventoryError.isEmpty() && !isUploadingImages,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = if (inventoryError.isNotEmpty() || isUploadingImages) Color(
                                0xFF6B7280
                            ) else Color(0xFF10B981),
                            disabledBackgroundColor = Color(0xFF9CA3AF)
                        )
                    ) {
                        if (isUploadingImages) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Uploading Images...", color = Color.White)
                            }
                        } else {
                            Text("Add Sale", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequesters[0].requestFocus()
    }
}