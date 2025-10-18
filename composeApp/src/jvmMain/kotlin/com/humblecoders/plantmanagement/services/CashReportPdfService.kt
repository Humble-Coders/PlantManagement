package com.humblecoders.plantmanagement.services

import com.humblecoders.plantmanagement.data.CashReport
import com.humblecoders.plantmanagement.data.CashReportCategory
import com.humblecoders.plantmanagement.data.CashReportType
import com.humblecoders.plantmanagement.repositories.CashReportSummary
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class CashReportPdfService {
    
    fun generateCashReportPdf(
        transactions: List<CashReport>,
        categories: List<CashReportCategory>,
        summary: CashReportSummary,
        filterInfo: String = ""
    ): ByteArray {
        val html = generateHtml(transactions, categories, summary, filterInfo)
        
        val os = ByteArrayOutputStream()
        PdfRendererBuilder()
            .useFastMode()
            .withHtmlContent(html, null)
            .toStream(os)
            .run()
        
        return os.toByteArray()
    }
    
    private fun generateHtml(
        transactions: List<CashReport>,
        categories: List<CashReportCategory>,
        summary: CashReportSummary,
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
                    body {
                        font-family: Arial, sans-serif;
                        margin: 20px;
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
                        display: flex;
                        justify-content: space-between;
                        margin-bottom: 30px;
                        gap: 20px;
                    }
                    .summary-card {
                        flex: 1;
                        padding: 15px;
                        border-radius: 8px;
                        text-align: center;
                        color: white;
                        font-weight: bold;
                    }
                    .summary-card.cash-in {
                        background-color: #10B981;
                    }
                    .summary-card.cash-out {
                        background-color: #EF4444;
                    }
                    .summary-card.net-change {
                        background-color: #3B82F6;
                    }
                    .summary-label {
                        font-size: 12px;
                        margin-bottom: 5px;
                    }
                    .summary-value {
                        font-size: 18px;
                    }
                    .transactions-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                    }
                    .transactions-table th,
                    .transactions-table td {
                        border: 1px solid #ddd;
                        padding: 12px;
                        text-align: left;
                    }
                    .transactions-table th {
                        background-color: #10B981;
                        color: white;
                        font-weight: bold;
                    }
                    .transactions-table tr:nth-child(even) {
                        background-color: #f9f9f9;
                    }
                    .transaction-type {
                        font-weight: bold;
                    }
                    .transaction-type.cash-in {
                        color: #10B981;
                    }
                    .transaction-type.cash-out {
                        color: #EF4444;
                    }
                    .amount {
                        font-weight: bold;
                        text-align: right;
                    }
                    .amount.cash-in {
                        color: #10B981;
                    }
                    .amount.cash-out {
                        color: #EF4444;
                    }
                    .date {
                        color: #666;
                        font-size: 12px;
                    }
                    .notes {
                        font-style: italic;
                        color: #666;
                        max-width: 200px;
                        word-wrap: break-word;
                    }
                    .footer {
                        margin-top: 30px;
                        text-align: center;
                        font-size: 12px;
                        color: #666;
                        border-top: 1px solid #ddd;
                        padding-top: 15px;
                    }
                </style>
            </head>
            <body>
                <div class="header">
                    <div class="title">Cash Report</div>
                    <div class="subtitle">Generated on $currentDate</div>
                </div>
        """)
        
        // Add filter information if any
        if (filterInfo.isNotEmpty()) {
            html.append("""
                <div class="filter-info">
                    <strong>Filters Applied:</strong> $filterInfo
                </div>
            """)
        }
        
        // Add summary cards
        html.append("""
            <div class="summary">
                <div class="summary-card cash-in">
                    <div class="summary-label">Total Cash In</div>
                    <div class="summary-value">Rs.${String.format("%.2f", summary.totalCashIn)}</div>
                </div>
                <div class="summary-card cash-out">
                    <div class="summary-label">Total Cash Out</div>
                    <div class="summary-value">Rs.${String.format("%.2f", summary.totalCashOut)}</div>
                </div>
                <div class="summary-card net-change">
                    <div class="summary-label">Net Change</div>
                    <div class="summary-value">Rs.${String.format("%.2f", summary.netChange)}</div>
                </div>
            </div>
        """)
        
        // Add transactions table
        html.append("""
            <table class="transactions-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Type</th>
                        <th>Category</th>
                        <th>Amount</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (transactions.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="5" style="text-align: center; padding: 20px; color: #666;">
                        No transactions found
                    </td>
                </tr>
            """)
        } else {
            transactions.forEach { transaction ->
                val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Unknown Category"
                val formattedDate = transaction.date?.let { 
                    dateFormat.format(Date(it.seconds * 1000)) 
                } ?: "Unknown Date"
                
                val typeClass = if (transaction.transactionType == CashReportType.CASH_IN) "cash-in" else "cash-out"
                val amountClass = if (transaction.transactionType == CashReportType.CASH_IN) "cash-in" else "cash-out"
                val amountPrefix = if (transaction.transactionType == CashReportType.CASH_IN) "+" else "-"
                
                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
                        <td class="transaction-type $typeClass">${transaction.transactionType.name.replace("_", " ")}</td>
                        <td>$categoryName</td>
                        <td class="amount $amountClass">$amountPrefix Rs.${String.format("%.2f", transaction.amount)}</td>
                        <td class="notes">${transaction.notes.ifEmpty { "-" }}</td>
                    </tr>
                """)
            }
        }
        
        html.append("""
                </tbody>
            </table>
            
            <div class="footer">
                <p>Total Transactions: ${transactions.size}</p>
                <p>Plant Management System - Cash Report</p>
            </div>
            </body>
            </html>
        """)
        
        return html.toString()
    }
}
