package com.example.pesalens.logic

import com.example.pesalens.PesaTransaction

data class MerchantCleanupRule(
    val originalName: String,
    val cleanedName: String,
    val category: String = "",
    val usageCount: Int = 0,
    val lastUsed: Long = 0L
)

class MerchantCleanupRules {
    private val rules = mutableMapOf<String, MerchantCleanupRule>()

    fun addRule(originalName: String, cleanedName: String, category: String = "") {
        rules[originalName.lowercase()] = MerchantCleanupRule(
            originalName = originalName,
            cleanedName = cleanedName,
            category = category,
            usageCount = 1,
            lastUsed = System.currentTimeMillis()
        )
    }

    fun getCleanedName(originalName: String): String {
        val key = originalName.lowercase()
        val rule = rules[key]

        if (rule != null) {
            rules[key] = rule.copy(usageCount = rule.usageCount + 1, lastUsed = System.currentTimeMillis())
        }

        return rule?.cleanedName ?: originalName
    }

    fun updateRule(originalName: String, cleanedName: String, category: String) {
        val key = originalName.lowercase()
        val existing = rules[key]

        rules[key] = MerchantCleanupRule(
            originalName = originalName,
            cleanedName = cleanedName,
            category = category,
            usageCount = existing?.usageCount ?: 1,
            lastUsed = System.currentTimeMillis()
        )
    }

    fun getMostUsedCleanups(): List<MerchantCleanupRule> {
        return rules.values.sortedByDescending { it.usageCount }
    }

    fun suggestCleanups(messyNames: List<String>): List<MerchantCleanupRule> {
        return messyNames
            .map { name -> rules[name.lowercase()] }
            .filterNotNull()
            .sortedByDescending { it.usageCount }
    }

    fun getAllRules(): List<MerchantCleanupRule> = rules.values.toList()

    fun export(): Map<String, String> = rules.mapValues { it.value.cleanedName }

    fun import(rulesMap: Map<String, String>) {
        rulesMap.forEach { (original, cleaned) ->
            addRule(original, cleaned)
        }
    }
}

/**
 * Multiple Lines/Wallets Tracker
 * Tracks spending across multiple telecom providers and accounts
 */
class MultipleWalletsTracker {
    private val wallets = mutableMapOf<String, WalletInfo>()

    data class WalletInfo(
        val walletId: String,
        val provider: String, // "Safaricom", "Airtel", "Telkom", "Faiba", "Bank", etc.
        val displayName: String,
        val phoneNumber: String? = null,
        val accountNumber: String? = null,
        val isActive: Boolean = true
    )

    fun addWallet(wallet: WalletInfo) {
        wallets[wallet.walletId] = wallet
    }

    fun removeWallet(walletId: String) {
        wallets.remove(walletId)
    }

    fun getWalletTransactions(
        walletId: String,
        transactions: List<PesaTransaction>
    ): List<PesaTransaction> {
        // In real app, this would filter by wallet-specific identifiers
        return transactions
    }

    fun getSpendingPerWallet(
        transactions: List<PesaTransaction>,
        startDate: Long,
        endDate: Long
    ): Map<String, Double> {
        return wallets.keys.associateWith { walletId ->
            getWalletTransactions(walletId, transactions)
                .filter { it.type == "Sent" && it.date in startDate..endDate }
                .sumOf { it.amount }
        }
    }

    fun getCombinedTotals(
        transactions: List<PesaTransaction>,
        startDate: Long,
        endDate: Long
    ): String {
        val byProvider = wallets.values.groupBy { it.provider }
            .mapValues { (_, walletList) ->
                walletList.sumOf { wallet ->
                    getWalletTransactions(wallet.walletId, transactions)
                        .filter { it.date in startDate..endDate }
                        .sumOf { it.amount }
                }
            }

        return byProvider.entries.joinToString("\n") { (provider, total) ->
            "$provider: Ksh $total"
        }
    }

    fun getActiveWallets(): List<WalletInfo> = wallets.values.filter { it.isActive }

    fun getAllWallets(): List<WalletInfo> = wallets.values.toList()
}

