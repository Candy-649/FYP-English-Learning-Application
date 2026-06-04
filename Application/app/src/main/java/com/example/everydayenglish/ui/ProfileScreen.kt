@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")
@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.everydayenglish.ui

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.everydayenglish.R
import com.example.everydayenglish.ui.theme.EverydayEnglishTheme
import com.example.everydayenglish.viewmodel.ProfileBubble
import com.example.everydayenglish.viewmodel.ProfileUiState
import com.example.everydayenglish.viewmodel.toBubbles
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private suspend fun copyUriToInternalStorage(
    context: Context,
    sourceUri: Uri,
    fileName: String
): Uri? = withContext(Dispatchers.IO) {
    try {
        val inputStream =
            context.contentResolver.openInputStream(sourceUri) ?: return@withContext null
        val file = File(context.filesDir, fileName)
        file.outputStream().use { out ->
            inputStream.use { it.copyTo(out) }
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}

@Composable
fun ProfileScreen(
    uiState: ProfileUiState,
    bubbles: List<ProfileBubble>,
    onBackClick: () -> Unit,
    onUserNameChange: (String) -> Unit = {},
    onBioChange: (String) -> Unit = {},
    onSaveProfile: () -> Unit = {},
    onAvatarChange: (Uri) -> Unit = {},
    onBackgroundChange: (Uri) -> Unit = {}
) {
    var isEditing by remember { mutableStateOf(false) }
    var editName  by remember { mutableStateOf("") }
    var editBio   by remember { mutableStateOf("") }

    val context = LocalContext.current
    val scope   = rememberCoroutineScope()
    //photo selector
    val avatarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val saved = copyUriToInternalStorage(context, it, "user_avatar.jpg")
                saved?.let { newUri -> onAvatarChange(newUri) }
            }
        }
    }

    val backgroundLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                val saved = copyUriToInternalStorage(context, it, "profile_background.jpg")
                saved?.let { newUri -> onBackgroundChange(newUri) }
            }
        }
    }

    val curvedShape = GenericShape { size, _ ->
        moveTo(0f, 120f)
        quadraticTo(size.width / 2, -80f, size.width, 120f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isEditing) "Edit Profile" else "Profile",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isEditing) isEditing = false else onBackClick()
                    }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Close
                            else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = if (isEditing) "Cancel" else "Back",
                            modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))
                        )
                    }
                },
                actions = {
                    if (isEditing) {
                        IconButton(onClick = {
                            onUserNameChange(editName.trim())
                            onBioChange(editBio.trim())
                            onSaveProfile()
                            isEditing = false
                        }) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Save",
                                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))
                            )
                        }
                    } else {
                        IconButton(onClick = {
                            editName = uiState.userName
                            editBio  = uiState.bio
                            isEditing = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                modifier = Modifier.padding(dimensionResource(R.dimen.padding_small))
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = uiState.profileBackground,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (isEditing) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(0.3f)
                            .background(Color.Black.copy(alpha = 0.35f))
                            .clickable { backgroundLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector        = Icons.Default.PhotoCamera,
                                contentDescription = "Change background",
                                tint               = Color.White,
                                modifier           = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text  = "Change background",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(),
                    shape = curvedShape
                ) {
                    Spacer(modifier = Modifier
                        .height(50.dp)
                        .fillMaxWidth())

                    // user name edit
                    if (isEditing) {
                        InlineEditField(
                            value     = editName,
                            onValueChange = { editName = it },
                            textStyle = MaterialTheme.typography.displaySmall,
                            hint      = "Username",
                            singleLine = true
                        )
                    } else {
                        Text(
                            text      = uiState.userName,
                            style     = MaterialTheme.typography.displaySmall,
                            modifier  = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Bio edit
                    if (isEditing) {
                        InlineEditField(
                            value     = editBio,
                            onValueChange = { editBio = it },
                            textStyle = MaterialTheme.typography.titleMedium,
                            hint      = "Add a bio...",
                            singleLine = false
                        )
                    } else {
                        Text(
                            text      = uiState.bio,
                            style     = MaterialTheme.typography.titleMedium,
                            modifier  = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    BubbleCloud(bubbles = bubbles)
                }

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = (-50).dp)
                ) {
                    AsyncImage(
                        model              = uiState.userAvatar,
                        contentDescription = "Avatar",
                        modifier           = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(
                                3.dp,
                                color = MaterialTheme.colorScheme.surfaceContainer,
                                shape = CircleShape
                            ),
                        contentScale = ContentScale.Crop
                    )
                    //avatar edit
                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.45f))
                                .clickable { avatarLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector        = Icons.Default.PhotoCamera,
                                contentDescription = "Change avatar",
                                tint               = Color.White,
                                modifier           = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InlineEditField(
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    hint: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface    = MaterialTheme.colorScheme.onSurface
    val dividerColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
    val density      = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val iconSize     = 14.dp

    // 文字为空时测 hint 宽度，否则测当前文字宽度，随输入实时刷新
    val measuredPx = remember(value, hint, textStyle) {
        val sample = value.ifEmpty { hint }
        textMeasurer.measure(sample, textStyle).size.width
    }
    val measuredDp = with(density) { measuredPx.toDp() }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxWidth()
    ) {
        val fieldWidth = (measuredDp + 8.dp)       // +8dp 留给末尾光标
            .coerceAtMost(maxWidth * 0.78f)         // 超长时截断，防溢出

        Row(verticalAlignment = Alignment.Bottom) {

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(fieldWidth)
            ) {
                BasicTextField(
                    value          = value,
                    onValueChange  = onValueChange,
                    singleLine     = singleLine,
                    maxLines       = if (singleLine) 1 else 3,
                    cursorBrush    = SolidColor(primaryColor),
                    textStyle      = textStyle.copy(
                        textAlign = TextAlign.Center,
                        color     = onSurface
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier         = Modifier.fillMaxWidth()
                        ) {
                            if (value.isEmpty()) {
                                Text(
                                    text  = hint,
                                    style = textStyle.copy(
                                        textAlign = TextAlign.Center,
                                        color     = onSurface.copy(alpha = 0.35f)
                                    )
                                )
                            }
                            innerTextField()
                        }
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                HorizontalDivider(thickness = 1.dp, color = dividerColor)
            }

            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector        = Icons.Default.Edit,
                contentDescription = null,
                modifier           = Modifier.size(iconSize),
                tint               = primaryColor.copy(alpha = 0.55f)
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
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
                    )
                }

                Text(
                    text = bubble.value.toString(),
                    fontSize = bubble.fontSize,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                AnimatedVisibility(
                    visible = selected
                ) {
                    Text(
                        text = bubble.description,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = Color.DarkGray
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
            ) {
                selectedBubble = null
            }
    ) {

        //计算
        val sorted = remember(bubbles) { bubbles.sortedByDescending { it.value } }
        val sizes = remember(sorted) { sorted.map { it.size } }

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

        //给数值
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
            userAvatar = "android.resource://com.example.everydayenglish/${R.drawable.default_avatar}".toUri(),
            profileBackground = "android.resource://com.example.everydayenglish/${R.drawable.default_profile_background}".toUri(),
            totalStudyDays = 45,
            totalSentencesCompleted = 1280,
            currentStreak = 7,
            dailyGoal = 20,
            todayProgress = 12
        )
        ProfileScreen(profileUiState, profileUiState.toBubbles(),    onBackClick = {})
    }
}