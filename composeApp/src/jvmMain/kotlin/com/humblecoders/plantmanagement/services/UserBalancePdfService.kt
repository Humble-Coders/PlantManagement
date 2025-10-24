package com.humblecoders.plantmanagement.services

import com.humblecoders.plantmanagement.data.UserBalance
import com.humblecoders.plantmanagement.data.BalanceTransfer
import com.humblecoders.plantmanagement.data.UserCashOutTransaction
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class UserBalancePdfService {
    
    fun generateUserBalanceReportPdf(
        userBalance: UserBalance?,
        balanceTransfers: List<BalanceTransfer>,
        userCashOutTransactions: List<UserCashOutTransaction>,
        filterInfo: String = ""
    ): ByteArray {
        val html = generateHtml(userBalance, balanceTransfers, userCashOutTransactions, filterInfo)

        val os = ByteArrayOutputStream()
        PdfRendererBuilder()
            .useFastMode()
            .withHtmlContent(html, null)
            .toStream(os)
            .run()

        return os.toByteArray()
    }
    
    private fun generateHtml(
        userBalance: UserBalance?,
        balanceTransfers: List<BalanceTransfer>,
        userCashOutTransactions: List<UserCashOutTransaction>,
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
                        border-bottom: 2px solid #06B6D4;
                        padding-bottom: 15px;
                    }
                    .title {
                        font-size: 24px;
                        font-weight: bold;
                        color: #06B6D4;
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
                    .summary-card.user-balances {
                        background-color: #f0f9ff;
                        border-color: #06B6D4;
                    }
                    .summary-card.balance-transfers {
                        background-color: #f0fdf4;
                        border-color: #10B981;
                    }
                    .summary-card.user-transactions {
                        background-color: #fef2f2;
                        border-color: #EF4444;
                    }
                    .summary-label {
                        font-size: 12px;
                        margin-bottom: 5px;
                        color: #6c757d;
                    }
                    .summary-value {
                        font-size: 18px;
                    }
                    .summary-value.user-balances {
                        color: #06B6D4;
                    }
                    .summary-value.balance-transfers {
                        color: #10B981;
                    }
                    .summary-value.user-transactions {
                        color: #EF4444;
                    }
                    .section-title {
                        font-size: 18px;
                        font-weight: bold;
                        margin-top: 30px;
                        margin-bottom: 15px;
                        padding: 10px;
                        border-radius: 5px;
                    }
                    .section-title.user-balances {
                        background-color: #f0f9ff;
                        color: #06B6D4;
                        border-left: 4px solid #06B6D4;
                    }
                    .section-title.balance-transfers {
                        background-color: #f0fdf4;
                        color: #10B981;
                        border-left: 4px solid #10B981;
                    }
                    .section-title.user-transactions {
                        background-color: #fef2f2;
                        color: #EF4444;
                        border-left: 4px solid #EF4444;
                    }
                    .data-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                    }
                    .data-table th,
                    .data-table td {
                        border: 1px solid #ddd;
                        padding: 12px;
                        text-align: left;
                    }
                    .data-table th {
                        background-color: #374151;
                        color: white;
                        font-weight: bold;
                    }
                    .data-table tr:nth-child(even) {
                        background-color: #f9f9f9;
                    }
                    .amount {
                        font-weight: bold;
                        text-align: right;
                    }
                    .amount.positive {
                        color: #10B981;
                    }
                    .amount.negative {
                        color: #EF4444;
                    }
                    .amount.neutral {
                        color: #06B6D4;
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
                    .user-name {
                        font-weight: bold;
                        color: #374151;
                    }
                    .transfer-type {
                        font-weight: bold;
                    }
                    .transfer-type.add {
                        color: #10B981;
                    }
                    .transfer-type.deduct {
                        color: #EF4444;
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
                           <div class="title">Shared User Balance Report</div>
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
        
        // Calculate summary statistics
        val sharedBalance = userBalance?.currentBalance ?: 0.0
        val totalTransfers = balanceTransfers.sumOf { it.amount }
        val totalUserTransactions = userCashOutTransactions.sumOf { it.amount }
        
        // Add summary cards
        html.append("""
            <div class="summary">
                <div class="summary-card user-balances">
                    <div class="summary-label">Shared User Balance</div>
                    <div class="summary-value user-balances">Rs.${String.format("%.2f", sharedBalance)}</div>
                </div>
                <div class="summary-card balance-transfers">
                    <div class="summary-label">Total Balance Transfers</div>
                    <div class="summary-value balance-transfers">Rs.${String.format("%.2f", totalTransfers)}</div>
                </div>
                <div class="summary-card user-transactions">
                    <div class="summary-label">Total User Transactions</div>
                    <div class="summary-value user-transactions">Rs.${String.format("%.2f", totalUserTransactions)}</div>
                </div>
            </div>
        """)
        
        // Add Shared User Balance section
        html.append("""
            <div class="section-title user-balances">Shared User Balance</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Balance Type</th>
                        <th>Current Balance</th>
                        <th>Last Updated</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (userBalance == null) {
            html.append("""
                <tr>
                    <td colspan="3" style="text-align: center; padding: 20px; color: #666;">
                        No shared user balance found
                    </td>
                </tr>
            """)
        } else {
            val formattedDate = userBalance.updatedAt?.let { 
                dateFormat.format(Date(it.seconds * 1000)) 
            } ?: "Unknown Date"
            
            val balanceClass = when {
                userBalance.currentBalance > 0 -> "positive"
                userBalance.currentBalance < 0 -> "negative"
                else -> "neutral"
            }
            
            html.append("""
                <tr>
                    <td class="user-name">Shared User Balance</td>
                    <td class="amount $balanceClass">Rs.${String.format("%.2f", userBalance.currentBalance)}</td>
                    <td class="date">$formattedDate</td>
                </tr>
            """)
        }
        
        html.append("""
                </tbody>
            </table>
        """)
        
        // Add Balance Transfers section
        html.append("""
            <div class="section-title balance-transfers">Balance Transfers (Admin to Shared Balance)</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>Admin</th>
                        <th>Transfer Type</th>
                        <th>Amount</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (balanceTransfers.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="5" style="text-align: center; padding: 20px; color: #666;">
                        No balance transfers found
                    </td>
                </tr>
            """)
        } else {
            balanceTransfers.forEach { transfer ->
                val formattedDate = transfer.createdAt?.let { 
                    dateFormat.format(Date(it.seconds * 1000)) 
                } ?: "Unknown Date"
                
                val transferTypeClass = transfer.transferType.name.lowercase()
                val amountClass = if (transfer.transferType.name == "ADD") "positive" else "negative"
                
                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
                        <td class="user-name">${transfer.fromAdminEmail.ifEmpty { "Unknown Admin" }}</td>
                        <td class="transfer-type $transferTypeClass">${transfer.transferType.name}</td>
                        <td class="amount $amountClass">Rs.${String.format("%.2f", transfer.amount)}</td>
                        <td class="notes">${transfer.notes.ifEmpty { "-" }}</td>
                    </tr>
                """)
            }
        }
        
        html.append("""
                </tbody>
            </table>
        """)
        
        // Add User Cash Out Transactions section
        html.append("""
            <div class="section-title user-transactions">User Cash Out Transactions</div>
            <table class="data-table">
                <thead>
                    <tr>
                        <th>Date</th>
                        <th>User</th>
                        <th>Amount</th>
                        <th>Previous Balance</th>
                        <th>New Balance</th>
                        <th>Notes</th>
                    </tr>
                </thead>
                <tbody>
        """)
        
        if (userCashOutTransactions.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="6" style="text-align: center; padding: 20px; color: #666;">
                        No user cash out transactions found
                    </td>
                </tr>
            """)
        } else {
            userCashOutTransactions.forEach { transaction ->
                val formattedDate = transaction.createdAt?.let { 
                    dateFormat.format(Date(it.seconds * 1000)) 
                } ?: "Unknown Date"
                
                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
                        <td class="user-name">${transaction.userName.ifEmpty { "Unknown User" }}</td>
                        <td class="amount negative">- Rs.${String.format("%.2f", transaction.amount)}</td>
                        <td class="amount neutral">Rs.${String.format("%.2f", transaction.previousBalance)}</td>
                        <td class="amount ${if (transaction.newBalance >= 0) "positive" else "negative"}">Rs.${String.format("%.2f", transaction.newBalance)}</td>
                        <td class="notes">${transaction.notes.ifEmpty { "-" }}</td>
                    </tr>
                """)
            }
        }
        
        html.append("""
                </tbody>
            </table>
            
            <div class="footer">
                <p>Shared Balance: ${if (userBalance != null) "Available" else "Not Available"} | Total Transfers: ${balanceTransfers.size} | Total User Transactions: ${userCashOutTransactions.size}</p>
                <p>Plant Management System - Shared User Balance Report</p>
            </div>
            </body>
            </html>
        """)
        
        return html.toString()
    }
}
