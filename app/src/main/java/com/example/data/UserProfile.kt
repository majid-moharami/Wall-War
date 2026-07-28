package com.example.data

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val displayName: String = "Guest Duelist",
    val email: String = "guest@wallwar.app",
    val photoUrl: String? = null,
    val trophies: Int = 1250,
    val xp: Int = 3450,
    val level: Int = 7,
    val rankTitle: String = "Neon Knight",
    val wins: Int = 0,
    val totalMatches: Int = 0,
    val wallsPlaced: Int = 0
)
