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
        accountantBalance: Double = 0.0,
        filterInfo: String = ""
    ): ByteArray {
        val html = generateHtml(transactions, categories, summary, accountantBalance, filterInfo)
        
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
        accountantBalance: Double,
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
                        font-weight: bold;
                        background-color: #f8f9fa;
                        border: 1px solid #dee2e6;
                    }
                    .summary-card.cash-in {
                        background-color: #f8f9fa;
                    }
                    .summary-card.cash-out {
                        background-color: #f8f9fa;
                    }
                    .summary-card.net-change {
                        background-color: #f8f9fa;
                    }
                    .summary-label {
                        font-size: 12px;
                        margin-bottom: 5px;
                        color: #6c757d;
                    }
                    .summary-value {
                        font-size: 18px;
                    }
                    .summary-value.cash-in {
                        color: #10B981;
                    }
                    .summary-value.cash-out {
                        color: #EF4444;
                    }
                    .summary-value.net-change.positive {
                        color: #10B981;
                    }
                    .summary-value.net-change.negative {
                        color: #EF4444;
                    }
                    .summary-value.accountant-balance.positive {
                        color: #10B981;
                    }
                    .summary-value.accountant-balance.negative {
                        color: #EF4444;
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
        val netChangeClass = if (summary.netChange >= 0) "positive" else "negative"
        val accountantBalanceClass = if (accountantBalance >= 0) "positive" else "negative"
        html.append("""
            <div class="summary">
                <div class="summary-card cash-in">
                    <div class="summary-label">Total Cash In</div>
                    <div class="summary-value cash-in">Rs.${String.format("%.2f", summary.totalCashIn)}</div>
                </div>
                <div class="summary-card cash-out">
                    <div class="summary-label">Total Cash Out</div>
                    <div class="summary-value cash-out">Rs.${String.format("%.2f", summary.totalCashOut)}</div>
                </div>
                <div class="summary-card net-change">
                    <div class="summary-label">Net Change</div>
                    <div class="summary-value net-change $netChangeClass">Rs.${String.format("%.2f", summary.netChange)}</div>
                </div>
                <div class="summary-card accountant-balance">
                    <div class="summary-label">Accountant Balance</div>
                    <div class="summary-value accountant-balance $accountantBalanceClass">Rs.${String.format("%.2f", accountantBalance)}</div>
                </div>
            </div>
        """)
        
        // Separate transactions into regular and accountant transactions
        val regularTransactions = transactions.filter { !it.accountantTransaction }
        val accountantTransactions = transactions.filter { it.accountantTransaction }
        
        // Separate regular transactions into cash in and cash out
        val regularCashInTransactions = regularTransactions.filter { it.transactionType == CashReportType.CASH_IN }
        val regularCashOutTransactions = regularTransactions.filter { it.transactionType == CashReportType.CASH_OUT }
        
        // Add Cash In transactions table
        html.append("""
            <h3 style="color: #10B981; margin-top: 30px; margin-bottom: 15px;">Cash In Transactions</h3>
            <table class="transactions-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Category</th>
                        <th>Amount</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (regularCashInTransactions.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="4" style="text-align: center; padding: 20px; color: #666;">
                        No cash in transactions found
                    </td>
                </tr>
            """)
        } else {
            regularCashInTransactions.forEach { transaction ->
                val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Unknown Category"
                val formattedDate = transaction.date?.let { 
                    dateFormat.format(Date(it.seconds * 1000)) 
                } ?: "Unknown Date"
                
                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
                        <td>$categoryName</td>
                        <td class="amount cash-in">+ Rs.${String.format("%.2f", transaction.amount)}</td>
                        <td class="notes">${transaction.notes.ifEmpty { "-" }}</td>
                    </tr>
                """)
            }
        }
        
        html.append("""
                </tbody>
            </table>
        """)
        
        // Add Cash Out transactions table
        html.append("""
            <h3 style="color: #EF4444; margin-top: 30px; margin-bottom: 15px;">Cash Out Transactions</h3>
            <table class="transactions-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Category</th>
                        <th>Amount</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (regularCashOutTransactions.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="4" style="text-align: center; padding: 20px; color: #666;">
                        No cash out transactions found
                    </td>
                </tr>
            """)
        } else {
            regularCashOutTransactions.forEach { transaction ->
                val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Unknown Category"
                val formattedDate = transaction.date?.let { 
                    dateFormat.format(Date(it.seconds * 1000)) 
                } ?: "Unknown Date"
                
                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
                        <td>$categoryName</td>
                        <td class="amount cash-out">- Rs.${String.format("%.2f", transaction.amount)}</td>
                        <td class="notes">${transaction.notes.ifEmpty { "-" }}</td>
                    </tr>
                """)
            }
        }
        
        html.append("""
                </tbody>
            </table>
        """)
        
        // Add Accountant Transactions table
        html.append("""
            <h3 style="color: #8B5CF6; margin-top: 30px; margin-bottom: 15px;">Accountant Transactions</h3>
            <table class="transactions-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Category</th>
                        <th>Amount</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (accountantTransactions.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="4" style="text-align: center; padding: 20px; color: #666;">
                        No accountant transactions found
                    </td>
                </tr>
            """)
        } else {
            accountantTransactions.forEach { transaction ->
                val categoryName = categories.find { it.id == transaction.categoryId }?.name ?: "Unknown Category"
                val formattedDate = transaction.date?.let { 
                    dateFormat.format(Date(it.seconds * 1000)) 
                } ?: "Unknown Date"
                
                val amountClass = if (transaction.transactionType == CashReportType.CASH_IN) "cash-in" else "cash-out"
                val amountPrefix = if (transaction.transactionType == CashReportType.CASH_IN) "+" else "-"
                
                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
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
        """)
        
        html.append("""
            
            <div class="footer">
                <p>Total Transactions: ${transactions.size} (Regular: ${regularTransactions.size}, Accountant: ${accountantTransactions.size})</p>
                <p>Plant Management System - Cash Report</p>
            </div>
            </body>
            </html>
        """)
        
        return html.toString()
    }
}
