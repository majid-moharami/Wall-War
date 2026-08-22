package com.wallwar.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.audio.SoundManager
import com.wallwar.data.nakama.NakamaConfig
import com.wallwar.model.BoardTheme
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonPurple

import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta

@Composable
fun SettingsScreen(
    soundManager: SoundManager,
    selectedTheme: BoardTheme,
    nakamaConfig: NakamaConfig = NakamaConfig(),
    onSelectTheme: (BoardTheme) -> Unit,
    onUpdateNakamaConfig: (host: String, port: Int, key: String, ssl: Boolean) -> Unit = { _, _, _, _ -> },
    onTestConnection: ((Boolean) -> Unit) -> Unit = {},
    onRestoreFromNakamaServer: ((Boolean, String) -> Unit) -> Unit = { _ -> },
    onExportDataBackup: ((String) -> Unit) -> Unit = { _ -> },
    onRestoreDataFromBackup: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onRestoreDefaultSettings: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current

    var serverHost by remember(nakamaConfig) { mutableStateOf(nakamaConfig.host) }
    var serverPort by remember(nakamaConfig) { mutableStateOf(nakamaConfig.port.toString()) }
    var serverKey by remember(nakamaConfig) { mutableStateOf(nakamaConfig.serverKey) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }

    var restoreStatusMessage by remember { mutableStateOf<String?>(null) }
    var restoreStatusIsSuccess by remember { mutableStateOf(true) }

    var showExportDialog by remember { mutableStateOf(false) }
    var exportedJsonText by remember { mutableStateOf("") }

    var showImportDialog by remember { mutableStateOf(false) }
    var importJsonText by remember { mutableStateOf("") }

    var showResetDialog by remember { mutableStateOf(false) }

    // Export Dialog
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Export Data Backup", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Copy this backup JSON code to save your user profile, settings, and match logs:", color = Color(0xFFA0ACCC))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = exportedJsonText,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF374151),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(exportedJsonText))
                        restoreStatusIsSuccess = true
                        restoreStatusMessage = "Backup JSON copied to clipboard!"
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                ) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Copy JSON", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("Close", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E2638)
        )
    }

    // Import / Restore Dialog
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = { Text("Restore Data from Backup", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Paste your JSON backup code below to restore profile, settings, and match history:", color = Color(0xFFA0ACCC))
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = importJsonText,
                        onValueChange = { importJsonText = it },
                        placeholder = { Text("Paste JSON backup code here...", color = Color(0xFF6B7280)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = Color(0xFF374151),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJsonText.isNotBlank()) {
                            onRestoreDataFromBackup(importJsonText) { success, msg ->
                                restoreStatusIsSuccess = success
                                restoreStatusMessage = msg
                                if (success) showImportDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonEmerald)
                ) {
                    Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restore Now", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E2638)
        )
    }

    // Reset Defaults Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Restore Default Settings?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset board theme, audio preferences, and server configuration back to factory default values.", color = Color(0xFFA0ACCC)) },
            confirmButton = {
                Button(
                    onClick = {
                        onRestoreDefaultSettings()
                        soundManager.isSoundEnabled = true
                        soundManager.isVibrationEnabled = true
                        serverHost = "10.0.2.2"
                        serverPort = "7350"
                        serverKey = "defaultkey"
                        restoreStatusIsSuccess = true
                        restoreStatusMessage = "Settings restored to factory defaults!"
                        showResetDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonMagenta)
                ) {
                    Text("Reset Defaults", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = Color(0xFF1E2638)
        )
    }

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
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Settings & Nakama Server",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Nakama Server Docker Configuration
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
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
                            text = "Nakama Server Config (Docker / Personal IP)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Connect to your custom server running Nakama Docker",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Port Explanation Note
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2638))
                        .border(1.dp, NeonCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Text(
                        text = "💡 Note: Port 7350 is Nakama's Client API & WebSocket port (required for the app). Port 7351 is only for the Web Console Admin dashboard in your browser.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFC0D0E0)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = serverHost,
                    onValueChange = { serverHost = it },
                    label = { Text("Server Host IP (e.g. 192.168.1.100 or MyServerIp)") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonCyan),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = serverPort,
                        onValueChange = { serverPort = it },
                        label = { Text("Port (Use 7350 for API)") },
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

                            // If user typed MyServerIp:7351 or 7351, auto-correct 7351 (console port) to 7350 (API port)
                            if (hostStr.contains(":")) {
                                val parts = hostStr.split(":")
                                hostStr = parts[0]
                                parts[1].toIntOrNull()?.let { extractedPort ->
                                    portInt = if (extractedPort == 7351) 7350 else extractedPort
                                }
                            }
                            if (portInt == 7351) {
                                portInt = 7350
                                serverPort = "7350"
                            }

                            onUpdateNakamaConfig(hostStr, portInt, serverKey, false)
                            testResultStatus = "Connecting..."
                            onTestConnection { success ->
                                testResultStatus = if (success) "Connected to Nakama Server!" else "Connection failed (Check Docker / IP)"
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Text("Save & Test Nakama", color = Color.Black, fontWeight = FontWeight.Bold)
                    }

                    testResultStatus?.let { status ->
                        Text(
                            text = status,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (status.contains("Connected")) Color(0xFF4CAF50) else Color(0xFFFF5252),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

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
                        text = "Sound Effects",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Move ticks & wall placement audio",
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
                        text = "Haptic Vibration",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vibrate on moves & wall placements",
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

        Spacer(modifier = Modifier.height(20.dp))

        // Board Theme Customizer Header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = null,
                tint = NeonPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Board Themes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        BoardTheme.values().forEach { theme ->
            Card(
                onClick = { onSelectTheme(theme) },
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedTheme == theme) NeonPurple.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(theme.primaryColor))
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = theme.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (selectedTheme == theme) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = NeonPurple
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Data Backup & Restore Section
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Data Backup & Restore",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Cloud sync, JSON backup export & restore",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Restore from Nakama Server Button
                Button(
                    onClick = {
                        restoreStatusMessage = "Restoring from Nakama Cloud..."
                        onRestoreFromNakamaServer { success, msg ->
                            restoreStatusIsSuccess = success
                            restoreStatusMessage = msg
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Default.CloudDownload, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Stats from Server", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Export JSON Backup
                    OutlinedButton(
                        onClick = {
                            onExportDataBackup { jsonStr ->
                                exportedJsonText = jsonStr
                                showExportDialog = true
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NeonCyan)
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Backup", color = NeonCyan, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    // Import & Restore JSON Backup
                    OutlinedButton(
                        onClick = {
                            importJsonText = ""
                            showImportDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, NeonEmerald)
                    ) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, tint = NeonEmerald, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Restore Backup", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Restore Factory Defaults Button
                OutlinedButton(
                    onClick = { showResetDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    border = BorderStroke(1.dp, NeonMagenta)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = NeonMagenta, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Default Settings", color = NeonMagenta, fontWeight = FontWeight.Bold)
                }

                restoreStatusMessage?.let { status ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = status,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (restoreStatusIsSuccess) NeonEmerald else NeonMagenta,
                        fontWeight = FontWeight.Bold
                    )
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
