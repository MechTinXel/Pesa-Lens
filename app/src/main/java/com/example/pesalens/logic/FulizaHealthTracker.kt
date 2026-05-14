package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction
import java.util.Calendar

data class FulizaHealthTracker(
    val currentUsage: Double = 0.0,
    val totalLimit: Double = 0.0,
    val repayments: Double = 0.0,
    val daysInOverdraft: Int = 0,
    val lastRepaymentDate: Long = 0L,
    val warnings: List<String> = emptyList()
)

fun calculateFulizaHealth(transactions: List<PesaTransaction>): FulizaHealthTracker {
    val fulizaTransactions = transactions.filter { it.isFuliza }
    
    // Sort by date to track balance changes
    val sorted = fulizaTransactions.sortedBy { it.date }
    
    var totalBorrowed = 0.0
    var totalRepaid = 0.0
    val overdraftDays = mutableSetOf<String>()
    
    fulizaTransactions.forEach { tx ->
        when (tx.type) {
            "Fuliza Borrow" -> totalBorrowed += tx.amount
            "Fuliza Repay" -> totalRepaid += tx.amount
        }
        
        // Track unique days where Fuliza was used
        val cal = Calendar.getInstance().apply { timeInMillis = tx.date }
        val dayKey = "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH)}-${cal.get(Calendar.DAY_OF_MONTH)}"
        overdraftDays.add(dayKey)
    }

    val currentUsage = (totalBorrowed - totalRepaid).coerceAtLeast(0.0)
    val latestLimit = transactions.sortedByDescending { it.date }.firstOrNull { it.fulizaLimit != null }?.fulizaLimit ?: 0.0

    val warnings = mutableListOf<String>()
    if (currentUsage > (latestLimit * 0.8) && latestLimit > 0) {
        warnings.add("High usage: You've used over 80% of your Fuliza limit.")
    }
    if (overdraftDays.size > 15) {
        warnings.add("Frequent use: You have used Fuliza on ${overdraftDays.size} different days recently.")
    }
    if (currentUsage > 0 && totalRepaid == 0.0 && fulizaTransactions.isNotEmpty()) {
        warnings.add("Reminder: Aim to repay your Fuliza balance to avoid daily maintenance fees.")
    }

    return FulizaHealthTracker(
        currentUsage = currentUsage,
        totalLimit = latestLimit,
        repayments = totalRepaid,
        daysInOverdraft = overdraftDays.size,
        warnings = warnings
    )
}
