/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.player

import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.changedToDownIgnoreConsumed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun VideoPlayerGestureDetector(
    onDoubleTapLeft: () -> Unit,
    onDoubleTapRight: () -> Unit,
    onSingleTap: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeUp: () -> Unit,
    onDragStart: () -> Unit = {},
    onDragEnd: () -> Unit = {},
    onDragCancel: () -> Unit = {},
    onVerticalSwipeLeft: (Float) -> Unit = {},
    onVerticalSwipeRight: (Float) -> Unit = {},
    onLongPressStart: () -> Unit = {},
    onLongPressEnd: () -> Unit = {},
    areVolumeBrightnessGesturesEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val doubleTapTimeout = 300L
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                coroutineScope {
                    var tapCount = 0
                    var tapJob: kotlinx.coroutines.Job? = null
                    var longPressJob: Job? = null
                    var isLongPressActive = false
                    var startPosition = androidx.compose.ui.geometry.Offset.Zero

                    awaitPointerEventScope {
                        val touchSlop = viewConfiguration.touchSlop
                        
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            val down = event.changes.find { it.changedToDownIgnoreConsumed() }

                            if (down != null) {
                                startPosition = down.position
                                tapCount++
                                tapJob?.cancel()
                                longPressJob?.cancel()
                                isLongPressActive = false

                                val isLeftSide = (down.position.x < size.width / 2)

                                if (tapCount >= 2) {
                                    down.consume()
                                    if (isLeftSide) onDoubleTapLeft() else onDoubleTapRight()
                                    tapCount = 0
                                } else {
                                    longPressJob = launch {
                                        delay(450L)
                                        isLongPressActive = true
                                        onLongPressStart()
                                    }

                                    tapJob = launch {
                                        delay(doubleTapTimeout)
                                        if (tapCount == 1 && !isLongPressActive) {
                                            onSingleTap()
                                        }
                                        tapCount = 0
                                    }
                                }
                            }

                            val firstChanged = event.changes.firstOrNull()
                            if (firstChanged != null && firstChanged.pressed) {
                                val distance = (firstChanged.position - startPosition).getDistance()
                                if (distance > touchSlop) {
                                    tapJob?.cancel()
                                    longPressJob?.cancel()
                                    tapCount = 0
                                }
                            }

                            val up = event.changes.find { !it.pressed }
                            if (up != null) {
                                longPressJob?.cancel()
                                if (isLongPressActive) {
                                    isLongPressActive = false
                                    onLongPressEnd()
                                }
                            }
                        }
                    }
                }
            }
            .pointerInput(areVolumeBrightnessGesturesEnabled) {
                var totalDrag = 0f
                var dragStartX = 0f
                detectVerticalDragGestures(
                    onDragStart = { offset ->
                        totalDrag = 0f
                        dragStartX = offset.x
                        if (areVolumeBrightnessGesturesEnabled) {
                            onDragStart()
                        }
                    },
                    onVerticalDrag = { change, dragAmount ->
                        change.consume()
                        totalDrag += dragAmount
                        
                        if (areVolumeBrightnessGesturesEnabled) {
                            val screenHeight = size.height
                            val screenWidth = size.width
                            val dragPercentage = -dragAmount / screenHeight
                            val sideMargin = screenWidth * 0.30f // 30% from each side
                            
                            if (change.position.x < sideMargin) {
                                onVerticalSwipeLeft(dragPercentage)
                            } else if (change.position.x > screenWidth - sideMargin) {
                                onVerticalSwipeRight(dragPercentage)
                            }
                        }
                    },
                    onDragEnd = {
                        val screenWidth = size.width
                        val sideMargin = screenWidth * 0.30f
                        
                        // Only trigger minimize/maximize if the drag started in the center "dead zone"
                        // to avoid conflicts with volume/brightness adjustments on the sides.
                        val startedInCenter = dragStartX in sideMargin..(screenWidth - sideMargin)

                        if (startedInCenter) {
                            if (totalDrag > 150) {
                                onSwipeDown()
                            } else if (totalDrag < -150) {
                                onSwipeUp()
                            }
                        }
                        onDragEnd()
                    },
                    onDragCancel = {
                        onDragCancel()
                    }
                )
            }
    ) {
        content()
    }
}
