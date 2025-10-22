// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/services/SalePdfGeneratorService.kt
package com.humblecoders.plantmanagement.services

import com.humblecoders.plantmanagement.data.Sale
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class SalePdfGeneratorService {

    suspend fun generateSaleBill(sale: Sale, outputFile: File): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val html = generateBillHtml(sale)

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

    private fun generateBillHtml(sale: Sale): String {
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
            border-bottom: 3px solid #10B981;
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
        
        .details-section {
            margin-bottom: 20px;
            padding: 15px;
            background-color: #F9FAFB;
            border-left: 4px solid #10B981;
        }
        
        .details-row {
            display: table;
            width: 100%;
            margin-bottom: 8px;
        }
        
        .details-label {
            display: table-cell;
            width: 40%;
            font-weight: bold;
            color: #374151;
        }
        
        .details-value {
            display: table-cell;
            width: 60%;
            color: #111827;
        }
        
        .calculations {
            margin-top: 20px;
            width: 60%;
            margin-left: auto;
        }
        
        .calc-row {
            display: table;
            width: 100%;
            padding: 8px;
            border-bottom: 1px solid #E5E7EB;
        }
        
        .calc-label {
            display: table-cell;
            width: 60%;
            font-weight: 600;
        }
        
        .calc-value {
            display: table-cell;
            width: 40%;
            text-align: right;
        }
        
        .calc-row.highlight {
            background-color: #111827;
            color: white;
            font-size: 12pt;
            font-weight: bold;
        }
        
        .calc-row.positive {
            background-color: #D1FAE5;
            color: #065F46;
        }
        
        .calc-row.negative {
            background-color: #FEE2E2;
            color: #991B1B;
        }
        
        .status-badge {
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
            border-left: 4px solid #10B981;
        }
        
        .notes-label {
            font-weight: bold;
            color: #374151;
            margin-bottom: 8px;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>SALE BILL</h1>
        <p>Plant Management System</p>
        <p>Generated on: ${currentDate}</p>
    </div>
    
    <div class="bill-info">
        <div class="bill-info-left">
            <p><span class="info-label">Customer:</span> <span class="info-value">${sale.firmName}</span></p>
            <p><span class="info-label">Sale Date:</span> <span class="info-value">${formatDate(sale.saleDate)}</span></p>
            <p><span class="info-label">Bill Number:</span> <span class="info-value">${sale.billNumber}</span></p>
            <p><span class="info-label">Portal Batch Number:</span> <span class="info-value">${sale.portalBatchNumber}</span></p>
        </div>
        <div class="bill-info-right">
            <p><span class="info-label">Sale Status:</span></p>
            <p><span class="status-badge ${getSaleStatusClass(sale.saleStatus.name)}">${sale.saleStatus.name.replace("_", " ")}</span></p>
        </div>
    </div>
    
    <div class="details-section">
        <h3 style="margin: 0 0 15px 0; color: #10B981;">Sale Details</h3>
        <div class="details-row">
            <div class="details-label">Quantity (Kg):</div>
            <div class="details-value">${formatAmount(sale.quantityKg)} kg (${sale.numberOfBags} bags)</div>
        </div>
        ${if (sale.extraQuantityKg > 0) """
        <div class="details-row">
            <div class="details-label">Extra Quantity (Indirect Discount):</div>
            <div class="details-value">${formatAmount(sale.extraQuantityKg)} kg</div>
        </div>
        <div class="details-row">
            <div class="details-label">Total Quantity Sent:</div>
            <div class="details-value">${formatAmount(sale.quantityKg + sale.extraQuantityKg)} kg</div>
        </div>
        """ else ""}
        <div class="details-row">
            <div class="details-label">Original Rate per Kg:</div>
            <div class="details-value">Rs ${formatAmount(sale.originalRatePerKg)}</div>
        </div>
        ${if (sale.discountType.name == "DISCOUNT_PREMIUM") """
        <div class="details-row">
            <div class="details-label">Discounted Rate per Kg:</div>
            <div class="details-value">Rs ${formatAmount(sale.discountedRatePerKg)}</div>
        </div>
        """ else ""}
        <div class="details-row">
            <div class="details-label">Deducted from Inventory:</div>
            <div class="details-value">${if (sale.deductFromInventory) "Yes" else "No"}</div>
        </div>
    </div>
    
    ${if (sale.truckNumber.isNotBlank() || sale.fareAmount > 0) """
    <div class="details-section">
        <h3 style="margin: 0 0 15px 0; color: #10B981;">Transport Details</h3>
        ${if (sale.truckNumber.isNotBlank()) """
        <div class="details-row">
            <div class="details-label">Truck Number:</div>
            <div class="details-value">${sale.truckNumber}</div>
        </div>
        """ else ""}
        ${if (sale.fareAmount > 0) """
        <div class="details-row">
            <div class="details-label">Fare Amount:</div>
            <div class="details-value">Rs ${formatAmount(sale.fareAmount)}</div>
        </div>
        <div class="details-row">
            <div class="details-label">Fare Paid By:</div>
            <div class="details-value">${sale.farePaidBy.name}</div>
        </div>
        """ else ""}
    </div>
    """ else ""}
    
    <div class="calculations">
        <div class="calc-row">
            <div class="calc-label">Portal Amount:</div>
            <div class="calc-value">Rs ${formatAmount(sale.portalAmount)}</div>
        </div>
        <div class="calc-row">
            <div class="calc-label">GST (5%):</div>
            <div class="calc-value">Rs ${formatAmount(sale.gstAmount)}</div>
        </div>
        <div class="calc-row highlight">
            <div class="calc-label">Total Portal Amount:</div>
            <div class="calc-value">Rs ${formatAmount(sale.totalPortalAmount)}</div>
        </div>
        
        <div style="height: 20px;"></div>
        
        <div class="calc-row">
            <div class="calc-label">Revenue Amount:</div>
            <div class="calc-value">Rs ${formatAmount(sale.revenueAmount)}</div>
        </div>
        <div class="calc-row">
            <div class="calc-label">GST (5%):</div>
            <div class="calc-value">Rs ${formatAmount(sale.gstAmount)}</div>
        </div>
        <div class="calc-row highlight">
            <div class="calc-label">Total Revenue Amount:</div>
            <div class="calc-value">Rs ${formatAmount(sale.totalRevenueAmount)}</div>
        </div>
        
        <div style="height: 20px;"></div>
        
        <div class="calc-row ${if (sale.differenceAmount >= 0) "positive" else "negative"}">
            <div class="calc-label">Difference Amount:</div>
            <div class="calc-value">${if (sale.differenceAmount >= 0) "+" else ""}Rs ${formatAmount(sale.differenceAmount)}</div>
        </div>
        <div class="calc-row">
            <div class="calc-label" style="font-size: 9pt; font-weight: normal; font-style: italic;">
                ${if (sale.differenceAmount >= 0) "(Customer pays us)" else "(We pay customer)"}
            </div>
            <div class="calc-value"></div>
        </div>
        
        <div style="height: 20px;"></div>
        
        <div class="calc-row">
            <div class="calc-label">Portal Amount Paid:</div>
            <div class="calc-value">Rs ${formatAmount(sale.portalAmountPaid)}</div>
        </div>
        <div class="calc-row">
            <div class="calc-label">Portal Pending:</div>
            <div class="calc-value" style="color: #DC2626;">Rs ${formatAmount(sale.totalPortalAmount - sale.portalAmountPaid)}</div>
        </div>
        
        <div style="height: 10px;"></div>
        
        <div class="calc-row">
            <div class="calc-label">Difference Amount Paid:</div>
            <div class="calc-value">Rs ${formatAmount(sale.differenceAmountPaid)}</div>
        </div>
        <div class="calc-row">
            <div class="calc-label">Difference Pending:</div>
            <div class="calc-value" style="color: #DC2626;">Rs ${formatAmount(sale.differenceAmount - sale.differenceAmountPaid)}</div>
        </div>
    </div>
    
    ${if (sale.notes.isNotBlank()) """
    <div class="notes">
        <div class="notes-label">Notes:</div>
        <div>${sale.notes}</div>
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

    private fun getSaleStatusClass(status: String): String {
        return when (status) {
            "PAID" -> "status-paid"
            "PENDING" -> "status-pending"
            "PARTIALLY_PAID" -> "status-partial"
            else -> "status-pending"
        }
    }

}