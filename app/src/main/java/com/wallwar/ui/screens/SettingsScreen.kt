package com.wallwar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.BuildConfig
import com.wallwar.R
import com.wallwar.audio.SoundManager
import com.wallwar.data.UserProfile
import com.wallwar.data.nakama.NakamaConfig
import com.wallwar.model.BoardTheme
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import com.wallwar.ui.theme.NeonPurple

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    userProfile: UserProfile,
    nakamaConfig: NakamaConfig = NakamaConfig(),
    selectedLanguage: String = "en",
    onSetLanguage: (String) -> Unit = {},
    boardTheme: BoardTheme = BoardTheme.ELEGANT_DARK,
    onSetBoardTheme: (BoardTheme) -> Unit = {},
    onUpdateNakamaConfig: (host: String, port: Int, key: String, ssl: Boolean) -> Unit = { _, _, _, _ -> },
    onTestConnection: ((Boolean, String) -> Unit) -> Unit = {},
    onUpdateCoinsAndLevel: (coins: Int, level: Int, onResult: (Boolean, String) -> Unit) -> Unit = { _, _, _ -> },
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var serverHost by remember(nakamaConfig) { mutableStateOf(nakamaConfig.host) }
    var serverPort by remember(nakamaConfig) { mutableStateOf(nakamaConfig.port.toString()) }
    var serverKey by remember(nakamaConfig) { mutableStateOf(nakamaConfig.serverKey) }
    var serverUseSsl by remember(nakamaConfig) { mutableStateOf(nakamaConfig.effectiveSsl) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }
    var testResultIsSuccess by remember { mutableStateOf(false) }
    var isTestingConnection by remember { mutableStateOf(false) }

    // Dev Tools State
    var targetCoinsInput by remember(userProfile.coins) { mutableStateOf(userProfile.coins.toString()) }
    var targetLevelInput by remember(userProfile.level) { mutableStateOf(userProfile.level.toString()) }
    var devStatusMessage by remember { mutableStateOf<String?>(null) }
    var devStatusIsSuccess by remember { mutableStateOf(true) }
    var isApplyingDevBoost by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        // Top Header Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_settings_back")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.btn_back)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Switcher Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_language),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.settings_language_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val isEn = selectedLanguage == "en"
                    val isFa = selectedLanguage == "fa"

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isEn) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                            .border(
                                1.5.dp,
                                if (isEn) NeonCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSetLanguage("en") }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "English",
                            fontWeight = if (isEn) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isEn) NeonCyan else Color(0xFFA0ACCC),
                            fontSize = 14.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isFa) NeonCyan.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface)
                            .border(
                                1.5.dp,
                                if (isFa) NeonCyan else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { onSetLanguage("fa") }
                            .padding(vertical = 12.dp, horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "فارسی",
                            fontWeight = if (isFa) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isFa) NeonCyan else Color(0xFFA0ACCC),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Sound Effects Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_sound_effects),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.settings_sound_effects_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundManager.isSoundEnabled,
                    onCheckedChange = { checked ->
                        soundManager.isSoundEnabled = checked
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Vibration Feedback Toggle
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Vibration,
                    contentDescription = null,
                    tint = NeonPurple,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_vibration),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.settings_vibration_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = soundManager.isVibrationEnabled,
                    onCheckedChange = { checked ->
                        soundManager.isVibrationEnabled = checked
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonPurple)
                )
            }
        }

        // ==========================================
        // DEBUG MODE ONLY SECTIONS
        // ==========================================
        if (BuildConfig.DEBUG) {
            Spacer(modifier = Modifier.height(24.dp))

            // Section Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = NeonAmber,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.settings_debug_controls),
                    style = MaterialTheme.typography.titleSmall,
                    color = NeonAmber,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 1. Dev Coins & Level Modifier Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NeonAmber.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = NeonAmber,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Coins & Level Modifier",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonAmber.copy(alpha = 0.15f))
                                .border(1.dp, NeonAmber, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEV ONLY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonAmber
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Current: Level ${userProfile.level} (${userProfile.rankTitle}) • ${userProfile.coins} Coins",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFA0ACCC)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Input fields for Target Coins and Target Level
                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = targetCoinsInput,
                            onValueChange = { targetCoinsInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Target Coins") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.MonetizationOn,
                                    contentDescription = null,
                                    tint = NeonAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonAmber,
                                focusedLabelColor = NeonAmber
                            ),
                            modifier = Modifier.weight(1.2f)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        OutlinedTextField(
                            value = targetLevelInput,
                            onValueChange = { targetLevelInput = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Level (1-50)") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonCyan,
                                focusedLabelColor = NeonCyan
                            ),
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quick Preset Chips
                    Text(
                        text = "Quick Presets:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8A99AD)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PresetChip(
                            text = "+10K Coins",
                            onClick = {
                                val current = targetCoinsInput.toIntOrNull() ?: userProfile.coins
                                targetCoinsInput = (current + 10_000).toString()
                            },
                            color = NeonAmber,
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            text = "+100K Coins",
                            onClick = {
                                val current = targetCoinsInput.toIntOrNull() ?: userProfile.coins
                                targetCoinsInput = (current + 100_000).toString()
                            },
                            color = NeonAmber,
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            text = "+1M Coins",
                            onClick = {
                                val current = targetCoinsInput.toIntOrNull() ?: userProfile.coins
                                targetCoinsInput = (current + 1_000_000).toString()
                            },
                            color = NeonAmber,
                            modifier = Modifier.weight(1f)
                        )
                        PresetChip(
                            text = "Lvl 30",
                            onClick = {
                                targetLevelInput = "30"
                            },
                            color = NeonCyan,
                            modifier = Modifier.weight(0.8f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Apply Button
                    Button(
                        onClick = {
                            val coins = targetCoinsInput.toIntOrNull() ?: userProfile.coins
                            val level = (targetLevelInput.toIntOrNull() ?: userProfile.level).coerceIn(1, 50)
                            isApplyingDevBoost = true
                            devStatusMessage = null
                            onUpdateCoinsAndLevel(coins, level) { success, msg ->
                                isApplyingDevBoost = false
                                devStatusIsSuccess = success
                                devStatusMessage = msg
                            }
                        },
                        enabled = !isApplyingDevBoost,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isApplyingDevBoost) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Syncing with Server...", color = Color.Black, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.Casino,
                                contentDescription = null,
                                tint = Color.Black
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Update Server & Profile", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }

                    devStatusMessage?.let { status ->
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (devStatusIsSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (devStatusIsSuccess) NeonEmerald else NeonMagenta,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = status,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (devStatusIsSuccess) NeonEmerald else NeonMagenta,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Nakama Server Docker Configuration Card (Debug Mode Only)
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Dns,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Nakama Server Config",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Docker / Custom Nakama IP",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonCyan.copy(alpha = 0.15f))
                                .border(1.dp, NeonCyan, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "DEBUG ONLY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = NeonCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = serverHost,
                        onValueChange = { serverHost = it },
                        label = { Text("Server Host / URL (e.g. https://nakama.wallwargame.com)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = serverPort,
                            onValueChange = { serverPort = it },
                            label = { Text("Port (7350 API)") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        OutlinedTextField(
                            value = serverKey,
                            onValueChange = { serverKey = it },
                            label = { Text("Server Key") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Port helper chips
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Port:", style = MaterialTheme.typography.bodySmall, color = Color.Gray, fontSize = 11.sp)
                        
                        val portOptions = remember {
                            listOf(
                                "7349" to "7349 (gRPC)",
                                "7350" to "7350 (API)",
                                "443" to "443 (HTTPS)",
                                "7351" to "7351 (Console)"
                            )
                        }

                        for ((portVal, portLabel) in portOptions) {
                            val isSelected = serverPort == portVal
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else Color(0xFF1E2638))
                                    .border(1.dp, if (isSelected) NeonCyan else Color.DarkGray, RoundedCornerShape(6.dp))
                                    .clickable {
                                        serverPort = portVal
                                        if (portVal == "443") {
                                            serverUseSsl = true
                                        }
                                    }
                                    .padding(horizontal = 7.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = portLabel,
                                    fontSize = 10.sp,
                                    color = if (isSelected) NeonCyan else Color(0xFFC0D0E0),
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // SSL / TLS Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141A28))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Use SSL / TLS",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = if (serverUseSsl) "SSL Enabled (HTTPS / WSS)" else "SSL Disabled (Plain HTTP / TCP)",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = if (serverUseSsl) NeonCyan else Color.Gray
                            )
                        }
                        Switch(
                            checked = serverUseSsl,
                            onCheckedChange = { serverUseSsl = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = NeonCyan,
                                checkedTrackColor = NeonCyan.copy(alpha = 0.5f)
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                var portInt = serverPort.toIntOrNull() ?: 7350
                                var hostStr = serverHost.trim()

                                if (hostStr.contains(":")) {
                                    val parts = hostStr.split(":")
                                    if (parts.size >= 2 && !hostStr.startsWith("http://") && !hostStr.startsWith("https://")) {
                                        hostStr = parts[0]
                                        parts[1].toIntOrNull()?.let { extractedPort ->
                                            portInt = extractedPort
                                        }
                                    }
                                }

                                val finalSsl = if (hostStr.startsWith("http://") || hostStr.startsWith("ws://")) {
                                    false
                                } else if (hostStr.startsWith("https://") || hostStr.startsWith("wss://")) {
                                    true
                                } else {
                                    serverUseSsl
                                }

                                onUpdateNakamaConfig(hostStr, portInt, serverKey, finalSsl)
                                testResultStatus = "Connecting to $hostStr:$portInt (SSL: $finalSsl)..."
                                testResultIsSuccess = false
                                isTestingConnection = true
                                onTestConnection { success, message ->
                                    isTestingConnection = false
                                    testResultIsSuccess = success
                                    testResultStatus = message
                                }
                            },
                            enabled = !isTestingConnection,
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                        ) {
                            Text(
                                text = if (isTestingConnection) "Testing..." else "Save & Test Nakama",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    testResultStatus?.let { status ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (testResultIsSuccess) NeonEmerald.copy(alpha = 0.12f) else NeonMagenta.copy(alpha = 0.12f))
                                .border(
                                    1.dp,
                                    if (testResultIsSuccess) NeonEmerald.copy(alpha = 0.6f) else NeonMagenta.copy(alpha = 0.6f),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (testResultIsSuccess) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (testResultIsSuccess) NeonEmerald else NeonMagenta,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (testResultIsSuccess) "Connection Successful" else "Connection Diagnostics & Exact Error",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (testResultIsSuccess) NeonEmerald else NeonMagenta
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = status,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFE2E8F0),
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "WallWar Android v1.0 • Nakama Online Engine",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
    }
}

@Composable
private fun PresetChip(
    text: String,
    onClick: () -> Unit,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = color,
            maxLines = 1
        )
    }
}
