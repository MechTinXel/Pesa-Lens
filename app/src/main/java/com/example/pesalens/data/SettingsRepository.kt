package com.example.pesalens.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "tinxel_settings")

data class CurrencyOption(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val kesToDisplayRate: Double
)

val CURRENCIES = listOf(
    CurrencyOption("KES", "Ksh", "Kenyan Shilling", "KE", 1.0),
    CurrencyOption("USD", "$", "US Dollar", "US", 0.0077),
    CurrencyOption("EUR", "EUR", "Euro", "EU", 0.0069),
    CurrencyOption("GBP", "GBP", "British Pound", "GB", 0.0059),
    CurrencyOption("UGX", "USh", "Ugandan Shilling", "UG", 27.6),
    CurrencyOption("TZS", "TSh", "Tanzanian Shilling", "TZ", 20.5),
    CurrencyOption("ZAR", "R", "South African Rand", "ZA", 0.135),
    CurrencyOption("NGN", "NGN", "Nigerian Naira", "NG", 11.7),
    CurrencyOption("GHS", "GHS", "Ghanaian Cedi", "GH", 0.084)
)

class SettingsRepository(context: Context) {
    private val dataStore = context.dataStore

    companion object {
        val THEME_KEY = stringPreferencesKey("theme_mode")
        val CURRENCY_KEY = stringPreferencesKey("currency_code")
        val SHOW_INCOME_KEY = booleanPreferencesKey("show_income")
        val SHOW_EXPENSES_KEY = booleanPreferencesKey("show_expenses")
    }

    val themeMode = dataStore.data.map { it[THEME_KEY] ?: "System" }
    val currencyCode = dataStore.data.map { it[CURRENCY_KEY] ?: "KES" }
    val showIncome = dataStore.data.map { it[SHOW_INCOME_KEY] ?: true }
    val showExpenses = dataStore.data.map { it[SHOW_EXPENSES_KEY] ?: true }

    suspend fun setThemeMode(mode: String) {
        dataStore.edit { it[THEME_KEY] = mode }
    }

    suspend fun setCurrency(code: String) {
        dataStore.edit { it[CURRENCY_KEY] = code }
    }

    suspend fun setShowIncome(show: Boolean) {
        dataStore.edit { it[SHOW_INCOME_KEY] = show }
    }

    suspend fun setShowExpenses(show: Boolean) {
        dataStore.edit { it[SHOW_EXPENSES_KEY] = show }
    }
}
