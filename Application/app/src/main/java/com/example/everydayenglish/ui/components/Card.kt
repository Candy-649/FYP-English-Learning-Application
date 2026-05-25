package com.example.everydayenglish.ui.components

import android.content.res.Configuration
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.KeyboardDoubleArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.R


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> SwipeActionWrapper(
    item: T,
    onDelete: (T) -> Unit,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    content: @Composable (T) -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled
    )

// 监听状态变化，手动处理
    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.EndToStart) {
            onDelete(item)
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        modifier = modifier,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val isDismissing = dismissState.targetValue == SwipeToDismissBoxValue.EndToStart
            val backgroundColor = if (isDismissing) Color.Red else Color.LightGray.copy(alpha = 0.3f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor, shape = RoundedCornerShape(cornerRadius))
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = if (isDismissing) Color.White else Color.Gray,
                    modifier = Modifier.scale(if (isDismissing) 1.2f else 1f)
                )
            }
        }
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(cornerRadius),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            content(item)
        }
    }
}

@Composable
fun SettingsSection(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,

) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = modifier
        ) {
            content()
        }
    }
}

@Composable
fun SettingsItem(text: String, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(16.dp)
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

@Composable
fun SettingsInfoItem(modifier: Modifier = Modifier, text: String,value: String, onClick: () -> Unit) {
    Column(modifier = Modifier
        .fillMaxWidth()
        .clickable { onClick() }
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = text
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}

@Composable
fun<T> SettingsSingleChoiceSection(
    modifier: Modifier = Modifier,
    options: List<T>,
    selectedIndex: Int,
    onOptionSelected: (T) -> Unit
) {
    SettingsSection {
        options.forEachIndexed { index, option ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clickable { onOptionSelected(option) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option.toString(),
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                if (index == selectedIndex) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected"
                    )
                }
            }

            if (index != options.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
fun SettingsMultipleChoiceSection(
    modifier: Modifier = Modifier,
    options: List<String>,
    selectedIndices: List<Int>,
    onOptionToggled: (Int) -> Unit
) {
    SettingsSection {
        options.forEachIndexed { index, option ->
            Row(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { onOptionToggled(index) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )

                if (selectedIndices.contains(index)) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected"
                    )
                }
            }

            if (index != options.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            }
        }
    }
}



@Composable
fun SettingsSwitchItem(modifier: Modifier = Modifier, text: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, style = MaterialTheme.typography.bodyLarge)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
    }
}
@Composable
fun FoldCard(
    title: String,
    modifier: Modifier = Modifier,
    expandedDefault: Boolean = false,
    content: @Composable () -> Unit
){
    var expanded by remember { mutableStateOf(expandedDefault) }
    Card(
        modifier = modifier.padding(4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = dimensionResource(R.dimen.padding_small))
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                )
                .padding(dimensionResource(R.dimen.padding_small))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(R.dimen.padding_small))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = dimensionResource(R.dimen.padding_small))
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = { expanded = !expanded},
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.expanded_icon_description),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            if (expanded){
                content()
            }
        }

    }
}

@Composable
fun ClickableInfoCard(
    title: String,
    value: String,
    onClick: () -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = title
            )
        }
    }
}

@Composable
fun JumpCard(
    icon: ImageVector,
    text: String,
    content: () -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { content() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ){
            Icon(
                imageVector = icon,
                contentDescription = text,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge
            )
            Icon(
                imageVector = Icons.Default.KeyboardDoubleArrowRight,
                contentDescription = text
            )
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String
){
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ){
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(R.dimen.padding_medium)),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    showBackground = true
)
@Composable
fun CardLightPreview() {
    Column {
        Text("Swipe Action Wrapper")
        SwipeActionWrapper(
            item = "Item",
            onDelete = {}
        ) { }
        Text("Setting Section")
        SettingsSection {
            SettingsItem(
                text = "Setting Item"
            ) {}
            SettingsSingleChoiceSection(
                options = listOf("Option 1", "Option 2"),
                selectedIndex = 0
            ) { }
            SettingsMultipleChoiceSection(
                options = listOf("Option 1", "Option 2"),
                selectedIndices = listOf(0)
            ) { }
            SettingsSwitchItem(
                text = "Switch Item",
                checked = true,
                onCheckedChange = {}
            )
        }
        Text("Fold Card")
        FoldCard(
            title = "Fold Card",
            expandedDefault = true
        ) {}
        Text("Clickable Info Card")
        ClickableInfoCard(
            title = "Clickable Info Card",
            value = "Value"
        ) {}
        Text("Jump Card")
        JumpCard(
            icon = Icons.Default.Check,
            text = "Jump Card"
        ) {}
        Text("Info Card")
        InfoCard(
            title = "Title",
            value = "Value"
        )
    }
}


@Preview(
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    showBackground = true
)
@Composable
fun CardNightPreview() {
    Column {
        Text("Swipe Action Wrapper")
        SwipeActionWrapper(
            item = "Item",
            onDelete = {}
        ) { }
        Text("Setting Section")
        SettingsSection {
            SettingsItem(
                text = "Setting Item"
            ) {}
            SettingsSingleChoiceSection(
                options = listOf("Option 1", "Option 2"),
                selectedIndex = 0
            ) { }
            SettingsMultipleChoiceSection(
                options = listOf("Option 1", "Option 2"),
                selectedIndices = listOf(0)
            ) { }
            SettingsSwitchItem(
                text = "Switch Item",
                checked = true,
                onCheckedChange = {}
            )
        }
        Text("Fold Card")
        FoldCard(
            title = "Fold Card",
            expandedDefault = true
        ) {}
        Text("Clickable Info Card")
        ClickableInfoCard(
            title = "Clickable Info Card",
            value = "Value"
        ) {}
        Text("Jump Card")
        JumpCard(
            icon = Icons.Default.Check,
            text = "Jump Card"
        ) {}
        Text("Info Card")
        InfoCard(
            title = "Title",
            value = "Value"
        )
    }

}