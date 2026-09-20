/*
 * VibeTube
 * Copyright (C) 2026 VibeTube contributors
 *
 * Licensed under GPL-3.0-or-later
 */
package com.rahul.vibetube1.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rahul.vibetube1.data.local.PreferencesManager
import com.rahul.vibetube1.domain.usecase.UpdateUserInterestsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val updateUserInterestsUseCase: UpdateUserInterestsUseCase
) : ViewModel() {

    private val _onboardingEvents = MutableSharedFlow<OnboardingEvent>()
    val onboardingEvents: SharedFlow<OnboardingEvent> = _onboardingEvents.asSharedFlow()

    fun saveInterests(selectedInterests: List<String>) {
        viewModelScope.launch {
            selectedInterests.forEach { interest ->
                updateUserInterestsUseCase(interest, 10.0f) // High initial weight for onboarding
            }
            preferencesManager.setOnboardingCompleted(true)
            _onboardingEvents.emit(OnboardingEvent.NavigateToHome)
        }
    }

    fun skipOnboarding() {
        viewModelScope.launch {
            preferencesManager.setOnboardingCompleted(true)
            _onboardingEvents.emit(OnboardingEvent.NavigateToHome)
        }
    }

    sealed interface OnboardingEvent {
        object NavigateToHome : OnboardingEvent
    }
}
