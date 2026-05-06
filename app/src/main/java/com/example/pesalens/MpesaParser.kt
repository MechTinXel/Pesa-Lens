package com.example.pesalens

import java.util.Locale

object MpesaParser {

    private val merchantMap = mapOf(
        "700700" to "KPLC (Postpaid)",
        "888888" to "KPLC (Prepaid)",
        "400222" to "Equity Bank",
        "522522" to "KCB Bank",
        "303030" to "Absa Bank",
        "222222" to "Family Bank",
        "982100" to "Zuku",
        "510800" to "DStv",
        "823000" to "Safaricom Home",
        "247247" to "Equity Paybill"
    )

    private val amountRegex = Regex("(?i)(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    private val referenceRegex = Regex("\\b([A-Z0-9]{8,12})\\s+Confirmed\\b", RegexOption.IGNORE_CASE)

    private val feePatterns = listOf(
        Regex("(?i)(?:transaction\\s*(?:cost|fee)|charge|charges?)\\s*(?:is|:|,)?\\s*(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Regex("(?i)(?:charged|cost)\\s*(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    )

    private val balancePatterns = listOf(
        Regex("(?i)(?:your\\s+|new\\s+)?(?:m[- ]?pesa|airtel\\s+money|t[- ]?kash|tkash|faiba|equitel)?\\s*(?:account\\s*)?balance\\s*(?:is|:)?\\s*(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Regex("(?i)(?:available|current|new)\\s+balance\\s*(?:is|:)?\\s*(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    )

    private val fulizaLimitPatterns = listOf(
        Regex("(?i)fuliza(?:\\s+m[- ]?pesa)?.{0,40}?(?:limit|allowance|available).{0,20}?(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)"),
        Regex("(?i)(?:limit|allowance|available).{0,30}?fuliza.{0,20}?(?:Kshs?|KES)\\.?\\s*([\\d,]+(?:\\.\\d{1,2})?)")
    )

    fun parseMessage(
        message: String,
        timestamp: Long,
        providerHint: NetworkProvider? = null
    ): PesaTransaction? {
        val provider = providerHint ?: NetworkProvider.detect(message) ?: return null
        val type = determineType(message)
        val amounts = amountRegex.findAll(message)
            .mapNotNull { it.groupValues.getOrNull(1)?.toMoney() }
            .toList()

        val balance = firstMoneyMatch(message, balancePatterns)
        val fulizaLimit = firstMoneyMatch(message, fulizaLimitPatterns)
        val amount = when {
            type == "Balance" -> 0.0
            amounts.isNotEmpty() -> amounts.first()
            balance != null || fulizaLimit != null -> 0.0
            else -> return null
        }

        return PesaTransaction(
            amount = amount,
            type = type,
            name = extractName(message, type, provider),
            date = timestamp,
            rawMessage = message,
            fee = firstMoneyMatch(message, feePatterns) ?: 0.0,
            isLoan = type == "Debt/Loan",
            balance = balance,
            fulizaLimit = fulizaLimit,
            provider = provider,
            reference = referenceRegex.find(message)?.groupValues?.getOrNull(1)
        )
    }

    private fun determineType(message: String): String {
        val lower = message.lowercase(Locale.ROOT)
        val hasMovementWord = listOf("sent", "received", "paid", "withdraw", "purchased", "bought").any { lower.contains(it) }
        val isBalanceOnly = lower.contains("balance") &&
            !hasMovementWord
        val isFulizaStatus = lower.contains("fuliza") &&
            listOf("limit", "allowance", "available").any { lower.contains(it) } &&
            !hasMovementWord

        return when {
            isFulizaStatus -> "Balance"
            isLoanMessage(message) -> "Debt/Loan"
            isBalanceOnly -> "Balance"
            lower.contains("received") || lower.contains("credited") -> "Received"
            lower.contains("withdraw") -> "Withdrawal"
            lower.contains("paybill") || lower.contains("pay bill") || lower.contains("account no") -> "Paybill"
            lower.contains("paid to") || lower.contains("buy goods") || lower.contains("till") || lower.contains("merchant") -> "Buy Goods"
            lower.contains("sent to") || lower.contains("transferred to") -> "Sent"
            lower.contains("airtime") || lower.contains("bundle") -> "Airtime/Data"
            else -> "Other"
        }
    }

    private fun extractName(message: String, type: String, provider: NetworkProvider): String {
        val patterns = when (type) {
            "Received" -> listOf(
                Regex("(?i)from\\s+(.+?)(?:\\s+on\\s+|\\s+at\\s+|\\.\\s*|$)")
            )
            "Sent", "Paybill", "Buy Goods" -> listOf(
                Regex("(?i)(?:sent to|paid to|transferred to|to)\\s+(.+?)(?:\\s+for\\s+|\\s+on\\s+|\\s+at\\s+|\\.\\s*|$)")
            )
            "Withdrawal" -> listOf(
                Regex("(?i)from\\s+(.+?)(?:\\s+on\\s+|\\s+at\\s+|\\.\\s*|$)")
            )
            "Airtime/Data" -> return "Airtime/Data"
            "Debt/Loan" -> return if (message.contains("M-Shwari", ignoreCase = true)) "M-Shwari" else "Fuliza"
            "Balance" -> return provider.shortName
            else -> listOf(
                Regex("(?i)(?:to|from|at)\\s+(.+?)(?:\\s+on\\s+|\\s+for\\s+|\\.\\s*|$)")
            )
        }

        val rawName = patterns
            .asSequence()
            .mapNotNull { it.find(message)?.groupValues?.getOrNull(1) }
            .map { cleanName(it) }
            .firstOrNull { it.isNotBlank() }
            ?: provider.shortName

        return merchantMap[rawName] ?: rawName
    }

    private fun cleanName(value: String): String =
        value
            .replace(Regex("\\s+"), " ")
            .trim()
            .trim('.', ',', '-', ':')

    private fun firstMoneyMatch(message: String, patterns: List<Regex>): Double? =
        patterns.asSequence()
            .mapNotNull { pattern -> pattern.find(message)?.groupValues?.getOrNull(1)?.toMoney() }
            .firstOrNull()

    private fun String.toMoney(): Double? =
        replace(",", "").toDoubleOrNull()

    private fun isLoanMessage(message: String): Boolean =
        message.contains("Fuliza", ignoreCase = true) ||
            message.contains("M-Shwari", ignoreCase = true)
}
