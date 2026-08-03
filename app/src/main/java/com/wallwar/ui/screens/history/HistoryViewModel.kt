package com.wallwar.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wallwar.data.AuthRepository
import com.wallwar.data.GameRepository
import com.wallwar.data.MatchRecord
import com.wallwar.data.nakama.NakamaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val repository: GameRepository,
    private val nakamaRepository: NakamaRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _nakamaHistory = MutableStateFlow<List<MatchRecord>>(emptyList())

    val matchHistory: StateFlow<List<MatchRecord>> = combine(
        repository.allMatches,
        _nakamaHistory
    ) { localList, nakamaList ->
        (nakamaList + localList).distinctBy { "${it.modeName}_${it.timestamp}" }.sortedByDescending { it.timestamp }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalWins: StateFlow<Int> = repository.playerWins
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val totalMatches: StateFlow<Int> = repository.totalMatches
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        loadServerHistory()
    }

    fun loadServerHistory() {
        viewModelScope.launch {
            val serverList = nakamaRepository.fetchMatchHistoryFromNakama()
            if (serverList.isNotEmpty()) {
                _nakamaHistory.value = serverList
            }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearAllHistory()
            _nakamaHistory.value = emptyList()
        }
    }
}
