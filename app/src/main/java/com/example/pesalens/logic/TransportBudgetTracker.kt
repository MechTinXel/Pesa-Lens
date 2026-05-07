package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction
import java.util.*

data class TransportSpend(
    val date: Long,
    val amount: Double,
    val type: String, // "Matatu", "Uber", "Bolt", "Taxi", "Other"
    val description: String = ""
)

class TransportBudgetTracker {
    private val spends = mutableListOf<TransportSpend>()

    fun recordSpend(spend: TransportSpend) {
        spends.add(spend)
    }

    fun detectFromTransaction(transaction: PesaTransaction): TransportSpend? {
        val type = detectTransportType(transaction.name) ?: return null

        return TransportSpend(
            date = transaction.date,
            amount = transaction.amount,
            type = type
        )
    }

    fun getWeeklyCommuteCost(): Double {
        val now = System.currentTimeMillis()
        val weekAgo = now - (7L * 24 * 60 * 60 * 1000)

        return spends.filter { it.date in weekAgo..now }
            .sumOf { it.amount }
    }

    fun getDailyAverageCommute(): Double {
        val now = System.currentTimeMillis()
        val thirtyDaysAgo = now - (30L * 24 * 60 * 60 * 1000)

        val recentSpends = spends.filter { it.date in thirtyDaysAgo..now }
        return if (recentSpends.isNotEmpty()) {
            recentSpends.sumOf { it.amount } / 30
        } else 0.0
    }

    fun getMonthlyTransportBudget(): Double {
        val now = System.currentTimeMillis()
        val monthAgo = now - (30L * 24 * 60 * 60 * 1000)

        return spends.filter { it.date in monthAgo..now }
            .sumOf { it.amount }
    }

    fun getSpendByType(): Map<String, Double> {
        return spends.groupBy { it.type }
            .mapValues { it.value.sumOf { spend -> spend.amount } }
    }

    fun getMostExpensiveTransportMode(): String? {
        return getSpendByType().maxByOrNull { it.value }?.key
    }

    fun getWeeklyBreakdown(): Map<String, Double> {
        val breakdown = mutableMapOf<String, Double>()
        val calendar = Calendar.getInstance()

        for (i in 0..6) {
            val dayStart = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            val dayEnd = calendar.timeInMillis

            val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
            val dayName = when (dayOfWeek) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "Day$dayOfWeek"
            }

            val daySpend = spends.filter { it.date in dayStart..dayEnd }
                .sumOf { it.amount }

            breakdown[dayName] = daySpend
        }

        return breakdown
    }

    fun getCommuteBudgetAdvice(): String {
        val weekly = getWeeklyCommuteCost()
        val monthly = getMonthlyTransportBudget()

        return """
            Weekly Commute Cost: Ksh $weekly
            Monthly Transport Budget: Ksh $monthly
            Daily Average: Ksh ${getDailyAverageCommute()}
            Most Used: ${getMostExpensiveTransportMode()}
            ${if (monthly > 5000) "⚠️ Your transport budget is high. Consider alternatives or carpooling." else "✓ Your transport spending is reasonable."}
        """.trimIndent()
    }

    fun getAllSpends(): List<TransportSpend> = spends

    private fun detectTransportType(transactionName: String): String? {
        val name = transactionName.lowercase()
        return when {
            name.contains("matatu") || name.contains("mpesa to matatu") || name.contains("fare") -> "Matatu"
            name.contains("uber") -> "Uber"
            name.contains("bolt") -> "Bolt"
            name.contains("taxi") -> "Taxi"
            name.contains("transport") || name.contains("commute") -> "Transport"
            else -> null
        }
    }
}

