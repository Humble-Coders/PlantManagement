package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.humblecoders.plantmanagement.data.Sale
import com.humblecoders.plantmanagement.data.SaleStatus
import com.humblecoders.plantmanagement.data.DiscountType
import com.humblecoders.plantmanagement.services.SalePdfGeneratorService
import com.humblecoders.plantmanagement.ui.DetailRow
import com.humblecoders.plantmanagement.utils.FileDialogUtils
import kotlinx.coroutines.launch

@Composable
fun ViewSaleDialog(
    sale: Sale,
    onDismiss: () -> Unit
) {
    val pdfService = remember { SalePdfGeneratorService() }
    val coroutineScope = rememberCoroutineScope()
    var isGeneratingPdf by remember { mutableStateOf(false) }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.width(700.dp).height(750.dp),
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
                        "Sale Details",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Download/Print Button
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                        isGeneratingPdf = true
                                    }
                                    try {
                                        val defaultFileName = "Sale_${sale.billNumber.replace(" ", "_")}_${sale.firmName.replace(" ", "_")}.pdf"
                                        
                                        val outputFile = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                            FileDialogUtils.showSaveDialog(
                                                title = "Save Sale Bill",
                                                defaultFilename = defaultFileName,
                                                allowedExtensions = listOf("pdf")
                                            )?.let { selectedFile ->
                                                val finalFile = if (!selectedFile.name.endsWith(".pdf", ignoreCase = true)) {
                                                    java.io.File(selectedFile.parent, "${selectedFile.name}.pdf")
                                                } else {
                                                    selectedFile
                                                }

                                                if (finalFile.exists()) {
                                                    finalFile.delete()
                                                }

                                                finalFile
                                            }
                                        }

                                        if (outputFile != null) {
                                            val pdfResult = pdfService.generateSaleBill(sale, outputFile)

                                            if (pdfResult.isSuccess) {
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
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF10B981))
                        ) {
                            if (isGeneratingPdf) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color(0xFF10B981),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.Print, contentDescription = "Download", tint = Color(0xFF10B981), modifier = Modifier.size(16.dp))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isGeneratingPdf) "Generating..." else "Download PDF", color = Color(0xFF10B981), fontSize = 12.sp)
                        }

                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                        }
                    }
                }

                // Scrollable Content
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 16.dp)
                ) {
                    // Basic Information
                    item {
                        Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Sale Information", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                                DetailRow("Customer", sale.firmName)
                                DetailRow("Sale Date", sale.saleDate)
                                DetailRow("Bill Number", sale.billNumber)
                                DetailRow("Portal Batch Number", sale.portalBatchNumber)
                            }
                        }
                    }

                    // Quantity Details
                    item {
                        Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Quantity Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                                DetailRow("Quantity", "${String.format("%.2f", sale.quantityKg)} kg (${sale.numberOfBags} bags)")
                                if (sale.extraQuantityKg > 0) {
                                    DetailRow("Extra Quantity (Indirect Discount)", "${String.format("%.2f", sale.extraQuantityKg)} kg")
                                    DetailRow("Total Quantity Sent", "${String.format("%.2f", sale.quantityKg + sale.extraQuantityKg)} kg", valueColor = Color(0xFF10B981))
                                }
                                DetailRow("Deducted from Inventory", if (sale.deductFromInventory) "Yes" else "No")
                            }
                        }
                    }

                    // Financial Calculations
                    item {
                        Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Financial Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))

                                DetailRow("Original Rate per Kg", "₹ ${String.format("%.2f", sale.originalRatePerKg)}")
                                if (sale.discountType == DiscountType.DISCOUNT_PREMIUM) {
                                    DetailRow("Discounted/Premium Rate per Kg", "₹ ${String.format("%.2f", sale.discountedRatePerKg)}")
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                DetailRow("Portal Amount", "₹ ${String.format("%.2f", sale.portalAmount)}")
                                DetailRow("GST (5%)", "₹ ${String.format("%.2f", sale.gstAmount)}")
                                DetailRow("Total Portal Amount", "₹ ${String.format("%.2f", sale.totalPortalAmount)}", valueColor = Color(0xFF10B981))

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(8.dp))

                                DetailRow("Revenue Amount", "₹ ${String.format("%.2f", sale.revenueAmount)}")
                                DetailRow("GST (5%)", "₹ ${String.format("%.2f", sale.gstAmount)}")
                                DetailRow("Total Revenue Amount", "₹ ${String.format("%.2f", sale.totalRevenueAmount)}", valueColor = Color(0xFF10B981))

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Difference Amount:", color = Color(0xFFF9FAFB), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "${if (sale.differenceAmount >= 0) "+" else ""}₹ ${String.format("%.2f", sale.differenceAmount)}",
                                            color = if (sale.differenceAmount >= 0) Color(0xFF10B981) else Color(0xFFEF4444),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = if (sale.differenceAmount >= 0) "(Customer pays us)" else "(We pay customer)",color = Color(0xFF9CA3AF),
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Payment Status
                    item {
                        Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Payment Status", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))

                                DetailRow("Portal Amount Paid", "₹ ${String.format("%.2f", sale.portalAmountPaid)}")
                                DetailRow("Portal Pending", "₹ ${String.format("%.2f", sale.totalPortalAmount - sale.portalAmountPaid)}", valueColor = Color(0xFFEF4444))
                                DetailRow(
                                    "Sale Status",
                                    sale.saleStatus.name.replace("_", " "),
                                    valueColor = when (sale.saleStatus) {
                                        SaleStatus.PAID -> Color(0xFF10B981)
                                        SaleStatus.PENDING -> Color(0xFFF59E0B)
                                        SaleStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(8.dp))

                                DetailRow("Difference Amount Paid", "₹ ${String.format("%.2f", sale.differenceAmountPaid)}")
                                DetailRow("Difference Pending", "₹ ${String.format("%.2f", if (sale.differenceAmount < 0) sale.differenceAmount + sale.differenceAmountPaid else sale.differenceAmount - sale.differenceAmountPaid)}", valueColor = Color(0xFFEF4444))
                                DetailRow(
                                    "Difference Status",
                                    sale.differenceStatus.name.replace("_", " "),
                                    valueColor = when (sale.differenceStatus) {
                                        com.humblecoders.plantmanagement.data.DifferenceStatus.PAID -> Color(0xFF10B981)
                                        com.humblecoders.plantmanagement.data.DifferenceStatus.PENDING -> Color(0xFFF59E0B)
                                        com.humblecoders.plantmanagement.data.DifferenceStatus.PARTIALLY_PAID -> Color(0xFF3B82F6)
                                    }
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider(color = Color(0xFF374151))
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Transport Details
                    if (sale.truckNumber.isNotBlank() || sale.fareAmount > 0) {
                        item {
                            Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Transport Details", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                                    if (sale.truckNumber.isNotBlank()) {
                                        DetailRow("Truck Number", sale.truckNumber)
                                    }
                                    if (sale.fareAmount > 0) {
                                        DetailRow("Fare Amount", "₹ ${String.format("%.2f", sale.fareAmount)}")
                                        DetailRow("Fare Paid By", sale.farePaidBy.name)
                                    }
                                }
                            }
                        }
                    }

                    // Documents
                    if (sale.imageUrls.isNotEmpty()) {
                        item {
                            Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Sale Documents", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    // Display documents in a grid
                                    androidx.compose.foundation.lazy.LazyRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        items(sale.imageUrls.size) { index ->
                                            val documentUrl = sale.imageUrls[index]
                                            SaleDocumentItem(
                                                documentUrl = documentUrl,
                                                documentIndex = index + 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Notes
                    if (sale.notes.isNotBlank()) {
                        item {
                            Card(backgroundColor = Color(0xFF111827), shape = RoundedCornerShape(8.dp)) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Notes", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                                    Divider(color = Color(0xFF374151), modifier = Modifier.padding(vertical = 4.dp))
                                    Text(sale.notes, color = Color(0xFFF9FAFB), fontSize = 14.sp)
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
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun SaleDocumentItem(
    documentUrl: String,
    documentIndex: Int
) {
    var showDocumentDialog by remember { mutableStateOf(false) }
    val isPdf = documentUrl.lowercase().contains(".pdf")
    
    Card(
        modifier = Modifier
            .size(120.dp)
            .clickable { showDocumentDialog = true },
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPdf) {
                // Show PDF icon
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf,
                            contentDescription = "PDF Document",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "PDF Document",
                            color = Color(0xFFF9FAFB),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "Click to view",
                            color = Color(0xFF9CA3AF),
                            fontSize = 8.sp
                        )
                    }
                }
            } else {
                // Show actual image thumbnail
                AsyncImage(
                    imageUrl = documentUrl,
                    contentDescription = "Sale Document $documentIndex",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    placeholder = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF374151)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color(0xFF10B981),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Loading...",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 10.sp
                                )
                            }
                        }
                    },
                    error = {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFF374151)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Image,
                                    contentDescription = "Image",
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Document $documentIndex",
                                    color = Color(0xFFF9FAFB),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Click to view",
                                    color = Color(0xFF9CA3AF),
                                    fontSize = 8.sp
                                )
                            }
                        }
                    }
                )
            }
            
            // Overlay with document number and click hint
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .background(
                        Color(0x80000000),
                        RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                    )
                    .padding(4.dp)
            ) {
                Text(
                    "Doc $documentIndex",
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
    
    // Document viewing dialog
    if (showDocumentDialog) {
        DocumentViewDialog(
            documentUrl = documentUrl,
            documentIndex = documentIndex,
            onDismiss = { showDocumentDialog = false }
        )
    }
}

@Composable
fun DocumentViewDialog(
    documentUrl: String,
    documentIndex: Int,
    onDismiss: () -> Unit
) {
    val isPdf = documentUrl.lowercase().contains(".pdf")
    
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .width(800.dp)
                .height(600.dp),
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
                        "Sale Document $documentIndex",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color(0xFF9CA3AF))
                    }
                }
                
                // Document content
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isPdf) {
                        // Show PDF info
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = "PDF Document",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(128.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "PDF Document",
                                color = Color(0xFFF9FAFB),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Click 'Open in Browser' to view the PDF",
                                color = Color(0xFF9CA3AF),
                                fontSize = 16.sp
                            )
                        }
                    } else {
                        // Show image
                        AsyncImage(
                            imageUrl = documentUrl,
                            contentDescription = "Sale Document $documentIndex",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit
                        )
                    }
                }
                
                // Footer with action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF111827))
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            // Open document in browser
                            try {
                                java.awt.Desktop.getDesktop().browse(java.net.URI(documentUrl))
                            } catch (e: Exception) {
                                println("Error opening browser: ${e.message}")
                            }
                        },
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981))
                    ) {
                        Text("Open in Browser", color = Color.White)
                    }
                    
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF6B7280))
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
    }
}