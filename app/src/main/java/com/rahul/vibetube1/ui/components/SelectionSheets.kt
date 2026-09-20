/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.components
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.filled.Check
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import com.rahul.vibetube1.domain.model.StreamItem
import com.rahul.vibetube1.domain.model.SubtitleItem
import com.rahul.vibetube1.utils.VideoChapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QualitySelectionSheet(
    videoStreams: List<StreamItem>,
    currentQuality: String?,
    preferredQuality: String,
    onDismiss: () -> Unit,
    onQualitySelected: (StreamItem?) -> Unit
) {
    val sortedStreams = remember(videoStreams) {
        videoStreams
            .filter { it.quality.isNotBlank() }
            .distinctBy { it.quality }
            .sortedByDescending { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.quality),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Current video",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item(key = "quality_auto") {
                    val isAutoSelected = preferredQuality.equals("Auto", ignoreCase = true)
                    val autoSubtitle = if (currentQuality != null) "Recommended • $currentQuality" else "Recommended"
                    ModernSelectionItemRow(
                        title = "Auto",
                        subtitle = autoSubtitle,
                        trailingTag = null,
                        isSelected = isAutoSelected,
                        testTag = "quality_option_auto",
                        onClick = {
                            onQualitySelected(null)
                            onDismiss()
                        }
                    )
                }

                items(
                    items = sortedStreams,
                    key = { "quality_${it.quality}_${it.format}" }
                ) { stream ->
                    val isSelected = !preferredQuality.equals("Auto", ignoreCase = true) && preferredQuality == stream.quality
                    val resolutionDesc = getQualityDescription(stream.quality)
                    val formatLabel = stream.format.uppercase().takeIf { it.isNotBlank() }

                    ModernSelectionItemRow(
                        title = stream.quality,
                        subtitle = resolutionDesc,
                        trailingTag = formatLabel,
                        isSelected = isSelected,
                        testTag = "quality_option_${stream.quality}",
                        onClick = {
                            onQualitySelected(stream)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

private fun getQualityDescription(quality: String): String {
    val q = quality.lowercase()
    val num = q.filter { it.isDigit() }.toIntOrNull() ?: 0
    return when {
        num >= 2160 || q.contains("4k") -> "4K Ultra HD"
        num >= 1440 || q.contains("2k") -> "Quad HD"
        num >= 1080 -> "Full HD"
        num >= 720 -> "HD"
        num >= 480 -> "SD"
        num >= 360 -> "Basic"
        num > 0 -> "Data saver"
        else -> "Standard"
    }
}

@Composable
fun ModernSelectionItemRow(
    title: String,
    subtitle: String? = null,
    trailingTag: String? = null,
    isSelected: Boolean,
    testTag: String? = null,
    onClick: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val containerColor = if (isSelected) {
        primaryColor.copy(alpha = 0.12f)
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) primaryColor else MaterialTheme.colorScheme.onSurface
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (!trailingTag.isNullOrBlank()) {
                Text(
                    text = trailingTag,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybackSpeedSelectionSheet(
    currentSpeed: Float,
    onDismiss: () -> Unit,
    onSpeedSelected: (Float) -> Unit
) {
    val speeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.playback_speed),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(speeds) { speed ->
                    val isSelected = speed == currentSpeed
                    val title = if (speed == 1.0f) stringResource(R.string.normal_speed) else "${speed}x"
                    ModernSelectionItemRow(
                        title = title,
                        isSelected = isSelected,
                        testTag = "speed_option_$speed",
                        onClick = {
                            onSpeedSelected(speed)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PitchSelectionSheet(
    currentPitch: Float,
    onDismiss: () -> Unit,
    onPitchSelected: (Float) -> Unit
) {
    val pitches = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f)
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = "Audio Pitch",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(pitches) { pitch ->
                    val isSelected = pitch == currentPitch
                    val title = if (pitch == 1.0f) "Normal" else "${pitch}x"
                    ModernSelectionItemRow(
                        title = title,
                        isSelected = isSelected,
                        testTag = "pitch_option_$pitch",
                        onClick = {
                            onPitchSelected(pitch)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadSelectionSheet(
    videoStreams: List<StreamItem>,
    audioStreams: List<StreamItem>,
    onDismiss: () -> Unit,
    onDownload: (StreamItem, Boolean, Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("vibetube_prefs", android.content.Context.MODE_PRIVATE) }
    
    var saveToDevice by remember { mutableStateOf(sharedPreferences.getBoolean("download_save_to_device", false)) }
    var selectedTab by remember { mutableStateOf(if (!saveToDevice) 0 else (if (videoStreams.isNotEmpty()) 0 else 1)) }
    
    val processedVideoStreams = remember(videoStreams) {
        videoStreams
            .filter { it.quality.isNotEmpty() && it.quality.first().isDigit() }
            .sortedByDescending { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
    }
    
    val processedAudioStreams = remember(audioStreams) {
        if (audioStreams.isNotEmpty()) {
            audioStreams
                .distinctBy { it.format }
                .sortedByDescending { it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 0 }
        } else {
            listOf(
                StreamItem(
                    url = "",
                    quality = "128kbps",
                    format = "m4a",
                    isAdaptive = true
                )
            )
        }
    }
    
    var selectedVideoStream by remember { mutableStateOf<StreamItem?>(processedVideoStreams.firstOrNull()) }
    var selectedAudioStream by remember { mutableStateOf<StreamItem?>(processedAudioStreams.firstOrNull()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp, start = 16.dp, end = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Download",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Choose format & quality",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Save to",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = saveToDevice,
                    onClick = { 
                        saveToDevice = true 
                        sharedPreferences.edit().putBoolean("download_save_to_device", true).apply()
                    },
                    label = { Text("Device") },
                    leadingIcon = { Icon(Icons.Default.PhoneAndroid, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !saveToDevice,
                    onClick = { 
                        saveToDevice = false 
                        selectedTab = 0
                        sharedPreferences.edit().putBoolean("download_save_to_device", false).apply()
                    },
                    label = { Text("ZyvoTube") },
                    leadingIcon = { Icon(Icons.Default.Save, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("VIDEO") },
                    leadingIcon = { Icon(Icons.Default.VideoFile, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("AUDIO") },
                    leadingIcon = { Icon(Icons.Default.AudioFile, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedTab == 0) {
                    items(processedVideoStreams) { stream ->
                        val isSelected = selectedVideoStream == stream
                        val resLabel = getResolutionLabel(stream.quality)
                        val sizeLabel = if (stream.size > 0) formatFileSize(stream.size) else "Size unavailable"
                        
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { selectedVideoStream = stream },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "${stream.quality} • $resLabel",
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = stream.format.uppercase(),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = sizeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                } else {
                    items(processedAudioStreams) { stream ->
                        val isSelected = selectedAudioStream == stream
                        val sizeLabel = if (stream.size > 0) formatFileSize(stream.size) else "Size unavailable"
                        val qualityLabel = stream.quality
                        
                        Surface(
                            shape = MaterialTheme.shapes.medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                            onClick = { selectedAudioStream = stream },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = stream.format.uppercase(),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = qualityLabel,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                                Text(
                                    text = sizeLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            val activeStream = if (selectedTab == 0) selectedVideoStream else selectedAudioStream
            Button(
                onClick = {
                    activeStream?.let {
                        onDownload(it, selectedTab == 1, saveToDevice)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = activeStream != null,
                colors = ButtonDefaults.buttonColors(containerColor = com.rahul.vibetube1.ui.theme.VibeTubeRed)
            ) {
                val labelText = if (activeStream != null) {
                    val quality = activeStream.quality
                    val label = if (selectedTab == 0) getResolutionLabel(quality) else activeStream.format.uppercase()
                    "Download • $quality".trim() + if (label.isNotBlank()) " $label" else ""
                } else {
                    "Download"
                }
                Text(text = labelText, color = androidx.compose.ui.graphics.Color.White)
            }
        }
    }
}

fun getResolutionLabel(quality: String): String {
    val res = quality.filter { it.isDigit() }.toIntOrNull() ?: 0
    return when {
        res >= 4320 -> "8K"
        res >= 2160 -> "4K"
        res >= 1440 -> "2K"
        res >= 1080 -> "Full HD"
        res >= 720 -> "HD"
        else -> ""
    }
}

fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "Size unavailable"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    if (digitGroups > 4) digitGroups = 4
    return java.text.DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubtitleSelectionSheet(
    subtitles: List<SubtitleItem>,
    currentLanguage: String?,
    isCcEnabled: Boolean,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.subtitles),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                item(key = "subtitle_off") {
                    ModernSelectionItemRow(
                        title = stringResource(R.string.off),
                        isSelected = !isCcEnabled,
                        testTag = "subtitle_option_off",
                        onClick = {
                            onLanguageSelected(null)
                            onDismiss()
                        }
                    )
                }
                
                items(
                    items = subtitles,
                    key = { "subtitle_${it.languageTag}_${it.isAutoGenerated}" }
                ) { subtitle ->
                    val locale = java.util.Locale.forLanguageTag(subtitle.languageTag)
                    val languageName = locale.displayLanguage.replaceFirstChar { it.uppercase() }
                    val title = if (subtitle.isAutoGenerated) {
                        stringResource(R.string.language_auto_generated, languageName, stringResource(R.string.auto_generated))
                    } else {
                        languageName
                    }
                    val isSelected = isCcEnabled && subtitle.languageTag == currentLanguage

                    ModernSelectionItemRow(
                        title = title,
                        subtitle = subtitle.languageTag,
                        isSelected = isSelected,
                        testTag = "subtitle_option_${subtitle.languageTag}",
                        onClick = {
                            onLanguageSelected(subtitle.languageTag)
                            onDismiss()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    playlists: List<com.rahul.vibetube1.data.local.LocalPlaylistEntity>,
    playlistsWithVideo: Set<Int> = emptySet(),
    onDismiss: () -> Unit,
    onPlaylistSelected: (com.rahul.vibetube1.data.local.LocalPlaylistEntity) -> Unit,
    onCreateNewPlaylist: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Add to Playlist",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            ListItem(
                headlineContent = { Text("Create New Playlist") },
                leadingContent = { Icon(Icons.Default.Add, null) },
                modifier = Modifier.clickable { onCreateNewPlaylist() }
            )
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            LazyColumn {
                items(playlists) { playlist ->
                    val isAdded = playlistsWithVideo.contains(playlist.id)
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = playlist.name,
                                fontWeight = if (isAdded) FontWeight.Bold else FontWeight.Normal,
                                color = if (isAdded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            ) 
                        },
                        leadingContent = { 
                            Icon(
                                imageVector = if (isAdded) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.PlaylistPlay, 
                                contentDescription = null,
                                tint = if (isAdded) MaterialTheme.colorScheme.primary else LocalContentColor.current
                            ) 
                        },
                        trailingContent = {
                            if (isAdded) {
                                Text(
                                    text = "Added",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier.clickable(enabled = !isAdded) { onPlaylistSelected(playlist) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDownloadSelectionSheet(
    onDismiss: () -> Unit,
    onDownload: (String) -> Unit
) {
    val options = listOf("4K", "1080p", "720p", "480p", "360p")
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.playlist_download_quality),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                items(options) { quality ->
                    ListItem(
                        headlineContent = { Text(text = quality) },
                        leadingContent = { 
                            Icon(
                                imageVector = if (quality == "1080p" || quality == "4K") Icons.Default.HighQuality else Icons.Default.VideoFile,
                                contentDescription = null,
                                tint = if (quality == "1080p" || quality == "4K") MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { onDownload(quality) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterSelectionSheet(
    chapters: List<VideoChapter>,
    currentPositionMs: Long,
    onDismiss: () -> Unit,
    onChapterSelected: (VideoChapter) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = "Video Chapters",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn {
                items(chapters) { chapter ->
                    val isCurrent = currentPositionMs >= chapter.startMs
                    ListItem(
                        headlineContent = {
                            Text(
                                text = chapter.title,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        supportingContent = {
                            Text(
                                text = chapter.formattedTimestamp(),
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
                                contentDescription = null,
                                tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        modifier = Modifier.clickable { onChapterSelected(chapter) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSelectionSheet(
    availableLocales: List<java.util.Locale>,
    currentLanguageTag: String?,
    onDismiss: () -> Unit,
    onLanguageSelected: (String?) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(
                text = stringResource(R.string.app_language),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(text = "System Default") },
                        leadingContent = { 
                            RadioButton(
                                selected = currentLanguageTag == null || currentLanguageTag == "",
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onLanguageSelected(null) }
                    )
                }
                
                items(availableLocales) { locale ->
                    val tag = locale.toLanguageTag()
                    ListItem(
                        headlineContent = { 
                            Text(text = locale.getDisplayLanguage(locale).replaceFirstChar { it.uppercase() }) 
                        },
                        supportingContent = { 
                            Text(text = locale.getDisplayLanguage(java.util.Locale.ENGLISH))
                        },
                        leadingContent = { 
                            RadioButton(
                                selected = currentLanguageTag == tag,
                                onClick = null 
                            ) 
                        },
                        modifier = Modifier.clickable { onLanguageSelected(tag) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun StreamListItem(stream: StreamItem, onDownload: (StreamItem) -> Unit) {
    ListItem(
        headlineContent = { Text(text = stream.quality) },
        supportingContent = { Text(text = stream.format) },
        trailingContent = {
            if (stream.quality.contains("1080") || stream.quality.contains("4K")) {
                Icon(
                    imageVector = Icons.Default.HighQuality,
                    contentDescription = "High Quality",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        modifier = Modifier.clickable { onDownload(stream) }
    )
}
