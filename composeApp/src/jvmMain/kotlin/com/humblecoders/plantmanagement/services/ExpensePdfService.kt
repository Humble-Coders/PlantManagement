// composeApp/src/jvmMain/kotlin/com/humblecoders/plantmanagement/services/ExpensePdfService.kt
package com.humblecoders.plantmanagement.services

import com.humblecoders.plantmanagement.data.Expense
import com.humblecoders.plantmanagement.data.ExpenseCategory
import com.humblecoders.plantmanagement.repositories.ExpenseSummary
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExpensePdfService {

    fun generateExpensePdf(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>,
        summary: ExpenseSummary,
        filterInfo: String = ""
    ): ByteArray {
        val html = generateHtml(expenses, categories, summary, filterInfo)

        val os = ByteArrayOutputStream()
        PdfRendererBuilder()
            .useFastMode()
            .withHtmlContent(html, null)
            .toStream(os)
            .run()

        return os.toByteArray()
    }

    private fun generateHtml(
        expenses: List<Expense>,
        categories: List<ExpenseCategory>,
        summary: ExpenseSummary,
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
                        border-bottom: 2px solid #EF4444;
                        padding-bottom: 15px;
                    }
                    .title {
                        font-size: 24px;
                        font-weight: bold;
                        color: #EF4444;
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
                        justify-content: center;
                        margin-bottom: 30px;
                    }
                    .summary-card {
                        padding: 15px 30px;
                        border-radius: 8px;
                        text-align: center;
                        color: white;
                        font-weight: bold;
                        background-color: #EF4444;
                    }
                    .summary-label {
                        font-size: 12px;
                        margin-bottom: 5px;
                    }
                    .summary-value {
                        font-size: 18px;
                    }
                    .expenses-table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-top: 20px;
                    }
                    .expenses-table th,
                    .expenses-table td {
                        border: 1px solid #ddd;
                        padding: 12px;
                        text-align: left;
                    }
                    .expenses-table th {
                        background-color: #EF4444;
                        color: white;
                        font-weight: bold;
                    }
                    .expenses-table tr:nth-child(even) {
                        background-color: #f9f9f9;
                    }
                    .amount {
                        font-weight: bold;
                        text-align: right;
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
                    <div class="title">Expense Report</div>
                    <div class="subtitle">Generated on $currentDate</div>
                </div>
        """)

        if (filterInfo.isNotEmpty()) {
            html.append("""
                <div class="filter-info">
                    <strong>Filters Applied:</strong> $filterInfo
                </div>
            """)
        }

        html.append("""
            <div class="summary">
                <div class="summary-card">
                    <div class="summary-label">Total Expenses</div>
                    <div class="summary-value">Rs.${String.format("%.2f", summary.totalExpenses)}</div>
                </div>
            </div>
        """)

        html.append("""
            <table class="expenses-table">
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

        if (expenses.isEmpty()) {
            html.append("""
                <tr>
                    <td colspan="4" style="text-align: center; padding: 20px; color: #666;">
                        No expenses found
                    </td>
                </tr>
            """)
        } else {
            expenses.forEach { expense ->
                val categoryName = categories.find { it.id == expense.categoryId }?.name ?: "Unknown Category"
                val formattedDate = expense.date?.let {
                    dateFormat.format(Date(it.seconds * 1000))
                } ?: "Unknown Date"

                html.append("""
                    <tr>
                        <td class="date">$formattedDate</td>
                        <td>$categoryName</td>
                        <td class="amount">Rs.${String.format("%.2f", expense.amount)}</td>
                        <td class="notes">${expense.notes.ifEmpty { "-" }}</td>
                    </tr>
                """)
            }
        }

        html.append("""
                </tbody>
            </table>
            
            <div class="footer">
                <p>Total Expenses: ${expenses.size}</p>
                <p>Plant Management System - Expense Report</p>
            </div>
            </body>
            </html>
        """)

        return html.toString()
    }
}