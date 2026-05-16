package com.example.pesalens.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesalens.LocalPrivacyMode
import com.example.pesalens.PesaTransaction
import com.example.pesalens.data.CurrencyOption
import com.example.pesalens.data.formatMoney
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(transactions: List<PesaTransaction>, currency: CurrencyOption) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTransaction by remember { mutableStateOf<PesaTransaction?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") } // Filter: All, Income, Expenses
    var selectedTimeFilter by remember { mutableStateOf("All") } // Time filter: All, Today, Week, Month
    val listState = rememberLazyListState()

    val filteredTransactions = remember(searchQuery, transactions, selectedFilter, selectedTimeFilter) {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance().apply { timeInMillis = now }

        transactions.filter { transaction ->
            // Apply time filter first
            val passesTimeFilter = when (selectedTimeFilter) {
                "Today" -> {
                    val today = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    transaction.date >= today.timeInMillis
                }
                "Week" -> {
                    val weekStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    transaction.date >= weekStart.timeInMillis
                }
                "Month" -> {
                    val monthStart = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    transaction.date >= monthStart.timeInMillis
                }
                else -> true // No time filter
            }

            // Apply type filter
            val passesTypeFilter = when (selectedFilter) {
                "Income" -> transaction.type == "Received"
                "Expenses" -> transaction.type != "Received" && transaction.type != "Balance"
                else -> true // "All"
            }

            // Apply search filter
            val passesSearchFilter = searchQuery.isBlank() ||
                    transaction.name.contains(searchQuery, ignoreCase = true) ||
                    transaction.rawMessage.contains(searchQuery, ignoreCase = true) ||
                    transaction.type.contains(searchQuery, ignoreCase = true)

            passesTimeFilter && passesTypeFilter && passesSearchFilter
        }
    }

    val grouped = remember(filteredTransactions) {
        filteredTransactions.groupBy {
            SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date(it.date))
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {

        // ── Quick actions & insights ───────────────────────────────────────
        AnimatedVisibility(visible = transactions.isNotEmpty() && !isSearchActive) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quick time filters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Today", "Week", "Month").forEach { period ->
                        FilterChip(
                            selected = selectedTimeFilter == period,
                            onClick = { selectedTimeFilter = period },
                            label = { Text(period, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { /* TODO: export functionality */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.FileDownload, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Export", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = { /* TODO: share functionality */ },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Share", style = MaterialTheme.typography.labelMedium)
                    }

                    OutlinedButton(
                        onClick = {
                            searchQuery = ""
                            selectedFilter = "All"
                            selectedTimeFilter = "All"
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Rounded.Clear, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Clear", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // ── Search bar: Minimal padding ─────────────────────────────────────────
        SearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { isSearchActive = false },
            active = isSearchActive,
            onActiveChange = { isSearchActive = it },
            placeholder = { Text("Search…", color = MaterialTheme.colorScheme.outline) },
            leadingIcon = {
                Icon(Icons.Rounded.Search, contentDescription = null,
                    tint = if (isSearchActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { searchQuery = ""; isSearchActive = false }) {
                        Icon(Icons.Rounded.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            tonalElevation = 0.dp
        ) {
            // Search suggestions (minimal)
            if (searchQuery.isBlank()) {
                listOf("Received", "Sent", "Paybill").forEach { type ->
                    ListItem(
                        headlineContent = { Text(type) },
                        leadingContent = { Icon(Icons.Rounded.Tag, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth().clickable { searchQuery = type; isSearchActive = false }
                    )
                }
            }
        }

        // ── Filter tabs ──────────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Income", "Expenses").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter, style = MaterialTheme.typography.labelMedium) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        if (filteredTransactions.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Rounded.SearchOff, contentDescription = null,
                        modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                    Text("No transactions found", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline)
                    if (searchQuery.isNotBlank()) {
                        TextButton(onClick = { searchQuery = "" }) { Text("Clear search") }
                    }
                }
            }
            return@Column
        }

        // ── Transaction list: Compact spacing ────────────────────────────────
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            grouped.forEach { (date, dailyTransactions) ->
                stickyHeader(key = date) {
                    DateHeader(date = date, count = dailyTransactions.size,
                        dayTotal = dailyTransactions.filter { it.type != "Received" }.sumOf { it.amount })
                }
                items(dailyTransactions, key = { it.rawMessage + it.date }) { transaction ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn() + slideInHorizontally()
                    ) {
                        RichTransactionCard(transaction = transaction, currency = currency) {
                            selectedTransaction = transaction
                            showSheet = true
                        }
                    }
                }
                item { Spacer(Modifier.height(4.dp)) }
            }
        }
    }

    // ── Detail bottom sheet ──────────────────────────────────────────────────
    if (showSheet && selectedTransaction != null) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            TransactionDetailContent(selectedTransaction!!, currency)
        }
    }
}

@Composable
private fun DateHeader(date: String, count: Int, dayTotal: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
            )
            Text(
                text = date,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    "$count txn${if (count > 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun RichTransactionCard(
    transaction: PesaTransaction,
    currency: CurrencyOption,
    onClick: () -> Unit
) {
    val privacyMode = LocalPrivacyMode.current.value
    val isReceived = transaction.type == "Received"
    val isLoan = transaction.isLoan || transaction.type == "Debt/Loan"

    val accentColor = when {
        isLoan     -> MaterialTheme.colorScheme.tertiary
        isReceived -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.error
    }

    val bgColor = when {
        isLoan     -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f)
        isReceived -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        else       -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
    }

    val typeIcon = when {
        isLoan                 -> Icons.Rounded.CreditCard
        isReceived             -> Icons.Rounded.ArrowDownward
        transaction.type == "Paybill"   -> Icons.Rounded.Receipt
        transaction.type == "Buy Goods" -> Icons.Rounded.ShoppingBag
        else                   -> Icons.Rounded.ArrowUpward
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Icon circle
            Surface(
                shape = CircleShape,
                color = accentColor.copy(alpha = 0.12f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(typeIcon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                }
            }

            // Name + type
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = accentColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = transaction.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = accentColor,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                    if (transaction.fee > 0) {
                        Text(
                            text = "Fee: ${formatMoney(transaction.fee, currency, decimals = 2)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = if (privacyMode) Modifier.blur(6.dp) else Modifier
                        )
                    }
                }
            }

            // Amount + time
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${if (isReceived) "+" else "-"} ${formatMoney(transaction.amount, currency, decimals = 2)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = if (privacyMode) Modifier.blur(12.dp) else Modifier
                )
                Text(
                    text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(transaction.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

// ── Detail sheet content ─────────────────────────────────────────────────────

@Composable
fun TransactionDetailContent(transaction: PesaTransaction, currency: CurrencyOption) {
    val isReceived = transaction.type == "Received"
    val isLoan = transaction.isLoan || transaction.type == "Debt/Loan"
    val accentColor = when {
        isLoan     -> MaterialTheme.colorScheme.tertiary
        isReceived -> MaterialTheme.colorScheme.primary
        else       -> MaterialTheme.colorScheme.error
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp)) {
        // Pull handle
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)
            .width(40.dp).height(4.dp).clip(CircleShape)
            .background(MaterialTheme.colorScheme.outlineVariant))

        Spacer(Modifier.height(20.dp))

        // Amount hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(accentColor.copy(alpha = 0.08f))
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${if (isReceived) "+" else "-"} ${formatMoney(transaction.amount, currency, decimals = 2)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = accentColor
                )
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Details list
        DetailRow("Type",        transaction.type)
        DetailRow("Date",        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(transaction.date)))
        DetailRow("Provider", transaction.provider.displayName)
        transaction.reference?.let { DetailRow("Reference", it) }
        if (transaction.fee > 0) DetailRow("Fee", formatMoney(transaction.fee, currency, decimals = 2))
        transaction.balance?.let { DetailRow("Balance after", formatMoney(it, currency, decimals = 2)) }
        transaction.fulizaLimit?.let { DetailRow("Fuliza limit", formatMoney(it, currency, decimals = 2)) }

        Spacer(Modifier.height(20.dp))

        Text("Original SMS", style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = transaction.rawMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp,
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}
