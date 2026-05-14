package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction
import java.util.*
import java.util.Calendar

data class BudgetInsights(
    val mpesaBalance: Double,
    val fulizaAllowance: Double, // This will now represent "Remaining Limit"
    val totalFees: Double,
    val totalDebt: Double
)

object BudgetEngine {

    fun calculateInsights(transactions: List<PesaTransaction>): BudgetInsights {
        val newestFirst = transactions.sortedByDescending { it.date }
        val currentBalance = newestFirst.firstOrNull { it.balance != null }?.balance ?: 0.0
        val lastFulizaLimit = newestFirst.firstOrNull { it.fulizaLimit != null }?.fulizaLimit ?: 0.0
        
        // Accurate Fuliza debt calculation based on all-time borrow vs repay
        val fulizaBorrowed = transactions.filter { it.type == "Fuliza Borrow" }.sumOf { it.amount }
        val fulizaRepaid = transactions.filter { it.type == "Fuliza Repay" }.sumOf { it.amount }
        val currentFulizaDebt = (fulizaBorrowed - fulizaRepaid).coerceAtLeast(0.0)

        // Remaining amount user can spend via Fuliza
        val remainingFuliza = (lastFulizaLimit - currentFulizaDebt).coerceAtLeast(0.0)

        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH)
        val currentYear = calendar.get(Calendar.YEAR)

        val thisMonthTransactions = transactions.filter {
            val tCal = Calendar.getInstance().apply { timeInMillis = it.date }
            tCal.get(Calendar.MONTH) == currentMonth && tCal.get(Calendar.YEAR) == currentYear
        }

        val totalFees = thisMonthTransactions.sumOf { it.fee }
        val otherDebt = thisMonthTransactions.filter { it.type == "Debt/Loan" && !it.isFuliza }.sumOf { it.amount }

        return BudgetInsights(
            mpesaBalance = currentBalance,
            fulizaAllowance = remainingFuliza,
            totalFees = totalFees,
            totalDebt = currentFulizaDebt + otherDebt
        )
    }
}
