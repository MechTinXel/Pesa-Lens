package com.example.pesalens.logic

data class PaybillCategory(
    val paybillCode: String,
    val detectedCategory: String,
    val isConfirmed: Boolean = false,
    val customName: String? = null
)

enum class TransactionCategory {
    KPLC, // Electricity (KPLC token)
    NAIVAS, // Supermarket
    QUICKMART, // Supermarket
    CARREFOUR, // Supermarket
    UBER, // Transportation/Ride
    BOLT, // Transportation/Ride
    SCHOOL_FEES, // Education
    BANK, // Banking/Financial
    RENT, // Housing
    BETTING, // Entertainment/Risky
    AIRTIME, // Telecom
    INTERNET, // Telecom
    WATER, // Utilities
    CHAMA, // Community
    SACCO, // Community
    LOAN, // Financial
    MAMA_MBOGA, // Shopping/Food
    SHOP, // Shopping
    RESTAURANT, // Food
    HOSPITAL, // Health
    OTHER
}

fun detectPaybillCategory(paybillCode: String, senderName: String): TransactionCategory {
    val combined = "$paybillCode $senderName".lowercase()

    return when {
        combined.contains("kplc") || paybillCode == "888880" -> TransactionCategory.KPLC
        combined.contains("naivas") || paybillCode == "891300" -> TransactionCategory.NAIVAS
        combined.contains("quickmart") || paybillCode == "400200" -> TransactionCategory.QUICKMART
        combined.contains("carrefour") || paybillCode == "381381" -> TransactionCategory.CARREFOUR
        combined.contains("uber") -> TransactionCategory.UBER
        combined.contains("bolt") -> TransactionCategory.BOLT
        combined.contains("school") || combined.contains("fee") -> TransactionCategory.SCHOOL_FEES
        combined.contains("bank") -> TransactionCategory.BANK
        combined.contains("rent") -> TransactionCategory.RENT
        combined.contains("bet") || combined.contains("stake") -> TransactionCategory.BETTING
        combined.contains("airtime") -> TransactionCategory.AIRTIME
        combined.contains("internet") || combined.contains("wifi") -> TransactionCategory.INTERNET
        combined.contains("water") -> TransactionCategory.WATER
        combined.contains("chama") -> TransactionCategory.CHAMA
        combined.contains("sacco") -> TransactionCategory.SACCO
        combined.contains("loan") -> TransactionCategory.LOAN
        combined.contains("mama") || combined.contains("grocer") -> TransactionCategory.MAMA_MBOGA
        combined.contains("shop") || combined.contains("supermarket") -> TransactionCategory.SHOP
        combined.contains("restaurant") || combined.contains("food") -> TransactionCategory.RESTAURANT
        combined.contains("hospital") || combined.contains("clinic") -> TransactionCategory.HOSPITAL
        else -> TransactionCategory.OTHER
    }
}

class PaybillAutoCategories {
    private val categoryRules = mutableMapOf<String, PaybillCategory>()

    fun getCategoryForPaybill(paybillCode: String): PaybillCategory? {
        return categoryRules[paybillCode]
    }

    fun confirmCategory(paybillCode: String, category: TransactionCategory) {
        val detected = detectPaybillCategory(paybillCode, "")
        val displayName = category.name.replace("_", " ")
        categoryRules[paybillCode] = PaybillCategory(
            paybillCode = paybillCode,
            detectedCategory = displayName,
            isConfirmed = true,
            customName = displayName
        )
    }

    fun setCustomName(paybillCode: String, customName: String) {
        val existing = categoryRules[paybillCode] ?: PaybillCategory(paybillCode, "OTHER")
        categoryRules[paybillCode] = existing.copy(customName = customName, isConfirmed = true)
    }

    fun getAllRules(): List<PaybillCategory> = categoryRules.values.toList()
}

