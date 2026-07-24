package com.example.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    gameRepository: GameRepository
) : ViewModel() {

    val totalWins: StateFlow<Int> = gameRepository.playerWins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMatches: StateFlow<Int> = gameRepository.totalMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}
