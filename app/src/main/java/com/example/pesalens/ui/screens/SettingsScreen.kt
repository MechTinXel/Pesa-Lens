package com.example.pesalens.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.example.pesalens.data.CURRENCIES
import com.example.pesalens.data.CurrencyOption
import com.example.pesalens.data.SecureStorage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTheme: String,
    onThemeChange: (String) -> Unit,
    currentCurrency: String = "KES",
    onCurrencyChange: (String) -> Unit = {}
) {
    val context       = LocalContext.current
    val secureStorage = remember { SecureStorage(context) }
    val settingsRepository = remember { SettingsRepository(context) }

    var showThemeDialog    by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showApiKeySheet    by remember { mutableStateOf(false) }
    var apiKeyMasked       by remember { mutableStateOf(
        if (secureStorage.hasApiKey()) "sk-ant-......${secureStorage.anthropicApiKey.takeLast(4)}" else "Not configured"
    )}

    val showIncome by settingsRepository.showIncome.collectAsState(initial = true)
    val showExpenses by settingsRepository.showExpenses.collectAsState(initial = true)
    val scope = rememberCoroutineScope()

    val selectedCurrency = CURRENCIES.firstOrNull { it.code == currentCurrency } ?: CURRENCIES.first()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // ── Appearance ──────────────────────────────────────────────────
            item {
                SettingsSectionHeader("Appearance")
            }
            item {
                SettingsCard {
                    SettingsRow(
                        icon        = Icons.Rounded.DarkMode,
                        title       = "Theme",
                        subtitle    = currentTheme,
                        onClick     = { showThemeDialog = true }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRow(
                        icon     = Icons.Rounded.CurrencyExchange,
                        title    = "Currency",
                        subtitle = "${selectedCurrency.flag} ${selectedCurrency.name} (${selectedCurrency.symbol})",
                        onClick  = { showCurrencyDialog = true }
                    )
                }
            }

            // ── Transaction Display ─────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionHeader("Transaction Display")
            }
            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("Show Income", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Display money received", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline) },
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.TrendingUp, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = showIncome,
                                onCheckedChange = { scope.launch { settingsRepository.setShowIncome(it) } }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    ListItem(
                        headlineContent = { Text("Show Expenses", fontWeight = FontWeight.Medium) },
                        supportingContent = { Text("Display money spent", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline) },
                        leadingContent = {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.TrendingDown, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        trailingContent = {
                            Switch(
                                checked = showExpenses,
                                onCheckedChange = { scope.launch { settingsRepository.setShowExpenses(it) } }
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── AI ──────────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionHeader("AI Advisor")
            }
            item {
                SettingsCard {
                    SettingsRow(
                        icon     = Icons.Rounded.Key,
                        title    = "Anthropic API Key",
                        subtitle = apiKeyMasked,
                        onClick  = { showApiKeySheet = true }
                    )
                }
            }
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(Icons.Rounded.Info, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Text(
                            "The AI Advisor works offline without a paid API. Add a Claude key only if you want cloud analysis; your key is stored encrypted on-device.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // ── Privacy ─────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionHeader("Privacy & Data")
            }
            item {
                SettingsCard {
                    SettingsRow(
                        icon     = Icons.Rounded.Lock,
                        title    = "Biometric Lock",
                        subtitle = "Enabled — Fingerprint / Face ID",
                        onClick  = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRow(
                        icon     = Icons.Rounded.PhoneAndroid,
                        title    = "Data Storage",
                        subtitle = "All data stays on your device",
                        onClick  = {}
                    )
                }
            }

            // ── About ───────────────────────────────────────────────────────
            item {
                Spacer(Modifier.height(4.dp))
                SettingsSectionHeader("About")
            }
            item {
                SettingsCard {
                    SettingsRow(
                        icon     = Icons.Rounded.Info,
                        title    = "TinXel",
                        subtitle = "Version 2.0 — Work as play",
                        onClick  = {}
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRow(
                        icon     = Icons.Rounded.Shield,
                        title    = "Privacy Policy",
                        subtitle = "Data never leaves your phone",
                        onClick  = {}
                    )
                }
            }

            item { Spacer(Modifier.height(32.dp)) }
        }
    }

    // ── Theme dialog ──────────────────────────────────────────────────────────
    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text("Theme", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    listOf("Light" to Icons.Rounded.LightMode,
                        "Dark"  to Icons.Rounded.DarkMode,
                        "System" to Icons.Rounded.BrightnessAuto).forEach { (mode, icon) ->
                        ListItem(
                            headlineContent = { Text(mode) },
                            leadingContent  = { Icon(icon, null) },
                            trailingContent = {
                                RadioButton(
                                    selected = mode == currentTheme,
                                    onClick  = { onThemeChange(mode); showThemeDialog = false }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── Currency dialog ───────────────────────────────────────────────────────
    if (showCurrencyDialog) {
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text("Display Currency", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    CURRENCIES.forEach { currency ->
                        ListItem(
                            headlineContent  = { Text("${currency.flag} ${currency.name}") },
                            supportingContent = { Text("${currency.symbol} - ${currency.code}") },
                            trailingContent  = {
                                RadioButton(
                                    selected = currency.code == currentCurrency,
                                    onClick  = {
                                        onCurrencyChange(currency.code)
                                        showCurrencyDialog = false
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // ── API Key bottom sheet ──────────────────────────────────────────────────
    if (showApiKeySheet) {
        ApiKeyBottomSheet(
            currentKey = secureStorage.anthropicApiKey,
            onSave = { newKey ->
                secureStorage.anthropicApiKey = newKey
                apiKeyMasked = if (newKey.isNotBlank())
                    "sk-ant-......${newKey.takeLast(4)}"
                else "Not configured"
                showApiKeySheet = false
            },
            onClear = {
                secureStorage.clearApiKey()
                apiKeyMasked = "Not configured"
                showApiKeySheet = false
            },
            onDismiss = { showApiKeySheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiKeyBottomSheet(
    currentKey: String,
    onSave: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var keyInput   by remember { mutableStateOf(currentKey) }
    var showKey    by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Anthropic API Key", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(
                "Your key is encrypted and stored locally. Never shared.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )

            OutlinedTextField(
                value         = keyInput,
                onValueChange = { keyInput = it },
                label         = { Text("API Key") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(14.dp),
                singleLine    = true,
                visualTransformation = if (showKey) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon  = {
                    IconButton(onClick = { showKey = !showKey }) {
                        Icon(if (showKey) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, null)
                    }
                }
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (currentKey.isNotBlank()) {
                    OutlinedButton(
                        onClick  = onClear,
                        modifier = Modifier.weight(1f),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Remove Key") }
                }
                Button(
                    onClick  = { onSave(keyInput.trim()) },
                    modifier = Modifier.weight(1f),
                    shape    = RoundedCornerShape(12.dp)
                ) { Text("Save Key") }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

// ── Reusable components ───────────────────────────────────────────────────────

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text  = title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape  = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent  = { Text(title, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(subtitle, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline) },
        leadingContent   = {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        },
        trailingContent = {
            if (onClick != {}) {
                Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(20.dp))
            }
        },
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    )
}
