package com.example.pesalens

import android.Manifest
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.pesalens.data.CURRENCIES
import com.example.pesalens.data.SettingsRepository
import com.example.pesalens.data.currencyForCode
import com.example.pesalens.data.formatMoney
import com.example.pesalens.logic.AIAssistant
import com.example.pesalens.logic.BudgetEngine
import com.example.pesalens.logic.BudgetInsights
import com.example.pesalens.ui.*
import com.example.pesalens.ui.components.TinXelLogo
import com.example.pesalens.ui.screens.*
import com.example.pesalens.ui.theme.PesaLensTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale

// CompositionLocal for Privacy Mode
val LocalPrivacyMode = compositionLocalOf { mutableStateOf(false) }

class MainActivity : FragmentActivity() {

    private lateinit var settingsRepository: SettingsRepository
    private var isUnlocked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        settingsRepository = SettingsRepository(this)

        showBiometricPrompt()

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = "System")
            val isPrivacyMode = remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            PesaLensTheme(themeMode = themeMode) {
                CompositionLocalProvider(LocalPrivacyMode provides isPrivacyMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        if (!isUnlocked) {
                            LockScreen(onRetry = { showBiometricPrompt() })
                        } else if (hasSmsPermission()) {
                            MainScreen(
                                onLoadTransactions = { loadMpesaMessages() },
                                currentTheme = themeMode,
                                onThemeChange = { mode ->
                                    scope.launch { settingsRepository.setThemeMode(mode) }
                                }
                            )
                        } else {
                            PermissionScreen(
                                modifier = Modifier.padding(WindowInsets.statusBars.asPaddingValues()),
                                onRequestPermission = { requestPermission() },
                                isPermissionGranted = false,
                            )
                        }
                    }
                }
            }
        }
    }

    private fun showBiometricPrompt() {
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked = true
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("TinXel Security")
            .setSubtitle("Unlock to access your financial data")
            .setNegativeButtonText("Exit")
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private suspend fun loadMpesaMessages(): List<PesaTransaction> = withContext(Dispatchers.IO) {
        val transactions = mutableListOf<PesaTransaction>()
        if (!hasSmsPermission()) return@withContext transactions

        val uri: Uri = "content://sms/inbox".toUri()
        val cursor: Cursor? = contentResolver.query(uri, null, null, null, "date DESC")

        cursor?.use {
            val bodyColumn = it.getColumnIndex("body")
            val dateColumn = it.getColumnIndex("date")
            val senderColumn = it.getColumnIndex("address")

            if (bodyColumn < 0 || dateColumn < 0) return@use

            while (it.moveToNext()) {
                val message = it.getString(bodyColumn)
                val timestamp = it.getLong(dateColumn)
                val sender = if (senderColumn >= 0) it.getString(senderColumn) else null

                val provider = NetworkProvider.detect(message, sender)
                if (provider != null) {
                    val parsed = MpesaParser.parseMessage(message, timestamp, provider)
                    parsed?.let { transaction -> transactions.add(transaction) }
                }
            }
        }
        return@withContext transactions
    }

    private fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermission() {
        permissionLauncher.launch(Manifest.permission.READ_SMS)
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
        recreate()
    }
}

@Composable
fun MainScreen(
    onLoadTransactions: suspend () -> List<PesaTransaction>,
    currentTheme: String,
    onThemeChange: (String) -> Unit
) {
    val navController = rememberNavController()
    var transactions by remember { mutableStateOf(listOf<PesaTransaction>()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        transactions = onLoadTransactions()
        isLoading = false
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository(context) }
    val themeMode by settingsRepository.themeMode.collectAsState(initial = "System")
    val currencyCode by settingsRepository.currencyCode.collectAsState(initial = "KES")
    val selectedCurrency = remember(currencyCode) { currencyForCode(currencyCode) }
    var selectedProviderName by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedYear by rememberSaveable { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    val selectedProvider = selectedProviderName?.let { runCatching { NetworkProvider.valueOf(it) }.getOrNull() }
    val availableYears = remember(transactions) {
        transactions.map { transactionYear(it.date) }.distinct().sortedDescending()
    }

    LaunchedEffect(availableYears) {
        if (availableYears.isNotEmpty() && selectedYear !in availableYears) {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            selectedYear = availableYears.firstOrNull { it == currentYear } ?: availableYears.first()
        }
    }

    val visibleTransactions = remember(transactions, selectedProvider, selectedYear) {
        transactions.filter { transaction ->
            (selectedProvider == null || transaction.provider == selectedProvider) &&
                transactionYear(transaction.date) == selectedYear
        }
    }

    val insights = remember(visibleTransactions) {
        BudgetEngine.calculateInsights(visibleTransactions)
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Filter out Settings from bottom bar
            val bottomNavItems = navItems.filter { it.route != Screen.Settings.route }

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeCap = StrokeCap.Round)
            }
        } else {
            val privacyMode = LocalPrivacyMode.current
            
            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = {
                    scope.launch {
                        isRefreshing = true
                        transactions = onLoadTransactions()
                        isRefreshing = false
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) {
                Column(modifier = Modifier.padding(innerPadding)) {
                    // Top TinXel Header with Privacy Toggle and Settings
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { privacyMode.value = !privacyMode.value }) {
                            Icon(
                                if (privacyMode.value) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Privacy"
                            )
                        }
                        TinXelLogo(modifier = Modifier.align(Alignment.CenterVertically))
                        IconButton(onClick = {
                            navController.navigate(Screen.Settings.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }) {
                            Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                        }
                    }

                    TransactionFilterBar(
                        selectedProviderName = selectedProviderName,
                        onProviderSelected = { selectedProviderName = it },
                        availableYears = availableYears,
                        selectedYear = selectedYear,
                        onYearSelected = { selectedYear = it }
                    )

                    // Top Insights Bar
                    AnimatedVisibility(
                        visible = visibleTransactions.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            InsightCard(
                                title = "${selectedProvider?.shortName ?: "Mobile Money"} Balance",
                                value = formatMoney(insights.mpesaBalance, selectedCurrency, decimals = 0),
                                subtitle = "Available Now",
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.weight(1f),
                                isSensitive = true
                            )
                            // Only show Fuliza for Safaricom
                            if (selectedProvider == null || selectedProvider == NetworkProvider.SAFARICOM) {
                                InsightCard(
                                    title = "Fuliza Allowance",
                                    value = formatMoney(insights.fulizaAllowance, selectedCurrency, decimals = 0),
                                    subtitle = "Spending Limit",
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.weight(1f),
                                    isSensitive = true
                                )
                            }
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = Screen.History.route,
                        modifier = Modifier.weight(1f)
                    ) {
                        composable(Screen.History.route) { HistoryScreen(visibleTransactions, selectedCurrency) }
                        composable(Screen.Outgoing.route) { AnalyticsScreen("Expenses", visibleTransactions, true, selectedCurrency) }
                        composable(Screen.Incoming.route) { AnalyticsScreen("Income", visibleTransactions, false, selectedCurrency) }
                        composable(Screen.AI.route) { AIScreen(visibleTransactions) }
                        composable(Screen.Settings.route) { 
                            SettingsScreen(
                                currentTheme = themeMode,
                                onThemeChange = { mode ->
                                    scope.launch { settingsRepository.setThemeMode(mode) }
                                },
                                currentCurrency = currencyCode,
                                onCurrencyChange = { code ->
                                    scope.launch { settingsRepository.setCurrency(code) }
                                }
                            )
                        }
                    }
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Provider Dropdown
            Box(modifier = Modifier.weight(1f)) {
                OutlinedButton(
                    onClick = { expandedProvider = !expandedProvider },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(selectedProviderName ?: "All Providers")
                }
                DropdownMenu(
                    expanded = expandedProvider,
                    onDismissRequest = { expandedProvider = false },
                    modifier = Modifier.fillMaxWidth(0.5f)
                ) {
                    DropdownMenuItem(
                        text = { Text("All") },
                        onClick = {
                            onProviderSelected(null)
                            expandedProvider = false
                        },
                        leadingIcon = if (selectedProviderName == null) { { Icon(Icons.Default.Visibility, null) } } else null
                    )
                    NetworkProvider.values().forEach { provider ->
                        DropdownMenuItem(
                            text = { Text(provider.shortName) },
                            onClick = {
                                onProviderSelected(provider.name)
                                expandedProvider = false
                            },
                            leadingIcon = if (selectedProviderName == provider.name) { { Icon(Icons.Default.Visibility, null) } } else null
                        )
                    }
                }
            }

            // Year Dropdown
            if (availableYears.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { expandedYear = !expandedYear },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedYear.toString())
                    }
                    DropdownMenu(
                        expanded = expandedYear,
                        onDismissRequest = { expandedYear = false },
                        modifier = Modifier.fillMaxWidth(0.5f)
                    ) {
                        availableYears.forEach { year ->
                            DropdownMenuItem(
                                text = { Text(year.toString()) },
                                onClick = {
                                    onYearSelected(year)
                                    expandedYear = false
                                },
                                leadingIcon = if (selectedYear == year) { { Icon(Icons.Default.Visibility, null) } } else null
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun transactionYear(timestamp: Long): Int =
    Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.YEAR)

@Composable
fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    color: Color,
    modifier: Modifier = Modifier,
    isSensitive: Boolean = false
) {
    val privacyMode = LocalPrivacyMode.current.value
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = color),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.labelMedium)
            Text(
                text = value, 
                style = MaterialTheme.typography.titleLarge, 
                fontWeight = FontWeight.Bold,
                modifier = if (isSensitive && privacyMode) Modifier.blur(12.dp) else Modifier
            )
            Text(
                text = subtitle, 
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun PermissionScreen(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    isPermissionGranted: Boolean
) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "📱 Pesa Lens needs SMS access", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "This app reads your M-Pesa messages to track spending.\n\nYour data stays on your phone.", style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onRequestPermission) { Text("Allow SMS Access") }
    }
}

@Composable
fun LockScreen(onRetry: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TinXelLogo()
            Spacer(modifier = Modifier.height(32.dp))
            Button(onClick = onRetry) {
                Text("Unlock App")
            }
        }
    }
}
@Composable
fun TransactionCard(transaction: PesaTransaction, onClick: () -> Unit = {}) {
    val privacyMode = LocalPrivacyMode.current.value
    
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transaction.type + if (transaction.fee > 0) " • Fee: Ksh ${transaction.fee}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (transaction.type == "Received") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = if (privacyMode && transaction.fee > 0) Modifier.blur(8.dp) else Modifier
                )
            }
            Text(
                text = "${if (transaction.type == "Received") "+" else "-"} Ksh ${String.format(Locale.US, "%,.2f", transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "Received") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = if (privacyMode) Modifier.blur(12.dp) else Modifier
            )
        }
    }
}
