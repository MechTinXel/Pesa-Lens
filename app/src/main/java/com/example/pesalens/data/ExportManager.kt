package com.example.pesalens.data

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Environment
import android.widget.Toast
import com.example.pesalens.PesaTransaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExportManager {

    fun generateMonthlyReport(
        context: Context,
        transactions: List<PesaTransaction>,
        monthName: String,
        currency: CurrencyOption
    ) {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint()

        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("PesaLens - Monthly Report: $monthName", 50f, 50f, paint)

        // Summary Header
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val totalSpent = transactions.filter { it.type != "Received" }.sumOf { it.amount }
        val totalReceived = transactions.filter { it.type == "Received" }.sumOf { it.amount }
        val totalFees = transactions.sumOf { it.fee }

        canvas.drawText("Total Income: ${formatMoney(totalReceived, currency, decimals = 2)}", 50f, 100f, paint)
        canvas.drawText("Total Spending: ${formatMoney(totalSpent, currency, decimals = 2)}", 50f, 120f, paint)
        canvas.drawText("Total Fees: ${formatMoney(totalFees, currency, decimals = 2)}", 50f, 140f, paint)

        // Table Header
        paint.isFakeBoldText = true
        canvas.drawText("Date", 50f, 200f, paint)
        canvas.drawText("Recipient/Sender", 150f, 200f, paint)
        canvas.drawText("Amount", 450f, 200f, paint)
        canvas.drawLine(50f, 210f, 550f, 210f, paint)

        // Transactions
        paint.isFakeBoldText = false
        var y = 230f
        transactions.take(20).forEach { // Sample first 20 for one page
            val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(it.date))
            canvas.drawText(dateStr, 50f, y, paint)
            canvas.drawText(it.name.take(25), 150f, y, paint)
            canvas.drawText(formatMoney(it.amount, currency, decimals = 2), 430f, y, paint)
            y += 25f
        }

        pdfDocument.finishPage(page)

        val fileName = "PesaLens_Report_${monthName}_${System.currentTimeMillis()}.pdf"
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), fileName)

        try {
            pdfDocument.writeTo(FileOutputStream(file))
            Toast.makeText(context, "Report saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error saving PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally {
            pdfDocument.close()
        }
    }
}
