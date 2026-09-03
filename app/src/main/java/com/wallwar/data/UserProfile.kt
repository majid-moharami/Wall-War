package com.wallwar.data

data class UserProfile(
    val isLoggedIn: Boolean = false,
    val displayName: String = "Guest Duelist",
    val email: String? = null,
    val photoUrl: String? = null,
    val trophies: Int = 0,
    val xp: Int = 0,
    val level: Int = 1,
    val rankTitle: String = "Novice Duelist",
    val wins: Int = 0,
    val totalMatches: Int = 0,
    val wallsPlaced: Int = 0,
    val coins: Int = 150,
    val currentWinStreak: Int = 0,
    val longestWinStreak: Int = 0,
    val nakamaUserId: String? = null
) {
    val rankTitleResId: Int
        @androidx.annotation.StringRes get() = when (rankTitle) {
            "Novice Duelist" -> com.wallwar.R.string.rank_novice_duelist
            "Neon Knight" -> com.wallwar.R.string.rank_neon_knight
            "Tactical Adept" -> com.wallwar.R.string.rank_tactical_adept
            "Grid Master" -> com.wallwar.R.string.rank_grid_master
            "Grand Champion" -> com.wallwar.R.string.rank_grand_champion
            "Apex Duelist" -> com.wallwar.R.string.rank_apex_duelist
            else -> com.wallwar.R.string.rank_novice_duelist
        }
}
