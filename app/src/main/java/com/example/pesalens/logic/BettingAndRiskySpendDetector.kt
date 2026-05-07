package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction

data class BettingTransaction(
    val date: Long,
    val amount: Double,
    val platform: String, // "Betika", "Sportybet", "1xbet", etc.
    val type: String // "Stake", "Win", "Bonus"
)

class BettingAndRiskySpendDetector {
    private val transactions = mutableListOf<BettingTransaction>()

    private val risky_keywords = listOf(
        "betika", "sportybet", "1xbet", "bet", "stake",
        "gambling", "casino", "lotto", "kina",
        "betting", "odds", "jackpot"
    )

    fun recordTransaction(transaction: BettingTransaction) {
        transactions.add(transaction)
    }

    fun detectFromTransaction(transaction: PesaTransaction): BettingTransaction? {
        val platform = detectBettingPlatform(transaction.name) ?: return null
        val type = if (transaction.type == "Sent") "Stake" else "Win"

        return BettingTransaction(
            date = transaction.date,
            amount = transaction.amount,
            platform = platform,
            type = type
        )
    }

    fun getTotalBettingSpend(startDate: Long, endDate: Long): Double {
        return transactions.filter {
            it.type == "Stake" && it.date in startDate..endDate
        }.sumOf { it.amount }
    }

    fun getTotalWinsReceived(startDate: Long, endDate: Long): Double {
        return transactions.filter {
            it.type == "Win" && it.date in startDate..endDate
        }.sumOf { it.amount }
    }

    fun getNetBettingLoss(startDate: Long, endDate: Long): Double {
        val spent = getTotalBettingSpend(startDate, endDate)
        val won = getTotalWinsReceived(startDate, endDate)
        return spent - won
    }

    fun getBettingTrend(): String {
        val now = System.currentTimeMillis()
        val currentMonth = now - (30L * 24 * 60 * 60 * 1000)
        val previousMonth = now - (60L * 24 * 60 * 60 * 1000)

        val currentSpend = getTotalBettingSpend(currentMonth, now)
        val previousSpend = getTotalBettingSpend(previousMonth, currentMonth)

        return when {
            currentSpend > previousSpend * 1.2 -> "Increasing ⬆️"
            currentSpend < previousSpend * 0.8 -> "Decreasing ⬇️"
            else -> "Stable"
        }
    }

    fun getBettingReport(): String {
        val now = System.currentTimeMillis()
        val monthStart = now - (30L * 24 * 60 * 60 * 1000)

        val totalSpent = getTotalBettingSpend(monthStart, now)
        val totalWon = getTotalWinsReceived(monthStart, now)
        val netLoss = getNetBettingLoss(monthStart, now)
        val trend = getBettingTrend()

        val platformBreakdown = transactions.filter { it.date in monthStart..now && it.type == "Stake" }
            .groupBy { it.platform }
            .mapValues { it.value.sumOf { t -> t.amount } }

        return """
            📊 Betting & Risky Spend Report
            ═══════════════════════════════════
            Total Staked: Ksh $totalSpent
            Total Won: Ksh $totalWon
            Net Loss: Ksh $netLoss 🔴
            Trend: $trend
            
            By Platform:
            ${platformBreakdown.entries.joinToString("\n") { "  ${it.key}: Ksh ${it.value}" }}
            
            ${if (netLoss > 10000) "⚠️ Warning: You've lost over Ksh 10,000 this month. Consider reducing betting." else ""}
        """.trimIndent()
    }

    fun isRiskyTransaction(transactionName: String): Boolean {
        return risky_keywords.any { keyword ->
            transactionName.lowercase().contains(keyword)
        }
    }

    fun getAllTransactions(): List<BettingTransaction> = transactions

    private fun detectBettingPlatform(transactionName: String): String? {
        val name = transactionName.lowercase()
        return when {
            name.contains("betika") -> "Betika"
            name.contains("sportybet") -> "Sportybet"
            name.contains("1xbet") || name.contains("1x bet") -> "1xbet"
            name.contains("bet365") || name.contains("bet 365") -> "Bet365"
            name.contains("kina") -> "Kina"
            name.contains("lotto") -> "Lotto"
            else -> null
        }
    }
}

