package com.example.pesalens.data

import java.util.Locale
import kotlin.math.abs

fun currencyForCode(code: String): CurrencyOption =
    CURRENCIES.firstOrNull { it.code == code } ?: CURRENCIES.first()

fun formatMoney(
    amountKes: Double,
    currency: CurrencyOption,
    decimals: Int = 0,
    includeCode: Boolean = false
): String {
    val converted = amountKes * currency.kesToDisplayRate
    val pattern = "%,.${decimals}f"
    val value = String.format(Locale.US, pattern, converted)
    val suffix = if (includeCode) " ${currency.code}" else ""
    return "${currency.symbol} $value$suffix"
}

fun formatCompactMoney(amountKes: Double, currency: CurrencyOption): String {
    val converted = amountKes * currency.kesToDisplayRate
    val absolute = abs(converted)
    val (scaled, suffix) = when {
        absolute >= 1_000_000_000 -> converted / 1_000_000_000 to "B"
        absolute >= 1_000_000 -> converted / 1_000_000 to "M"
        absolute >= 1_000 -> converted / 1_000 to "K"
        else -> converted to ""
    }
    val decimals = if (abs(scaled) >= 10 || suffix.isEmpty()) 0 else 1
    return "${currency.symbol} ${String.format(Locale.US, "%,.${decimals}f", scaled)}$suffix"
}
