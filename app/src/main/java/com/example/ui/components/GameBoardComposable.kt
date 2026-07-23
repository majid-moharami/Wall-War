package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.engine.GameEngine
import com.example.model.BoardTheme
import com.example.model.GameState
import com.example.model.Position
import com.example.model.Wall
import kotlin.math.roundToInt

@Composable
fun GameBoardComposable(
    gameState: GameState,
    boardTheme: BoardTheme,
    isWallMode: Boolean,
    isWallHorizontal: Boolean,
    validHighlights: List<Position>,
    onCellClick: (r: Int, c: Int) -> Unit,
    onPlaceWall: (r: Int, c: Int, isHorizontal: Boolean) -> Unit,
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

    // Drag-and-drop state for live preview wall placement
    var activeHoverWall by remember { mutableStateOf<Wall?>(null) }
    var isValidHover by remember { mutableStateOf(false) }

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

        val stepX = cellW + gapW
        val stepY = cellH + gapH

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(gameState, isWallMode, isWallHorizontal) {
                    detectTapGestures { offset ->
                        val currentTurn = gameState.turn
                        if (isWallMode) {
                            val targetC = ((offset.x / stepX) - 0.5f).roundToInt().coerceIn(0, cols - 2)
                            val targetR = ((offset.y / stepY) - 0.5f).roundToInt().coerceIn(0, rows - 2)
                            onPlaceWall(targetR, targetC, isWallHorizontal)
                        } else {
                            val c = (offset.x / stepX).toInt().coerceIn(0, cols - 1)
                            val r = (offset.y / stepY).toInt().coerceIn(0, rows - 1)
                            onCellClick(r, c)
                        }
                    }
                }
                .pointerInput(gameState, isWallHorizontal) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val currentTurn = gameState.turn
                            val targetC = ((offset.x / stepX) - 0.5f).roundToInt().coerceIn(0, cols - 2)
                            val targetR = ((offset.y / stepY) - 0.5f).roundToInt().coerceIn(0, rows - 2)
                            val candidate = Wall(targetR, targetC, isWallHorizontal, currentTurn)
                            activeHoverWall = candidate
                            isValidHover = GameEngine.canPlaceWall(gameState, currentTurn, candidate)
                        },
                        onDrag = { change, _ ->
                            val offset = change.position
                            val currentTurn = gameState.turn
                            val targetC = ((offset.x / stepX) - 0.5f).roundToInt().coerceIn(0, cols - 2)
                            val targetR = ((offset.y / stepY) - 0.5f).roundToInt().coerceIn(0, rows - 2)
                            val candidate = Wall(targetR, targetC, isWallHorizontal, currentTurn)
                            activeHoverWall = candidate
                            isValidHover = GameEngine.canPlaceWall(gameState, currentTurn, candidate)
                        },
                        onDragEnd = {
                            val wall = activeHoverWall
                            if (wall != null && isValidHover) {
                                onPlaceWall(wall.r, wall.c, wall.isHorizontal)
                            }
                            activeHoverWall = null
                            isValidHover = false
                        },
                        onDragCancel = {
                            activeHoverWall = null
                            isValidHover = false
                        }
                    )
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
                    val x = c * stepX
                    val y = r * stepY

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

            // 2. Draw Placed Walls with Player-Specific Color Indicators
            for (wall in gameState.walls) {
                val wallOwnerColor = if (wall.playerOwner == 0) themePawn1 else themePawn2
                val darkBorder = if (wall.playerOwner == 0) Color(0xFF381E72) else Color(0xFF5C4000)

                if (wall.isHorizontal) {
                    val x = wall.c * stepX
                    val y = wall.r * stepY + cellH + (gapH * 0.1f)
                    val wallWidth = cellW * 2 + gapW
                    val wallHeight = gapH * 0.8f

                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                wallOwnerColor,
                                wallOwnerColor.copy(alpha = 0.85f),
                                wallOwnerColor
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawRoundRect(
                        color = darkBorder.copy(alpha = 0.6f),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2f)
                    )

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(x + 8f, y + wallHeight * 0.35f),
                        size = Size(wallWidth - 16f, wallHeight * 0.3f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                } else {
                    val x = wall.c * stepX + cellW + (gapW * 0.1f)
                    val y = wall.r * stepY
                    val wallWidth = gapW * 0.8f
                    val wallHeight = cellH * 2 + gapH

                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                wallOwnerColor,
                                wallOwnerColor.copy(alpha = 0.85f),
                                wallOwnerColor
                            )
                        ),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawRoundRect(
                        color = darkBorder.copy(alpha = 0.6f),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 2f)
                    )

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(x + wallWidth * 0.35f, y + 8f),
                        size = Size(wallWidth * 0.3f, wallHeight - 16f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                }
            }

            // 3. Live Drag Hover Preview Wall
            val hover = activeHoverWall
            if (hover != null) {
                val previewColor = if (isValidHover) Color(0xFF81C784) else Color(0xFFFFB4AB)
                val strokeColor = if (isValidHover) Color(0xFF2E7D32) else Color(0xFFB71C1C)

                if (hover.isHorizontal) {
                    val x = hover.c * stepX
                    val y = hover.r * stepY + cellH + (gapH * 0.1f)
                    val wallWidth = cellW * 2 + gapW
                    val wallHeight = gapH * 0.8f

                    drawRoundRect(
                        color = previewColor.copy(alpha = 0.75f),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 4f)
                    )
                } else {
                    val x = hover.c * stepX + cellW + (gapW * 0.1f)
                    val y = hover.r * stepY
                    val wallWidth = gapW * 0.8f
                    val wallHeight = cellH * 2 + gapH

                    drawRoundRect(
                        color = previewColor.copy(alpha = 0.75f),
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    drawRoundRect(
                        color = strokeColor,
                        topLeft = Offset(x, y),
                        size = Size(wallWidth, wallHeight),
                        cornerRadius = CornerRadius(8f, 8f),
                        style = Stroke(width = 4f)
                    )
                }
            }

            // 4. Draw Player Pawns (3D Polished Spheres with Custom Player Symbols)
            for (p in gameState.pawns.indices) {
                val pawnPos = gameState.pawns[p]
                val centerX = pawnPos.c * stepX + cellW / 2f
                val centerY = pawnPos.r * stepY + cellH / 2f
                val pawnRadius = minOf(cellW, cellH) * 0.38f

                val isTurn = gameState.turn == p && gameState.winner == null

                // Drop Shadow
                drawCircle(
                    color = Color.Black.copy(alpha = 0.45f),
                    radius = pawnRadius * 1.05f,
                    center = Offset(centerX + 3f, centerY + 5f)
                )

                // Active Pulse Ring
                if (isTurn) {
                    val auraColor = if (p == 0) themePawn1 else themePawn2
                    drawCircle(
                        color = auraColor.copy(alpha = 0.35f),
                        radius = pawnRadius * 1.45f,
                        center = Offset(centerX, centerY)
                    )
                    drawCircle(
                        color = auraColor,
                        radius = pawnRadius * 1.2f,
                        center = Offset(centerX, centerY),
                        style = Stroke(width = 3f)
                    )
                }

                // 3D Gradient Sphere
                val sphereGradients = if (p == 0) {
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFE8DEF8),
                        Color(0xFFD0BCFF),
                        Color(0xFF7C5CFF),
                        Color(0xFF280068)
                    )
                } else {
                    listOf(
                        Color(0xFFFFFFFF),
                        Color(0xFFFFF9C4),
                        Color(0xFFFFD700),
                        Color(0xFFFF9100),
                        Color(0xFF522800)
                    )
                }

                drawCircle(
                    brush = Brush.radialGradient(
                        colors = sphereGradients,
                        center = Offset(centerX - pawnRadius * 0.35f, centerY - pawnRadius * 0.35f),
                        radius = pawnRadius * 1.6f
                    ),
                    radius = pawnRadius,
                    center = Offset(centerX, centerY)
                )

                // Glossy Highlight Arc
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = pawnRadius * 0.32f,
                    center = Offset(centerX - pawnRadius * 0.32f, centerY - pawnRadius * 0.32f)
                )

                // Distinctive Symbol Emblem
                val symbolColor = if (p == 0) Color(0xFF280068) else Color(0xFF3E1F00)
                if (p == 0) {
                    // Star Emblem for P1
                    val path = Path().apply {
                        val r = pawnRadius * 0.45f
                        val cx = centerX
                        val cy = centerY + r * 0.05f
                        moveTo(cx, cy - r)
                        lineTo(cx + r * 0.3f, cy - r * 0.2f)
                        lineTo(cx + r * 0.95f, cy - r * 0.2f)
                        lineTo(cx + r * 0.4f, cy + r * 0.25f)
                        lineTo(cx + r * 0.6f, cy + r * 0.9f)
                        lineTo(cx, cy + r * 0.5f)
                        lineTo(cx - r * 0.6f, cy + r * 0.9f)
                        lineTo(cx - r * 0.4f, cy + r * 0.25f)
                        lineTo(cx - r * 0.95f, cy - r * 0.2f)
                        lineTo(cx - r * 0.3f, cy - r * 0.2f)
                        close()
                    }
                    drawPath(path = path, color = symbolColor)
                } else {
                    // Lightning Bolt Emblem for P2
                    val path = Path().apply {
                        val s = pawnRadius * 0.55f
                        val cx = centerX
                        val cy = centerY
                        moveTo(cx + s * 0.1f, cy - s)
                        lineTo(cx - s * 0.5f, cy + s * 0.1f)
                        lineTo(cx - s * 0.05f, cy + s * 0.1f)
                        lineTo(cx - s * 0.25f, cy + s)
                        lineTo(cx + s * 0.45f, cy - s * 0.15f)
                        lineTo(cx, cy - s * 0.15f)
                        close()
                    }
                    drawPath(path = path, color = symbolColor)
                }
            }
        }
    }
}

