package com.example.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameRepository
import com.example.data.MatchRecord
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: GameRepository
) : ViewModel() {

    val matchHistory: StateFlow<List<MatchRecord>> = repository.allMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWins: StateFlow<Int> = repository.playerWins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMatches: StateFlow<Int> = repository.totalMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
        }
    }
}
