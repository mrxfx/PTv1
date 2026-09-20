/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Headphones
import androidx.compose.material.icons.outlined.HighQuality
import androidx.compose.material.icons.outlined.Star
import com.rahul.vibetube1.BuildConfig
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.rahul.vibetube1.ui.theme.VibeTubeRed

private const val INSTAGRAM_URL = "https://www.instagram.com/mr.haldar__"

@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = modifier
                .fillMaxWidth(0.90f)
                .widthIn(max = 430.dp)
                .padding(vertical = 16.dp)
                .testTag("whats_new_dialog"),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            tonalElevation = 6.dp,
            shadowElevation = 18.dp
        ) {
            WhatsNewDialogContent(onDismiss = onDismiss)
        }
    }
}

@Composable
internal fun WhatsNewDialogContent(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxWidth()) {
        // Subtle top decorative organic fluid curves
        TopBackgroundWaves(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
        )

        // Close Button top-right with accessible touch target (~40dp)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f))
                    .testTag("whats_new_close_button")
                    .semantics { contentDescription = "Close What's New" }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close What's New",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        // Entire dialog content (No scrolling, exact reference rhythm and hierarchy)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 14.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. ZyvoTube Logo (~50dp)
            ZyvoTubeLogoBadge(modifier = Modifier.size(50.dp))

            Spacer(modifier = Modifier.height(4.dp))

            // 2. ZyvoTube Wordmark & Tagline
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Zyvo",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = VibeTubeRed,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Tube",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    letterSpacing = (-0.5).sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "Watch Videos Your Way.",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Title: What's New in v${AppVersion.name}
            Text(
                text = buildAnnotatedString {
                    append("What's New in ")
                    withStyle(SpanStyle(color = VibeTubeRed, fontWeight = FontWeight.ExtraBold)) {
                        append("v${BuildConfig.VERSION_NAME}")
                    }
                },
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(5.dp))

            Text(
                text = "A fresher, faster and better video experience.",
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(5.dp))

            // Decorative red accent indicator (pill + dot)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 18.dp, height = 3.5.dp)
                        .clip(RoundedCornerShape(1.75.dp))
                        .background(VibeTubeRed)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(3.5.dp)
                        .clip(CircleShape)
                        .background(VibeTubeRed)
                )
            }

            Spacer(modifier = Modifier.height(9.dp))

            // 4. Update Badge Pill
            Surface(
                shape = RoundedCornerShape(50),
                color = if (MaterialTheme.colorScheme.surface.isDark()) Color(0xFF33161A) else Color(0xFFFFEEF0),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "✨",
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "New Update • More Possibilities",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (MaterialTheme.colorScheme.surface.isDark()) Color(0xFFFF9EAA) else Color(0xFFBE123C)
                    )
                }
            }

            Spacer(modifier = Modifier.height(13.dp))

            // 5. Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                Text(
                    text = "Premium Features",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(7.dp))

            // 6. Five Professional Premium Feature Rows
            HighlightRow(
                icon = Icons.Outlined.Block,
                title = "Ad-Free",
                description = "Enjoy videos without ads."
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            HighlightRow(
                icon = Icons.Outlined.HighQuality,
                title = "4K Quality",
                description = "Up to 4K playback."
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            HighlightRow(
                icon = Icons.Outlined.FileDownload,
                title = "Offline Downloads",
                description = "Save videos for offline viewing."
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            HighlightRow(
                icon = Icons.Outlined.Headphones,
                title = "Background Playback",
                description = "Keep listening outside the app."
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                thickness = 0.5.dp
            )

            HighlightRow(
                icon = Icons.Outlined.Star,
                title = "Exclusive Perks",
                description = "More premium ZyvoTube benefits."
            )

            Spacer(modifier = Modifier.height(11.dp))

            // 7. Developer Attribution
            Text(
                text = "Developed by ❤️",
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Rahul Haldar",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 8. Support Divider
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.7.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
                Text(
                    text = "  THANK YOU FOR YOUR SUPPORT  ",
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    thickness = 0.7.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)
                )
            }

            Spacer(modifier = Modifier.height(11.dp))

            // 9. Primary Button: [ Got it → ]
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("whats_new_got_it_button")
                    .semantics { contentDescription = "Dismiss What's New" },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = VibeTubeRed,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Got it",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(17.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 10. Secondary Button: Follow on Instagram
            Surface(
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL)).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("whats_new_follow_button")
                    .semantics { contentDescription = "Follow on Instagram" }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    InstagramGlyph(modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Follow on Instagram",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(5.dp))

            // 11. Footer note
            Text(
                text = "Stay connected for updates and behind the scenes!",
                fontSize = 9.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun HighlightRow(
    icon: ImageVector,
    title: String,
    description: String,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.surface.isDark()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Simple monochrome or subtle red-accent vector icon container
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isDark) Color(0xFFE50914).copy(alpha = 0.12f)
                    else Color(0xFFFFEBEE)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = VibeTubeRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Title and Description with clean typography
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp,
                maxLines = 2
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Small subtle chevron (18dp)
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.40f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// -------------------------------------------------------------
// Custom Graphic Components (Logo, Background, Illustrated Icons)
// -------------------------------------------------------------

@Composable
private fun TopBackgroundWaves(modifier: Modifier = Modifier) {
    val isDark = MaterialTheme.colorScheme.surface.isDark()
    
    Box(
        modifier = modifier.background(
            brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                colors = listOf(
                    if (isDark) VibeTubeRed.copy(alpha = 0.18f) else VibeTubeRed.copy(alpha = 0.12f),
                    if (isDark) VibeTubeRed.copy(alpha = 0.05f) else VibeTubeRed.copy(alpha = 0.03f),
                    Color.Transparent
                )
            )
        )
    ) {
        val waveColor = if (isDark) VibeTubeRed.copy(alpha = 0.08f) else Color(0xFFFF8A9E).copy(alpha = 0.18f)
        val waveColor2 = if (isDark) VibeTubeRed.copy(alpha = 0.04f) else Color(0xFFFFB4C0).copy(alpha = 0.14f)

        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Top Left Wave
            val path1 = Path().apply {
                moveTo(0f, 0f)
                lineTo(w * 0.50f, 0f)
                cubicTo(
                    w * 0.45f, h * 0.35f,
                    w * 0.18f, h * 0.75f,
                    0f, h * 0.60f
                )
                close()
            }
            drawPath(path1, waveColor)

            // Top Right Wave
            val path2 = Path().apply {
                moveTo(w, 0f)
                lineTo(w * 0.50f, 0f)
                cubicTo(
                    w * 0.55f, h * 0.40f,
                    w * 0.82f, h * 0.85f,
                    w, h * 0.70f
                )
                close()
            }
            drawPath(path2, waveColor2)
        }
    }
}

@Composable
private fun ZyvoTubeLogoBadge(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Red rounded rectangle
        drawRoundRect(
            color = VibeTubeRed,
            size = Size(w, h),
            cornerRadius = CornerRadius(w * 0.28f, h * 0.28f)
        )

        // Draw 3 horizontal white waves matching ZyvoTube brand
        val waveStroke = w * 0.07f
        val startX = w * 0.22f
        val endX = w * 0.78f
        val waveWidth = endX - startX

        val waveYPositions = listOf(h * 0.36f, h * 0.50f, h * 0.64f)

        for (y in waveYPositions) {
            val wavePath = Path().apply {
                moveTo(startX, y)
                cubicTo(
                    startX + waveWidth * 0.12f, y - h * 0.06f,
                    startX + waveWidth * 0.22f, y + h * 0.06f,
                    startX + waveWidth * 0.34f, y
                )
                cubicTo(
                    startX + waveWidth * 0.46f, y - h * 0.06f,
                    startX + waveWidth * 0.56f, y + h * 0.06f,
                    startX + waveWidth * 0.68f, y
                )
                cubicTo(
                    startX + waveWidth * 0.80f, y - h * 0.06f,
                    startX + waveWidth * 0.90f, y + h * 0.06f,
                    endX, y
                )
            }
            drawPath(
                path = wavePath,
                color = Color.White,
                style = Stroke(width = waveStroke, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun VibeTubeLogoBadge(modifier: Modifier = Modifier) = ZyvoTubeLogoBadge(modifier)


@Composable
private fun RocketIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft circular background
        drawCircle(
            color = Color(0xFFFFEEEE),
            radius = w * 0.48f,
            center = center
        )

        // Flame
        val flamePath = Path().apply {
            moveTo(w * 0.38f, h * 0.68f)
            cubicTo(w * 0.32f, h * 0.85f, w * 0.42f, h * 0.88f, w * 0.44f, h * 0.74f)
            close()
        }
        drawPath(flamePath, Color(0xFFFF9800))

        // Rocket body
        val rocketBody = Path().apply {
            moveTo(w * 0.72f, h * 0.22f)
            cubicTo(w * 0.75f, h * 0.35f, w * 0.62f, h * 0.58f, w * 0.46f, h * 0.68f)
            lineTo(w * 0.38f, h * 0.60f)
            cubicTo(w * 0.48f, h * 0.44f, w * 0.65f, h * 0.25f, w * 0.72f, h * 0.22f)
            close()
        }
        drawPath(rocketBody, Color(0xFFF3F4F6))

        // Nose cone (Red)
        val noseCone = Path().apply {
            moveTo(w * 0.72f, h * 0.22f)
            cubicTo(w * 0.74f, h * 0.28f, w * 0.68f, h * 0.34f, w * 0.62f, h * 0.36f)
            lineTo(w * 0.58f, h * 0.32f)
            cubicTo(w * 0.64f, h * 0.26f, w * 0.70f, h * 0.22f, w * 0.72f, h * 0.22f)
            close()
        }
        drawPath(noseCone, VibeTubeRed)

        // Fins (Red)
        val fin1 = Path().apply {
            moveTo(w * 0.42f, h * 0.64f)
            lineTo(w * 0.30f, h * 0.68f)
            lineTo(w * 0.36f, h * 0.56f)
            close()
        }
        drawPath(fin1, VibeTubeRed)

        // Window (Cyan)
        drawCircle(
            color = Color(0xFF0284C7),
            radius = w * 0.06f,
            center = Offset(w * 0.56f, h * 0.42f)
        )
    }
}

@Composable
private fun ShortsIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft background
        drawCircle(
            color = Color(0xFFF3E8FF),
            radius = w * 0.48f,
            center = center
        )

        // Clapperboard base (Dark Slate)
        drawRoundRect(
            color = Color(0xFF1E293B),
            topLeft = Offset(w * 0.24f, h * 0.38f),
            size = Size(w * 0.52f, h * 0.38f),
            cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
        )

        // Top clapper stripe (Red & White stripes)
        val topRect = Path().apply {
            moveTo(w * 0.24f, h * 0.36f)
            lineTo(w * 0.76f, h * 0.30f)
            lineTo(w * 0.76f, h * 0.38f)
            lineTo(w * 0.24f, h * 0.44f)
            close()
        }
        drawPath(topRect, Color(0xFFDC2626))

        // Center play / film icon inside clapper
        val playPath = Path().apply {
            moveTo(w * 0.45f, h * 0.50f)
            lineTo(w * 0.57f, h * 0.57f)
            lineTo(w * 0.45f, h * 0.64f)
            close()
        }
        drawPath(playPath, Color.White)
    }
}

@Composable
private fun SearchIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Soft background
        drawCircle(
            color = Color(0xFFEFF6FF),
            radius = w * 0.48f,
            center = center
        )

        // Magnifier lens circle
        val lensCenter = Offset(w * 0.44f, h * 0.44f)
        val lensRadius = w * 0.20f

        drawCircle(
            color = Color(0xFF60A5FA).copy(alpha = 0.5f),
            radius = lensRadius,
            center = lensCenter
        )

        drawCircle(
            color = Color(0xFF2563EB),
            radius = lensRadius,
            center = lensCenter,
            style = Stroke(width = w * 0.07f)
        )

        drawLine(
            color = Color(0xFF475569),
            start = Offset(lensCenter.x + lensRadius * 0.7f, lensCenter.y + lensRadius * 0.7f),
            end = Offset(w * 0.76f, h * 0.76f),
            strokeWidth = w * 0.09f,
            cap = StrokeCap.Round
        )
    }
}

@Composable
private fun BackgroundPlayIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = Color(0xFFFFEEEE),
            radius = w * 0.48f,
            center = center
        )

        val playPath = Path().apply {
            moveTo(w * 0.38f, h * 0.32f)
            lineTo(w * 0.70f, h * 0.50f)
            lineTo(w * 0.38f, h * 0.68f)
            close()
        }
        drawPath(playPath, VibeTubeRed)
    }
}

@Composable
private fun DownloadIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        drawCircle(
            color = Color(0xFFF0FDF4),
            radius = w * 0.48f,
            center = center
        )

        drawRoundRect(
            color = Color(0xFF64748B),
            topLeft = Offset(w * 0.26f, h * 0.62f),
            size = Size(w * 0.48f, h * 0.16f),
            cornerRadius = CornerRadius(w * 0.04f, h * 0.04f)
        )

        val arrowHead = Path().apply {
            moveTo(w * 0.36f, h * 0.46f)
            lineTo(w * 0.50f, h * 0.58f)
            lineTo(w * 0.64f, h * 0.46f)
            lineTo(w * 0.55f, h * 0.46f)
            lineTo(w * 0.55f, h * 0.30f)
            lineTo(w * 0.45f, h * 0.30f)
            lineTo(w * 0.45f, h * 0.46f)
            close()
        }
        drawPath(arrowHead, Color(0xFF22C55E))
    }
}

@Composable
private fun InstagramGlyph(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val igColor = Color(0xFFE1306C)
        val strokeW = w * 0.10f

        drawRoundRect(
            color = igColor,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = Size(w * 0.84f, h * 0.84f),
            cornerRadius = CornerRadius(w * 0.26f, h * 0.26f),
            style = Stroke(width = strokeW)
        )

        drawCircle(
            color = igColor,
            radius = w * 0.22f,
            center = center,
            style = Stroke(width = strokeW)
        )

        drawCircle(
            color = igColor,
            radius = w * 0.06f,
            center = Offset(w * 0.70f, h * 0.30f)
        )
    }
}

private fun Color.isDark(): Boolean {
    val luminance = 0.299f * red + 0.587f * green + 0.114f * blue
    return luminance < 0.5f
}
