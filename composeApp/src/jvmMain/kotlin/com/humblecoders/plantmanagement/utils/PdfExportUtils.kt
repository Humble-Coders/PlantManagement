package com.humblecoders.plantmanagement.utils

import com.humblecoders.plantmanagement.data.InventoryItem
import com.humblecoders.plantmanagement.data.Entity

object PdfExportUtils {
    
    fun exportToPdf(
        title: String,
        fileName: String,
        data: List<Any>,
        columns: List<ColumnDefinition>,
        itemDetailsExtractor: (Any) -> String? = { null }
    ) {
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
                        .details { margin: 0; padding-left: 16px; color: #374151; }
                      </style>
                    </head>
                    <body>
                      <div class='header'>
                        <h2 style='margin:0 0 4px 0;'>$title</h2>
                        <div class='muted'>Generated on: $now • Total Records: ${data.size}</div>
                      </div>
                      <table>
                        <thead>
                          <tr>
                    """.trimIndent()
                )

                columns.forEach { column ->
                    append("""<th style='width:${column.width}'>${column.header}</th>""")
                }

                append("""
                        </tr>
                      </thead>
                      <tbody>
                """.trimIndent())

                data.forEach { item ->
                    append("<tr>")
                    columns.forEach { column ->
                        val value = column.extractor(item)
                        val alignment = if (column.rightAlign) " class='right'" else ""
                        append("<td$alignment>$value</td>")
                    }
                    append("</tr>")
                    
                    // Add item details if available
                    val details = itemDetailsExtractor(item)
                    if (details != null) {
                        append("""
                            <tr>
                              <td colspan='${columns.size}' class='details'>$details</td>
                            </tr>
                        """.trimIndent())
                    }
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

            val file = java.io.File(System.getProperty("user.home"), "Downloads/$fileName")
            java.io.FileOutputStream(file).use { os ->
                val builder = com.openhtmltopdf.pdfboxout.PdfRendererBuilder()
                builder.withHtmlContent(html, null)
                builder.toStream(os)
                builder.run()
            }
            println("$title saved to: ${file.absolutePath}")

            try {
                if (java.awt.Desktop.isDesktopSupported()) {
                    java.awt.Desktop.getDesktop().open(file)
                }
            } catch (e: Exception) {
                println("Could not open file automatically: ${e.message}")
            }

        } catch (e: Exception) {
            println("Error exporting $title: ${e.message}")
            e.printStackTrace()
        }
    }

    data class ColumnDefinition(
        val header: String,
        val width: String,
        val extractor: (Any) -> String,
        val rightAlign: Boolean = false
    )

    // Inventory export
    fun exportInventoryItems(items: List<InventoryItem>) {
        val columns = listOf(
            ColumnDefinition("Name", "25%", { (it as InventoryItem).name }),
            ColumnDefinition("Quantity", "15%", { String.format("%.2f", (it as InventoryItem).quantity) }, true),
            ColumnDefinition("Unit", "10%", { (it as InventoryItem).unit }),
            ColumnDefinition("Category", "15%", { (it as InventoryItem).categoryType.name.replace("_", " ") }),
            ColumnDefinition("Avg. Purchase Price", "20%", { "Rs. ${String.format("%.2f", (it as InventoryItem).averagePurchasePrice)}" }, true),
            ColumnDefinition("Created", "15%", { 
                (it as InventoryItem).createdAt?.let { timestamp ->
                    java.time.Instant.ofEpochSecond(timestamp.seconds)
                        .atZone(java.time.ZoneId.systemDefault())
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                } ?: "Unknown"
            })
        )

        exportToPdf(
            title = "Inventory Items",
            fileName = "inventory_items_${System.currentTimeMillis()}.pdf",
            data = items,
            columns = columns
        )
    }

    // Customer export
    fun exportCustomers(customers: List<Entity>) {
        val columns = listOf(
            ColumnDefinition("Firm Name", "25%", { (it as Entity).firmName }),
            ColumnDefinition("Contact Person", "20%", { (it as Entity).contactPerson }),
            ColumnDefinition("Contact No", "15%", { (it as Entity).contactNo }),
            ColumnDefinition("City", "15%", { (it as Entity).city }),
            ColumnDefinition("State", "10%", { (it as Entity).state }),
            ColumnDefinition("GSTIN", "15%", { (it as Entity).gstin })
        )

        exportToPdf(
            title = "Customers",
            fileName = "customers_${System.currentTimeMillis()}.pdf",
            data = customers,
            columns = columns,
            itemDetailsExtractor = { entity ->
                val e = entity as Entity
                val balance = String.format("%.2f", e.balance)
                val balanceText = if (e.balance >= 0) "Rs. $balance" else "Rs. $balance (Credit)"
                "Balance: $balanceText"
            }
        )
    }
}