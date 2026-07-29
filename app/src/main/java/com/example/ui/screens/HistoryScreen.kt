package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.MatchRecord
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonBorder
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonDarkBg
import com.example.ui.theme.NeonDarkCard
import com.example.ui.theme.NeonDarkSurface
import com.example.ui.theme.NeonEmerald
import com.example.ui.theme.NeonMagenta
import com.example.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class ChartType(val title: String) {
    RATING_TREND("Rating Trend"),
    WALLS_VS_MOVES("Walls & Moves")
}

@Composable
fun HistoryScreen(
    matchHistory: List<MatchRecord>,
    totalWins: Int,
    totalMatches: Int,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winRate = if (totalMatches > 0) ((totalWins.toFloat() / totalMatches) * 100).toInt() else 0
    val avgWalls = if (totalMatches > 0) matchHistory.map { it.totalWallsPlaced }.average().toInt() else 0
    val avgMoves = if (totalMatches > 0) matchHistory.map { it.totalMoves }.average().toInt() else 0

    var selectedChartType by remember { mutableStateOf(ChartType.RATING_TREND) }
    var selectedMatchForDetails by remember { mutableStateOf<MatchRecord?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Match History?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text("This will reset all recorded match statistics. This action cannot be undone.", color = Color(0xFFA0ACCC)) },
            containerColor = NeonDarkCard,
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear All", color = NeonMagenta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(NeonDarkBg)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Top Navigation Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(NeonDarkSurface)
                        .border(1.dp, NeonBorder, CircleShape)
                        .testTag("btn_history_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TACTICAL ANALYTICS",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = "Match History & Charts",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                if (matchHistory.isNotEmpty()) {
                    IconButton(
                        onClick = { showClearDialog = true },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(NeonDarkSurface)
                            .border(1.dp, NeonMagenta.copy(alpha = 0.5f), CircleShape)
                            .testTag("btn_clear_history")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteSweep,
                            contentDescription = "Clear History",
                            tint = NeonMagenta
                        )
                    }
                }
            }
        }

        // Summary Stats Bento Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBentoCard(
                    title = "Total Matches",
                    value = "$totalMatches",
                    icon = Icons.Default.SportsEsports,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatBentoCard(
                    title = "Win Rate",
                    value = "$winRate%",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatBentoCard(
                    title = "Avg Walls",
                    value = "$avgWalls / game",
                    icon = Icons.Default.Layers,
                    accentColor = NeonAmber,
                    modifier = Modifier.weight(1f)
                )
                StatBentoCard(
                    title = "Avg Moves",
                    value = "$avgMoves / game",
                    icon = Icons.Default.Speed,
                    accentColor = NeonPurple,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Interactive Game Chart Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonMagenta.copy(alpha = 0.6f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedChartType == ChartType.RATING_TREND) Icons.Default.ShowChart else Icons.Default.BarChart,
                                    contentDescription = null,
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Performance Chart",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Chart Type Selector
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ChartType.values().forEach { chartType ->
                                FilterChip(
                                    selected = selectedChartType == chartType,
                                    onClick = { selectedChartType = chartType },
                                    label = {
                                        Text(
                                            text = chartType.title,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = NeonCyan,
                                        selectedLabelColor = Color.Black,
                                        containerColor = NeonDarkSurface,
                                        labelColor = Color(0xFFA0ACCC)
                                    )
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Canvas Chart Render
                    val displayMatches = if (matchHistory.isNotEmpty()) matchHistory.takeLast(10) else getSampleBenchmarkMatches()

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonDarkSurface)
                            .border(1.dp, NeonBorder, RoundedCornerShape(12.dp))
                    ) {
                        if (selectedChartType == ChartType.RATING_TREND) {
                            NeonRatingTrendChart(
                                matches = displayMatches,
                                onSelectMatch = { match -> selectedMatchForDetails = match }
                            )
                        } else {
                            NeonWallsVsMovesBarChart(
                                matches = displayMatches,
                                onSelectMatch = { match -> selectedMatchForDetails = match }
                            )
                        }
                    }

                    // Selected match popover detail
                    AnimatedVisibility(visible = selectedMatchForDetails != null) {
                        selectedMatchForDetails?.let { match ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NeonDarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, NeonCyan),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = "Selected Match: ${match.modeName} vs ${match.opponentName}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = "${match.totalMoves} moves • ${match.totalWallsPlaced} walls placed",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFFA0ACCC)
                                        )
                                    }

                                    val isWin = match.winnerPlayer == 0
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isWin) NeonEmerald.copy(alpha = 0.2f) else NeonMagenta.copy(alpha = 0.2f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isWin) "VICTORY" else "DEFEAT",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isWin) NeonEmerald else NeonMagenta
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recent Matches List Section Header
        item {
            Text(
                text = "MATCH LOGS",
                style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFA0ACCC),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }

        if (matchHistory.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, NeonBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No recorded matches yet.\nPlay a match in Tactical Arena to see logs!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0ACCC),
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(matchHistory.reversed()) { match ->
                NeonMatchHistoryCard(match)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun StatBentoCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, NeonBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA0ACCC)
                )
            }
        }
    }
}

@Composable
private fun NeonRatingTrendChart(
    matches: List<MatchRecord>,
    onSelectMatch: (MatchRecord) -> Unit
) {
    var tappedIndex by remember { mutableStateOf<Int?>(null) }
    val points = remember(matches) {
        var cumRating = 1200
        matches.map { match ->
            if (match.winnerPlayer == 0) {
                cumRating += 25 + (match.totalWallsPlaced * 2)
            } else {
                cumRating = max(1000, cumRating - 18)
            }
            cumRating
        }
    }

    val minVal = (points.minOrNull() ?: 1000) - 30
    val maxVal = (points.maxOrNull() ?: 1300) + 30

    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1000),
        label = "chart_anim"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Legend Header Bar inside chart container
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Rating Path", fontSize = 10.sp, color = Color(0xFFA0ACCC), fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonEmerald)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Victory", fontSize = 10.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NeonMagenta)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Defeat", fontSize = 10.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                }
            }
            if (tappedIndex != null) {
                Text(
                    text = "Rating: ${points.getOrNull(tappedIndex ?: 0) ?: ""}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = NeonCyan
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(matches) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val paddingLeft = 45.dp.toPx()
                        val paddingRight = 20.dp.toPx()
                        val usableWidth = width - paddingLeft - paddingRight
                        val stepX = if (points.size > 1) usableWidth / (points.size - 1) else usableWidth

                        val closestIdx = points.indices.minByOrNull { idx ->
                            val x = paddingLeft + idx * stepX
                            kotlin.math.abs(x - offset.x)
                        }

                        if (closestIdx != null && closestIdx < matches.size) {
                            tappedIndex = closestIdx
                            onSelectMatch(matches[closestIdx])
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val paddingLeft = 45.dp.toPx()
            val paddingRight = 20.dp.toPx()
            val paddingTop = 15.dp.toPx()
            val paddingBottom = 20.dp.toPx()

            val usableWidth = width - paddingLeft - paddingRight
            val usableHeight = height - paddingTop - paddingBottom

            // Draw horizontal grid lines & Y-Axis labels
            val gridLines = 3
            val range = maxVal - minVal
            for (i in 0..gridLines) {
                val y = paddingTop + (usableHeight / gridLines) * i
                val valAtY = maxVal - (range / gridLines) * i

                drawLine(
                    color = NeonBorder.copy(alpha = 0.35f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            if (points.isEmpty()) return@Canvas

            val stepX = if (points.size > 1) usableWidth / (points.size - 1) else usableWidth

            fun getY(valNum: Int): Float {
                val ratio = (valNum - minVal).toFloat() / range
                return (paddingTop + usableHeight * (1f - ratio * animatedProgress)).coerceIn(paddingTop, height - paddingBottom)
            }

            val pathPoints = points.mapIndexed { index, valNum ->
                Offset(paddingLeft + index * stepX, getY(valNum))
            }

            // Draw Smooth Area Gradient under curve
            if (pathPoints.size >= 2) {
                val fillPath = Path().apply {
                    moveTo(pathPoints.first().x, height - paddingBottom)
                    lineTo(pathPoints.first().x, pathPoints.first().y)

                    for (i in 0 until pathPoints.size - 1) {
                        val p1 = pathPoints[i]
                        val p2 = pathPoints[i + 1]
                        val controlX1 = p1.x + (p2.x - p1.x) / 2
                        val controlY1 = p1.y
                        val controlX2 = p1.x + (p2.x - p1.x) / 2
                        val controlY2 = p2.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                    }

                    lineTo(pathPoints.last().x, height - paddingBottom)
                    close()
                }

                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            NeonCyan.copy(alpha = 0.35f),
                            NeonCyan.copy(alpha = 0.01f)
                        ),
                        startY = paddingTop,
                        endY = height - paddingBottom
                    )
                )

                // Draw Line Path
                val linePath = Path().apply {
                    moveTo(pathPoints.first().x, pathPoints.first().y)
                    for (i in 0 until pathPoints.size - 1) {
                        val p1 = pathPoints[i]
                        val p2 = pathPoints[i + 1]
                        val controlX1 = p1.x + (p2.x - p1.x) / 2
                        val controlY1 = p1.y
                        val controlX2 = p1.x + (p2.x - p1.x) / 2
                        val controlY2 = p2.y
                        cubicTo(controlX1, controlY1, controlX2, controlY2, p2.x, p2.y)
                    }
                }

                drawPath(
                    path = linePath,
                    color = NeonCyan,
                    style = Stroke(width = 3.dp.toPx())
                )
            }

            // Draw data point dots & selection highlights
            pathPoints.forEachIndexed { idx, point ->
                val match = matches.getOrNull(idx)
                val isWin = match?.winnerPlayer == 0
                val dotColor = if (isWin) NeonEmerald else NeonMagenta
                val isSelected = tappedIndex == idx

                if (isSelected) {
                    // Vertical guide line
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.5f),
                        start = Offset(point.x, paddingTop),
                        end = Offset(point.x, height - paddingBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }

                // Outer Glow ring
                drawCircle(
                    color = if (isSelected) NeonCyan else dotColor.copy(alpha = 0.35f),
                    radius = if (isSelected) 10.dp.toPx() else 6.5.dp.toPx(),
                    center = point
                )

                // Inner solid dot
                drawCircle(
                    color = if (isSelected) Color.White else dotColor,
                    radius = if (isSelected) 5.dp.toPx() else 3.5.dp.toPx(),
                    center = point
                )
            }
        }
    }
}

@Composable
private fun NeonWallsVsMovesBarChart(
    matches: List<MatchRecord>,
    onSelectMatch: (MatchRecord) -> Unit
) {
    var tappedIndex by remember { mutableStateOf<Int?>(null) }
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800),
        label = "bar_anim"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        // Legend Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonCyan)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Walls Placed", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp, 10.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(NeonMagenta)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Total Moves", fontSize = 10.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                }
            }

            if (tappedIndex != null && tappedIndex!! < matches.size) {
                val m = matches[tappedIndex!!]
                Text(
                    text = "${m.totalWallsPlaced} Walls / ${m.totalMoves} Moves",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(matches) {
                    detectTapGestures { offset ->
                        val width = size.width
                        val paddingLeft = 35.dp.toPx()
                        val paddingRight = 20.dp.toPx()
                        val usableWidth = width - paddingLeft - paddingRight
                        val groupWidth = usableWidth / matches.size

                        val idx = ((offset.x - paddingLeft) / groupWidth).toInt()
                        if (idx in matches.indices) {
                            tappedIndex = idx
                            onSelectMatch(matches[idx])
                        }
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            val paddingLeft = 35.dp.toPx()
            val paddingRight = 20.dp.toPx()
            val paddingTop = 15.dp.toPx()
            val paddingBottom = 20.dp.toPx()

            val usableWidth = width - paddingLeft - paddingRight
            val usableHeight = height - paddingTop - paddingBottom

            if (matches.isEmpty()) return@Canvas

            val maxVal = (matches.maxOfOrNull { max(it.totalMoves, it.totalWallsPlaced) } ?: 30).coerceAtLeast(15)

            // Grid reference lines
            val gridCount = 3
            for (i in 0..gridCount) {
                val y = paddingTop + (usableHeight / gridCount) * i
                drawLine(
                    color = NeonBorder.copy(alpha = 0.35f),
                    start = Offset(paddingLeft, y),
                    end = Offset(width - paddingRight, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                )
            }

            val groupWidth = usableWidth / matches.size
            val barWidth = (groupWidth * 0.32f).coerceAtMost(16.dp.toPx())

            matches.forEachIndexed { idx, match ->
                val groupStartX = paddingLeft + idx * groupWidth + (groupWidth - barWidth * 2 - 4.dp.toPx()) / 2

                val wallsHeight = (match.totalWallsPlaced.toFloat() / maxVal) * usableHeight * animatedProgress
                val movesHeight = (match.totalMoves.toFloat() / maxVal) * usableHeight * animatedProgress

                val wallsY = height - paddingBottom - wallsHeight
                val movesY = height - paddingBottom - movesHeight

                val isSelected = tappedIndex == idx

                // Walls Bar (Cyan)
                drawRoundRect(
                    color = if (isSelected) NeonCyan else NeonCyan.copy(alpha = 0.85f),
                    topLeft = Offset(groupStartX, wallsY),
                    size = Size(barWidth, wallsHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )

                // Moves Bar (Magenta)
                drawRoundRect(
                    color = if (isSelected) NeonMagenta else NeonMagenta.copy(alpha = 0.85f),
                    topLeft = Offset(groupStartX + barWidth + 4.dp.toPx(), movesY),
                    size = Size(barWidth, movesHeight),
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun NeonMatchHistoryCard(match: MatchRecord) {
    val isUserWin = match.winnerPlayer == 0
    val dateFormat = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(match.timestamp))

    Card(
        colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, if (isUserWin) NeonEmerald.copy(alpha = 0.4f) else NeonMagenta.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(if (isUserWin) NeonEmerald.copy(alpha = 0.15f) else NeonMagenta.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isUserWin) "🏆" else "⚔️",
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${match.modeName} vs ${match.opponentName}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${match.totalMoves} moves • ${match.totalWallsPlaced} walls • ${match.durationSeconds}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0ACCC)
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFA0ACCC).copy(alpha = 0.7f)
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isUserWin) NeonEmerald.copy(alpha = 0.2f) else NeonMagenta.copy(alpha = 0.2f))
                    .border(1.dp, if (isUserWin) NeonEmerald else NeonMagenta, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isUserWin) "VICTORY" else "DEFEAT",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = if (isUserWin) NeonEmerald else NeonMagenta
                )
            }
        }
    }
}

private fun getSampleBenchmarkMatches(): List<MatchRecord> {
    val now = System.currentTimeMillis()
    return listOf(
        MatchRecord(1, "Classic Duel", "AI (Easy)", 0, 14, 6, 45, now - 36000000),
        MatchRecord(2, "Classic Duel", "AI (Normal)", 0, 18, 8, 62, now - 28000000),
        MatchRecord(3, "Wall Race", "Pass & Play", 1, 22, 10, 80, now - 20000000),
        MatchRecord(4, "Classic Duel", "AI (Hard)", 0, 16, 7, 50, now - 14000000),
        MatchRecord(5, "Wall Race", "AI (Hard)", 0, 24, 12, 95, now - 8000000),
        MatchRecord(6, "Classic Duel", "Pass & Play", 0, 19, 9, 68, now - 2000000)
    )
}
