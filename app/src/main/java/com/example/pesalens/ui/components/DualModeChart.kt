package com.example.pesalens.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.MultilineChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.*

enum class ChartType { BAR, LINE }

@Composable
fun DualModeChart(
    data: List<Pair<String, Double>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    var chartType by remember { mutableStateOf(ChartType.BAR) }
    var selectedIndex by remember { mutableStateOf(-1) }
    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1.0).coerceAtLeast(1.0)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (selectedIndex != -1) data[selectedIndex].first else "Annual Insight",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.outline
            )
            
            SingleChoiceSegmentedButtonRow {
                SegmentedButton(
                    selected = chartType == ChartType.BAR,
                    onClick = { chartType = ChartType.BAR },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    icon = { Icon(Icons.Default.BarChart, contentDescription = null) }
                ) {
                    Text("Bar")
                }
                SegmentedButton(
                    selected = chartType == ChartType.LINE,
                    onClick = { chartType = ChartType.LINE },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    icon = { Icon(Icons.Default.MultilineChart, contentDescription = null) }
                ) {
                    Text("Line")
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.fillMaxWidth().height(250.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(data) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = (offset.x / (size.width / (data.size))).toInt().coerceIn(0, data.size - 1)
                            },
                            onDrag = { change, _ ->
                                selectedIndex = (change.position.x / (size.width / (data.size))).toInt().coerceIn(0, data.size - 1)
                            },
                            onDragEnd = { selectedIndex = -1 },
                            onDragCancel = { selectedIndex = -1 }
                        )
                    }
            ) {
                val stepX = size.width / (data.size)
                val chartHeight = size.height - 40.dp.toPx()

                if (chartType == ChartType.BAR) {
                    data.forEachIndexed { index, pair ->
                        val barWidth = stepX * 0.7f
                        val left = index * stepX + (stepX - barWidth) / 2
                        val barHeight = (pair.second / maxVal * chartHeight).toFloat()
                        
                        drawRoundRect(
                            color = if (index == selectedIndex) color else color.copy(alpha = 0.4f),
                            topLeft = Offset(left, chartHeight - barHeight),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                } else {
                    val path = Path()
                    data.forEachIndexed { index, pair ->
                        val x = index * stepX + stepX / 2
                        val y = chartHeight - (pair.second / maxVal * chartHeight).toFloat()
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }

                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                    )

                    // Gradient fill
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(data.size * stepX - stepX / 2, chartHeight)
                        lineTo(stepX / 2, chartHeight)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(color.copy(alpha = 0.3f), Color.Transparent),
                            endY = chartHeight
                        )
                    )
                }

                // Interaction Line
                if (selectedIndex != -1) {
                    val x = selectedIndex * stepX + stepX / 2
                    drawLine(
                        color = color.copy(alpha = 0.3f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
                    )
                }
            }
        }

        // Animated Info Card
        if (selectedIndex != -1) {
            val item = data[selectedIndex]
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(item.first, style = MaterialTheme.typography.labelSmall)
                        Text(
                            "Ksh ${String.format(Locale.US, "%,.2f", item.second)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
            }
        }
    }
}
