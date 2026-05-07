package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction

data class FamilySupportPayment(
    val date: Long,
    val recipient: String, // "Mother", "Father", "Sister", "Brother", "Child", "Dependant"
    val amount: Double,
    val relationship: String,
    val description: String = ""
)

class FamilySupportTracker {
    private val payments = mutableListOf<FamilySupportPayment>()

    fun recordPayment(payment: FamilySupportPayment) {
        payments.add(payment)
    }

    fun detectFromTransaction(transaction: PesaTransaction): FamilySupportPayment? {
        val relationship = detectFamilyRelationship(transaction.name) ?: return null

        return FamilySupportPayment(
            date = transaction.date,
            recipient = transaction.name,
            amount = transaction.amount,
            relationship = relationship
        )
    }

    fun getTotalFamilySupport(startDate: Long, endDate: Long): Double {
        return payments.filter { it.date in startDate..endDate }
            .sumOf { it.amount }
    }

    fun getSupportByRecipient(): Map<String, Double> {
        return payments.groupBy { it.recipient }
            .mapValues { it.value.sumOf { payment -> payment.amount } }
    }

    fun getSupportByRelationship(): Map<String, Double> {
        return payments.groupBy { it.relationship }
            .mapValues { it.value.sumOf { payment -> payment.amount } }
    }

    fun getMonthlyAverageFamilySupport(): Double {
        if (payments.isEmpty()) return 0.0

        val now = System.currentTimeMillis()
        val sixMonthsAgo = now - (180L * 24 * 60 * 60 * 1000)

        val recentPayments = payments.filter { it.date in sixMonthsAgo..now }
        return if (recentPayments.isNotEmpty()) {
            recentPayments.sumOf { it.amount } / 6
        } else 0.0
    }

    fun isFamilySupportIncreasing(months: Int = 3): Boolean {
        val now = System.currentTimeMillis()
        var totalCurrent = 0.0
        var totalPrevious = 0.0

        payments.forEach { payment ->
            if (payment.date > now - (months * 30L * 24 * 60 * 60 * 1000)) {
                totalCurrent += payment.amount
            } else if (payment.date > now - (months * 2 * 30L * 24 * 60 * 60 * 1000)) {
                totalPrevious += payment.amount
            }
        }

        return totalCurrent > totalPrevious
    }

    fun getFamilySupportSummary(): String {
        val total = payments.sumOf { it.amount }
        val monthly = getMonthlyAverageFamilySupport()
        val isIncreasing = isFamilySupportIncreasing()
        val byRelationship = getSupportByRelationship()

        return """
            Total Family Support: Ksh $total
            Monthly Average: Ksh $monthly
            Trend: ${if (isIncreasing) "Increasing" else "Stable/Decreasing"}
            Breakdown: ${byRelationship.entries.joinToString(", ") { "${it.key}: Ksh ${it.value}" }}
        """.trimIndent()
    }

    fun getAllPayments(): List<FamilySupportPayment> = payments

    private fun detectFamilyRelationship(transactionName: String): String? {
        val name = transactionName.lowercase()
        return when {
            name.contains("mama") || name.contains("mother") -> "Mother"
            name.contains("papa") || name.contains("father") || name.contains("dad") -> "Father"
            name.contains("sister") || name.contains("sis") -> "Sister"
            name.contains("brother") || name.contains("bro") -> "Brother"
            name.contains("child") || name.contains("kid") -> "Child"
            name.contains("dependant") || name.contains("dependent") -> "Dependant"
            name.contains("parent") || name.contains("relatives") -> "Parents"
            else -> null
        }
    }
}

