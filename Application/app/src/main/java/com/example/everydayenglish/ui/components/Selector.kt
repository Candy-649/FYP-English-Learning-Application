package com.example.everydayenglish.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.pow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    initialIndex: Int = 0,          // 新增
    itemHeight: Dp = 40.dp,
    visibleCount: Int = 5,
    selectedValue: (T) -> Unit
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex  // 用它初始化
    )
    val coroutineScope = rememberCoroutineScope()
    var currentSelected by remember { mutableStateOf(items.getOrElse(initialIndex) { items.first() }) }

    val middleIndex = visibleCount / 2
    LazyColumn(
        state = listState,
        modifier = modifier.height(itemHeight * visibleCount),
        contentPadding = PaddingValues(vertical = itemHeight * middleIndex),
    ) {
        itemsIndexed(items) { index, item ->
            val layoutInfo = listState.layoutInfo
            val info = layoutInfo.visibleItemsInfo.firstOrNull { it.index == index }
            val maxDistance = layoutInfo.viewportSize.height.toFloat() / 2f
            val alpha: Float = info?.let {
                0.3f + (1f - (info.offset.toFloat() / maxDistance).absoluteValue).pow(2.5f) * 0.7f
            } ?: 0.3f
            Box(
                modifier = Modifier
                    .height(itemHeight)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = item.toString(),
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
                )
            }
        }
    }
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centerIndex = listState.firstVisibleItemIndex
            currentSelected = items.getOrNull(centerIndex) ?: currentSelected
            selectedValue(currentSelected)
            coroutineScope.launch {
                listState.animateScrollToItem(centerIndex)
            }
        }
    }
}

@Composable
fun <T> RulerPicker(
    items: List<T>,
    modifier: Modifier = Modifier,
    initialTick: Int = 0,
    itemWidth: Dp = 10.dp,
    visibleCount: Int = 30,
    selectedValue: (T) -> Unit
) {
    val scrollState = rememberScrollState()
    val itemWidthPx = with(LocalDensity.current) { itemWidth.toPx() }
    var currentTick by remember { mutableIntStateOf(0) }
    val middleIndex = visibleCount / 2
    val contentPadding = middleIndex * itemWidth
    val highLight = MaterialTheme.colorScheme.primary
    val otherColor = MaterialTheme.colorScheme.onSurface
    val maxHeight = 15.dp
    val minHeight = 10.dp
    val minWidth = 3f
    val maxWidth = 6f

    Card(
        modifier = modifier
            .size(width = itemWidth * visibleCount, height = 40.dp)
            .clip(RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Text(
            text = items[currentTick].toString(),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .horizontalScroll(scrollState)
                .width(items.size * itemWidth + 2 * contentPadding)
        ) {
            val spacing = itemWidth.toPx()
            val startPaddingPx = contentPadding.toPx()

            items.forEachIndexed { index, _ ->
                val x = startPaddingPx + index * spacing
                val distance = abs(index - currentTick)

                val height = if (distance >= 2) {
                    minHeight
                } else {
                    val diff = maxHeight - minHeight
                    minHeight + diff * (2 - distance) / 2
                }

                val strokeWidth = if (distance >= 2) {
                    minWidth
                } else {
                    val diff = maxWidth - minWidth
                    minWidth + diff * (2 - distance) / 2
                }

                drawLine(
                    color = if (index in (currentTick - 2)..(currentTick + 2))
                        highLight else otherColor,
                    start = Offset(x, size.height),
                    end = Offset(x, size.height - height.toPx()),
                    strokeWidth = strokeWidth
                )
            }
        }

        LaunchedEffect(Unit) {
            snapshotFlow { itemWidth }
                .first { it > 0.dp }

            scrollState.scrollTo((initialTick * itemWidthPx).toInt())
        }

        LaunchedEffect(scrollState) {
            snapshotFlow { scrollState.value }
                .collect { offset ->
                    val rawTick = (offset / itemWidthPx).roundToInt()
                    currentTick = rawTick.coerceIn(0, items.lastIndex)
                    selectedValue(items[currentTick])
                }
        }
    }
}





@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
fun PickerSheetLightPreview() {
    RulerPicker(
        items = (0..100).map { it / 10f },
        selectedValue = {}
    )
}


@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun PickerSheetNightPreview() {

    RulerPicker(
        items = (0..100).map { it / 10f },
        selectedValue = {}
    )
}