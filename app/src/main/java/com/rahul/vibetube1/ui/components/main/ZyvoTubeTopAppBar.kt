/*
 * ZyvoTube
 * Copyright (C) 2026 ZyvoTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.components.main
import com.rahul.vibetube1.R
import com.rahul.vibetube1.BuildConfig

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.rahul.vibetube1.MainViewModel
import com.rahul.vibetube1.ui.navigation.Destination
import com.rahul.vibetube1.ui.screens.settings.UpdateViewModel
import com.rahul.vibetube1.ui.theme.IncognitoPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZyvoTubeTopAppBar(
    isIncognitoMode: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        windowInsets = WindowInsets(0, 0, 0, 0),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Waves,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = buildAnnotatedString {
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Black)) {
                            append("Zyvo")
                        }
                        withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Black)) {
                            append("Tube")
                        }
                    },
                    style = MaterialTheme.typography.titleLarge.copy(
                        letterSpacing = (-1).sp
                    )
                )

                if (isIncognitoMode) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = IncognitoPurple.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.incognito_label),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = Color.White
                        )
                    }
                }
            }
        },
        actions = {
            if (currentRoute?.contains("Search") == false) {
                IconButton(onClick = { navController.navigate(Destination.Search()) }) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = stringResource(R.string.search),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            val isLibrary = currentRoute?.contains("Library") == true
            if (isLibrary) {
                val updateInfo by updateViewModel.updateInfo.collectAsStateWithLifecycle()
                IconButton(onClick = { navController.navigate(Destination.Settings) }) {
                    BadgedBox(
                        badge = {
                            if (updateInfo.hasUpdate) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(8.dp)
                                )
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            } else {
                IconButton(onClick = { navController.navigate(Destination.Library) }) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = stringResource(R.string.tab_you),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
fun VibeTubeTopAppBar(
    isIncognitoMode: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) = ZyvoTubeTopAppBar(
    isIncognitoMode = isIncognitoMode,
    currentRoute = currentRoute,
    navController = navController,
    mainViewModel = mainViewModel,
    updateViewModel = updateViewModel,
    modifier = modifier
)

@Composable
fun PlayTubeTopAppBar(
    isIncognitoMode: Boolean,
    currentRoute: String?,
    navController: NavHostController,
    mainViewModel: MainViewModel,
    updateViewModel: UpdateViewModel,
    modifier: Modifier = Modifier
) = ZyvoTubeTopAppBar(
    isIncognitoMode = isIncognitoMode,
    currentRoute = currentRoute,
    navController = navController,
    mainViewModel = mainViewModel,
    updateViewModel = updateViewModel,
    modifier = modifier
)

