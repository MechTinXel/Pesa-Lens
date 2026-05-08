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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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

        // Limit to last 6 months for faster loading
        val sixMonthsAgo = System.currentTimeMillis() - (6L * 30 * 24 * 60 * 60 * 1000)
        val uri: Uri = "content://sms/inbox".toUri()
        val cursor: Cursor? = contentResolver.query(uri, null, "date > ?", arrayOf(sixMonthsAgo.toString()), "date DESC")

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
    val showIncome by settingsRepository.showIncome.collectAsState(initial = true)
    val showExpenses by settingsRepository.showExpenses.collectAsState(initial = true)
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

    // Defer insights calculation for faster initial UI
    val insights by produceState<BudgetInsights?>(initialValue = null, visibleTransactions) {
        value = BudgetEngine.calculateInsights(visibleTransactions)
    }

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route
            
            // Filter out Settings from bottom bar
            val bottomNavItems = navItems.filter { it.route != Screen.Settings.route }

            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                bottomNavItems.forEach { screen ->
                    NavigationBarItem(
                        icon = {
                            Icon(
                                screen.icon,
                                contentDescription = screen.title,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        label = { Text(screen.title, style = MaterialTheme.typography.labelSmall) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    strokeCap = StrokeCap.Round,
                    modifier = Modifier.size(48.dp)
                )
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Minimalist Top Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TinXelLogo(modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "TinXel",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Compact Filter Bar
                    TransactionFilterBar(
                        selectedProviderName = selectedProviderName,
                        onProviderSelected = { selectedProviderName = it },
                        availableYears = availableYears,
                        selectedYear = selectedYear,
                        onYearSelected = { selectedYear = it }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Compact Balance & Fuliza Row
                    AnimatedVisibility(
                        visible = visibleTransactions.isNotEmpty() && insights != null,
                        enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                        exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        modifier = Modifier.padding(horizontal = 12.dp)
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
                            // Only show Fuliza for Safaricom
                            if (selectedProvider == null || selectedProvider == NetworkProvider.MPESA) {
                                InsightCard(
                                    title = "Fuliza Allowance",
                                    value = formatMoney(insights?.fulizaAllowance ?: 0.0, selectedCurrency, decimals = 0),
                                    subtitle = "Spending Limit",
                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                    isSensitive = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Show/hide Money In/Out analytics based on settings
                    if (showIncome || showExpenses) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (showIncome) {
                                Button(
                                    onClick = { navController.navigate(Screen.Incoming.route) },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Money In", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                            if (showExpenses) {
                                Button(
                                    onClick = { navController.navigate(Screen.Outgoing.route) },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text("Money Out", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    NavHost(
                        navController = navController,
                        startDestination = Screen.History.route,
                        modifier = Modifier.weight(1f),
                        enterTransition = {
                            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300)) + scaleOut(targetScale = 0.95f, animationSpec = tween(300))
                        }
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Provider Dropdown
        Box(modifier = Modifier.weight(1f)) {
            OutlinedButton(
                onClick = { expandedProvider = !expandedProvider },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    selectedProviderName?.let { NetworkProvider.valueOf(it).shortName } ?: "All",
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
                NetworkProvider.values().forEach { provider ->
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

        // Year Dropdown
        if (availableYears.isNotEmpty()) {
            Box(modifier = Modifier.weight(0.8f)) {
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
                    modifier = Modifier.fillMaxWidth(0.5f)
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
fun PermissionScreen(
    modifier: Modifier = Modifier,
    onRequestPermission: () -> Unit,
    isPermissionGranted: Boolean
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "📱",
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                Text(
                    text = "SMS Access Required",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "PesaLens reads your M-Pesa messages to track spending. Your data stays on your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }

        Button(
            onClick = onRequestPermission,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Allow SMS Access", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun LockScreen(onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Card(
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 32.dp),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                TinXelLogo(modifier = Modifier.size(80.dp))
            }
        }

        Text(
            text = "Unlock to Continue",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(48.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text("Authenticate", style = MaterialTheme.typography.labelLarge)
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
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
                if (transaction.fee > 0) {
                    Text(
                        text = transaction.type + " • Fee: Ksh ${transaction.fee}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (transaction.type == "Received") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = if (privacyMode) Modifier.blur(8.dp) else Modifier,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = transaction.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (transaction.type == "Received") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = "${if (transaction.type == "Received") "+" else "-"} Ksh ${String.format(Locale.US, "%,.0f", transaction.amount)}",
                style = MaterialTheme.typography.labelLarge,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (transaction.type == "Received") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                modifier = if (privacyMode) Modifier.blur(12.dp) else Modifier,
                maxLines = 1
            )
        }
    }
}
