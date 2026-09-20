package com.rahul.vibetube1.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/**
 * WhatsNewSheet - ModalBottomSheet alternative presentation for VibeTube's release updates.
 * Features pure white background for light mode, deep dark for dark mode,
 * and a subtle low-opacity red-to-pink gradient effect at the bottom while
 * ensuring the Follow button and all interactive elements remain fully accessible.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsNewSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
    val isDark = isSystemInDarkTheme() || (MaterialTheme.colorScheme.surface.red +
            MaterialTheme.colorScheme.surface.green +
            MaterialTheme.colorScheme.surface.blue) / 3f < 0.5f

    // Clean, pure white background for light mode and deep dark background for dark mode
    val sheetBackground = if (isDark) Color(0xFF0F0F0F) else Color.White

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetBackground,
        contentColor = if (isDark) Color.White else Color(0xFF1E293B),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )
        },
        modifier = modifier.testTag("whats_new_sheet")
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
        ) {
            // Subtle, low-opacity red-to-pink gradient effect at the very bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                if (isDark) Color(0xFFE50914).copy(alpha = 0.07f)
                                else Color(0xFFF9F8A5).copy(alpha = 0.12f)
                            )
                        )
                    )
            )

            // Content container - Follow and Got it buttons remain fully visible & interactive
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .padding(bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                WhatsNewDialogContent(onDismiss = onDismiss)
            }
        }
    }
}
