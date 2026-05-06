package com.example.pesalens.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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

@Composable
fun InteractiveLineChart(
    data: List<Pair<String, Double>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) return

    var selectedIndex by remember { mutableStateOf(-1) }
    val maxVal = (data.maxByOrNull { it.second }?.second ?: 1.0).coerceAtLeast(1.0)
    
    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(200.dp)) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                selectedIndex = (offset.x / (size.width / (data.size - 1))).toInt().coerceIn(0, data.size - 1)
                            },
                            onDrag = { change, _ ->
                                selectedIndex = (change.position.x / (size.width / (data.size - 1))).toInt().coerceIn(0, data.size - 1)
                            },
                            onDragEnd = { selectedIndex = -1 },
                            onDragCancel = { selectedIndex = -1 }
                        )
                    }
            ) {
                val path = Path()
                val stepX = size.width / (data.size - 1)
                
                data.forEachIndexed { index, pair ->
                    val x = index * stepX
                    val y = size.height - (pair.second / maxVal * size.height).toFloat()
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                // Draw Area Gradient
                val fillPath = Path().apply {
                    addPath(path)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.3f), Color.Transparent)
                    )
                )

                // Draw Line
                drawPath(
                    path = path,
                    color = color,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )

                // Draw Scrubber
                if (selectedIndex != -1) {
                    val x = selectedIndex * stepX
                    drawLine(
                        color = color.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                    drawCircle(
                        color = color,
                        radius = 6.dp.toPx(),
                        center = Offset(x, size.height - (data[selectedIndex].second / maxVal * size.height).toFloat())
                    )
                }
            }
        }

        // Info Display
        if (selectedIndex != -1) {
            val item = data[selectedIndex]
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(item.first, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "Ksh ${String.format(Locale.US, "%,.2f", item.second)}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
        }
    }
}
