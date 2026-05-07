package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction

class MonthlySurvivalAnalysis {

    fun calculateMonthlyNecessary(
        dailyExpenses: List<PesaTransaction>,
        daysRemainingInMonth: Int
    ): Double {
        val now = System.currentTimeMillis()
        val monthStart = now - (30L * 24 * 60 * 60 * 1000)

        val thisMonthExpenses = dailyExpenses.filter {
            it.type == "Sent" && it.date in monthStart..now
        }.sumOf { it.amount }

        val dailyAverage = thisMonthExpenses / (30 - daysRemainingInMonth).coerceAtLeast(1)
        val neededToFinish = dailyAverage * daysRemainingInMonth

        return neededToFinish
    }

    fun getSurvivalSummary(transactions: List<PesaTransaction>): String {
        val now = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = now }

        val today = calendar.get(java.util.Calendar.DAY_OF_MONTH)
        val maxDay = calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
        val daysRemaining = maxDay - today

        val needed = calculateMonthlyNecessary(transactions, daysRemaining)

        return """
            💰 Monthly Survival Number for $daysRemaining Days Remaining
            ═════════════════════════════════════════════════
            Daily Spending Average: Ksh ${calculateDailyAverage(transactions)}
            You need: Ksh $needed to finish this month comfortably.
        """.trimIndent()
    }

    private fun calculateDailyAverage(transactions: List<PesaTransaction>): Double {
        val now = System.currentTimeMillis()
        val monthStart = now - (30L * 24 * 60 * 60 * 1000)

        val monthlyExpenses = transactions.filter {
            it.type == "Sent" && it.date in monthStart..now
        }.sumOf { it.amount }

        return monthlyExpenses / 30
    }
}

data class BlackTaxPayment(
    val date: Long,
    val amount: Double,
    val recipient: String,
    val description: String = ""
)

class BlackTaxTracker {
    private val payments = mutableListOf<BlackTaxPayment>()

    fun recordPayment(payment: BlackTaxPayment) {
        payments.add(payment)
    }

    fun getTotalBlackTax(startDate: Long, endDate: Long): Double {
        return payments.filter { it.date in startDate..endDate }
            .sumOf { it.amount }
    }

    fun getMonthlyBlackTax(): Double {
        val now = System.currentTimeMillis()
        val monthStart = now - (30L * 24 * 60 * 60 * 1000)
        return getTotalBlackTax(monthStart, now)
    }

    fun isBlackTaxIncreasing(): Boolean {
        val now = System.currentTimeMillis()
        val currentMonth = now - (30L * 24 * 60 * 60 * 1000)
        val previousMonth = now - (60L * 24 * 60 * 60 * 1000)

        val currentTotal = getTotalBlackTax(currentMonth, now)
        val previousTotal = getTotalBlackTax(previousMonth, currentMonth)

        return currentTotal > previousTotal
    }

    fun getBlackTaxPercentage(totalIncome: Double): Double {
        val monthlyBlackTax = getMonthlyBlackTax()
        return if (totalIncome > 0) (monthlyBlackTax / totalIncome) * 100 else 0.0
    }

    fun getBlackTaxReport(totalIncome: Double): String {
        val monthlyTotal = getMonthlyBlackTax()
        val percentage = getBlackTaxPercentage(totalIncome)
        val isIncreasing = isBlackTaxIncreasing()

        val recipients = payments.groupBy { it.recipient }
            .mapValues { it.value.sumOf { p -> p.amount } }

        return """
            🎫 Black Tax / Support Pressure Report
            ═══════════════════════════════════════
            Total This Month: Ksh $monthlyTotal
            Percentage of Income: ${String.format("%.1f", percentage)}%
            Trend: ${if (isIncreasing) "Increasing ⬆️" else "Decreasing ⬇️"}
            
            Support Recipients:
            ${recipients.entries.joinToString("\n") { "  ${it.key}: Ksh ${it.value}" }}
            
            ${if (percentage > 30) "⚠️ Warning: Your support obligations exceed 30% of income." else ""}
        """.trimIndent()
    }

    fun getAllPayments(): List<BlackTaxPayment> = payments
}

