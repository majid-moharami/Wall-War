package com.example.data.nakama

data class NakamaConfig(
    val host: String = "10.0.2.2", // e.g. 10.0.2.2 or MyServerIp or MyServerIp:7351
    val port: Int = 7350,
    val serverKey: String = "defaultkey",
    val useSsl: Boolean = false
) {
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
            val scheme = if (useSsl) "https" else "http"
            return "$scheme://$cleanHost:$effectivePort"
        }

    val wsBaseUrl: String
        get() {
            val scheme = if (useSsl) "wss" else "ws"
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
    val avatarUrl: String? = null
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
