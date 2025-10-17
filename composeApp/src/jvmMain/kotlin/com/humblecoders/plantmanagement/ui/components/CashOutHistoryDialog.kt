package com.humblecoders.plantmanagement.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Print
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.humblecoders.plantmanagement.data.CashOut
import com.humblecoders.plantmanagement.viewmodels.PurchaseViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun CashOutHistoryDialog(
    purchaseViewModel: PurchaseViewModel,
    onDismiss: () -> Unit
) {
    val cashOuts = purchaseViewModel.purchaseState.cashOuts.sortedByDescending { it.createdAt?.seconds ?: 0L }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            backgroundColor = Color(0xFF1F2937),
            elevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Cash Out History",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF9FAFB)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = {
                                exportCashOutsToPdf(cashOuts)
                            },
                            colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF10B981), contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = "Print", tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Print/Download")
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color(0xFF9CA3AF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (cashOuts.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No cash out history available",
                            color = Color(0xFF9CA3AF),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cashOuts) { cashOut ->
                            CashOutHistoryItem(cashOut = cashOut)
                        }
                    }
                }
            }
        }
    }
}

private fun exportCashOutsToPdf(cashOuts: List<CashOut>) {
    try {
        val now = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        )

        val html = buildString {
            append(
                """
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
                    <h2 style='margin:0 0 4px 0;'>Cash Out History</h2>
                    <div class='muted'>Generated on: $now • Total Records: ${cashOuts.size}</div>
                  </div>
                  <table>
                    <thead>
                      <tr>
                        <th style='width:28%'>Date</th>
                        <th style='width:20%'>Amount</th>
                        <th style='width:52%'>Allocations</th>
                      </tr>
                    </thead>
                    <tbody>
                """.trimIndent()
            )

            val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")

            cashOuts.forEach { cashOut ->
                val formattedDate = cashOut.createdAt?.let {
                    Instant.ofEpochSecond(it.seconds)
                        .atZone(ZoneId.systemDefault())
                        .format(dateFormatter)
                } ?: "Unknown Date"
                val amountStr = String.format("%.2f", cashOut.totalAmount)

                append(
                    """
                    <tr>
                      <td>$formattedDate</td>
                      <td class='right'>Rs. $amountStr</td>
                      <td>
                """.trimIndent()
                )

                if (cashOut.purchaseAllocations.isNotEmpty()) {
                    val itemsHtml = cashOut.purchaseAllocations.joinToString(separator = "") { alloc ->
                        """
                          <div>• ${alloc.firmName} (${alloc.purchaseDate}) — Rs. ${String.format("%.2f", alloc.allocatedAmount)} (${alloc.newPaymentStatus.name.replace("_", " ")})</div>
                        """.trimIndent()
                    }
                    append(itemsHtml)
                } else {
                    append("No allocations")
                }

                if (cashOut.notes.isNotBlank()) {
                    append("<div style='color:#6b7280;margin-top:4px;'>Notes: ${cashOut.notes}</div>")
                }

                append("""
                      </td>
                    </tr>
                """.trimIndent())
            }

            append(
                """
                    </tbody>
                  </table>
                </body>
                </html>
                """.trimIndent()
            )
        }

        val fileName = "cash_out_history_${System.currentTimeMillis()}.pdf"
        val file = java.io.File(System.getProperty("user.home"), "Downloads/$fileName")
        java.io.FileOutputStream(file).use { os ->
            val builder = com.openhtmltopdf.pdfboxout.PdfRendererBuilder()
            builder.withHtmlContent(html, null)
            builder.toStream(os)
            builder.run()
        }
        println("Cash out history saved to: ${file.absolutePath}")

        try {
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(file)
            }
        } catch (_: Exception) {}

    } catch (e: Exception) {
        println("Error exporting cash outs: ${e.message}")
    }
}

@Composable
private fun CashOutHistoryItem(cashOut: CashOut) {
    var expanded by remember { mutableStateOf(false) }
    
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a")
    val formattedDate = cashOut.createdAt?.let {
        Instant.ofEpochSecond(it.seconds)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    } ?: "Unknown Date"

    Card(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = Color(0xFF374151),
        shape = RoundedCornerShape(12.dp),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedDate,
                        color = Color(0xFF9CA3AF),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "₹${String.format("%.2f", cashOut.totalAmount)}",
                        color = Color(0xFF10B981),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                TextButton(
                    onClick = { expanded = !expanded },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = Color(0xFF10B981)
                    )
                ) {
                    Text(if (expanded) "Hide Details" else "Show Details")
                }
            }

            // Notes
            if (cashOut.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Notes: ${cashOut.notes}",
                    color = Color(0xFF9CA3AF),
                    fontSize = 14.sp,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            // Allocations Summary
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${cashOut.purchaseAllocations.size} Purchase(s) Paid",
                    color = Color(0xFFF9FAFB),
                    fontSize = 14.sp
                )
                Text(
                    text = "Total: ₹${String.format("%.2f", cashOut.purchaseAllocations.sumOf { it.allocatedAmount })}",
                    color = Color(0xFFFBBF24),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Expanded Details
            if (expanded) {
                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color(0xFF4B5563), thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                // Column Headers
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF1F2937))
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Firm",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1.5f),
                        fontSize = 13.sp
                    )
                    Text(
                        "Date",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp
                    )
                    Text(
                        "Amount",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(0.8f),
                        fontSize = 13.sp
                    )
                    Text(
                        "Status",
                        color = Color(0xFFF9FAFB),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f),
                        fontSize = 13.sp
                    )
                }

                cashOut.purchaseAllocations.forEach { allocation ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = allocation.firmName,
                            color = Color(0xFFF9FAFB),
                            modifier = Modifier.weight(1.5f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = allocation.purchaseDate,
                            color = Color(0xFF9CA3AF),
                            modifier = Modifier.weight(1f),
                            fontSize = 13.sp
                        )
                        Text(
                            text = "₹${String.format("%.2f", allocation.allocatedAmount)}",
                            color = Color(0xFF10B981),
                            modifier = Modifier.weight(0.8f),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        val statusColor = when (allocation.newPaymentStatus.name) {
                            "PAID" -> Color(0xFF10B981)
                            "PARTIALLY_PAID" -> Color(0xFFFBBF24)
                            else -> Color(0xFFEF4444)
                        }
                        
                        Card(
                            backgroundColor = statusColor.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = when (allocation.newPaymentStatus.name) {
                                    "PAID" -> "Paid"
                                    "PARTIALLY_PAID" -> "Partial"
                                    else -> "Pending"
                                },
                                color = statusColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

