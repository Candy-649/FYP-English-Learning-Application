package com.example.everydayenglish.ui.components

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun PushDrawer(
    drawerState: DrawerState,
    drawerContent: @Composable (ColumnScope.() -> Unit),
    content: @Composable (() -> Unit)
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val screenWidthPx = with(density) { (LocalConfiguration.current.screenWidthDp.dp * 0.8f).toPx() }

    val offsetX = remember { Animatable(-screenWidthPx) }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            offsetX.animateTo(0f, animationSpec = tween(durationMillis = 300))
        } else {
            offsetX.animateTo(-screenWidthPx, animationSpec = tween(durationMillis = 300))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {

        content()

        if (offsetX.value > -screenWidthPx + 1) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.32f))
                    .pointerInput(Unit) {
                        detectTapGestures {
                            scope.launch { drawerState.close() }
                        }
                    }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { screenWidthPx.toDp() })
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                )
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            drawerContent()
        }
    }
}

class DrawerState(initialOpen: Boolean = false) {
    var isOpen by mutableStateOf(initialOpen)
    fun open() { isOpen = true }
    fun close() { isOpen = false }
    fun toggle() { isOpen = !isOpen }
}






@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun PushDrawerNightPreview() {
    EverydayEnglishTheme {
        val drawerState = remember { DrawerState() }
        PushDrawer(
            drawerState = drawerState,
            drawerContent = {}
        ) {

        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
fun PushDrawerLightPreview() {
    EverydayEnglishTheme {
        val drawerState = remember { DrawerState() }
        PushDrawer(
            drawerState = drawerState,
            drawerContent = {}
        ) {

        }
    }
}