package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.model.BoardTheme
import com.example.model.GameState
import com.example.model.Position

@Composable
fun GameBoardComposable(
    gameState: GameState,
    boardTheme: BoardTheme,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    validHighlights: List<Position>,
    onCellClick: (r: Int, c: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val cols = gameState.cols
    val rows = gameState.rows

    val themeGridBg = Color(boardTheme.gridBg)
    val themeCellBg = Color(boardTheme.cellBg)
    val themeWallColor = Color(boardTheme.wallColor)
    val themePrimary = Color(boardTheme.primaryColor)
    val themePawn1 = Color(0xFF7C5CFF)
    val themePawn2 = Color(0xFFFFB800)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(cols.toFloat() / rows.toFloat())
            .padding(8.dp)
            .testTag("game_board_canvas"),
        contentAlignment = Alignment.Center
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()

        // Cell dimensions accounting for wall spacing
        val gapRatio = 0.18f
        val cellW = width / (cols + (cols - 1) * gapRatio)
        val cellH = height / (rows + (rows - 1) * gapRatio)
        val gapW = cellW * gapRatio
        val gapH = cellH * gapRatio

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gameState, isWallMode, isWallHorizontal) {
                    detectTapGestures { offset ->
                        // Convert touch offset to grid cell (r, c)
                        val c = (offset.x / (cellW + gapW)).toInt().coerceIn(0, cols - 1)
                        val r = (offset.y / (cellH + gapH)).toInt().coerceIn(0, rows - 1)
                        onCellClick(r, c)
                    }
                }
        ) {
            // Draw Outer Board Background
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(themeGridBg, themeGridBg.copy(alpha = 0.9f))
                ),
                cornerRadius = CornerRadius(24f, 24f)
            )

            // 1. Draw Grid Cells
            for (r in 0 until rows) {
                for (c in 0 until cols) {
                    val x = c * (cellW + gapW)
                    val y = r * (cellH + gapH)

                    val isHighlight = validHighlights.contains(Position(r, c))
                    val cellColor = if (isHighlight) {
                        themePrimary.copy(alpha = 0.35f)
                    } else {
                        themeCellBg
                    }

                    drawRoundRect(
                        color = cellColor,
                        topLeft = Offset(x, y),
                        size = Size(cellW, cellH),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    if (isHighlight) {
                        drawRoundRect(
                            color = themePrimary,
                            topLeft = Offset(x, y),
                            size = Size(cellW, cellH),
                            cornerRadius = CornerRadius(12f, 12f),
                            style = Stroke(width = 4f)
                        )
                    }
                }
            }

            // 2. Draw Placed Walls
            for (wall in gameState.walls) {
                if (wall.isHorizontal) {
                    // Horizontal Wall spans cols: wall.c and wall.c + 1
                    val x = wall.c * (cellW + gapW)
                    val y = wall.r * (cellH + gapH) + cellH + (gapH * 0.1f)
                    val wallWidth = cellW * 2 + gapW
                    val wallHeight = gapH * 0.8f

                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(themeWallColor, themeWallColor.copy(alpha = 0.8f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                } else {
                    // Vertical Wall spans rows: wall.r and wall.r + 1
                    val x = wall.c * (cellW + gapW) + cellW + (gapW * 0.1f)
                    val y = wall.r * (cellH + gapH)
                    val wallWidth = gapW * 0.8f
                    val wallHeight = cellH * 2 + gapH

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(themeWallColor, themeWallColor.copy(alpha = 0.8f))
                        ),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }

            // 3. Draw Player Pawns
            for (p in gameState.pawns.indices) {
                val pawnPos = gameState.pawns[p]
                val centerX = pawnPos.c * (cellW + gapW) + cellW / 2f
                val centerY = pawnPos.r * (cellH + gapH) + cellH / 2f
                val pawnRadius = minOf(cellW, cellH) * 0.36f

                val pawnColor = if (p == 0) themePawn1 else themePawn2
                val isTurn = gameState.turn == p && gameState.winner == null

                // Outer aura ring for active turn player
                if (isTurn) {
                    drawCircle(
                        color = pawnColor.copy(alpha = 0.3f),
                        radius = pawnRadius * 1.35f,
                        center = Offset(centerX, centerY)
                    )
                }

                // Main Pawn Base
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(pawnColor, pawnColor.copy(alpha = 0.85f)),
                        center = Offset(centerX - pawnRadius * 0.3f, centerY - pawnRadius * 0.3f),
                        radius = pawnRadius * 1.5f
                    ),
                    radius = pawnRadius,
                    center = Offset(centerX, centerY)
                )

                // Glossy inner highlight
                drawCircle(
                    color = Color.White.copy(alpha = 0.45f),
                    radius = pawnRadius * 0.4f,
                    center = Offset(centerX - pawnRadius * 0.3f, centerY - pawnRadius * 0.3f)
                )

                // Crown insignia inside pawn
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = pawnRadius * 0.2f,
                    center = Offset(centerX, centerY)
                )
            }
        }
    }
}
