package com.example.pesalens.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pesalens.PesaTransaction
import com.example.pesalens.data.CurrencyOption
import com.example.pesalens.data.ExportManager
import com.example.pesalens.data.formatCompactMoney
import com.example.pesalens.data.formatMoney
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    title: String,
    transactions: List<PesaTransaction>,
    isOutgoing: Boolean,
    currency: CurrencyOption
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }
    var useLineChart by remember { mutableStateOf(false) }
    var selectedBarIndex by remember { mutableStateOf<Int?>(null) }

    val accent = if (isOutgoing) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary

    val filtered = remember(searchQuery, transactions) {
        transactions.filter {
            it.name.contains(searchQuery, ignoreCase = true) &&
                    (if (isOutgoing) it.type != "Received" && it.type != "Balance" else it.type == "Received")
        }
    }

    // Group by "MMM yyyy" so the year always appears on the label
    val chartData: List<Pair<String, Double>> = remember(filtered) {
        filtered
            .groupBy { SimpleDateFormat("MMM yy", Locale.getDefault()).format(Date(it.date)) }
            .entries
            .sortedWith(Comparator { a, b ->
                val fmt = SimpleDateFormat("MMM yy", Locale.getDefault())
                (fmt.parse(a.key)?.time ?: 0L).compareTo(fmt.parse(b.key)?.time ?: 0L)
            })
            .map { (label, list) -> label to list.sumOf { it.amount } }
    }

    val groupedByName = remember(filtered) {
        filtered.groupBy { it.name }
            .map { (name, txns) -> Triple(name, txns.sumOf { it.amount }, txns.size) }
            .sortedByDescending { it.second }
    }

    val totalAmount = remember(filtered) { filtered.sumOf { it.amount } }
    val totalFees   = remember(filtered) { filtered.sumOf { it.fee } }
    val txCount     = filtered.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    AnimatedContent(targetState = isSearchExpanded, label = "searchToggle") { searching ->
                        if (searching) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Filter by name…") },
                                modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedContainerColor = Color.Transparent
                                )
                            )
                        } else {
                            Text(title, fontWeight = FontWeight.Bold)
                        }
                    }
                },
                actions = {
                    IconButton(onClick = {
                        ExportManager.generateMonthlyReport(context, filtered, title, currency)
                    }) {
                        Icon(Icons.Rounded.FileDownload, contentDescription = "Export PDF")
                    }
                    IconButton(onClick = {
                        isSearchExpanded = !isSearchExpanded
                        if (!isSearchExpanded) searchQuery = ""
                    }) {
                        Icon(
                            if (isSearchExpanded) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = "Toggle search"
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Summary cards ────────────────────────────────────────────────
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SummaryChip("Total", formatMoney(totalAmount, currency, decimals = 0), accent, Modifier.weight(1f))
                    SummaryChip("Count", "$txCount txns", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    if (isOutgoing && totalFees > 0)
                        SummaryChip("Fees", formatMoney(totalFees, currency, decimals = 0), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                }
            }

            // ── Chart section ────────────────────────────────────────────────
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Trend by Month",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold)
                                selectedBarIndex?.let { i ->
                                    if (i < chartData.size) {
                                        val (label, value) = chartData[i]
                                        Text(
                                            "$label - ${formatMoney(value, currency, decimals = 0)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = accent
                                        )
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                FilterChip(
                                    selected = !useLineChart,
                                    onClick = { useLineChart = false },
                                    label = { Text("Bar", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.BarChart, null, modifier = Modifier.size(14.dp))
                                    }
                                )
                                FilterChip(
                                    selected = useLineChart,
                                    onClick = { useLineChart = true },
                                    label = { Text("Line", style = MaterialTheme.typography.labelSmall) },
                                    leadingIcon = {
                                        Icon(Icons.Rounded.ShowChart, null, modifier = Modifier.size(14.dp))
                                    }
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (chartData.isEmpty()) {
                            Box(
                                Modifier.fillMaxWidth().height(200.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.BarChart, null,
                                        Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
                                    Text("No data yet", color = MaterialTheme.colorScheme.outline,
                                        style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        } else {
                            AnimatedContent(targetState = useLineChart, label = "chartMode",
                                transitionSpec = { fadeIn() togetherWith fadeOut() }) { line ->
                                if (line) {
                                    TinXelLineChart(
                                        data = chartData,
                                        color = accent,
                                        currency = currency,
                                        selectedIndex = selectedBarIndex,
                                        onIndexSelected = { selectedBarIndex = it },
                                        modifier = Modifier.fillMaxWidth().height(220.dp)
                                    )
                                } else {
                                    TinXelBarChart(
                                        data = chartData,
                                        color = accent,
                                        currency = currency,
                                        selectedIndex = selectedBarIndex,
                                        onIndexSelected = { selectedBarIndex = it },
                                        modifier = Modifier.fillMaxWidth().height(220.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Top contacts header ──────────────────────────────────────────
            item {
                Text(
                    if (isOutgoing) "Top Recipients" else "Top Senders",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            // ── Contact rows ─────────────────────────────────────────────────
            items(groupedByName) { (name, total, count) ->
                val fraction = if (totalAmount > 0) (total / totalAmount).toFloat() else 0f
                ContactBarRow(name = name, total = total, count = count,
                    fraction = fraction, accent = accent, currency = currency)
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ── Summary chip ─────────────────────────────────────────────────────────────

@Composable
private fun SummaryChip(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        color = color.copy(alpha = 0.1f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color)
            Text(value, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

private fun shouldDrawAxisLabel(index: Int, lastIndex: Int, selectedIndex: Int?, labelStep: Int): Boolean =
    index == 0 || index == lastIndex || index == selectedIndex || index % labelStep == 0

private fun DrawScope.drawValueScale(maxVal: Double, currency: CurrencyOption, labelColor: Color, chartHeight: Float) {
    val paint = android.graphics.Paint().apply {
        textSize = 10.dp.toPx()
        color = android.graphics.Color.argb(
            (labelColor.alpha * 255).toInt(),
            (labelColor.red * 255).toInt(),
            (labelColor.green * 255).toInt(),
            (labelColor.blue * 255).toInt()
        )
        textAlign = android.graphics.Paint.Align.LEFT
        isAntiAlias = true
    }
    drawContext.canvas.nativeCanvas.drawText(formatCompactMoney(maxVal, currency), 2.dp.toPx(), 10.dp.toPx(), paint)
    drawContext.canvas.nativeCanvas.drawText(formatCompactMoney(0.0, currency), 2.dp.toPx(), chartHeight - 4.dp.toPx(), paint)
}

// ── Contact bar row ───────────────────────────────────────────────────────────

@Composable
private fun ContactBarRow(
    name: String,
    total: Double,
    count: Int,
    fraction: Float,
    accent: Color,
    currency: CurrencyOption
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "bar_$name"
    )
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium, maxLines = 1,
                modifier = Modifier.weight(1f).padding(end = 8.dp))
            Text("${formatMoney(total, currency, decimals = 0)} - ${count}x",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Box(
                Modifier.fillMaxWidth(animatedFraction).height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent)
            )
        }
    }
}

// ── Bar chart ────────────────────────────────────────────────────────────────

@Composable
fun TinXelBarChart(
    data: List<Pair<String, Double>>,
    color: Color,
    currency: CurrencyOption,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    val maxVal = data.maxOfOrNull { it.second } ?: 1.0

    // Animate each bar height
    val animatedHeights = data.mapIndexed { i, (_, v) ->
        animateFloatAsState(
            targetValue = (v / maxVal).toFloat(),
            animationSpec = tween(600, delayMillis = i * 60, easing = FastOutSlowInEasing),
            label = "bar$i"
        ).value
    }

    val density = LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val labelStep = ((data.size + 5) / 6).coerceAtLeast(1)

    Canvas(
        modifier = modifier
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val barWidth = size.width / data.size.toFloat()
                    val index = (tapOffset.x / barWidth).toInt().coerceIn(0, data.size - 1)
                    onIndexSelected(if (selectedIndex == index) null else index)
                }
            }
            .pointerInput(data) {
                detectHorizontalDragGestures { change, _ ->
                    val barWidth = size.width / data.size.toFloat()
                    val index = (change.position.x / barWidth).toInt().coerceIn(0, data.size - 1)
                    onIndexSelected(index)
                }
            }
    ) {
        val chartHeight = size.height - 32.dp.toPx()
        val barWidth = size.width / data.size.toFloat()
        val barPad = barWidth * 0.2f

        data.forEachIndexed { i, (label, _) ->
            val barH = animatedHeights[i] * chartHeight
            val left = i * barWidth + barPad
            val top  = chartHeight - barH
            val isSelected = selectedIndex == i

            // bar
            drawRoundRect(
                color = if (isSelected) color else color.copy(alpha = 0.65f),
                topLeft = Offset(left, top),
                size = Size(barWidth - barPad * 2, barH),
                cornerRadius = CornerRadius(6.dp.toPx())
            )

            // selection highlight
            if (isSelected) {
                drawRoundRect(
                    color = color.copy(alpha = 0.15f),
                    topLeft = Offset(i * barWidth, 0f),
                    size = Size(barWidth, chartHeight),
                    cornerRadius = CornerRadius(8.dp.toPx())
                )
            }

            if (shouldDrawAxisLabel(i, data.lastIndex, selectedIndex, labelStep)) {
                val paint = android.graphics.Paint().apply {
                    textSize = with(density) { 9.sp.toPx() }
                    this.color = android.graphics.Color.argb(
                        (labelColor.alpha * 255).toInt(),
                        (labelColor.red * 255).toInt(),
                        (labelColor.green * 255).toInt(),
                        (labelColor.blue * 255).toInt()
                    )
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    i * barWidth + barWidth / 2,
                    size.height - 2.dp.toPx(),
                    paint
                )
            }
        }

        // baseline
        drawLine(
            color = labelColor.copy(alpha = 0.3f),
            start = Offset(0f, chartHeight),
            end   = Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )
        drawValueScale(maxVal, currency, labelColor, chartHeight)
    }
}

// ── Line chart ────────────────────────────────────────────────────────────────

@Composable
fun TinXelLineChart(
    data: List<Pair<String, Double>>,
    color: Color,
    currency: CurrencyOption,
    selectedIndex: Int?,
    onIndexSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        TinXelBarChart(data, color, currency, selectedIndex, onIndexSelected, modifier)
        return
    }

    val maxVal = data.maxOfOrNull { it.second } ?: 1.0
    val density = LocalDensity.current
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val labelStep = ((data.size + 5) / 6).coerceAtLeast(1)

    // Animate points
    val animProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "lineAnim"
    )

    Canvas(
        modifier = modifier
            .pointerInput(data) {
                detectTapGestures { tapOffset ->
                    val step = size.width / (data.size - 1).toFloat()
                    val index = ((tapOffset.x / step) + 0.5f).toInt().coerceIn(0, data.size - 1)
                    onIndexSelected(if (selectedIndex == index) null else index)
                }
            }
            .pointerInput(data) {
                detectHorizontalDragGestures { change, _ ->
                    val step = size.width / (data.size - 1).toFloat()
                    val index = ((change.position.x / step) + 0.5f).toInt().coerceIn(0, data.size - 1)
                    onIndexSelected(index)
                }
            }
    ) {
        val chartHeight = size.height - 32.dp.toPx()
        val step = size.width / (data.size - 1).toFloat()

        fun xOf(i: Int) = i * step
        fun yOf(v: Double) = chartHeight - (v / maxVal * chartHeight).toFloat()

        val points = data.mapIndexed { i, (_, v) -> Offset(xOf(i), yOf(v)) }

        // Fill gradient under curve
        val fillPath = Path().apply {
            moveTo(points.first().x, chartHeight)
            points.forEach { lineTo(it.x, it.y) }
            lineTo(points.last().x, chartHeight)
            close()
        }
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
                startY = 0f, endY = chartHeight
            )
        )

        // Animated line
        val linePath = Path()
        points.take((points.size * animProgress).toInt().coerceAtLeast(1)).forEachIndexed { i, pt ->
            if (i == 0) linePath.moveTo(pt.x, pt.y) else linePath.lineTo(pt.x, pt.y)
        }
        drawPath(linePath, color = color, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))

        // Dots + labels
        data.forEachIndexed { i, (label, _) ->
            val pt = points[i]
            val isSelected = selectedIndex == i

            // vertical guideline on select
            if (isSelected) {
                drawLine(color.copy(alpha = 0.25f), Offset(pt.x, 0f), Offset(pt.x, chartHeight), strokeWidth = 1.dp.toPx())
            }

            val showPoint = isSelected || data.size <= 14 || i % labelStep == 0 || i == data.lastIndex
            if (showPoint) {
                drawCircle(
                    color = if (isSelected) color else color.copy(alpha = 0.7f),
                    radius = if (isSelected) 6.dp.toPx() else 3.5.dp.toPx(),
                    center = pt
                )
            }
            if (isSelected) {
                drawCircle(Color.White, radius = 3.dp.toPx(), center = pt)
            }

            if (shouldDrawAxisLabel(i, data.lastIndex, selectedIndex, labelStep)) {
                val paint = android.graphics.Paint().apply {
                    textSize = with(density) { 9.sp.toPx() }
                    this.color = android.graphics.Color.argb(
                        (labelColor.alpha * 255).toInt(),
                        (labelColor.red * 255).toInt(),
                        (labelColor.green * 255).toInt(),
                        (labelColor.blue * 255).toInt()
                    )
                    textAlign = android.graphics.Paint.Align.CENTER
                    isAntiAlias = true
                }
                drawContext.canvas.nativeCanvas.drawText(
                    label, pt.x, size.height - 2.dp.toPx(), paint
                )
            }
        }

        // baseline
        drawLine(
            labelColor.copy(alpha = 0.3f),
            Offset(0f, chartHeight), Offset(size.width, chartHeight),
            strokeWidth = 1.dp.toPx()
        )
        drawValueScale(maxVal, currency, labelColor, chartHeight)
    }
}
