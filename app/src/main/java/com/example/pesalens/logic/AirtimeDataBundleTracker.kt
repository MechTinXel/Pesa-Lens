package com.example.pesalens.logic

enum class TelecomProvider {
    SAFARICOM, AIRTEL, TELKOM, FAIBA
}

data class AirtimeSpend(
    val provider: TelecomProvider,
    val date: Long,
    val amount: Double,
    val type: String // "Airtime", "Data Bundle", "Internet"
)

class AirtimeDataBundleTracker {
    private val spends = mutableListOf<AirtimeSpend>()

    fun detectFromTransaction(transaction: PesaTransaction): AirtimeSpend? {
        val provider = detectTelecomProvider(transaction.name)
        if (provider != null && isAirtimeOrDataTransaction(transaction.name)) {
            return AirtimeSpend(
                provider = provider,
                date = transaction.date,
                amount = transaction.amount,
                type = detectType(transaction.name)
            )
        }
        return null
    }

    fun getProviderSpend(provider: TelecomProvider, startDate: Long, endDate: Long): Double {
        return spends.filter {
            it.provider == provider && it.date in startDate..endDate
        }.sumOf { it.amount }
    }

    fun getProviderSpends(): Map<TelecomProvider, Double> {
        val now = System.currentTimeMillis()
        val monthStart = now - (30L * 24 * 60 * 60 * 1000)

        return TelecomProvider.values().associateWith { provider ->
            getProviderSpend(provider, monthStart, now)
        }
    }

    fun getTotalAirtimeSpend(startDate: Long, endDate: Long): Double {
        return spends.filter { it.type == "Airtime" && it.date in startDate..endDate }
            .sumOf { it.amount }
    }

    fun getTotalDataSpend(startDate: Long, endDate: Long): Double {
        return spends.filter { it.type == "Data Bundle" && it.date in startDate..endDate }
            .sumOf { it.amount }
    }

    fun getHighestDrainerProvider(): TelecomProvider? {
        val now = System.currentTimeMillis()
        val monthStart = now - (30L * 24 * 60 * 60 * 1000)

        return spends.filter { it.date in monthStart..now }
            .groupBy { it.provider }
            .mapValues { it.value.sumOf { spend -> spend.amount } }
            .maxByOrNull { it.value }?.key
    }

    fun addSpend(spend: AirtimeSpend) {
        spends.add(spend)
    }

    fun getAllSpends(): List<AirtimeSpend> = spends

    private fun detectTelecomProvider(senderName: String): TelecomProvider? {
        val name = senderName.lowercase()
        return when {
            name.contains("safaricom") -> TelecomProvider.SAFARICOM
            name.contains("airtel") -> TelecomProvider.AIRTEL
            name.contains("telkom") -> TelecomProvider.TELKOM
            name.contains("faiba") -> TelecomProvider.FAIBA
            else -> null
        }
    }

    private fun isAirtimeOrDataTransaction(transactionName: String): Boolean {
        val name = transactionName.lowercase()
        return name.contains("airtime") || name.contains("data") ||
               name.contains("bundle") || name.contains("internet") ||
               name.contains("topup")
    }

    private fun detectType(transactionName: String): String {
        val name = transactionName.lowercase()
        return when {
            name.contains("data") || name.contains("bundle") -> "Data Bundle"
            name.contains("internet") -> "Internet"
            else -> "Airtime"
        }
    }
}

