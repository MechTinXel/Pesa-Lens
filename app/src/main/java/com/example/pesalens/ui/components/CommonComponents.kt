package com.example.pesalens.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pesalens.LocalPrivacyMode
import com.example.pesalens.PesaTransaction
import com.example.pesalens.data.CurrencyOption
import com.example.pesalens.data.formatMoney

@Composable
fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    isSensitive: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val privacyMode = LocalPrivacyMode.current.value

    Card(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring(dampingRatio = 0.6f)),
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                maxLines = 1
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = if (isSensitive && privacyMode) Modifier.blur(12.dp) else Modifier,
                maxLines = 1
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1
            )
        }
    }
}

@Composable
fun TransactionCard(
    transaction: PesaTransaction,
    currency: CurrencyOption,
    onClick: () -> Unit = {}
) {
    val privacyMode = LocalPrivacyMode.current.value

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.7f)),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                val typeColor = if (transaction.type == "Received")
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                if (transaction.fee > 0) {
                    Text(
                        text = "${transaction.type} • Fee: ${formatMoney(transaction.fee, currency, decimals = 0)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        modifier = if (privacyMode) Modifier.blur(8.dp) else Modifier,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = transaction.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = typeColor,
                        modifier = if (privacyMode) Modifier.blur(8.dp) else Modifier,
                        maxLines = 1
                    )
                }
            }

            Text(
                text = "${if (transaction.type == "Received") "+" else "-"} ${
                    formatMoney(transaction.amount, currency, decimals = 0)
                }",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "Received")
                    MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = if (privacyMode) Modifier.blur(12.dp) else Modifier,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StandardDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), 
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}
