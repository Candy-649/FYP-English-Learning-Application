@file:Suppress("COMPOSE_APPLIER_CALL_MISMATCH")

package com.example.everydayenglish.ui

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.everydayenglish.util.cropBitmap
import com.example.everydayenglish.util.decodeBitmap
import com.example.everydayenglish.util.resizeBitmap
import com.example.everydayenglish.util.saveBitmapAsJpeg

import kotlinx.coroutines.launch

enum class CropShape { CIRCLE, RECT }

// cropRect is the single source of truth: it's the exact region of the source bitmap
// (in bitmap pixel coordinates) currently shown inside the on-screen mask. Pinch-zoom
// resizes it, drag moves it, and Confirm crops exactly that rect - what's drawn on
// screen and what gets cropped can never disagree, because they're the same rect.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropScreen(
    sourceUri: Uri,
    shape: CropShape,
    // For RECT: pass null to derive the mask ratio from the current window's size
    // (so the framing matches what ContentScale.Crop will actually show on this
    // device), or pass a fixed ratio to override. Ignored for CIRCLE (always 1:1).
    aspectRatio: Float? = null,
    outputFileName: String,
    maxOutputDimension: Int = 1024,
    onConfirm: (Uri) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize
    val scope = rememberCoroutineScope()

    val effectiveAspectRatio = aspectRatio
        ?: (windowSize.width.toFloat() / windowSize.height.toFloat())

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    // The crop window, in bitmap pixel coordinates.
    var cropRect by remember { mutableStateOf<Rect?>(null) }
    // The initial (fully-zoomed-out) size of that window - bounds how far the user
    // can zoom in (1/5 of this) or out (back to this, the "cover" fit).
    var initialCropSize by remember { mutableStateOf<Size?>(null) }

    LaunchedEffect(sourceUri) {
        sourceBitmap = decodeBitmap(context, sourceUri)
        cropRect = null
        initialCropSize = null
    }

    fun confirmCrop() {
        val bitmap = sourceBitmap ?: return
        val rect = cropRect ?: return
        isSaving = true
        scope.launch {
            val cropped = cropBitmap(
                bitmap,
                rect.left.toInt(),
                rect.top.toInt(),
                rect.width.toInt(),
                rect.height.toInt()
            )
            val resized = resizeBitmap(cropped, maxOutputDimension)
            val saved = saveBitmapAsJpeg(context, resized, outputFileName)
            isSaving = false
            saved?.let { onConfirm(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Adjust") },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = !isSaving) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { confirmCrop() },
                        enabled = cropRect != null && !isSaving
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Confirm")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            val bitmap = sourceBitmap
            if (bitmap == null) {
                CircularProgressIndicator(color = Color.White)
            } else {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val maskWidthDp: Dp
                    val maskHeightDp: Dp
                    if (shape == CropShape.CIRCLE) {
                        val side = minOf(maxWidth, maxHeight) * 0.8f
                        maskWidthDp = side
                        maskHeightDp = side
                    } else {
                        var w = maxWidth * 0.9f
                        var h = w / effectiveAspectRatio
                        if (h > maxHeight * 0.7f) {
                            h = maxHeight * 0.7f
                            w = h * effectiveAspectRatio
                        }
                        maskWidthDp = w
                        maskHeightDp = h
                    }

                    val maskWidthPx = with(density) { maskWidthDp.toPx() }
                    val maskHeightPx = with(density) { maskHeightDp.toPx() }

                    // Initial crop window: the largest centered region of the bitmap that
                    // matches the mask's aspect ratio - same idea as ContentScale.Crop's
                    // initial fit, computed once per bitmap/mask-size combination.
                    LaunchedEffect(bitmap, maskWidthPx, maskHeightPx) {
                        if (maskWidthPx <= 0f || maskHeightPx <= 0f) return@LaunchedEffect
                        val maskAspect = maskWidthPx / maskHeightPx
                        val bitmapAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
                        val initW: Float
                        val initH: Float
                        if (bitmapAspect > maskAspect) {
                            initH = bitmap.height.toFloat()
                            initW = initH * maskAspect
                        } else {
                            initW = bitmap.width.toFloat()
                            initH = initW / maskAspect
                        }
                        val left = (bitmap.width - initW) / 2f
                        val top = (bitmap.height - initH) / 2f
                        cropRect = Rect(left, top, left + initW, top + initH)
                        initialCropSize = Size(initW, initH)
                    }

                    val rect = cropRect

                    Box(
                        modifier = Modifier
                            .size(maskWidthDp, maskHeightDp)
                            .clip(if (shape == CropShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp))
                            .border(
                                width = 2.dp,
                                color = Color.White.copy(alpha = 0.8f),
                                shape = if (shape == CropShape.CIRCLE) CircleShape else RoundedCornerShape(8.dp)
                            )
                            .pointerInput(bitmap, maskWidthPx, maskHeightPx) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val current = cropRect ?: return@detectTransformGestures
                                    val init = initialCropSize ?: return@detectTransformGestures

                                    // bitmap px per screen px at the current crop size - since
                                    // cropRect always gets drawn stretched to exactly fill the
                                    // mask, this ratio is exact, not approximated.
                                    val screenToBitmapScale = current.width / maskWidthPx

                                    val newWidth = (current.width / zoom)
                                        .coerceIn(init.width / 5f, init.width)
                                    val newHeight = (current.height / zoom)
                                        .coerceIn(init.height / 5f, init.height)

                                    val centerX = current.left + current.width / 2f
                                    val centerY = current.top + current.height / 2f

                                    // Dragging right should feel like dragging the photo right,
                                    // i.e. the crop window moves left relative to the bitmap.
                                    val newCenterX = centerX - pan.x * screenToBitmapScale
                                    val newCenterY = centerY - pan.y * screenToBitmapScale

                                    var newLeft = newCenterX - newWidth / 2f
                                    var newTop = newCenterY - newHeight / 2f

                                    newLeft = newLeft.coerceIn(0f, (bitmap.width - newWidth).coerceAtLeast(0f))
                                    newTop = newTop.coerceIn(0f, (bitmap.height - newHeight).coerceAtLeast(0f))

                                    cropRect = Rect(newLeft, newTop, newLeft + newWidth, newTop + newHeight)
                                }
                            }
                    ) {
                        if (rect != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                // Direct source-rect -> dest-rect blit. No ContentScale, no
                                // separate transform layer to keep in sync - what's drawn IS
                                // the region that gets cropped on confirm.
                                drawImage(
                                    image = bitmap.asImageBitmap(),
                                    srcOffset = IntOffset(rect.left.toInt(), rect.top.toInt()),
                                    srcSize = IntSize(
                                        rect.width.toInt().coerceAtLeast(1),
                                        rect.height.toInt().coerceAtLeast(1)
                                    ),
                                    dstSize = IntSize(size.width.toInt(), size.height.toInt())
                                )
                            }
                        }
                    }
                }

                if (isSaving) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            }
        }
    }
}