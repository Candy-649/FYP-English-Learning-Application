package com.example.everydayenglish.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale
import coil.imageLoader
import coil.request.ImageRequest

// Warms Coil's cache for a remote image ahead of when it'll actually be displayed - meant
// to be called during a step that already has a loading indicator (login, splash sync),
// so the AsyncImage on MainScreen/ProfileScreen finds it already cached instead of starting
// a fresh network fetch the moment the user lands there. Best-effort: a failed/slow prefetch
// just means that first real load falls back to fetching it itself, same as before this existed.
suspend fun prefetchImage(context: Context, uri: Uri) {
    if (uri == Uri.EMPTY) return
    if (uri.scheme != "http" && uri.scheme != "https") return // local/resource URIs load instantly, nothing to warm
    try {
        val request = ImageRequest.Builder(context)
            .data(uri)
            .build()
        context.imageLoader.execute(request)
    } catch (e: Exception) {
        Log.w("ImageUtils", "prefetchImage failed for $uri", e)
    }
}

// Decodes a Bitmap from a content Uri, downsampling on the way in so a full-resolution
// camera photo (often 10+ MB) doesn't get fully decoded into memory just to be cropped,
// then corrects for EXIF rotation - BitmapFactory returns the raw pixel grid, which for
// a lot of camera photos is actually landscape-shaped even though the photo displays
// upright everywhere else, with the rotation stored as EXIF metadata instead of baked
// into the pixels. Skipping this makes cover/crop math align to the wrong edge.
suspend fun decodeBitmap(context: Context, uri: Uri, maxDimension: Int = 2000): Bitmap? =
    withContext(Dispatchers.IO) {
        try {
            Log.d("ImageUtils", "decodeBitmap: opening $uri")

            // First pass: read bounds only, to compute the sample size.
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            val boundsStream = context.contentResolver.openInputStream(uri)
            if (boundsStream == null) {
                Log.w("ImageUtils", "decodeBitmap: openInputStream returned null for $uri")
                return@withContext null
            }
            boundsStream.use { BitmapFactory.decodeStream(it, null, boundsOptions) }
            Log.d(
                "ImageUtils",
                "decodeBitmap: bounds = ${boundsOptions.outWidth}x${boundsOptions.outHeight}, " +
                        "mimeType=${boundsOptions.outMimeType}"
            )

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                Log.w("ImageUtils", "decodeBitmap: invalid bounds, BitmapFactory couldn't read this stream")
                return@withContext null
            }

            var sampleSize = 1
            while (boundsOptions.outWidth / sampleSize > maxDimension ||
                boundsOptions.outHeight / sampleSize > maxDimension
            ) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, decodeOptions)
            }
            Log.d("ImageUtils", "decodeBitmap: decoded=${decoded != null}, sampleSize=$sampleSize")

            if (decoded == null) return@withContext null

            val rotationDegrees = exifRotationDegrees(context, uri)
            Log.d("ImageUtils", "decodeBitmap: exif rotation=$rotationDegrees")
            rotateBitmapIfNeeded(decoded, rotationDegrees)
        } catch (e: Exception) {
            Log.e("ImageUtils", "decodeBitmap failed for $uri", e)
            null
        }
    }

// Reads the EXIF orientation tag and maps it to the degrees needed to display the image
// upright. Uses the framework's android.media.ExifInterface(InputStream) constructor
// (API 24+), not the separate androidx.exifinterface library, so no new dependency.
private fun exifRotationDegrees(context: Context, uri: Uri): Int = try {
    context.contentResolver.openInputStream(uri)?.use { stream ->
        val exif = ExifInterface(stream)
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    } ?: 0
} catch (e: Exception) {
    Log.w("ImageUtils", "exifRotationDegrees: failed to read EXIF for $uri", e)
    0
}

private fun rotateBitmapIfNeeded(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

// Crops the given rect (in bitmap pixel coordinates) out of the source bitmap,
// clamping to the bitmap's bounds first so float rounding at the edges can't crash it.
fun cropBitmap(source: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
    val safeX = x.coerceIn(0, source.width - 1)
    val safeY = y.coerceIn(0, source.height - 1)
    val safeWidth = width.coerceIn(1, source.width - safeX)
    val safeHeight = height.coerceIn(1, source.height - safeY)
    return Bitmap.createBitmap(source, safeX, safeY, safeWidth, safeHeight)
}

// Downscales so the longer side is at most maxDimension, preserving aspect ratio.
// No-op if the bitmap is already smaller than that.
fun resizeBitmap(source: Bitmap, maxDimension: Int): Bitmap {
    val longerSide = maxOf(source.width, source.height)
    if (longerSide <= maxDimension) return source
    val scale = maxDimension.toFloat() / longerSide
    val targetWidth = (source.width * scale).toInt().coerceAtLeast(1)
    val targetHeight = (source.height * scale).toInt().coerceAtLeast(1)
    return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
}

// Writes the bitmap as a JPEG into the app's internal files dir and returns a file:// Uri.
suspend fun saveBitmapAsJpeg(
    context: Context,
    bitmap: Bitmap,
    fileName: String,
    quality: Int = 85
): Uri? = withContext(Dispatchers.IO) {
    try {
        val file = File(context.filesDir, fileName)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        Uri.fromFile(file)
    } catch (_: Exception) {
        null
    }
}