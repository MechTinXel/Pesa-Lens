package com.example.pesalens.logic

import java.util.*

data class KPLCToken(
    val date: Long,
    val unitsBought: Double,
    val costPerUnit: Double,
    val totalCost: Double,
    val estimatedNextPurchase: Long = 0L
)

class KPLCTokenTracker {
    private val tokens = mutableListOf<KPLCToken>()

    fun addToken(date: Long, unitsBought: Double, totalCost: Double) {
        val costPerUnit = totalCost / unitsBought
        val token = KPLCToken(
            date = date,
            unitsBought = unitsBought,
            costPerUnit = costPerUnit,
            totalCost = totalCost
        )
        tokens.add(token)
    }

    fun detectFromTransaction(transaction: PesaTransaction): KPLCToken? {
        if (!transaction.name.contains("KPLC", ignoreCase = true)) return null

        // Simple heuristic: KPLC tokens are usually around 500-5000 units
        // This would need actual parsing from SMS content
        return KPLCToken(
            date = transaction.date,
            unitsBought = estimateUnits(transaction.amount),
            costPerUnit = estimateCostPerUnit(transaction.amount),
            totalCost = transaction.amount
        )
    }

    fun getMonthlyElectricitySpend(startDate: Long, endDate: Long): Double {
        return tokens.filter { it.date in startDate..endDate }
            .sumOf { it.totalCost }
    }

    fun getExpectedNextPurchaseDate(): Long {
        if (tokens.isEmpty()) return 0L

        val sortedTokens = tokens.sortedByDescending { it.date }
        val lastToken = sortedTokens.first()

        // Estimate based on average monthly consumption
        val monthlySpend = getMonthlyElectricitySpend(
            lastToken.date - (30L * 24 * 60 * 60 * 1000),
            lastToken.date
        )

        // Simple heuristic: if average monthly is X, estimate next purchase in ~30 days
        val calendar = Calendar.getInstance().apply { timeInMillis = lastToken.date }
        calendar.add(Calendar.DAY_OF_MONTH, 30)

        return calendar.timeInMillis
    }

    fun getAverageCostPerUnit(): Double {
        return if (tokens.isNotEmpty()) {
            tokens.mapNotNull { it.costPerUnit }.average()
        } else 0.0
    }

    private fun estimateUnits(amount: Double): Double {
        // KPLC: typically Ksh5-10 per unit
        return amount / 7.0 // Average of 7 per unit
    }

    private fun estimateCostPerUnit(amount: Double): Double {
        // This would be calculated from actual token data
        return 7.0 // Default estimate
    }

    fun getAllTokens(): List<KPLCToken> = tokens
}

