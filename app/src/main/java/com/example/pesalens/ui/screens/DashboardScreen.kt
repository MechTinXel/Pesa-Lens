package com.example.pesalens.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pesalens.NetworkProvider
import com.example.pesalens.PesaTransaction
import com.example.pesalens.data.CurrencyOption
import com.example.pesalens.data.formatMoney
import com.example.pesalens.logic.BudgetInsights
import com.example.pesalens.ui.components.InsightCard
import com.example.pesalens.ui.components.TransactionCard
import com.example.pesalens.ui.components.TinXelLogo

@Composable
fun DashboardScreen(
    transactions: List<PesaTransaction>,
    selectedCurrency: CurrencyOption,
    insights: BudgetInsights?,
    selectedProviderName: String?,
    onProviderSelected: (String?) -> Unit,
    availableYears: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit,
    showIncome: Boolean,
    showExpenses: Boolean,
    onNavigateToExpenses: () -> Unit,
    onNavigateToIncome: () -> Unit,
    onNavigateToFuliza: () -> Unit
) {
    val selectedProvider = selectedProviderName?.let {
        runCatching { NetworkProvider.valueOf(it) }.getOrNull()
    }

    val visibleTransactions = remember(transactions, selectedProvider, selectedYear) {
        transactions.filter { transaction ->
            (selectedProvider == null || transaction.provider == selectedProvider) &&
                    transactionYear(transaction.date) == selectedYear
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            TransactionFilterBar(
                selectedProviderName = selectedProviderName,
                onProviderSelected = onProviderSelected,
                availableYears = availableYears,
                selectedYear = selectedYear,
                onYearSelected = onYearSelected
            )
        }

        item {
            AnimatedVisibility(
                visible = visibleTransactions.isNotEmpty() && insights != null,
                enter = expandVertically() + fadeIn(),
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    InsightCard(
                        title = "${selectedProvider?.shortName ?: "Mobile Money"} Balance",
                        value = formatMoney(insights?.mpesaBalance ?: 0.0, selectedCurrency, decimals = 0),
                        subtitle = "Available Now",
                        color = MaterialTheme.colorScheme.primaryContainer,
                        isSensitive = true,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedProvider == null || selectedProvider == NetworkProvider.MPESA) {
                        InsightCard(
                            title = "Remaining Fuliza",
                            value = formatMoney(insights?.fulizaAllowance ?: 0.0, selectedCurrency, decimals = 0),
                            subtitle = "Available to Spend",
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            isSensitive = true,
                            modifier = Modifier.weight(1f),
                            onClick = onNavigateToFuliza
                        )
                    }
                }
            }
        }


        item {
            Text(
                "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        }

        val recentTransactions = visibleTransactions.take(10)
        if (recentTransactions.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No transactions yet", color = MaterialTheme.colorScheme.outline)
                }
            }
        } else {
            items(recentTransactions) { transaction ->
                Box(Modifier.padding(horizontal = 12.dp)) {
                    TransactionCard(transaction, selectedCurrency)
                }
            }
        }
    }
}

@Composable
private fun TransactionFilterBar(
    selectedProviderName: String?,
    onProviderSelected: (String?) -> Unit,
    availableYears: List<Int>,
    selectedYear: Int,
    onYearSelected: (Int) -> Unit
) {
    var expandedProvider by remember { mutableStateOf(false) }
    var expandedYear by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expandedProvider = !expandedProvider },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    selectedProviderName?.let { NetworkProvider.valueOf(it).shortName } ?: "All Providers",
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1
                )
            }
            DropdownMenu(
                expanded = expandedProvider,
                onDismissRequest = { expandedProvider = false },
                modifier = Modifier.fillMaxWidth(0.5f)
            ) {
                DropdownMenuItem(
                    text = { Text("All", style = MaterialTheme.typography.bodyMedium) },
                    onClick = {
                        onProviderSelected(null)
                        expandedProvider = false
                    }
                )
                NetworkProvider.entries.forEach { provider ->
                    DropdownMenuItem(
                        text = { Text(provider.shortName, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onProviderSelected(provider.name)
                            expandedProvider = false
                        }
                    )
                }
            }
        }

        if (availableYears.isNotEmpty()) {
            Box(modifier = Modifier.weight(0.6f)) {
                OutlinedButton(
                    onClick = { expandedYear = !expandedYear },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        selectedYear.toString(),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                DropdownMenu(
                    expanded = expandedYear,
                    onDismissRequest = { expandedYear = false },
                    modifier = Modifier.fillMaxWidth(0.3f)
                ) {
                    availableYears.forEach { year ->
                        DropdownMenuItem(
                            text = { Text(year.toString(), style = MaterialTheme.typography.bodyMedium) },
                            onClick = {
                                onYearSelected(year)
                                expandedYear = false
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun transactionYear(timestamp: Long): Int =
    java.util.Calendar.getInstance().apply { timeInMillis = timestamp }.get(java.util.Calendar.YEAR)

// Re-using InsightCard and TransactionCard from MainActivity for now, 
// but they should eventually be moved to a common components package.
// For this task, I'll copy them here or keep them in MainActivity if they are public.
// Actually, they are private/internal in MainActivity. I should make them accessible.
