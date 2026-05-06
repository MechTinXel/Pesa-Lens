package com.example.pesalens.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun BarChart(
    data: List<Pair<String, Double>>,
    color: Color,
    modifier: Modifier = Modifier
) {
    val maxVal = data.maxByOrNull { it.second }?.second ?: 1.0
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animatedProgress.snapTo(0f)
        animatedProgress.animateTo(1f, animationSpec = tween(800))
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, value) ->
                val barHeightFraction = (value / maxVal).toFloat() * animatedProgress.value
                
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .fillMaxHeight(barHeightFraction.coerceIn(0.01f, 1f))
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRect(
                            color = color,
                            size = size
                        )
                    }
                }
            }
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            data.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(40.dp),
                    maxLines = 1
                )
            }
        }
    }
}
