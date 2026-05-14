@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.everydayenglish.ui

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.ProfileBubble
import com.example.everydayenglish.viewmodel.ProfileUiState
import com.example.everydayenglish.viewmodel.calculateBubbleSize
import com.example.everydayenglish.viewmodel.calculateFontSize
import com.example.everydayenglish.viewmodel.toBubbles
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    bubbles: List<ProfileBubble>
){
    val curvedShape = GenericShape { size, _ ->
        moveTo(0f, 120f)

        quadraticTo(
            size.width / 2,
            -80f,
            size.width,
            120f
        )

        lineTo(size.width, size.height)
        lineTo(0f, size.height)

        close()
    }
    Box(
        modifier = Modifier.fillMaxSize()
    ){
        AsyncImage(
            model = uiState.profileBackground,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.75f)
                .align(Alignment.BottomCenter)
        ){
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(),
                shape = curvedShape
            ) {
                Spacer(modifier = Modifier
                    .height(50.dp)
                    .fillMaxWidth()
                )
                Text(
                    uiState.userName,
                    style = MaterialTheme.typography.displaySmall,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                Text(
                    uiState.bio,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                BubbleCloud(
                    bubbles = bubbles
                )

            }
            AsyncImage(
                model = uiState.userAvatar,
                contentDescription = "Avatar",
                modifier = Modifier
                    .size(100.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = (-50).dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(
                        3.dp,
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        CircleShape
                    ),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
fun BubbleItem(
    bubble: ProfileBubble,
    selected: Boolean,
    dimmed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val alpha by animateFloatAsState(
        targetValue =
            if(dimmed)
                0.35f
            else
                1f,
        label = ""
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
            }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.95f),
                        Color(0xFFD6ECFF).copy(alpha = 0.8f),
                        Color(0xFFAEDBFF).copy(alpha = 0.65f)
                    )
                )
            )
            .clickable {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints {

            val safeWidth = maxWidth * 0.7f

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(safeWidth)
                    .align(Alignment.Center)
            ) {

                AnimatedVisibility(
                    visible = selected
                ) {
                    Text(
                        text = bubble.title,
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                }

                Text(
                    text = bubble.value.toString(),
                    fontSize = calculateFontSize(bubble.value),
                    fontWeight = FontWeight.Bold
                )

                AnimatedVisibility(
                    visible = selected
                ) {
                    Text(
                        text = bubble.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun BubbleCloud(bubbles: List<ProfileBubble>) {
    var selectedBubble by remember { mutableStateOf<ProfileBubble?>(null) }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
        .clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() }
        ){
            selectedBubble = null
        }
    ) {

        // --- 纯计算，remember 缓存 ---
        val sorted = remember(bubbles) { bubbles.sortedByDescending { it.value } }
        val sizes = remember(sorted) { sorted.map { calculateBubbleSize(it.value) } }

        val rawPositions = remember(sorted) {
            buildList {
                sorted.forEachIndexed { index, _ ->
                    if (index == 0) {
                        add(0.dp to 0.dp)
                    } else {
                        val angle = Math.toRadians((index * 75).toDouble())
                        val distance = 120.dp + (index * 20).dp
                        add(
                            (distance * cos(angle).toFloat()) to
                                    (distance * sin(angle).toFloat())
                        )
                    }
                }
            }
        }

        val anchorCenters = remember(sorted, maxWidth, maxHeight) {
            val minX = rawPositions.mapIndexed { i, (x, _) -> x - sizes[i] / 2 }.min()
            val maxX = rawPositions.mapIndexed { i, (x, _) -> x + sizes[i] / 2 }.max()
            val minY = rawPositions.mapIndexed { i, (_, y) -> y - sizes[i] / 2 }.min()
            val maxY = rawPositions.mapIndexed { i, (_, y) -> y + sizes[i] / 2 }.max()
            val shiftX = maxWidth / 2 - (minX + maxX) / 2
            val shiftY = maxHeight / 2 - (minY + maxY) / 2

            sorted.mapIndexed { index, _ ->
                (shiftX + rawPositions[index].first) to
                        (shiftY + rawPositions[index].second)
            }
        }

        val expandedRadius = maxWidth * 3f / 8f  // 3/4直径的半径

        val expandedCenters = remember(anchorCenters, expandedRadius) {
            anchorCenters.map { (cx, cy) ->
                cx.coerceIn(expandedRadius, maxWidth - expandedRadius) to
                        cy.coerceIn(expandedRadius, maxHeight - expandedRadius)
            }
        }

        // --- 渲染，动画在 key 块内 ---
        sorted.forEachIndexed { index, bubble ->
            key(bubble.id) {
                val selected = selectedBubble == bubble
                val originalRadius = sizes[index] / 2

                val animRadius by animateDpAsState(
                    targetValue = if (selected) expandedRadius else originalRadius,
                    label = "r"
                )
                val animCx by animateDpAsState(
                    targetValue = if (selected) expandedCenters[index].first
                    else anchorCenters[index].first,
                    label = "cx"
                )
                val animCy by animateDpAsState(
                    targetValue = if (selected) expandedCenters[index].second
                    else anchorCenters[index].second,
                    label = "cy"
                )

                BubbleItem(
                    bubble = bubble,
                    selected = selected,
                    dimmed = selectedBubble != null && !selected,
                    onClick = { selectedBubble = if (selected) null else bubble },
                    modifier = Modifier
                        .offset(x = animCx - animRadius, y = animCy - animRadius)
                        .size(animRadius * 2)
                )
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun ProfileScreenPreview(){
    EverydayEnglishTheme {
        val profileUiState = ProfileUiState(
            userName = "Candy",
            userAvatar = Uri.parse(
                "android.resource://com.example.everydayenglish/${R.drawable.default_avatar}"
            ),
            profileBackground = Uri.parse(
                "android.resource://com.example.everydayenglish/${R.drawable.default_profile_background}"
            ),
            totalStudyDays = 45,
            totalSentencesCompleted = 1280,
            currentStreak = 7,
            dailyGoal = 20,
            todayProgress = 12
        )
        ProfileScreen(profileUiState, profileUiState.toBubbles())
    }
}