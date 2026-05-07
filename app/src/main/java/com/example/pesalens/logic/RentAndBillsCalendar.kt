package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction
import java.util.*

data class RecurringBill(
    val id: String,
    val name: String,
    val category: String, // "Rent", "WiFi", "Water", "School Fees", "Chama", "SACCO", "Loan"
    val amount: Double,
    val dueDate: Int, // Day of month
    val frequency: String = "Monthly",
    val lastPaidDate: Long = 0L,
    val nextDueDate: Long = 0L,
    val isOverdue: Boolean = false
)

class RentAndBillsCalendar {
    private val bills = mutableListOf<RecurringBill>()

    fun detectRecurringPayment(transaction: PesaTransaction): RecurringBill? {
        val category = detectBillCategory(transaction.name) ?: return null

        return RecurringBill(
            id = transaction.name,
            name = transaction.name,
            category = category,
            amount = transaction.amount,
            dueDate = Calendar.getInstance().apply { timeInMillis = transaction.date }.get(Calendar.DAY_OF_MONTH),
            lastPaidDate = transaction.date,
            nextDueDate = calculateNextDueDate(transaction.date, category)
        )
    }

    fun addBill(bill: RecurringBill) {
        bills.add(bill)
    }

    fun getUpcomingBills(daysAhead: Int = 7): List<RecurringBill> {
        val now = System.currentTimeMillis()
        val futureDate = now + (daysAhead * 24 * 60 * 60 * 1000)

        return bills.filter { bill ->
            bill.nextDueDate in now..futureDate
        }.sortedBy { it.nextDueDate }
    }

    fun getOverdueBills(): List<RecurringBill> {
        val now = System.currentTimeMillis()
        return bills.filter { it.nextDueDate < now }.sortedByDescending { it.nextDueDate }
    }

    fun getTotalMonthlyBills(): Double {
        return bills.filter { it.frequency == "Monthly" }.sumOf { it.amount }
    }

    fun getBillsByCategory(): Map<String, Double> {
        return bills.groupBy { it.category }
            .mapValues { it.value.sumOf { bill -> bill.amount } }
    }

    fun remindBefore(daysAhead: Int): List<String> {
        val now = System.currentTimeMillis()
        val futureDate = now + (daysAhead * 24 * 60 * 60 * 1000)

        return bills.filter { bill ->
            bill.nextDueDate in now..futureDate
        }.map { bill ->
            val daysUntilDue = ((bill.nextDueDate - now) / (1000 * 60 * 60 * 24)).toInt()
            "${bill.name}: ${bill.category} of Ksh ${bill.amount} due in $daysUntilDue days"
        }
    }

    fun getAllBills(): List<RecurringBill> = bills

    private fun detectBillCategory(transactionName: String): String? {
        val name = transactionName.lowercase()
        return when {
            name.contains("rent") || name.contains("landlord") -> "Rent"
            name.contains("wifi") || name.contains("internet") -> "WiFi"
            name.contains("water") -> "Water"
            name.contains("school") || name.contains("fee") -> "School Fees"
            name.contains("chama") -> "Chama"
            name.contains("sacco") -> "SACCO"
            name.contains("loan") -> "Loan"
            else -> null
        }
    }

    private fun calculateNextDueDate(lastDate: Long, category: String): Long {
        val calendar = Calendar.getInstance().apply { timeInMillis = lastDate }
        calendar.add(Calendar.MONTH, 1)
        return calendar.timeInMillis
    }
}

