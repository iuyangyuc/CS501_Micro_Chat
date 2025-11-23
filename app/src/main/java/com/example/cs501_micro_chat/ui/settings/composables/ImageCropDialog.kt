/**
 * ImageCropDialog.kt
 *
 * 图片裁剪对话框 - 允许用户裁剪头像为正方形
 * Image Crop Dialog - Allows users to crop avatar to square
 *
 * 功能 / Features:
 * - 正方形裁剪框 / Square crop frame
 * - 缩放和拖动图片 / Zoom and pan image
 * - 确认和取消按钮 / Confirm and cancel buttons
 *
 * @author CS501 Team
 * @date 2025-11-22
 */
package com.example.cs501_micro_chat.ui.settings.composables

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.cs501_micro_chat.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max
import kotlin.math.min

/**
 * 图片裁剪对话框
 * Image cropping dialog with zoom and pan support
 */
@Composable
fun ImageCropDialog(
    imageUri: Uri,
    onDismiss: () -> Unit,
    onCropComplete: (Uri) -> Unit
) {
    val context = LocalContext.current
    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var cropTrigger by remember { mutableStateOf(false) }

    // Load bitmap from URI
    LaunchedEffect(imageUri) {
        withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(imageUri)
                val loadedBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                bitmap = loadedBitmap
                isLoading = false
            } catch (e: Exception) {
                e.printStackTrace()
                isLoading = false
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Title bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.crop_avatar_title),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.crop_avatar_close),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                HorizontalDivider()

                // Crop area
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        isLoading -> {
                            CircularProgressIndicator()
                        }
                        bitmap != null -> {
                            CropImageView(
                                bitmap = bitmap!!,
                                cropTrigger = cropTrigger,
                                onCropComplete = { croppedBitmap ->
                                    isSaving = true
                                    cropTrigger = false
                                    saveCroppedImage(context, croppedBitmap) { uri ->
                                        isSaving = false
                                        if (uri != null) {
                                            onCropComplete(uri)
                                        }
                                    }
                                }
                            )
                        }
                        else -> {
                            Text(
                                text = stringResource(R.string.crop_avatar_failed),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                // Action buttons
                HorizontalDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isSaving
                    ) {
                        Text(stringResource(R.string.crop_avatar_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            cropTrigger = true
                        },
                        enabled = !isSaving && bitmap != null
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = stringResource(R.string.crop_avatar_button),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.crop_avatar_button))
                    }
                }
            }
        }
    }
}

/**
 * 可缩放和平移的图片裁剪视图
 * Zoomable and pannable image crop view
 */
@Composable
private fun CropImageView(
    bitmap: Bitmap,
    cropTrigger: Boolean,
    onCropComplete: (Bitmap) -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var initialScale by remember { mutableStateOf(1f) }

    val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }

    // Calculate initial scale when canvas size is known
    LaunchedEffect(canvasSize) {
        if (canvasSize != IntSize.Zero && initialScale == 1f) {
            val canvasWidth = canvasSize.width.toFloat()
            val canvasHeight = canvasSize.height.toFloat()
            val cropSize = min(canvasWidth, canvasHeight) * 0.8f

            val imageWidth = bitmap.width.toFloat()
            val imageHeight = bitmap.height.toFloat()

            // Calculate scale to fit the image within crop frame
            val scaleToFitWidth = cropSize / imageWidth
            val scaleToFitHeight = cropSize / imageHeight
            initialScale = max(scaleToFitWidth, scaleToFitHeight)
            scale = initialScale
        }
    }

    // Handle crop trigger
    LaunchedEffect(cropTrigger) {
        if (cropTrigger && canvasSize != IntSize.Zero) {
            withContext(Dispatchers.Default) {
                val croppedBitmap = performCrop(
                    bitmap = bitmap,
                    canvasSize = canvasSize,
                    scale = scale,
                    offsetX = offsetX,
                    offsetY = offsetY,
                    initialScale = initialScale
                )
                withContext(Dispatchers.Main) {
                    onCropComplete(croppedBitmap)
                }
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .onSizeChanged { canvasSize = it }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val oldScale = scale
                    scale = (scale * zoom).coerceIn(initialScale * 0.5f, initialScale * 5f)

                    val canvasWidth = canvasSize.width.toFloat()
                    val canvasHeight = canvasSize.height.toFloat()
                    val cropSize = min(canvasWidth, canvasHeight) * 0.8f

                    // Calculate scaled image dimensions
                    val imageWidth = bitmap.width * scale
                    val imageHeight = bitmap.height * scale

                    // Calculate max offsets (how far we can drag)
                    val maxOffsetX = max(0f, (imageWidth - cropSize) / 2)
                    val maxOffsetY = max(0f, (imageHeight - cropSize) / 2)

                    // Update offsets with pan gesture
                    offsetX = (offsetX + pan.x).coerceIn(-maxOffsetX, maxOffsetX)
                    offsetY = (offsetY + pan.y).coerceIn(-maxOffsetY, maxOffsetY)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate crop frame size (square)
            val cropSize = min(canvasWidth, canvasHeight) * 0.8f
            val cropLeft = (canvasWidth - cropSize) / 2
            val cropTop = (canvasHeight - cropSize) / 2

            // Calculate scaled image dimensions
            val imageWidth = bitmap.width * scale
            val imageHeight = bitmap.height * scale

            // Center the image initially, then apply offsets
            val imageCenterX = canvasWidth / 2
            val imageCenterY = canvasHeight / 2

            val left = imageCenterX - imageWidth / 2 + offsetX
            val top = imageCenterY - imageHeight / 2 + offsetY

            drawImage(
                image = imageBitmap,
                dstOffset = androidx.compose.ui.unit.IntOffset(left.toInt(), top.toInt()),
                dstSize = androidx.compose.ui.unit.IntSize(imageWidth.toInt(), imageHeight.toInt())
            )

            // Draw semi-transparent overlay (outside crop area)
            // Top
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, 0f),
                size = Size(canvasWidth, cropTop)
            )
            // Bottom
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, cropTop + cropSize),
                size = Size(canvasWidth, canvasHeight - cropTop - cropSize)
            )
            // Left
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(0f, cropTop),
                size = Size(cropLeft, cropSize)
            )
            // Right
            drawRect(
                color = Color.Black.copy(alpha = 0.5f),
                topLeft = Offset(cropLeft + cropSize, cropTop),
                size = Size(canvasWidth - cropLeft - cropSize, cropSize)
            )

            // Draw crop frame border
            drawRect(
                color = Color.White,
                topLeft = Offset(cropLeft, cropTop),
                size = Size(cropSize, cropSize),
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw grid lines
            val gridLineColor = Color.White.copy(alpha = 0.5f)
            val third = cropSize / 3

            // Vertical lines
            drawLine(
                color = gridLineColor,
                start = Offset(cropLeft + third, cropTop),
                end = Offset(cropLeft + third, cropTop + cropSize),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridLineColor,
                start = Offset(cropLeft + third * 2, cropTop),
                end = Offset(cropLeft + third * 2, cropTop + cropSize),
                strokeWidth = 1.dp.toPx()
            )

            // Horizontal lines
            drawLine(
                color = gridLineColor,
                start = Offset(cropLeft, cropTop + third),
                end = Offset(cropLeft + cropSize, cropTop + third),
                strokeWidth = 1.dp.toPx()
            )
            drawLine(
                color = gridLineColor,
                start = Offset(cropLeft, cropTop + third * 2),
                end = Offset(cropLeft + cropSize, cropTop + third * 2),
                strokeWidth = 1.dp.toPx()
            )
        }
    }
}

/**
 * 执行实际的图片裁剪
 * Perform actual bitmap cropping
 */
private fun performCrop(
    bitmap: Bitmap,
    canvasSize: IntSize,
    scale: Float,
    offsetX: Float,
    offsetY: Float,
    initialScale: Float
): Bitmap {
    val canvasWidth = canvasSize.width.toFloat()
    val canvasHeight = canvasSize.height.toFloat()

    // Calculate crop frame size (square)
    val cropSize = min(canvasWidth, canvasHeight) * 0.8f
    val cropLeft = (canvasWidth - cropSize) / 2
    val cropTop = (canvasHeight - cropSize) / 2

    // Calculate scaled image dimensions
    val imageWidth = bitmap.width * scale
    val imageHeight = bitmap.height * scale

    // Calculate image position on canvas
    val imageCenterX = canvasWidth / 2
    val imageCenterY = canvasHeight / 2

    val imageLeft = imageCenterX - imageWidth / 2 + offsetX
    val imageTop = imageCenterY - imageHeight / 2 + offsetY

    // Calculate crop region relative to image
    val cropLeftRelativeToImage = cropLeft - imageLeft
    val cropTopRelativeToImage = cropTop - imageTop

    // Convert to original bitmap coordinates
    val scaleRatio = bitmap.width / imageWidth

    val x = (cropLeftRelativeToImage * scaleRatio).coerceIn(0f, bitmap.width.toFloat() - 1)
    val y = (cropTopRelativeToImage * scaleRatio).coerceIn(0f, bitmap.height.toFloat() - 1)
    val size = (cropSize * scaleRatio).coerceIn(1f, min(bitmap.width - x, bitmap.height - y))

    // Create cropped bitmap
    val cropX = x.toInt().coerceIn(0, bitmap.width - 1)
    val cropY = y.toInt().coerceIn(0, bitmap.height - 1)
    val cropWidth = size.toInt().coerceIn(1, bitmap.width - cropX)
    val cropHeight = size.toInt().coerceIn(1, bitmap.height - cropY)

    return Bitmap.createBitmap(bitmap, cropX, cropY, cropWidth, cropHeight)
}

/**
 * 保存裁剪后的图片
 * Save cropped image to cache directory
 */
private fun saveCroppedImage(
    context: Context,
    bitmap: Bitmap,
    onComplete: (Uri?) -> Unit
) {
    try {
        val file = File.createTempFile("cropped_avatar_", ".jpg", context.cacheDir)
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
        }
        val uri = Uri.fromFile(file)
        onComplete(uri)
    } catch (e: Exception) {
        e.printStackTrace()
        onComplete(null)
    }
}

