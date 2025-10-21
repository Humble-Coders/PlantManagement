package com.humblecoders.plantmanagement.services

import com.humblecoders.plantmanagement.data.Purchase
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class PdfGeneratorService {
    
    suspend fun generatePurchaseBill(purchase: Purchase, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val html = generateBillHtml(purchase)
            
            FileOutputStream(outputFile).use { os ->
                val builder = PdfRendererBuilder()
                builder.useFastMode()
                builder.withHtmlContent(html, null)
                builder.toStream(os)
                builder.run()
            }
            
            Result.success(outputFile)
        } catch (e: Exception) {
            println("Error generating PDF: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    private fun generateBillHtml(purchase: Purchase): String {
        val currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        
        return """
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8"/>
    <style>
        @page {
            size: A4;
            margin: 20mm;
        }
        
        body {
            font-family: 'Helvetica', 'Arial', sans-serif;
            font-size: 10pt;
            line-height: 1.4;
            color: #333;
        }
        
        .header {
            text-align: center;
            margin-bottom: 30px;
            border-bottom: 3px solid #06B6D4;
            padding-bottom: 20px;
        }
        
        .header h1 {
            margin: 0;
            color: #111827;
            font-size: 28pt;
        }
        
        .header p {
            margin: 5px 0;
            color: #6B7280;
        }
        
        .bill-info {
            display: table;
            width: 100%;
            margin-bottom: 30px;
        }
        
        .bill-info-left, .bill-info-right {
            display: table-cell;
            width: 50%;
            vertical-align: top;
        }
        
        .bill-info-right {
            text-align: right;
        }
        
        .info-label {
            font-weight: bold;
            color: #374151;
        }
        
        .info-value {
            color: #111827;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 20px;
        }
        
        table thead {
            background-color: #06B6D4;
            color: white;
        }
        
        table th {
            padding: 12px 8px;
            text-align: left;
            font-weight: 600;
        }
        
        table td {
            padding: 10px 8px;
            border-bottom: 1px solid #E5E7EB;
        }
        
        table tbody tr:nth-child(even) {
            background-color: #F9FAFB;
        }
        
        .text-right {
            text-align: right;
        }
        
        .text-center {
            text-align: center;
        }
        
        .totals {
            margin-top: 20px;
            width: 50%;
            margin-left: auto;
        }
        
        .totals table {
            margin-bottom: 0;
        }
        
        .totals td {
            border: none;
            padding: 8px;
        }
        
        .totals .grand-total {
            font-size: 14pt;
            font-weight: bold;
            background-color: #111827;
            color: white;
        }
        
        .payment-status {
            display: inline-block;
            padding: 4px 12px;
            border-radius: 4px;
            font-weight: 600;
            font-size: 9pt;
        }
        
        .status-paid {
            background-color: #D1FAE5;
            color: #065F46;
        }
        
        .status-pending {
            background-color: #FEF3C7;
            color: #92400E;
        }
        
        .status-partial {
            background-color: #DBEAFE;
            color: #1E40AF;
        }
        
        .footer {
            margin-top: 50px;
            padding-top: 20px;
            border-top: 2px solid #E5E7EB;
            text-align: center;
            color: #6B7280;
            font-size: 9pt;
        }
        
        .notes {
            margin-top: 20px;
            padding: 15px;
            background-color: #F3F4F6;
            border-left: 4px solid #06B6D4;
        }
        
        .notes-label {
            font-weight: bold;
            color: #374151;
            margin-bottom: 8px;
        }
        
        /* Avoid page breaks inside table rows */
        tr {
            page-break-inside: avoid;
        }
        
        /* Keep header with first row */
        thead {
            display: table-header-group;
        }
        
        tfoot {
            display: table-footer-group;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>PURCHASE BILL</h1>
        <p>Plant Management System</p>
        <p>Generated on: ${currentDate}</p>
    </div>
    
    <div class="bill-info">
        <div class="bill-info-left">
            <p><span class="info-label">Vendor:</span> <span class="info-value">${purchase.firmName}</span></p>
            <p><span class="info-label">Purchase Date:</span> <span class="info-value">${formatDate(purchase.purchaseDate)}</span></p>
            <p><span class="info-label">Bill ID:</span> <span class="info-value">${purchase.id.take(8).uppercase()}</span></p>
        </div>
        <div class="bill-info-right">
            <p><span class="info-label">Payment Status:</span></p>
            <p><span class="payment-status ${getStatusClass(purchase.paymentStatus.name)}">${purchase.paymentStatus.name.replace("_", " ")}</span></p>
        </div>
    </div>
    
    <table>
        <thead>
            <tr>
                <th style="width: 5%;">#</th>
                <th style="width: 35%;">Item Description</th>
                <th style="width: 15%;" class="text-center">Quantity</th>
                <th style="width: 15%;" class="text-right">Price/Unit</th>
                <th style="width: 15%;" class="text-right">GST</th>
                <th style="width: 15%;" class="text-right">Total</th>
            </tr>
        </thead>
        <tbody>
            ${generateItemRows(purchase)}
        </tbody>
    </table>
    
    <div class="totals">
        <table>
            <tr>
                <td><strong>Subtotal:</strong></td>
                <td class="text-right">Rs ${formatAmount(purchase.totalAmount)}</td>
            </tr>
            ${if (purchase.gstRate > 0) """
            <tr>
                <td><strong>GST (${purchase.gstRate.toInt()}%):</strong></td>
                <td class="text-right">Rs ${formatAmount(purchase.gstAmount)}</td>
            </tr>
            """ else ""}
            <tr class="grand-total">
                <td><strong>Grand Total:</strong></td>
                <td class="text-right">Rs ${formatAmount(purchase.grandTotal)}</td>
            </tr>
            <tr>
                <td><strong>Amount Paid:</strong></td>
                <td class="text-right">Rs ${formatAmount(purchase.amountPaid)}</td>
            </tr>
            ${if (purchase.grandTotal - purchase.amountPaid > 0) """
            <tr style="color: #DC2626;">
                <td><strong>Balance Due:</strong></td>
                <td class="text-right"><strong>Rs ${formatAmount(purchase.grandTotal - purchase.amountPaid)}</strong></td>
            </tr>
            """ else ""}
        </table>
    </div>
    
    ${if (purchase.notes.isNotBlank()) """
    <div class="notes">
        <div class="notes-label">Notes:</div>
        <div>${purchase.notes}</div>
    </div>
    """ else ""}
    
    <div class="footer">
        <p>Thank you for your business!</p>
        <p>This is a computer-generated document and does not require a signature.</p>
    </div>
</body>
</html>
        """.trimIndent()
    }
    
    private fun generateItemRows(purchase: Purchase): String {
        return purchase.items.mapIndexed { index, item ->
            """
            <tr>
                <td class="text-center">${index + 1}</td>
                <td>${item.itemName}</td>
                <td class="text-center">${formatAmount(item.quantity)} ${item.unit}</td>
                <td class="text-right">Rs ${formatAmount(item.pricePerUnit)}</td>
                <td class="text-right">-</td>
                <td class="text-right">Rs ${formatAmount(item.totalPrice)}</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")
    }
    
    private fun formatAmount(amount: Double): String {
        return String.format("%.2f", amount)
    }
    
    private fun formatDate(dateStr: String): String {
        return try {
            val date = LocalDate.parse(dateStr)
            date.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } catch (e: Exception) {
            dateStr
        }
    }
    
    private fun getStatusClass(status: String): String {
        return when (status) {
            "PAID" -> "status-paid"
            "PENDING" -> "status-pending"
            "PARTIALLY_PAID" -> "status-partial"
            else -> "status-pending"
        }
    }
}

