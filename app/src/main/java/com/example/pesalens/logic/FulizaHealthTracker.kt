package com.example.pesalens.logic

data class FulizaHealthTracker(
    val currentUsage: Double = 0.0,
    val totalLimit: Double = 0.0,
    val repayments: Double = 0.0,
    val daysInOverdraft: Int = 0,
    val lastRepaymentDate: Long = 0L,
    val warnings: List<String> = emptyList()
)

fun calculateFulizaHealth(transactions: List<PesaTransaction>): FulizaHealthTracker {
    var totalUsage = 0.0
    var totalRepaid = 0.0
    var daysInOverdraft = 0

    transactions.forEach { transaction ->
        if (transaction.name.contains("Fuliza", ignoreCase = true)) {
            if (transaction.type == "Sent") {
                totalUsage += transaction.amount
            } else if (transaction.type == "Received") {
                totalRepaid += transaction.amount
            }
        }
    }

    val warnings = mutableListOf<String>()
    if (totalUsage > 10000) {
        daysInOverdraft = transactions.count {
            it.name.contains("Fuliza", ignoreCase = true)
        }
        if (daysInOverdraft > 0) {
            warnings.add("You are using Fuliza $daysInOverdraft days this month.")
        }
    }

    return FulizaHealthTracker(
        currentUsage = totalUsage,
        repayments = totalRepaid,
        daysInOverdraft = daysInOverdraft,
        warnings = warnings
    )
}

