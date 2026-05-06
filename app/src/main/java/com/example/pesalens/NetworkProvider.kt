package com.example.pesalens

/**
 * Supported Kenyan mobile money network providers.
 * The parser also accepts generic Kenyan KES transaction messages as OTHER.
 */
enum class NetworkProvider(
    val displayName: String,
    val shortName: String,
    val badge: String,
    val keywords: List<String>
) {
    MPESA(
        displayName = "M-Pesa (Safaricom)",
        shortName = "M-Pesa",
        badge = "MP",
        keywords = listOf("M-PESA", "MPESA", "Safaricom", "Fuliza", "M-Shwari")
    ),
    AIRTEL(
        displayName = "Airtel Money",
        shortName = "Airtel",
        badge = "AM",
        keywords = listOf("Airtel Money", "AIRTEL MONEY", "Airtel", "AIRTEL")
    ),
    FAIBA(
        displayName = "Faiba Mobile Money",
        shortName = "Faiba",
        badge = "FB",
        keywords = listOf("Faiba", "FAIBA", "JTL")
    ),
    TELKOM(
        displayName = "T-Kash (Telkom)",
        shortName = "T-Kash",
        badge = "TK",
        keywords = listOf("T-kash", "TKASH", "T-Kash", "Telkom", "TELKOM")
    ),
    EQUITEL(
        displayName = "Equitel",
        shortName = "Equitel",
        badge = "EQ",
        keywords = listOf("Equitel", "EQUITEL", "EazzyPay", "Eazzy Pay")
    ),
    OTHER(
        displayName = "Other Kenyan Provider",
        shortName = "Other",
        badge = "KE",
        keywords = emptyList()
    );

    fun matches(message: String): Boolean =
        keywords.any { message.contains(it, ignoreCase = true) }

    companion object {
        fun detect(message: String, sender: String? = null): NetworkProvider? {
            val combined = listOfNotNull(sender, message).joinToString(" ")
            values()
                .filter { it != OTHER }
                .firstOrNull { it.matches(combined) }
                ?.let { return it }

            return if (looksLikeKenyanMoneyMessage(combined)) OTHER else null
        }

        private fun looksLikeKenyanMoneyMessage(message: String): Boolean {
            val lower = message.lowercase()
            val hasKenyanAmount = lower.contains("ksh") || lower.contains("kes")
            val hasTransactionWord = listOf(
                "confirmed",
                "received",
                "sent",
                "paid",
                "withdraw",
                "balance",
                "transaction",
                "airtime",
                "bundle"
            ).any { lower.contains(it) }

            return hasKenyanAmount && hasTransactionWord
        }
    }
}

/**
 * Unified transaction model across Kenyan mobile money providers.
 */
data class PesaTransaction(
    val amount: Double,
    val type: String,
    val name: String,
    val date: Long,
    val rawMessage: String,
    val fee: Double = 0.0,
    val isLoan: Boolean = false,
    val balance: Double? = null,
    val fulizaLimit: Double? = null,
    val provider: NetworkProvider = NetworkProvider.OTHER,
    val reference: String? = null
)
