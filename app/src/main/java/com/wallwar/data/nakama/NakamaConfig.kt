package com.wallwar.data.nakama

data class NakamaConfig(
    val host: String = "https://nakama.wallwargame.com",
    val port: Int = 7349,
    val serverKey: String = "uReirVWP9KAKwsi96zmsB2iDEKCUELzT",
    val useSsl: Boolean = true
) {
    val effectiveSsl: Boolean
        get() {
            val lower = host.trim().lowercase()
            if (lower.startsWith("http://") || lower.startsWith("ws://")) return false
            if (lower.startsWith("https://") || lower.startsWith("wss://")) return true
            return useSsl
        }

    val cleanHost: String
        get() {
            val raw = host
                .replace("http://", "")
                .replace("https://", "")
                .replace("ws://", "")
                .replace("wss://", "")
                .trim()
                .split("/")[0]
            return if (raw.contains(":")) raw.split(":")[0] else raw
        }

    val effectivePort: Int
        get() {
            val raw = host
                .replace("http://", "")
                .replace("https://", "")
                .replace("ws://", "")
                .replace("wss://", "")
                .trim()
                .split("/")[0]
            if (raw.contains(":")) {
                val p = raw.split(":")[1].toIntOrNull()
                if (p != null) return p
            }
            return port
        }

    val httpBaseUrl: String
        get() {
            val scheme = if (effectiveSsl) "https" else "http"
            return "$scheme://$cleanHost:$effectivePort"
        }

    val wsBaseUrl: String
        get() {
            val scheme = if (effectiveSsl) "wss" else "ws"
            return "$scheme://$cleanHost:$effectivePort"
        }
}

data class NakamaFriend(
    val userId: String,
    val username: String,
    val displayName: String,
    val isOnline: Boolean = true,
    val level: Int = 1,
    val trophies: Int = 0,
    val avatarUrl: String? = null,
    val state: Int = 0 // 0 = Mutual Friend, 1 = Invite Sent, 2 = Invite Received
)

data class LeaderboardEntry(
    val rank: Int,
    val userId: String,
    val username: String,
    val displayName: String,
    val trophies: Int,
    val wins: Int,
    val level: Int = 1,
    val avatarUrl: String? = null
)

enum class OnlineMatchState {
    DISCONNECTED,
    CONNECTING,
    IDLE,
    SEARCHING_MATCH,
    IN_MATCH,
    ERROR
}

data class OnlineGameTurn(
    val turnPlayerIndex: Int,
    val timeRemainingSeconds: Int = 30
)
