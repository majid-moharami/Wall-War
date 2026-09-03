package com.wallwar.ui.screens

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wallwar.R
import com.wallwar.data.MatchRecord
import com.wallwar.data.UserProfile
import com.wallwar.ui.theme.NeonAmber
import com.wallwar.ui.theme.NeonBorder
import com.wallwar.ui.theme.NeonCyan
import com.wallwar.ui.theme.NeonDarkBg
import com.wallwar.ui.theme.NeonDarkCard
import com.wallwar.ui.theme.NeonDarkSurface
import com.wallwar.ui.theme.NeonEmerald
import com.wallwar.ui.theme.NeonMagenta
import com.wallwar.ui.theme.NeonPurple
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

enum class ChartType(val title: String) {
    RATING_TREND("Rating Trend"),
    WALLS_VS_MOVES("Walls & Moves"),
    WIN_RATE_MOMENTUM("Win Rate %")
}

@Composable
fun HistoryScreen(
    userProfile: UserProfile,
    matches: List<MatchRecord>,
    totalWins: Int,
    totalMatches: Int,
    onClearHistory: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val winRate = if (totalMatches > 0) ((totalWins.toFloat() / totalMatches) * 100).toInt() else 0

    var selectedMatchForDetails by remember { mutableStateOf<MatchRecord?>(null) }
    var showClearDialog by remember { mutableStateOf(false) }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.history_clear_title), color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.history_clear_desc), color = Color(0xFFA0ACCC)) },
            containerColor = NeonDarkCard,
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearHistory()
                        selectedMatchForDetails = null
                        showClearDialog = false
                    }
                ) {
                    Text(stringResource(R.string.history_clear_confirm), color = NeonMagenta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.btn_cancel), color = Color.White)
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
                        contentDescription = stringResource(R.string.btn_back),
                        tint = NeonCyan
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.history_analytics_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonCyan,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )
                    Text(
                        text = stringResource(R.string.history_online_history_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
                if (matches.isNotEmpty()) {
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
                            contentDescription = stringResource(R.string.history_clear_confirm),
                            tint = NeonMagenta
                        )
                    }
                }
            }
        }

        // Summary Stats (Total Matches & Win Rate)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatBentoCard(
                    title = stringResource(R.string.history_total_matches),
                    value = "$totalMatches",
                    icon = Icons.Default.SportsEsports,
                    accentColor = NeonCyan,
                    modifier = Modifier.weight(1f)
                )
                StatBentoCard(
                    title = stringResource(R.string.history_win_rate),
                    value = "$winRate%",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = NeonEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Rating Trend Chart Section
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = NeonDarkCard),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.5.dp, Brush.horizontalGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonMagenta.copy(alpha = 0.6f)))),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(NeonCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ShowChart,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.history_rating_trend),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Canvas Chart Render
                    if (matches.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonDarkSurface)
                                .border(1.dp, NeonBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = stringResource(R.string.history_empty_analytics),
                                color = Color(0xFFA0ACCC),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    } else {
                        val displayMatches = matches.reversed().takeLast(12)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonDarkSurface)
                                .border(1.dp, NeonBorder, RoundedCornerShape(12.dp))
                        ) {
                            NeonRatingTrendChart(
                                matches = displayMatches,
                                baseRating = 1200 + userProfile.trophies,
                                onSelectMatch = { match -> selectedMatchForDetails = match }
                            )
                        }
                    }

                    // Selected match popover detail
                    AnimatedVisibility(visible = selectedMatchForDetails != null) {
                        selectedMatchForDetails?.let { match ->
                            val isWin = match.winnerPlayer == 0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = NeonDarkSurface),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isWin) NeonEmerald.copy(alpha = 0.6f) else NeonMagenta.copy(alpha = 0.6f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp)
                                ) {
                                    // Header: Arena & Opponent with clean truncation on left, Badge and Close button on right
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(end = 8.dp)
                                        ) {
                                            Text(
                                                text = match.modeName,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = stringResource(R.string.history_vs_opponent, match.opponentName),
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = NeonCyan,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(if (isWin) NeonEmerald.copy(alpha = 0.2f) else NeonMagenta.copy(alpha = 0.2f))
                                                    .border(1.dp, if (isWin) NeonEmerald else NeonMagenta, RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(
                                                    text = if (isWin) stringResource(R.string.history_victory_gain) else stringResource(R.string.history_defeat_loss),
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = if (isWin) NeonEmerald else NeonMagenta
                                                )
                                            }

                                            IconButton(
                                                onClick = { selectedMatchForDetails = null },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = stringResource(R.string.btn_close),
                                                    tint = Color(0xFFA0ACCC),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Match Stats Row
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF0F1420))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = if (match.durationSeconds > 0) stringResource(R.string.history_match_duration, match.durationSeconds) else stringResource(R.string.history_online_arena_match),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFA0ACCC)
                                        )
                                        val dateFormat = SimpleDateFormat("MMM dd • HH:mm", Locale.getDefault())
                                        Text(
                                            text = dateFormat.format(Date(match.timestamp)),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFA0ACCC)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Online Match Logs Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = null,
                        tint = Color(0xFFA0ACCC),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.history_logs_header),
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFFA0ACCC),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }

                Text(
                    text = stringResource(R.string.history_records_count, matches.size),
                    style = MaterialTheme.typography.labelSmall,
                    color = NeonCyan,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (matches.isEmpty()) {
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
                            text = stringResource(R.string.history_empty_log),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFA0ACCC),
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(matches) { match ->
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
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
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
    baseRating: Int,
    onSelectMatch: (MatchRecord) -> Unit
) {
    var tappedIndex by remember { mutableStateOf<Int?>(null) }
    val points = remember(matches) {
        var cumRating = max(1000, baseRating - (matches.size * 5))
        matches.map { match ->
            if (match.winnerPlayer == 0) {
                cumRating += 25
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonCyan))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.history_rating_path), fontSize = 10.sp, color = Color(0xFFA0ACCC), fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonEmerald))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.history_win_stat), fontSize = 10.sp, color = NeonEmerald, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(NeonMagenta))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(stringResource(R.string.history_loss_stat), fontSize = 10.sp, color = NeonMagenta, fontWeight = FontWeight.Bold)
                }
            }
            if (tappedIndex != null) {
                Text(
                    text = stringResource(R.string.history_rating_selected, points.getOrNull(tappedIndex ?: 0)?.toString() ?: ""),
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

            val gridLines = 3
            val range = maxVal - minVal
            for (i in 0..gridLines) {
                val y = paddingTop + (usableHeight / gridLines) * i
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

            pathPoints.forEachIndexed { idx, point ->
                val match = matches.getOrNull(idx)
                val isWin = match?.winnerPlayer == 0
                val dotColor = if (isWin) NeonEmerald else NeonMagenta
                val isSelected = tappedIndex == idx

                if (isSelected) {
                    drawLine(
                        color = NeonCyan.copy(alpha = 0.5f),
                        start = Offset(point.x, paddingTop),
                        end = Offset(point.x, height - paddingBottom),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f))
                    )
                }

                drawCircle(
                    color = if (isSelected) NeonCyan else dotColor.copy(alpha = 0.35f),
                    radius = if (isSelected) 10.dp.toPx() else 6.5.dp.toPx(),
                    center = point
                )

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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.history_match_vs, match.modeName, match.opponentName),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (match.durationSeconds > 0) stringResource(R.string.history_match_duration, match.durationSeconds) else stringResource(R.string.history_online_duel),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFA0ACCC),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
                    text = if (isUserWin) stringResource(R.string.history_victory) else stringResource(R.string.history_defeat),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    color = if (isUserWin) NeonEmerald else NeonMagenta
                )
            }
        }
    }
}
