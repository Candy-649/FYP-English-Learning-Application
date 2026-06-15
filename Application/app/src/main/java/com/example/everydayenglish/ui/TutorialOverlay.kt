package com.example.everydayenglish.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

data class TutorialStep(
    val bounds: Rect?,
    val title: String,
    val description: String
)

@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableIntStateOf(0) }
    val step = steps.getOrNull(currentStep) ?: run { onDismiss(); return }
    val bounds = step.bounds

    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val screenWidthPx  = with(density) { configuration.screenWidthDp.dp.toPx() }
    val screenHeightPx = with(density) { configuration.screenHeightDp.dp.toPx() }

    val animSpec = tween<Float>(durationMillis = 450, easing = FastOutSlowInEasing)

    val pad = 24f
    val targetLeft   = (bounds?.left   ?: 0f)          - pad
    val targetTop    = (bounds?.top    ?: 0f)           - pad
    val targetRight  = (bounds?.right  ?: screenWidthPx)  + pad
    val targetBottom = (bounds?.bottom ?: screenHeightPx) + pad

    val left   by animateFloatAsState(targetLeft,   animSpec, label = "left")
    val top    by animateFloatAsState(targetTop,    animSpec, label = "top")
    val right  by animateFloatAsState(targetRight,  animSpec, label = "right")
    val bottom by animateFloatAsState(targetBottom, animSpec, label = "bottom")

    Box(Modifier.fillMaxSize()) {

        // 1. 画遮罩 + 挖洞
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        ) {
            drawRect(Color.Black.copy(alpha = 0.65f))
            drawRoundRect(
                color = Color.Transparent,
                topLeft = Offset(left, top),
                size = Size(right - left, bottom - top),
                cornerRadius = CornerRadius(28f),
                blendMode = BlendMode.Clear
            )
            drawRoundRect(
                color = Color.White.copy(alpha = 0.35f),
                topLeft = Offset(left - 3f, top - 3f),
                size = Size(right - left + 6f, bottom - top + 6f),
                cornerRadius = CornerRadius(30f),
                style = Stroke(width = 3f)
            )
        }

        // 2. 消耗点击（在 Card 下层）
        Spacer(
            Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { }
        )

        // 3. Tooltip 卡片（最上层，按钮点击正常）
        Card(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // 步骤点
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.forEachIndexed { index, _ ->
                        Box(
                            Modifier
                                .size(if (index == currentStep) 20.dp else 8.dp, 8.dp)
                                .background(
                                    color = if (index == currentStep)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(4.dp)
                                )
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = step.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Skip", color = MaterialTheme.colorScheme.outline)
                    }
                    Button(onClick = {
                        if (currentStep < steps.lastIndex) currentStep++
                        else onDismiss()
                    }) {
                        Text(if (currentStep == steps.lastIndex) "Done" else "Next")
                    }
                }
            }
        }
    }
}