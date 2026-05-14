package com.example.pesalens.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesalens.PesaTransaction
import com.example.pesalens.data.CurrencyOption
import com.example.pesalens.data.formatMoney
import com.example.pesalens.logic.calculateFulizaHealth
import com.example.pesalens.ui.components.StandardDetailRow
import com.example.pesalens.ui.components.TransactionCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FulizaScreen(
    transactions: List<PesaTransaction>,
    currency: CurrencyOption,
    onBack: () -> Unit
) {
    val health = remember(transactions) { calculateFulizaHealth(transactions) }
    val fulizaTransactions = remember(transactions) { 
        transactions.filter { it.isFuliza }.sortedByDescending { it.date }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fuliza Health", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Current Balance (Debt)", style = MaterialTheme.typography.labelMedium)
                        Text(
                            formatMoney(health.currentUsage, currency),
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        val remaining = (health.totalLimit - health.currentUsage).coerceAtLeast(0.0)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Limit: ${formatMoney(health.totalLimit, currency)}", style = MaterialTheme.typography.labelSmall)
                            Text("Remaining: ${formatMoney(remaining, currency)}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                        
                        LinearProgressIndicator(
                            progress = { if (health.totalLimit > 0) (health.currentUsage / health.totalLimit).toFloat() else 0f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.error,
                            trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
                        )
                    }
                }
            }

            if (health.warnings.isNotEmpty()) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        health.warnings.forEach { warning ->
                            Surface(
                                color = MaterialTheme.colorScheme.errorContainer,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(Icons.Rounded.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    Text(warning, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text("Repayment Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StandardDetailRow("Total Repaid", formatMoney(health.repayments, currency))
                        StandardDetailRow("Days in Overdraft", "${health.daysInOverdraft} days")
                    }
                }
            }

            item {
                Text("Fuliza History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }

            if (fulizaTransactions.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No Fuliza history found", color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                items(fulizaTransactions) { tx ->
                    TransactionCard(tx, currency)
                }
            }

            item {
                Surface(
                    color = MaterialTheme.colorScheme.tertiaryContainer,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Info, contentDescription = null)
                        Column {
                            Text("Pro Tip", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text(
                                "Repay your Fuliza early to grow your limit and reduce interest costs.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}
