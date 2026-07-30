package com.example.data

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val displayName: String = "Guest Duelist",
    val email: String = "guest@wallwar.app",
    val photoUrl: String? = null,
    val trophies: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val rankTitle: String = "Novice Duelist",
    val wins: Int = 0,
    val totalMatches: Int = 0,
    val wallsPlaced: Int = 0,
    val coins: Int = 150,
    val nakamaUserId: String? = null
)
