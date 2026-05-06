package com.example.pesalens.logic

data class ChamaTransaction(
    val date: Long,
    val type: String, // "Contribution", "Welfare", "Loan", "Disbursement"
    val amount: Double,
    val member: String = "",
    val description: String = ""
)

data class ChamaMember(
    val name: String,
    val joinDate: Long,
    val totalContributions: Double = 0.0,
    val loansReceived: Double = 0.0,
    val guarantorFor: List<String> = emptyList(),
    val missedContributions: Int = 0
)

class ChamaOrSACCOTracker {
    private val transactions = mutableListOf<ChamaTransaction>()
    private val members = mutableListOf<ChamaMember>()

    fun recordTransaction(transaction: ChamaTransaction) {
        transactions.add(transaction)
    }

    fun addMember(member: ChamaMember) {
        members.add(member)
    }

    fun getMonthlyContributions(startDate: Long, endDate: Long): Double {
        return transactions.filter {
            it.type == "Contribution" && it.date in startDate..endDate
        }.sumOf { it.amount }
    }

    fun getTotalWelfarePayments(startDate: Long, endDate: Long): Double {
        return transactions.filter {
            it.type == "Welfare" && it.date in startDate..endDate
        }.sumOf { it.amount }
    }

    fun getTotalLoansDispersed(): Double {
        return transactions.filter { it.type == "Loan" }
            .sumOf { it.amount }
    }

    fun getMemberLoansReceived(memberName: String): Double {
        return transactions.filter {
            it.type == "Disbursement" && it.member == memberName
        }.sumOf { it.amount }
    }

    fun trackMissedContributions(): List<Pair<String, Int>> {
        return members.map { it.name to it.missedContributions }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
    }

    fun getGuarantorObligations(memberName: String): List<String> {
        return members.firstOrNull { it.name == memberName }?.guarantorFor ?: emptyList()
    }

    fun getMemberSummary(memberName: String): String {
        val member = members.firstOrNull { it.name == memberName } ?: return "Member not found"

        return """
            $memberName - Chama Summary
            Total Contributions: Ksh ${member.totalContributions}
            Loans Received: Ksh ${member.loansReceived}
            Guarantor for: ${member.guarantorFor.size} member(s)
            Missed Contributions: ${member.missedContributions}
        """.trimIndent()
    }

    fun getAllTransactions(): List<ChamaTransaction> = transactions
    fun getAllMembers(): List<ChamaMember> = members
}

