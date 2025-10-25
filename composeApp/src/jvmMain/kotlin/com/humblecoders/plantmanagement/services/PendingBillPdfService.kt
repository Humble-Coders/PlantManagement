package com.humblecoders.plantmanagement.services

import com.humblecoders.plantmanagement.data.PendingBill
import com.humblecoders.plantmanagement.data.PendingBillStatus
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class PendingBillPdfService {
    
    fun generatePendingBillReportPdf(
        bills: List<PendingBill>,
        filterInfo: String = ""
    ): ByteArray {
        val html = generateHtml(bills, filterInfo)
        
        val os = ByteArrayOutputStream()
        PdfRendererBuilder()
            .useFastMode()
            .withHtmlContent(html, null)
            .toStream(os)
            .run()
        
        return os.toByteArray()
    }
    
    private fun generateHtml(
        bills: List<PendingBill>,
        filterInfo: String
    ): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val currentDate = SimpleDateFormat("dd MMM yyyy 'at' HH:mm", Locale.getDefault()).format(Date())
        
        val html = StringBuilder()
        html.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8"/>
                <style>
                    @page {
                        size: A4 landscape;
                        margin: 15mm;
                    }
                    body {
                        font-family: Arial, sans-serif;
                        margin: 0;
                        padding: 10px;
                        color: #333;
                    }
                    .header {
                        text-align: center;
                        margin-bottom: 30px;
                        border-bottom: 2px solid #10B981;
                        padding-bottom: 15px;
                    }
                    .title {
                        font-size: 24px;
                        font-weight: bold;
                        color: #10B981;
                        margin-bottom: 5px;
                    }
                    .subtitle {
                        font-size: 14px;
                        color: #666;
                    }
                    .filter-info {
                        background-color: #f3f4f6;
                        padding: 10px;
                        border-radius: 5px;
                        margin-bottom: 20px;
                        font-size: 12px;
                        color: #666;
                    }
                    .summary {
                        margin-bottom: 30px;
                    }
                    .summary-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                    }
                    .summary-cell {
                        width: 20%;
                        padding: 15px;
                        text-align: center;
                        font-weight: bold;
                        background-color: #f8f9fa;
                        border: 1px solid #dee2e6;
                        vertical-align: top;
                    }
                    .summary-cell.total-bills {
                        background-color: #e3f2fd;
                        border-color: #2196f3;
                    }
                    .summary-cell.total-quantity {
                        background-color: #e8f5e8;
                        border-color: #4caf50;
                    }
                    .summary-cell.total-portal {
                        background-color: #fff3e0;
                        border-color: #ff9800;
                    }
                    .summary-cell.total-revenue {
                        background-color: #f3e5f5;
                        border-color: #9c27b0;
                    }
                    .summary-cell.total-difference {
                        background-color: #ffebee;
                        border-color: #f44336;
                    }
                    .summary-label {
                        font-size: 12px;
                        color: #666;
                        margin-bottom: 5px;
                    }
                    .summary-value {
                        font-size: 18px;
                        color: #333;
                    }
                    .table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 20px;
                        font-size: 10px;
                        table-layout: fixed;
                    }
                    .table th {
                        background-color: #10B981;
                        color: white;
                        padding: 8px 4px;
                        text-align: left;
                        font-weight: bold;
                        border: 1px solid #0d9488;
                        font-size: 9px;
                    }
                    .table td {
                        padding: 6px 4px;
                        border: 1px solid #d1d5db;
                        vertical-align: top;
                        font-size: 9px;
                        word-wrap: break-word;
                    }
                    .table tr:nth-child(even) {
                        background-color: #f9fafb;
                    }
                    .table tr:hover {
                        background-color: #f3f4f6;
                    }
                    .status-billed {
                        background-color: #d1fae5;
                        color: #065f46;
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 10px;
                        font-weight: bold;
                    }
                    .status-pending {
                        background-color: #fef3c7;
                        color: #92400e;
                        padding: 4px 8px;
                        border-radius: 4px;
                        font-size: 10px;
                        font-weight: bold;
                    }
                    .amount-positive {
                        color: #059669;
                        font-weight: bold;
                    }
                    .amount-negative {
                        color: #dc2626;
                        font-weight: bold;
                    }
                    .amount-neutral {
                        color: #6b7280;
                    }
                    .footer {
                        margin-top: 30px;
                        padding-top: 15px;
                        border-top: 1px solid #d1d5db;
                        text-align: center;
                        font-size: 12px;
                        color: #666;
                    }
                    .page-break {
                        page-break-before: always;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())
        
        // Header
        html.append("""
            <div class="header">
                <div class="title">Pending Bill History Report</div>
                <div class="subtitle">Generated on $currentDate</div>
            </div>
        """.trimIndent())
        
        // Filter info
        if (filterInfo.isNotBlank()) {
            html.append("""
                <div class="filter-info">
                    <strong>Applied Filters:</strong> $filterInfo
                </div>
            """.trimIndent())
        }
        
        // Summary table
        val totalBills = bills.size
        val totalQuantity = bills.sumOf { it.quantityKg }
        val totalPortalAmount = bills.sumOf { it.totalPortalAmount }
        val totalRevenueAmount = bills.sumOf { it.totalRevenueAmount }
        val totalDifferenceAmount = bills.sumOf { it.differenceAmount }
        val billedCount = bills.count { it.status == PendingBillStatus.BILLED }
        val pendingCount = bills.count { it.status == PendingBillStatus.PENDING_BILLED }
        
        html.append("""
            <div class="summary">
                <table class="summary-table">
                    <tr>
                        <td class="summary-cell" style="background-color: #e3f2fd; border-color: #2196f3;">
                            <div class="summary-label">Total Bills</div>
                            <div class="summary-value">$totalBills</div>
                            <div style="font-size: 10px; margin-top: 5px;">
                                Billed: $billedCount | Pending: $pendingCount
                            </div>
                        </td>
                        <td class="summary-cell" style="background-color: #e8f5e8; border-color: #4caf50;">
                            <div class="summary-label">Total Quantity</div>
                            <div class="summary-value">${String.format("%.2f", totalQuantity)} kg</div>
                        </td>
                        <td class="summary-cell" style="background-color: #fff3e0; border-color: #ff9800;">
                            <div class="summary-label">Total Portal Amount</div>
                            <div class="summary-value">Rs ${String.format("%.0f", totalPortalAmount)}</div>
                        </td>
                        <td class="summary-cell" style="background-color: #f3e5f5; border-color: #9c27b0;">
                            <div class="summary-label">Total Revenue</div>
                            <div class="summary-value">Rs ${String.format("%.0f", totalRevenueAmount)}</div>
                        </td>
                        <td class="summary-cell" style="background-color: #ffebee; border-color: #f44336;">
                            <div class="summary-label">Total Difference</div>
                            <div class="summary-value ${if (totalDifferenceAmount >= 0) "amount-positive" else "amount-negative"}">
                                Rs ${String.format("%.0f", totalDifferenceAmount)}
                            </div>
                        </td>
                    </tr>
                </table>
            </div>
        """.trimIndent())
        
        // Bills table
        html.append("""
            <table class="table">
                <thead>
                    <tr>
                        <th style="width: 8%;">Bill #</th>
                        <th style="width: 15%;">Firm Name</th>
                        <th style="width: 8%;">Date</th>
                        <th style="width: 8%;">Status</th>
                        <th style="width: 8%;">Qty (kg)</th>
                        <th style="width: 10%;">Portal Amt</th>
                        <th style="width: 10%;">Revenue Amt</th>
                        <th style="width: 8%;">Difference</th>
                        <th style="width: 8%;">Cleared</th>
                        <th style="width: 8%;">Rate/Kg</th>
                        <th style="width: 9%;">Batch</th>
                    </tr>
                </thead>
                <tbody>
        """.trimIndent())
        
        bills.forEach { bill ->
            val statusClass = if (bill.status == PendingBillStatus.BILLED) "status-billed" else "status-pending"
            val statusText = when (bill.status) {
                PendingBillStatus.BILLED -> "Billed"
                PendingBillStatus.PENDING_BILLED -> "Pending"
            }
            val differenceClass = when {
                bill.differenceAmount > 0 -> "amount-positive"
                bill.differenceAmount < 0 -> "amount-negative"
                else -> "amount-neutral"
            }
            
            html.append("""
                <tr>
                    <td>${bill.billNumber}</td>
                    <td>${bill.firmName}</td>
                    <td>${bill.billDate}</td>
                    <td><span class="$statusClass">$statusText</span></td>
                    <td>${String.format("%.1f", bill.quantityKg)}</td>
                    <td>Rs ${String.format("%.0f", bill.totalPortalAmount)}</td>
                    <td>Rs ${String.format("%.0f", bill.totalRevenueAmount)}</td>
                    <td class="$differenceClass">Rs ${String.format("%.0f", bill.differenceAmount)}</td>
                    <td>${String.format("%.1f", bill.clearedQuantity)}</td>
                    <td>Rs ${String.format("%.0f", bill.originalRatePerKg)}</td>
                    <td>${bill.portalBatchNumber}</td>
                </tr>
            """.trimIndent())
        }
        
        html.append("""
                </tbody>
            </table>
        """.trimIndent())
        
        // Footer
        html.append("""
            <div class="footer">
                <p>This report was generated by Plant Management System</p>
                <p>Total records: $totalBills bills</p>
            </div>
        """.trimIndent())
        
        html.append("""
            </body>
            </html>
        """.trimIndent())
        
        return html.toString()
    }
}
